from paramiko import SSHClient, SSHException
import logging
import logging.config
from installer.constants import *

class SDKDInstaller(object):
    def __init__(self, args, **kwargs):
        logging.config.fileConfig('logging.conf')
        self.logger = logging.getLogger('SDKInstaller')
        self.remote = kwargs['remote']
        self.remote_connection = SSHClient()
        self.remote_channel = None
        self.remote_path = kwargs['remote_path']

    def start_remote_connection(self):
        self.remote_connection.load_host_keys()
        try:
            self.remote_connection.connect(self, self.remote)
            self.remote_channel = self.remote_connection.get_transport().open_session()
        except Exception as e:
            raise e

    def execute_command(self, command):
        self.remote_channel.exec_command(command)
        error = None

        if self.remote_channel.recv_stderr_ready() == True:
            error = self.remote_channel.recv_stderr(1024)

        if self.remote_channel.recv_exit_status() != 0:
            self.logger.error("Failed to execute command",command, error)
            exit(1)

    def install(self):
        pass

    def install_sdkd(self):
        pass

    def install_harness(self):
        pass

class NETInstaller(SDKDInstaller):
    def __init__(self, args, **kwargs):
        super.__init__(self, args, kwargs)
        self.sdk_commit = kwargs['sdk_commit']
        self.sdkd_commit = kwargs['sdkd_commit']
        self.gerrit_checkout = kwargs['gerrit_checkout']
        self.sdk_repo = NETSDK_REPO
        if kwargs['sdk_repo'] != None:
            self.sdk_repo = kwargs['sdk_repo']

    def install_sdk(self):
        self.logger.info("Cloning SDKD Repo")
        self.execute_command('cd {0}'.format(self.remote_path))
        self.execute_command('rd /s /q couchbase-net-client')
        self.execute_command('git clone {0}'.format(self.sdk_repo))
        self.execute_command('cd couchbase-net-client')
        if self.gerrit_checkout != None:
            self.execute_command('{0}'.format(self.gerrit_checkout))

        self.execute_command('git checkout {0}'.format(self.sdk_commit))
        self.execute_command('cd src')
        self.execute_command('nuget restore')


    def install_sdkd(self):
        self.execute_command('cd {0}'.format(self.remote_path))
        self.execute_command('rd /s /q sdkd-net')
        self.execute_command('git clone {0}'.format(NETSDKD_REPO))
        self.execute_command('cd sdkd-net')
        self.execute_command('git checkout {0}'.format(self.sdkd_commit))
        self.execute_command('cp src/SdkdConsole/App_logs.config.back App.config')
        self.logger.info("Nuget restore")
        self.execute_command('cd src')
        self.execute_command('nuget restore')
        self.logger.info("Building sdkd solution")

        self.execute_command('{0} /ds  /maxcpucount:4 Sdkd.sln'.format(MSBUILD_EXE))


class CInstaller(SDKDInstaller):
    def __init__(self, args, **kwargs):
        super.__init__(self, args, kwargs)

    def install_sdk(self):
        pass

    def install_sdkd(self):
        pass


class JavaInstaller(SDKDInstaller):
    def __init__(self, args, **kwargs):
        super.__init__(self, args, kwargs)

    def install_sdk(self):
        pass

    def install_sdkd(self):
        pass


class PythonInstaller(SDKDInstaller):
    def __init__(self, args, **kwargs):
        super.__init__(self, args, kwargs)

    def install_sdk(self):
        pass

    def install_sdkd(self):
        pass

