/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.couchbase.sdkdclient.handle;
import com.couchbase.client.java.Cluster;
import com.couchbase.sdkdclient.logging.LogUtil;
import org.slf4j.Logger;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Class representing a connection to a single handle. A handle represents an
 * opaque but isolated flow of control for a given SDK. Some SDKs may choose
 * to implement this as a separate thread while other SDKs may make this an
 * asynchronous flow of control.
 *
 * A handle is configured using a {@link HandleOptions} object, and is created
 * @see HandleOptions
 */
public class Handle {

  static private Logger logger = LogUtil.getLogger(Handle.class);
  public final static int HANDLE_CTL = 0x01;
  static private final AtomicInteger idSerial = new AtomicInteger(100);
  private final int id;
  private final Socket sock;

  /**
   * Creates a new Handle object which will utilize a socket
   *
   * @param s An initialized and connected socket to be used as the
   * communications conduit for this handle
   * @throws IOException
   */
  public Handle( Socket s, int opts) throws IOException {
    sock = s;
    if ((opts & HANDLE_CTL) != 0) {
      id = 0;
    } else {
      id = idSerial.getAndIncrement();
    }
  }


  public Handle(int opts) {
    sock = null;
    if ((opts & HANDLE_CTL) != 0) {
      id = 0;
    } else {
      id = idSerial.getAndIncrement();
    }
  }



  /**
   * @return the unique Handle ID
   */
  public int getId() {
    return id;
  }
}