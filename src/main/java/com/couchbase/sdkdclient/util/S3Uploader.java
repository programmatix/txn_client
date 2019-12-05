/**
 * Copyright 2013, Couchbase Inc.
 */
package com.couchbase.sdkdclient.util;

import com.couchbase.sdkdclient.logging.LogUtil;
import com.couchbase.sdkdclient.options.ini.IniParser;
import com.couchbase.sdkdclient.options.ini.IniSection;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jets3t.service.S3Service;
import org.jets3t.service.S3ServiceException;
import org.jets3t.service.ServiceException;
import org.jets3t.service.acl.AccessControlList;
import org.jets3t.service.impl.rest.httpclient.RestS3Service;
import org.jets3t.service.model.S3Object;
import org.jets3t.service.security.AWSCredentials;
import org.jets3t.service.security.ProviderCredentials;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * Class to simplify uploading of stuff to S3
 * @author mnunberg
 */
public class S3Uploader {
  private final static Logger logger = LogUtil.getLogger(S3Uploader.class);
  private S3Service svc;
  final String bktName;
  final File credsFile;

  /**
   * Creates a new S3 Uploader object.
   * @param prefix The bucket name to use
   * @param s3Creds The credentials file for S3. This can be created by executing
   * {@code brun --gen-creds }
   */
  public S3Uploader(String prefix, File s3Creds) {
    bktName = prefix;
    credsFile = s3Creds;
  }

  private static ProviderCredentials credsFromS3cfg() throws IOException {
    File cfgfile = new File(System.getProperty("user.home"), ".s3cfg");
    if (!cfgfile.exists()) {
      return null;
    }
    IniParser parser = IniParser.create(cfgfile);
    parser.parse();
    IniSection section = parser.getSection("default");
    String accessKey = section.get("access_key", null);
    String secretKey = section.get("secret_key", null);

    if (accessKey == null || secretKey == null) {
      throw new IOException("Expected to find both access_key and secret_key in s3cfg");
    }
    return new AWSCredentials(accessKey, secretKey);
  }

  /**
   * Actually connect to the S3 service.
   * @throws IOException On connection or auth failure.
   */
  public void connect() throws IOException {
    ProviderCredentials creds;
    if (credsFile == null) {
      logger.warn("Credentials file not found. Will attempt to load from ~/.s3cfg");
      creds = credsFromS3cfg();
    } else {
      try {
        creds = ProviderCredentials.load("", credsFile);
      } catch (ServiceException ex) {
        throw new IOException(ex);
      }
    }

    try {
      svc = new RestS3Service(creds);
      //FIXME: Required hack for HttpComponents 4.3.1
      svc.setHttpClient(HttpClientBuilder.create().build());

    } catch (S3ServiceException ex) {
      throw new IOException(ex);
    }
  }

  /**
   * Upload a file to S3
   * @param strm The stream to use. Exclusive with {@code f}
   * @param f The {@link File} object to use. Exclusive with {@code strm}
   * @param name The name the object should have on the server
   * @param contentType The content-type
   * @return The publicly-accessible URL for the uploaded file.
   * @throws IOException
   */
  public URL uploadFile(InputStream strm,
                        File f,
                        String name,
                        String contentType) throws IOException {

    S3Object object = new S3Object();
    if (strm != null) {
      object.setDataInputStream(strm);
    } else {
      object.setDataInputFile(f);
      object.setContentLength(f.length());
    }

    object.setName(name);
    object.setAcl(AccessControlList.REST_CANNED_PUBLIC_READ);
    object.setContentType(contentType);

    try {
      svc.putObject(bktName, object);
      return new URL("http://" + bktName + ".s3.amazonaws.com/" + name);
    } catch (S3ServiceException ex) {
      throw new IOException(ex);
    }
  }

  @SuppressWarnings("unused")
  public URL uploadFile(InputStream strm, String name) throws IOException {
    return uploadFile(strm, null, name, "text/plain");
  }

  @SuppressWarnings("unused")
  public URL uploadFile(File f, String ctype) throws IOException {
    return uploadFile(null, f, f.getName(), ctype);
  }
}
