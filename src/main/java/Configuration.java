import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class Configuration{
    private  String ParamsFile;
    private  boolean setStorageMode;
    private  String sshusername;
    private  String sshpassword;
    private  String workload;
    private  boolean installCluster;
    private  String clusterVersion;
    private  String bucketpassword;
    private  String ramp;
    private  String rebound;
    private  String testsuite;
    private  String test;
    private  boolean upgrade;
    private  String upgradeVersion;
    private  String downloadurl;
    private  String buildtype;
    private  String textexecuteuser;
    private  String textexecutepassword;



    private LinkedList<Host> hosts = new LinkedList<Host>();


    Configuration(String ParamsFile){
        this.ParamsFile = ParamsFile;
    }

    public class Host {
        String ip;
        String[] hostservices;
        public  Host(String ipAddress, String services)
        {
            ip=ipAddress;
            hostservices = services.split(",");
        }

        public void printConfig(){
            System.out.println("Host with IP: "+ip+" has services: " );
            printservices();
        }
        private void printservices(){
            for(int i =0;i<hostservices.length;i++){
                System.out.println(hostservices[i]);
            }
        }
    }

    public void readandStoreParams() throws IOException, ParseException {
        Object obj = new JSONParser().parse(new FileReader(ParamsFile));
        JSONObject jo = (JSONObject) obj;
        setStorageMode = (boolean)jo.get("setStorageMode");
        sshusername = (String) jo.get("ssh-username");
        sshpassword = (String) jo.get("ssh-password");
        workload = (String) jo.get("workload");
        installCluster = (boolean) jo.get("installCluster");
        clusterVersion = (String) jo.get("clusterVersion");
        bucketpassword = (String) jo.get("bucket-password");
        ramp = (String) jo.get("ramp");
        rebound = (String) jo.get("rebound");
        testsuite = (String) jo.get("testsuite");
        test = (String) jo.get("test");
        upgrade = (boolean) jo.get("upgrade");
        upgradeVersion = (String) jo.get("upgradeVersion");
        downloadurl = (String) jo.get("downloadurl");
        buildtype = (String) jo.get("buildtype");
        textexecuteuser = (String) jo.get("textexecuteuser");
        textexecutepassword = (String) jo.get("textexecutepassword");

        extractnodeConfig(jo);
    }

    private void extractnodeConfig(JSONObject jo) {
        Map nodeArray = (Map) jo.get("cluster");
        Iterator<Map.Entry> itr1 = nodeArray.entrySet().iterator();
        while (itr1.hasNext()) {
            Map.Entry pair = itr1.next();
            JSONObject node = (JSONObject) nodeArray.get(pair.getKey());
            String ip = (String) node.get("ip");
            String services = (String) node.get("services");
            hosts.add(new Host(ip,services));
        }
    }

    public void printConfiguration(){
        System.out.println("ParamsFile: "+ParamsFile);
        System.out.println("setStorageMode: "+setStorageMode);
        System.out.println("sshusername: "+sshusername);
        System.out.println("sshpassword: "+sshpassword);
       for(int i =0;i<hosts.size();i++){
           hosts.get(i).printConfig();
       }
    }

    public boolean getsetStorageMode(){
        return setStorageMode;
    }

    public String getsshusername(){
        return sshusername;
    }

    public String getsshpassword(){
        return sshpassword;
    }

    public boolean getinstallCluster(){
        return installCluster;
    }

    public String getworkload(){
        return workload;
    }

    public LinkedList<Host> getClusterNodes(){
        return hosts;
    }
    public String getclusterVersion(){
        return clusterVersion;
    }

    public String getramp(){
        return ramp;
    }

    public String getrebound(){
        return rebound;
    }

    public String gettestsuite(){
        return testsuite;
    }

    public String gettest(){
        return test;
    }

    public String getbucketpassword(){
        return bucketpassword;
    }

    public boolean getupgrade(){
        return upgrade;
    }
    public String getdownloadurl(){
        return downloadurl;
    }

    public String getUpgradeVersion(){
        return upgradeVersion;
    }

    public String getbuildtype(){
        return buildtype;
    }

    public String gettextexecuteuser(){
        return textexecuteuser;
    }

    public String gettextexecutepassword(){
        return textexecutepassword;
    }




}
