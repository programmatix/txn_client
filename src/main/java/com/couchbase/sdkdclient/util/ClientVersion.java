/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.couchbase.sdkdclient.util;

import com.couchbase.sdkdclient.logging.LogUtil;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.slf4j.Logger;

/**
 *
 * @author mnunberg
 */
public class ClientVersion {
  static final Logger logger = LogUtil.getLogger(ClientVersion.class);
  static final Properties props = new Properties();
  static {
    InputStream strm = ClientVersion.class.getResourceAsStream("/git.properties");
    if (strm == null) {
      logger.warn("Couldn't load version information");
    } else {
      try {
        props.load(strm);
      } catch (IOException ex) {
        logger.error("While loading git.properties", ex);
      }
    }
  }

  public static String getRevision() {
    return props.getProperty("git.commit.id", "<UNKNOWN>");
  }
}
