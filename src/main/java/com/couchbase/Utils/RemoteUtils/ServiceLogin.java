package com.couchbase.Utils.RemoteUtils;/*
 * Copyright (c) 2013 Couchbase, Inc.
 */

import java.net.URI;

public class ServiceLogin {
  final private String username;
  final private String password;
  final private Integer port;


  public int getPort() {
    if (port == null) {
      throw new IllegalArgumentException("Port not assigned");
    }
    return port;
  }

  public boolean hasPort() {
    return port != null;
  }

  public String getUsername() {
    return username;
  }

  public  String getPassword() {
    return password;
  }

  public boolean isEmpty() {
    return password == null && username == null;
  }


  public ServiceLogin(String user, String pass, int port) {
    if (user == null || user.isEmpty()) {
      username = null;
    } else {
      username = user;
    }

    if (pass == null || pass.isEmpty()) {
      password = null;
    } else {
      password = pass;
    }

    if (port > 0) {
      this.port = port;
    } else {
      this.port = null;
    }

  }


  public ServiceLogin(ServiceLogin other, String deflUser, String deflPass, int deflPort) {
    if (other.username != null) {
      username = other.username;
    } else {
      username = deflUser;
    }
    if (other.password != null) {
      password = other.password;
    } else {
      password = deflPass;
    }
    if (other.port != null) {
      port = other.port;
    } else if (deflPort < 1) {
      throw new IllegalArgumentException("Default port must be specified");
    } else {
      port = deflPort;
    }
  }

  public static ServiceLogin create(final String user, final String pass, final int port) {
    return new ServiceLogin(user, pass, port);
  }

  public static ServiceLogin createEmpty() {
    return new ServiceLogin(null, null, -1);
  }

  public static ServiceLogin create(URI uri, String user, String pass, int port) {
    String authStr = uri.getUserInfo();
    int portNum = uri.getPort();
    if (portNum < 1) {
      portNum = port;
    }

    if (authStr != null) {
      String[] parts = authStr.split(":", 2);
      user = parts[0];
      if (parts.length == 2) {
        pass = parts[1];
      }
    }

    return create(user, pass, portNum);
  }

  public static ServiceLogin create(URI uri, ServiceLogin deflParams) {
    return create(uri, deflParams.getUsername(),
                  deflParams.getPassword(),
                  deflParams.hasPort() ? deflParams.getPort() : -1);
  }
}
