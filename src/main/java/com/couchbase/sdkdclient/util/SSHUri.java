/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.couchbase.sdkdclient.util;

import java.text.ParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author mnunberg
 */
public class SSHUri {

  static final Pattern regex = Pattern.compile("([^@]+)@([^:]+):(.*)");

  public static SSHUri parse(String s) throws ParseException {
    Matcher m = regex.matcher(s);
    if (!m.matches()) {
      throw new ParseException("Format must be user@host:/path: " + s, 0);
    }

    SSHUri ret = new SSHUri();
    ret.username = m.group(1);
    ret.host = m.group(2);
    ret.path = m.group(3);
    return ret;

  }
  private String username;
  private String path;
  private String host;

  public String getUser() {
    return username;
  }

  public String getPath() {
    return path;
  }

  public String getHost() {
    return host;
  }
}
