package com.couchbase.sdkdclient.options;

/**
 * Static utility class containing various option prefixes used by various
 * plugins.
 */
public class OptionDomains {
  public final static OptionPrefix BUCKET = new OptionPrefix("bucket", "bkt");
  public final static OptionPrefix CLUSTER = new OptionPrefix("cluster", "cl");
  public final static OptionPrefix FAILOVER =new OptionPrefix("failover", "fo");
  public final static OptionPrefix REBALANCE = new OptionPrefix("rebalance", "rb");
  public final static OptionPrefix SERVICE = new OptionPrefix("service", "svc", "svcfail");
  public final static OptionPrefix REXEC = new OptionPrefix("exec", "rexec");
  public final static OptionPrefix SCENARIO = new OptionPrefix("scenario", "sc");
  public final static OptionPrefix BASIC_SCENARIO = new OptionPrefix("basic", "bsc");
  public final static OptionPrefix KV_WORKLOAD = new OptionPrefix("kv", "dsw");
  public final static OptionPrefix DCP_WORKLOAD = new OptionPrefix("dcp", "dcp");
  public final static OptionPrefix CBAS_WORKLOAD = new OptionPrefix("cbas", "cbas");
  public final static OptionPrefix CBFT_WORKLOAD = new OptionPrefix("cbft", "cbft");
  public final static OptionPrefix SUBDOC_WORKLOAD = new OptionPrefix("subdoc", "sd");
  public final static OptionPrefix SPATIAL_WORKLOAD = new OptionPrefix("spatial", "spatial");
  public final static OptionPrefix VQ_WORKLOAD = new OptionPrefix("views", "vdsw");
  public final static OptionPrefix NQ_WORKLOAD = new OptionPrefix("n1ql", "ndsw");
  public final static OptionPrefix HYBRID_WORKLOAD = new OptionPrefix("wlhybrid", "hdsw");
  public final static OptionPrefix N1QLHYBRID_WORKLOAD = new OptionPrefix("n1qlhybrid", "nhdsw");
  public final static OptionPrefix INIT_WORKLOAD = new OptionPrefix("instantiate", "inst");
  public final static OptionPrefix INSTALLER = new OptionPrefix("install");
  public final static OptionPrefix ALT_ADDR = new OptionPrefix("altaddr");
  public final static OptionPrefix TXN = new OptionPrefix("transaction", "txn");

  private OptionDomains() {}
}
