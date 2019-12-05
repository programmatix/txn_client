/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.couchbase.sdkdclient.cluster;

import com.couchbase.cbadmin.assets.Bucket;
import com.couchbase.cbadmin.client.BucketConfig;
import com.couchbase.sdkdclient.options.*;

/**
 *
 * @author mnunberg
 */
public class CBBucketOptions implements OptionConsumer {
  private IntOption optRamSize =
          OptBuilder.startInt("ram")
          .help("Memory size for bucket")
          .defl("256")
          .argName("MB")
          .build();

  private StringOption optSaslPassword =
          OptBuilder.startString("password")
          .help("SASL Password for bucket")
          .defl("password")
          .build();

  private StringOption optName =
          OptBuilder.startString("name")
          .help("Name for bucket")
          .defl("default")
          .build();

  private BoolOption optAlsoDefault =
          OptBuilder.startBool("add-default")
          .help("When creating a non-default bucket, also create the default")
          .defl("false")
          .build();


  private EnumOption<Bucket.BucketType> optType =
          OptBuilder.start("type", Bucket.BucketType.class)
          .defl("COUCHBASE")
          .help("Bucket type to use")
          .build()
          .addChoice("membase", Bucket.BucketType.COUCHBASE)
          .addChoice("memcache", Bucket.BucketType.MEMCACHED)
          .addChoice("ephemeral", Bucket.BucketType.EPHEMERAL);

  private EnumOption<Bucket.EphemeralBucketEvictionPolicy> optEphemeralEvictionPolicy =
          OptBuilder.start("ephemeralEvictionPolicy", Bucket.EphemeralBucketEvictionPolicy.class)
                  .defl("noEviction")
                  .help("ephemeral bucket eviction policy")
                  .build()
                  .addChoice("noEviction", Bucket.EphemeralBucketEvictionPolicy.NOEVICTION)
                  .addChoice("nruEviction", Bucket.EphemeralBucketEvictionPolicy.NRUEVICTION);

  private IntOption optReplicas =
          OptBuilder.startInt("replicas")
          .help("Replica count for bucket")
          .argName("REPLICA_COUNT")
          .defl("1")
          .build();

  @Override
  public OptionTree getOptionTree() {
    return new OptionTreeBuilder()
            .group("bucket")
            .description("Options controlling the creation of buckets")
            .source(this, CBBucketOptions.class)
            .prefix(OptionDomains.BUCKET)
            .build();
  }

  public String getName() {
      return optName.getValue();
  }

  public String getPassword() {
      return optSaslPassword.getValue();
  }

  public BucketConfig buildMainBucketOptions() {
    BucketConfig bconf = new BucketConfig(getName());
    bconf.bucketType = optType.getValue();
    bconf.ramQuotaMB = optRamSize.getValue();
    bconf.replicaCount = optReplicas.getValue();
    bconf.evictionPolicy = optEphemeralEvictionPolicy.getValue();
    String passwd = optSaslPassword.getValue();
    if (passwd != null && passwd.length() > 0) {
      bconf.setSaslPassword(passwd);
    }
    return bconf;
  }

  public boolean shouldAddDefaultBucket() {
    return optAlsoDefault.getValue();
  }
}
