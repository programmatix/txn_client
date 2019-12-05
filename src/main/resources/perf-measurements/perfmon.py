import os
import socket
import json
import threading
import psutil
import subprocess
import shlex

class Perfmon(object):
    def __init__(self, port=9100):
        self.port = port
        self.sock = None
        self.collector = None

    def _decode_list(self, data):
        rv = []
        for item in data:
            if isinstance(item, unicode):
                item = item.encode('utf-8')
            elif isinstance(item, list):
                item = self._decode_list(item)
            elif isinstance(item, dict):
                item = self._decode_dict(item)
            rv.append(item)
        return rv

    def _decode_dict(self,data):
        rv = {}
        for key, value in data.iteritems():
            if isinstance(key, unicode):
                key = key.encode('utf-8')
            if isinstance(value, unicode):
                value = value.encode('utf-8')
            elif isinstance(value, list):
                value = self._decode_list(value)
            elif isinstance(value, dict):
                value = self._decode_dict(value)
            rv[key] = value
        return rv

    def handleConnection(self, conn):
        data = conn.recv(1024)
	print "data",data
        request = json.loads(data, object_hook=self._decode_dict)
        if request['command'] == 'START MONITOR':
            self.startMonitor(request)
            res = 'OK'
        elif request['command'] == 'STOP MONITOR':
            res = self.stopMonitor()
            print res
        elif request['command'] == 'STOP':
             self.stop()
        conn.sendall(res)
        conn.close()

    def startMonitor(self, request):
        self.collector = Collector(request=request)
        self.collector.start()

    def stopMonitor(self):
        self.collector.stopCollecting()
        res = self.collector.getResponse()
        del self.collector
        return res

    def stop(self):
        self.sock.close()
        os._exit(0)

    def start(self):
        self.sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        try:
            self.sock.bind(('',self.port))
        except socket.error as msg:
            print 'Bind failed. Error code:' + str(msg[0])
        self.sock.listen(10)
        while 1:
            conn, addr = self.sock.accept()
            self.handleConnection(conn)



class Collector(threading.Thread):
    def __init__(self, request=None, cancelled=False):
        threading.Thread.__init__(self, group=None, target=None, name=None, verbose=None)
        self.cancelled = cancelled
        self.request = request
        self.mem_usage = ["mem_usage"] #vmem size
        self.cpu_usage = ["cpu_usage"] #cpu percent utilization
        self.rss_usage = ["rss"] #rss
        self.cpu_idle = ["cpu_idle"]
        self.cpu_user = ["cpu_user"]
        self.cpu_system = ["cpu_system"]
        self.net_usage = ["net_usage"]
        self._done = False
	self.cleanup()

    def cleanup(self):
	command = "rm -rf iptraf.log parsed_output; rm -rf /var/run/iptraf/*; rm -rf /var/log/iptraf/*"
        p = subprocess.Popen(command, shell=True)
        p.wait()

    def run(self):
        self.startIpTraf()
        self.startCollecting()

    def startCollecting(self):
            while not self.cancelled:
                try:
                    for p in psutil.process_iter():
                        if p.name() == 'mc:listener' or p.name() == 'memcached':
                            vms =  p.as_dict(attrs=['memory_info']).get('memory_info').vms
                            rss =  p.as_dict(attrs=['memory_info']).get('memory_info').rss
                            cpu_percent = self.cpu_usage.append(p.as_dict(attrs=['cpu_percent']).get('cpu_percent'))
			    
                            self.mem_usage.append(vms)
                            self.cpu_usage.append(cpu_percent)
                            self.rss_usage.append(rss)
			    break
                    cpu_times = psutil.cpu_times_percent(self.request['iterwait'])
                    self.cpu_idle.append(cpu_times.idle)
                    self.cpu_user.append(cpu_times.user)
                    self.cpu_system.append(cpu_times.system)

                except Exception as e:
                    print e.message
		time.sleep(self.request['thinktime'] if self.request['thinktime'] == None else 1)
            self._done = True

    def startIpTraf(self):
        command = "sudo iptraf -i eth0 -L $PWD/iptraf.log -B /dev/null"
        p = subprocess.Popen(command, shell=True)
        p.wait()

    def stopCollecting(self):
        self.cancelled = True

        #stop iptraf using sigusr2
        for p in psutil.process_iter():
                if p.name() == "iptraf":
                    pid = p.pid
                    command = "kill -SIGUSR2 "+ str(pid)
                    args = shlex.split(command)
                    p = subprocess.Popen(args)
                    p.wait()


    def getResponse(self):
        #parse iptraf log - maybe refactor
        command = "cat iptraf.log | grep TCP " 
	command += "| grep from\ "+ self.request['clientAddr'] + " | grep to\ "
	command += self.request['serverAddr'] +":11210 | grep packets | "
        command += "awk 'BEGIN{FS = \";\"}{if ($0 ~ /packets/) {if ($6 ~ /FIN sent/) {print $7}}}' > parsed_output"
        p = subprocess.Popen(command, shell=True, stdout=subprocess.PIPE)
        p.wait()
        with open('parsed_output', 'rb') as out:
            line = out.readline()
            self.net_usage.append(line[:-1])

        response = dict()
        response["mem_usage"] = self.mem_usage
        response["cpu_usage"] = self.cpu_usage
        response["net_usage"] = self.net_usage
        response["cpu_idle"] = self.cpu_idle
        response["cpu_system"] = self.cpu_system
        response["cpu_user"] = self.cpu_user
        response["rss_usage"] = self.rss_usage
        return json.dumps(response)


perfmon = Perfmon()
perfmon.start()
