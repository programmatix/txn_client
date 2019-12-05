/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.couchbase.sdkdclient.protocol;

/**
 *
 * @author mnunberg
 */
public class Strings {

  public final static String REQID = "ReqID";
  public final static String CMD = "Command";
  public final static String HID = "Handle";
  public final static String REQDATA = "CommandData";
  public final static String RESDATA = "ResponseData";
  public final static String DSREQ_DSTYPE = "DSType";
  public final static String DSREQ_DS = "DS";
  public final static String STATUS = "Status";
  public final static String ERRSTR = "ErrorString";
  public final static String DSSEED_KSIZE = "KSize";
  public final static String DSSEED_VSIZE = "VSize";
  public final static String DSSEED_COUNT = "Count";
  public final static String DSSEED_KSEED = "KSeed";
  public final static String DSSEED_VSEED = "VSeed";
  public final static String DSSEED_REPEAT = "Repeat";
  public final static String DSSPATIAL_KSEED = "KSeed";
  public final static String DSSPATIAL_VSEED = "VSeed";
  public final static String DSSPATIAL_COUNT = "Count";
  public final static String DSSPATIAL_LATBASE = "LatBase";
  public final static String DSSPATIAL_LONGBASE = "LongBase";
  public final static String DSSPATIAL_LATINCR = "LatIncr";
  public final static String DSSPATIAL_LONGINCR = "LongIncr";
  public final static String DSSUBDOC_KSEED = "KSeed";
  public final static String DSSUBDOC_KVCOUNT = "Count";
  public final static String DSFTS_COUNT = "Count";
  public final static String DSCBAS_COUNT = "Count";

  public final static String DSINLINE_ITEMS = "Items";
  public final static String DS_ID = "ID";
  public final static String DSREQ_OPTS = "Options";
  public final static String DSREQ_DELAY = "DelayMsec";
  public final static String DSREQ_DELAY_MIN = "DelayMin";
  public final static String DSREQ_DELAY_MAX = "DelayMax";
  public final static String DSREQ_FULL = "Detailed";
  public final static String DSREQ_MULTI = "Multi";
  public final static String DSREQ_EXPIRY = "Expiry";
  public final static String DSREQ_ITERWAIT = "IterWait";
  public final static String DSREQ_CONTINUOUS = "Continuous";
  public final static String DSREQ_TIMERES = "TimeRes";
  public final static String DSRES_STATS = "Summary";
  public final static String DSRES_FULL = "Details";
  public final static String DSRES_TIMINGS = "Timings";
  public final static String DSTYPE_SEEDED = "DSTYPE_SEEDED";
  public final static String DSTYPE_SPATIAL = "DSTYPE_SPATIAL";
  public final static String DSTYPE_N1QL = "DSTYPE_N1QL";
  public final static String DSTYPE_DCP = "DSTYPE_DCP";
  public final static String DSTYPE_FTS = "DSTYPE_FTS";
  public final static String DSTYPE_CBAS = "DSTYPE_CBAS";
  public final static String DSTYPE_SD = "DSTYPE_SD";
  public final static String DSREQ_ASYNC = "Async";
  public final static String DSNQ_COUNT = "NQCount";

  public final static String DS_PAYLOAD = "DS";
  public final static String DS_PRELOAD = "Preload";

