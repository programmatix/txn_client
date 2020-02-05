package com.couchbase.Couchbase.Couchbase;

import com.couchbase.Exceptions.HarnessError;
import com.couchbase.Exceptions.HarnessException;
import com.couchbase.Exceptions.RestApiException;
import com.couchbase.Logging.LogUtil;
import com.couchbase.Utils.RemoteUtils.RemoteCommands;
import com.couchbase.Utils.RemoteUtils.SSHConnection;
import com.couchbase.Utils.RemoteUtils.SSHLoggingCommand;
import com.couchbase.Utils.Retryer;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;
import com.couchbase.InputParameters.inputParameters;

public class CouchbaseInstaller {
    final private static Logger logger = LogUtil.getLogger(CouchbaseInstaller.class);
    private inputParameters inputParameters;
    VersionTuple vTuple;
    public static final String ARCH_32 = "x86";
    public static final String ARCH_64 = "x86_64";
    public static final String AMD_64 = "amd64";
    public static final String RSRC_SCRIPT = "installer/cluster-install.py";
    static final String SHERLOCK_BUILD_URL = "http://172.23.120.24/builds/latestbuilds/couchbase-server/sherlock/";
    static final String WATSON_BUILD_URL = "http://172.23.120.24/builds/latestbuilds/couchbase-server/watson/";
    static final String SPOCK_BUILD_URL = "http://172.23.120.24/builds/latestbuilds/couchbase-server/spock/";
    static final String VULCAN_BUILD_URL = "http://172.23.120.24/builds/latestbuilds/couchbase-server/vulcan/";
    static final String ALICE_BUILD_URL="http://172.23.120.24/builds/latestbuilds/couchbase-server/alice/";
    static final String MADHATTER_BUILD_URL="http://172.23.120.24/builds/latestbuilds/couchbase-server/mad-hatter/";

    public CouchbaseInstaller(inputParameters inputParameters){
        this.inputParameters=inputParameters;
    }

    static class VersionTuple {
        final String full;
        final String major;
        final String minor;
        final String patch;
        final String build;

        public VersionTuple(String major, String minor, String patch, String full, String build) {
            this.major = major;
            this.minor = minor;
            this.patch = patch;
            this.full = full;
            this.build = build;
        }

    }

    static public abstract class RestRetryer extends Retryer<RestApiException> {
        RestRetryer(int seconds) {
            super(seconds * 1000, 500, RestApiException.class);
        }

        @Override
        protected void handleError(RestApiException caught) throws RestApiException, InterruptedException {
            if (caught.getStatusLine().getStatusCode() >= 500) {
                call();
            } else if (caught.getStatusLine().getStatusCode() == 409) {
                logger.error("N1QL Index was not deleted from previous run");
                return;
            } else {
                throw HarnessException.create(HarnessError.CLUSTER, caught);
            }
        }
    }

    public static VersionTuple parse(String vString) throws Exception {
        String[] parts = vString.split("\\.");
        String trailer[] = parts[parts.length - 1].split("-");
        return new VersionTuple(parts[0], parts[1], trailer[0], vString, trailer[1]);
    }

    public void installcouchbase() throws ExecutionException {
            logger.info("Installing couchbase now");
            Collection<inputParameters.Host> nodes= inputParameters.getClusterNodes();
            ExecutorService svc = Executors.newFixedThreadPool(nodes.size());
            List<Future> futures = new ArrayList<Future>();

            for (final inputParameters.Host node : nodes) {
                Future f = svc.submit(new Callable<Object>() {
                    @Override
                    public Object call() throws Exception {
                        runNode(node);
                        return null;
                    }
                });
                futures.add(f);
            }

            svc.shutdown();
            for (Future f : futures) {
                try {
                    f.get();
                } catch (InterruptedException ex) {
                    throw new ExecutionException(ex);
                }
            }
    }

    public void runNode(inputParameters.Host node) throws IOException {
        /**
         * Now to get system information.. we need SSH
         */
         SSHConnection sshConn;
        sshConn = new SSHConnection(inputParameters.getsshusername(),
                                    inputParameters.getsshpassword(),
                                    node.ip);
        sshConn.connect();
        logger.debug("SSH Initialized for {}", this);

        RemoteCommands.OSInfo osInfo = RemoteCommands.getSystemInfo(sshConn);

        try{
            if (inputParameters.getupgrade()) {
                vTuple = parse(inputParameters.getUpgradeVersion());
                inputParameters.setclusterVersion(inputParameters.getUpgradeVersion());
            }else{
                vTuple =parse(inputParameters.getclusterVersion());
            }
        } catch (Exception ex) {
            throw new IOException("Unable to parse version " + ex.getStackTrace());
        }
        /**
         * Build URL.
         */

        URL dlUrl = new URL(buildURL(vTuple, osInfo));

        InputStream is = getInstallScript();

        String remoteScript = "cluster-install.py";

        //noinspection OctalInteger
        sshConn.copyTo(remoteScript, is, 0755);

        SSHLoggingCommand cmd = new SSHLoggingCommand(sshConn, "python " + remoteScript + " " + dlUrl);
        try {
            cmd.execute();
            cmd.waitForExit(Integer.MAX_VALUE);
        }catch(Exception e){
            logger.error("Unable to install CB due to error: "+ e);
            System.exit(-1);
        }
        finally {
            cmd.close();
        }
    }


    private static InputStream getInstallScript() {
        InputStream is = CouchbaseInstaller.class
                .getClassLoader().getResourceAsStream(RSRC_SCRIPT);
        if (is == null) {
            throw new RuntimeException("Can't find script:" + RSRC_SCRIPT);
        }
        return is;
    }

    private String buildURL(VersionTuple vTuple, RemoteCommands.OSInfo osInfo) throws IOException {
        if (inputParameters.getdownloadurl() != "") {
            return inputParameters.getdownloadurl();
        }
        if (osInfo.getArch() == null || osInfo.getPlatform() == null || osInfo.getPackageType() == null) {
            throw new IOException("Unable to get os info");
        }
        String baseUrl = "";
        if (vTuple.major.equals("4")) {
            int minor = Integer.parseInt(vTuple.minor);
            baseUrl = SHERLOCK_BUILD_URL;
            if (minor >= 5) {
                baseUrl = WATSON_BUILD_URL;
            }
        } else if (vTuple.major.equals("5")) {
            int minor = Integer.parseInt(vTuple.minor);
            baseUrl = SPOCK_BUILD_URL;
            if (minor >= 5){
                baseUrl = VULCAN_BUILD_URL;
            }
        }
        else if (vTuple.major.equals("6")) {
            int minor = Integer.parseInt(vTuple.minor);
            baseUrl = ALICE_BUILD_URL;
            if (minor >= 5){
                baseUrl = MADHATTER_BUILD_URL;
            }
        }
        if (baseUrl == "") {
            throw new IOException("Base url not found for build. Installer supports only sherlock, watson and spock");
        }

        StringBuilder urlStr = new StringBuilder();
        urlStr.append(baseUrl + vTuple.build + "/");
        urlStr.append("couchbase-server-" + inputParameters.getbuildtype());
        if (osInfo.getPlatform().contains("centos")) {
            urlStr.append("-");
            urlStr.append(vTuple.full + "-");
            urlStr.append(osInfo.getPlatform());
            urlStr.append("." + osInfo.getArch() + "." + osInfo.getPackageType());
        } else {
            urlStr.append("_");
            urlStr.append(vTuple.full + "-");
            urlStr.append(osInfo.getPlatform());
            urlStr.append("_" + osInfo.getArch() + "." + osInfo.getPackageType());
        }
        return urlStr.toString();
    }

}