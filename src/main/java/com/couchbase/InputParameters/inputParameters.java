package com.couchbase.InputParameters;

import com.couchbase.Constants.defaults;
import com.couchbase.Logging.LogUtil;
import com.couchbase.Tests.Transactions.transactionTests;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class inputParameters{
    private  String ParamsFile;
    private  boolean setStorageMode;
    private  String sshusername;
    private  String sshpassword;
    private  String workload;
    private  boolean installcouchbase;
    private  boolean initializecluster;

    private  String clusterVersion;
    private  String ramp;
    private  String rebound;
    private  boolean upgrade;
    private  String upgradeVersion;
    private  String downloadurl;
    private  String buildtype;
    private  String textexecuteuser;
    private  String textexecutepassword;
    private boolean useSSH;
    private String nodeQuota;
    private String bucketname;
    private String bucketpassword;
    private boolean adddefaultbucket;
    private  int usemaxconn;
    private String bucketType;
    private  String n1qlFieldsToIndex;
    private  boolean enableAutoFailOver;
    private int AutoFailoverTimeout;
    private String n1qlIndexName;
    private String n1qlIndexType;
    private int bucketReplicaCount;
    private String bucketEphemeralEvictionPolicy;
    private int bucketRamSize;
    private String bucketSaslpassword;
    private int clusterPort;
    private String testtype;
    private String testsuite;
    private String testname;




    private LinkedList<Host> hosts = new LinkedList<Host>();


    public inputParameters(String ParamsFile){
        this.ParamsFile = ParamsFile;
    }

    public class Host {
        public String ip;
        public String hostservices;
        public  Host(String ipAddress, String services)
        {
            ip=ipAddress;
            hostservices = services;
        }

        public void printConfig(){
            System.out.println("Host with IP: "+ip+" has services: "+ hostservices);
        }
    }




    public void readandStoreParams() throws IOException, ParseException{
        Object obj = new JSONParser().parse(new FileReader(ParamsFile));
        JSONObject jo = (JSONObject) obj;

        try{
            setStorageMode = (boolean)jo.get("setStorageMode");
        }catch(NullPointerException e){
            setStorageMode = defaults.setStorageMode;
        }

        try{
            sshusername = (String) jo.get("ssh-username");
        }catch(NullPointerException e){
            sshusername = defaults.sshusername;
        }

        try{
            sshpassword = (String) jo.get("ssh-password");
        }catch(NullPointerException e){
            sshpassword = defaults.sshpassword;
        }

        try{
            workload = (String) jo.get("workload");
        }catch(NullPointerException e){
            System.out.println("No workload given. This is mandatory for the test framework to execute tests");
            System.exit(-1);
        }

        try{
            installcouchbase = (boolean) jo.get("installcouchbase");
        }catch(NullPointerException e){
            installcouchbase = defaults.installcouchbase;
        }

        try{
            initializecluster = (boolean) jo.get("initializecluster");
        }catch(NullPointerException e){
            initializecluster = defaults.initializecluster;
        }

        try{
            clusterVersion = (String) jo.get("clusterVersion");
        }catch(NullPointerException e){
            if(installcouchbase){
                System.out.println("Requested to install couchbase but couchbase version is not given");
                System.exit(-1);
            }
            clusterVersion = defaults.clusterVersion;
        }

        try{
            bucketname = (String) jo.get("bucketname");
        }catch(NullPointerException e){
            bucketname = defaults.bucketname;
        }

        try{
            bucketpassword = (String) jo.get("bucketpassword");
        }catch(NullPointerException e){
            bucketpassword = defaults.bucketpassword;
        }

        try{
            adddefaultbucket = (boolean) jo.get("adddefaultbucket");
        }catch(NullPointerException e){
            adddefaultbucket = defaults.adddefaultbucket;
        }

        try{
            usemaxconn = Integer.parseInt((String) jo.get("usemaxconn"));
        }catch(NullPointerException e){
            usemaxconn = defaults.usemaxconn;
        }

        try{
            bucketType = (String) jo.get("bucketType");
        }catch(NullPointerException e){
            bucketType = defaults.bucketType;
        }

        try{
            bucketRamSize = Integer.parseInt((String) jo.get("bucketRamSize"));
        }catch(NullPointerException e){
            bucketRamSize = defaults.bucketRamSize;
        }

        try{
            bucketEphemeralEvictionPolicy = (String) jo.get("bucketEphemeralEvictionPolicy");
        }catch(NullPointerException e){
            bucketEphemeralEvictionPolicy = defaults.bucketEphemeralEvictionPolicy;
        }

        try{
            bucketReplicaCount = Integer.parseInt((String) jo.get("bucketReplicaCount"));
        }catch(NullPointerException e){
            bucketReplicaCount = defaults.bucketReplicaCount;
        }

        try{
            bucketSaslpassword = (String) jo.get("bucketSaslpassword");
        }catch(NullPointerException e){
            bucketSaslpassword = defaults.bucketSaslpassword;
        }



        try{
            ramp = (String) jo.get("ramp");
        }catch(NullPointerException e){
            ramp = defaults.ramp;
        }

        try{
            rebound = (String) jo.get("rebound");
        }catch(NullPointerException e){
            rebound = defaults.rebound;
        }


        try{
            upgrade = (boolean) jo.get("upgrade");
        }catch(NullPointerException e){
            upgrade = defaults.upgrade;
        }

        try{
            upgradeVersion = (String) jo.get("upgradeVersion");
        }catch(NullPointerException e){
            upgradeVersion = defaults.upgradeVersion;
        }

        try{
            downloadurl = (String) jo.get("downloadurl");
        }catch(NullPointerException e){
            downloadurl = defaults.downloadurl;
        }

        try{
            buildtype = (String) jo.get("buildtype");
        }catch(NullPointerException e){
            buildtype = defaults.buildtype;
        }

        try{
            textexecuteuser = (String) jo.get("textexecuteuser");
        }catch(NullPointerException e){
            textexecuteuser = defaults.textexecuteuser;
        }

        try{
            textexecutepassword = (String) jo.get("textexecutepassword");
        }catch(NullPointerException e){
            textexecutepassword = defaults.textexecutepassword;
        }

        try{
            useSSH = (boolean) jo.get("useSSH");
        }catch(NullPointerException e){
            useSSH = defaults.useSSH;
        }

        try{
            nodeQuota = (String) jo.get("nodeQuota");
        }catch(NullPointerException e){
            nodeQuota = defaults.nodeQuota;
        }

        try{
            n1qlFieldsToIndex = (String) jo.get("n1qlFieldsToIndex");
        }catch(NullPointerException e){
            n1qlFieldsToIndex = defaults.n1qlFieldsToIndex;
        }

        try{
            n1qlIndexName = (String) jo.get("n1qlIndexName");
        }catch(NullPointerException e){
            n1qlIndexName = defaults.n1qlIndexName;
        }

        try{
            n1qlIndexType = (String) jo.get("n1qlIndexType");
        }catch(NullPointerException e){
            n1qlIndexType = defaults.n1qlIndexType;
        }


        try{
            enableAutoFailOver = (boolean) jo.get("enableAutoFailOver");
        }catch(NullPointerException e){
            enableAutoFailOver = defaults.enableAutoFailOver;
        }

        try{
            AutoFailoverTimeout = Integer.parseInt((String) jo.get("AutoFailoverTimeout"));
        }catch(NullPointerException e){
            AutoFailoverTimeout = defaults.AutoFailoverTimeout;
        }

        try{
            clusterPort = Integer.parseInt((String) jo.get("clusterPort"));
        }catch(NullPointerException e){
            clusterPort = defaults.clusterPort;
        }

        try{
            String logoptions =  (String) jo.get("loggerLevel");
            LogUtil.setLevelFromSpec(logoptions);
        }catch(NullPointerException e){
            LogUtil.setLevelFromSpec("all:INFO");
        }

        try{
            testtype =  (String) jo.get("testtype");
            if(testtype.equals("txn") || testtype.equals("")) {
                testtype = "txn";
            }
        }catch(NullPointerException e){
            testtype = "txn";
        }

        try{
            testsuite =  (String) jo.get("testsuite");
            if(testsuite.equals("")){
                testsuite= "basic";
            }
        }catch(NullPointerException e){
            testtype = "basic";
        }

        try{
            testname =  (String) jo.get("testname");
            if(testname.equals("")){
                testname= "all";
            }
        }catch(NullPointerException e){
            testname = "simplecommit";
        }

        extractnodeConfig(jo);
        validatetestparams();
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

    private void validatetestparams(){





    }


    public void printConfiguration(){
        System.out.println("ParamsFile: " + ParamsFile);
        System.out.println("setStorageMode: " + setStorageMode);
        System.out.println("sshusername: " + sshusername);
        System.out.println("sshpassword: " + sshpassword);
        System.out.println("workload: " + workload);
        System.out.println("installcouchbase: " + installcouchbase);
        System.out.println("initializecluster: " + initializecluster);
        System.out.println("clusterVersion: " + clusterVersion);
        System.out.println("bucketpassword: " + bucketpassword);
        System.out.println("ramp: " + ramp);
        System.out.println("rebound: " + rebound);
        System.out.println("testsuite: " + testsuite);
        System.out.println("upgrade: " + upgrade);
        System.out.println("upgradeVersion: " + upgradeVersion);
        System.out.println("downloadurl: " + downloadurl);
        System.out.println("buildtype: " + buildtype);
        System.out.println("textexecuteuser: " + textexecuteuser);
        System.out.println("textexecutepassword: " + textexecutepassword);
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



    public String getworkload(){
        return workload;
    }

    public LinkedList<Host> getClusterNodes(){
        return hosts;
    }

    public String getclusterVersion(){
        return clusterVersion;
    }
    public void setclusterVersion(String Version){
         this.clusterVersion=Version;
    }


    public String getramp(){
        return ramp;
    }

    public String getrebound(){
        return rebound;
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

    public boolean getinstallcouchbase(){
        return installcouchbase;
    }

    public boolean getinitializecluster(){
        return initializecluster;
    }

    public boolean shouldUseSSH(){return useSSH;}

    public String getNodeQuota(){return nodeQuota;}

    public String getbucketname(){return bucketname;}

    public String getbucketpassword(){return bucketpassword;}

    public boolean getadddefaultbucket(){return adddefaultbucket;}

    public int getusemaxconn(){return usemaxconn;}

    public String getbucketType(){return bucketType;}

    public String getn1qlFieldsToIndex(){return n1qlFieldsToIndex;}

    public boolean getenableAutoFailOver(){return enableAutoFailOver;}

    public int getAutoFailoverTimeout(){return AutoFailoverTimeout;}

    public String getn1qlIndexName(){return n1qlIndexName;}

    public String getn1qlIndexType(){return n1qlIndexType;}

    public int getbucketRamSize(){return bucketRamSize;}

    public int getbucketReplicaCount(){return bucketReplicaCount;}

    public String getbucketEphemeralEvictionPolicy(){return bucketEphemeralEvictionPolicy;}

    public String getbucketSaslpassword(){return bucketSaslpassword;}

    public int getclusterPort(){return clusterPort;}

    public String gettesttype(){return testtype;}

    public String gettestsuite(){return testsuite;}

    public String gettestname(){return testname;}






}
