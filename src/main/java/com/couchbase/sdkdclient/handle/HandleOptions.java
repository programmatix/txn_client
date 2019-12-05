/*
 * Copyright (c) 2013 Couchbase, Inc.
 */
package com.couchbase.sdkdclient.handle;
import java.net.URI;
import java.util.List;

/**
 * Class used to doConfigure an SDKD {@link Handle}
 */
public interface HandleOptions {
    public String getBucketName();

  /**
   * Gets the SASL password.
   * @return The sasl password. May be empty or {@code null}
   */
  public String getBucketPassword();

  /**
   * Gets the hostname for the entry point node
   * @return Primary hostname to connect ti.
   */
  public URI getEntryPoint();

  /**
   * A list of 0 or more nodes to connect to for backups if the entrypoint
   * goes down
   * @return A list of 0 or more strings.
   */
  public List<URI> getAuxNodes();

  /**
   * Get type of connection: with/out ssl
   * @return true if you want to use ssl
   */
  public boolean getUseSSL();
  /**
   * Get the cluster certificate to be passed to the driver
   */
  public String getClusterCertificate();

  /**
   * Get the autofailover property to be passed to the driver
   */
  public int getAutoFailoverSetting();
}
