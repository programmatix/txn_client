package com.couchbase.Constants;

public class defaults {

    public final static boolean setStorageMode = false;
    public final static String sshpassword = "couchbasego";
    public final static String sshusername = "root";
    public final static boolean installcouchbase = true;
    public final static boolean initializecluster = true;
    public final static String  clusterVersion = "";
    public final static String  bucketname = "default";
    public final static String  bucketpassword = "password";
    public final static boolean adddefaultbucket = false;

    public final static String  ramp = "120";
    public final static String  rebound = "120";
    public final static boolean upgrade = false;
    public final static String  testsuite = "";
    public final static String  test = "";
    public final static String  upgradeVersion = "";
    public final static String  downloadurl = "";
    public final static String  buildtype = "enterprise";
    public final static String  textexecuteuser = "root";
    public final static String  textexecutepassword = "couchbasego";
    public final static boolean useSSH = true;
    public final static boolean enableAutoFailOver=false;
    public final static int AutoFailoverTimeout = 5 ;



    public final static String ADMIN_USER = "Administrator";
    public final static String PASSWORD = "password";
    public final static String DEFAULT_KEY = "Test";
    public final static String CONTENT_NAME= "content";
    public final static String DEFAULT_CONTENT_VALUE = "default";
    public final static String UPDATED_CONTENT_VALUE = "updated";
    public final static int RestTimeout = 45;
    public final static int numGroups = 1;

    public final static String bucketEphemeralEvictionPolicy = "noEviction";
    public final static String bucketSaslpassword = "password";
    public final static int bucketRamSize = 256;
    public final static int bucketReplicaCount = 1;


    public final static String groupPolicy ="SEPARATE";
    public final static String[] ipalias = new String[0];
    public final static String  nodeQuota = "1500";
    public final static String  bucketType = "COUCHBASE";
    public final static  String n1qlFieldsToIndex = "tag,type";
    public final static  String n1qlIndexName = "n1qlIdx1";
    public final static  String n1qlIndexType = "secondary";


    public final static  int usemaxconn=125;
    public final static  int clusterPort = 8091;







}