  // Durability Requirements
  public final static String DSREQ_DUR_PERSIST = "PersistTo";
  public final static String DSREQ_DUR_REPLICATE = "ReplicateTo";
  public final static String DSREQ_REPLICA_READ = "ReplicaRead";
  public final static String TMS_BASE = "Base";
  public final static String TMS_COUNT = "Count";
  public final static String TMS_MIN = "Min";
  public final static String TMS_MAX = "Max";
  public final static String TMS_PERCENTILE = "Percentile";
  public final static String PERCENTILE_FACTOR = "PercentileFactor";
  public final static String TMS_AVG = "Avg";
  public final static String TMS_ECS = "Errors";
  public final static String TMS_WINS = "Windows";
  public final static String TMS_STEP = "Step";
  public final static String HANDLE_HOSTNAME = "Hostname";
  public final static String HANDLE_PORT = "Port";
  public final static String HANDLE_SSL = "SSL";
  public final static String HANDLE_BUCKET = "Bucket";
  public final static String HANDLE_USERNAME = "Username";
  public final static String HANDLE_PASSWORD = "Password";
  public final static String HANDLE_OPTIONS = "Options";
  public final static String HANDLE_OPT_TMO = "Timeout";
  public final static String HANDLE_OPT_BACKUPS = "OtherNodes";
  public final static String HANDLE_CERT = "ClusterCertificate";
  public final static String HANDLE_AUTOFAILOVER_MS = "AutoFailover";
  // TTL Command parameters
  public final static String TTL_SECONDS = "Seconds";
  // View Query Parameters
  public final static String QVOPT_STALE = "stale";
  public final static String QVOPT_LIMIT = "limit";
  public final static String QVOPT_ONERR = "on_error";
  public final static String QVOPT_DESC = "descending";
  public final static String QVOPT_SKIP = "skip";
  public final static String QVOPT_REDUCE = "reduce";
  public final static String QVOPT_INCDOCS = "include_docs";
  public final static String QV_ONERR_CONTINUE = "continue";
  public final static String QV_ONERR_STOP = "stop";
  public final static String QV_STALE_UPDATEAFTER = "update_after";
  // View Load Options
  public final static String V_SCHEMA = "Schema";
  public final static String V_INFLATEBASE = "InflateContent";
  public final static String V_INFLATECOUNT = "InflateLevel";
  public final static String V_KIDENT = "KIdent";
  public final static String V_KSEQ = "KVSequence";
  public final static String V_DESNAME = "DesignName";
  public final static String V_MRNAME = "ViewName";
  // View Query Control Options
  public final static String V_QOPTS = "ViewParameters";
  public final static String V_QDELAY = "ViewQueryDelay";
  public final static String V_QITERCOUNT = "ViewQueryCount";
  // Not strings, but they are protocol constants
  public final static int VR_IX_IDENT = 0;
  public final static int VR_IX_INFLATEBASE = 1;
  public final static int VR_IX_INFLATECOUNT = 2;
  public final static int VR_IX_INFLATEBLOB = 3;
  public final static int VR_IX_MAX = 4;

  // N1QL Options
  public final static String NQ_PARAM = "NQParam";
  public final static String NQ_PARAMVALUES = "NQParamValues";
  public final static String NQ_INDEX_ENGINE = "NQIndexEngine";
  public final static String NQ_INDEX_TYPE = "NQIndexType";
  public final static String NQ_PREPARED = "NQPrepared";
  public final static String NQ_PARAMETERIZED = "NQParameterized";
  public final static String NQ_DEFAULT_INDEX_NAME = "NQDefaultIndexName";
  public final static String NQ_SCANCONSISTENCY = "NQScanConsistency";
  public final static String NQ_BATCHSIZE = "NQBatchSize";

  // Subdoc Options
  public final static String SD_DOC = "SDSchema";
  public final static String SD_PATH = "SDPath";
  public final static String SD_VALUE = "SDValue";

  public final static String SD_COMMAND  = "SDCommand";

  // FTS Options
  public final static String FTS_INDEX_NAME = "FTSIndexName";
  public final static String FTS_CONSISTENCY="FTSConsistency";

  //DCP Options
  public final static String DCP_KEY = "DCPKey";
  public final static String DCP_VALUE = "DCPValue";
  public final static String DCP_DELTA = "DCPDelta";

  //Txn Options:
  public final static String TXN_PAYLOAD = "TXNKey";
  public final static String TXN_NODES_INIT = "TXN_NODES_INIT";
  public final static String TXN_REPLICAS = "TXN_REPLICAS";
  public final static String TXN_COMMIT = "TXN_COMMIT";
  public final static String TXN_OP_TYPE = "TXN_OP_TYPE";
  public final static String TXN_GROUP = "TXN_GROUP";
  public final static String TXN_OS_TYPE = "TXN_OS_TYPE";

  //Create Txn Params
  public final static String TXN_DURABILITY = "TXN_DURABILITY";
  public final static String TXN_TIMEOUT = "TXN_TIMEOUT";
  public final static String TXN = "TXN";


  public final static String TXN_TOTAL_DOCS = "totaldocs";
  public final static String TXN_BATCHSIZE = "batchsize";
  public final static String TXN_NTHREADS = "nthreads";
  public final static String TXN_KEYS = "txnKeys";
  public final static String TXN_LOAD_DATA= "TXN_LOAD_DATA";
  public final static String TXN_UPDATEKEYS= "UpdateKeys";
  public final static String TXN_DELETEKEYS = "DeleteKeys";


}

