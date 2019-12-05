/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.couchbase.sdkdclient.rundb;

import com.couchbase.sdkdclient.logging.LogUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 */
public class DatabasePath {
  final static Logger logger = LogUtil.getLogger(DatabasePath.class);
  public final static String H2_SUFFIX = ".h2.db";
  // User provided 'File' object
  final private File userInput;

  // Path of the database on the filesystem, may be different from input
  final private File fsPath;

  // Database connection string
  final private String connectionPath;

  // Database format
  final private RunDB.Format format;

  final static Pattern ptrnH2 =
          Pattern.compile(Pattern.quote(H2_SUFFIX)+'$',
                          Pattern.CASE_INSENSITIVE);

  /**
   * Get the {@link File} object provided by the user.
   * @return
   */
  public File getInput() {
    return userInput;
  }

  /**
   * Get the JDBC connection string
   * @return The connection string to be used for JDBC
   */
  public String getDbPath() {
    return connectionPath;
  }

  /**
   * Get the format of the database. This is the file format and also directs
   * behavior regarding features such as filenaming and compression.
   * @return the format
   */
  public RunDB.Format getFormat() {
    return format;
  }

  /**
   * Get the path of the database file as it exists on the local filesystem
   * @return The path
   */
  public File getFilesystemPath() {
    return fsPath;
  }

  public File compressH2() throws IOException {
    File outfile = new File(connectionPath + ".zip");
    ZipOutputStream zout = new ZipOutputStream(new FileOutputStream(outfile));
    zout.setLevel(9);
    zout.putNextEntry(new ZipEntry(fsPath.getName()));
    IOUtils.copy(new FileInputStream(fsPath), zout);
    zout.close();
    return outfile;
  }


  private String getCompressedDbName(File zipfile) throws IOException {
    ZipFile zf = new ZipFile(zipfile, ZipFile.OPEN_READ);
    String ret = null;
    try {
      Enumeration<? extends ZipEntry> entries = zf.entries();
      while (entries.hasMoreElements()) {
        ZipEntry entry = entries.nextElement();
        if (entry.getName().endsWith(H2_SUFFIX)) {
          ret = entry.getName();
          break;
        }
      }
    } finally {
      zf.close();
    }
    if (ret == null) {
      throw new IOException("No database found within zipfile");
    }
    Matcher mh2 = ptrnH2.matcher(ret);
    if (mh2.find()) {
      ret = mh2.replaceAll("");
    }
    return ret;
  }


  /**
   * Open the database path with heuristically determining if it exists.
   * @param f
   */
  private DatabasePath(File f) {
    userInput = f;
    String fStr = f.toString();

    if (f.exists()) {
      fsPath = f;
      Matcher mh2 = ptrnH2.matcher(fStr);
      if (mh2.find()) {

        // H2 absolute path
        connectionPath = mh2.replaceAll("");
        format = RunDB.Format.H2;

      } else if (FilenameUtils.isExtension(fsPath.toString(), "zip")) {
        // H2 zip format
        format = RunDB.Format.H2;

        try {
          connectionPath = String.format("zip:%s!/%s",
                                         fsPath.getAbsolutePath(),
                                         getCompressedDbName(fsPath));
        } catch (IOException ex) {
          throw new RuntimeException("Not a valid compressed path");
        }

        logger.trace(connectionPath);

      } else {
        format = RunDB.Format.SQLITE;
        connectionPath = fStr;
      }
    } else {

      // Does not exist.. do we have an H2?
      File h2File = new File(fStr + H2_SUFFIX);
      if (h2File.exists()) {
        fsPath = h2File;
        connectionPath = fStr;
        format = RunDB.Format.H2;

      } else {
        fsPath = userInput;
        connectionPath = null;
        format = RunDB.Format.SQLITE;
      }
    }
  }

  /**
   * Create a new file, with the given format.
   */
  private DatabasePath(File f, RunDB.Format fmt) {
    userInput = f;
    format = fmt;
    Matcher h2Matcher = ptrnH2.matcher(f.toString());
    if (fmt == RunDB.Format.H2) {
      if (h2Matcher.find()) {
        // Ok..
        fsPath = f;
        connectionPath = h2Matcher.replaceAll("");
      } else {
        fsPath = new File(f.toString() + H2_SUFFIX);
        connectionPath = userInput.toString();
      }
    } else {
      fsPath = userInput;
      connectionPath = fsPath.toString();
    }
  }

  private DatabasePath(RunDB.Format fmt) {
    userInput = null;
    format = fmt;
    fsPath = null;
    if (fmt == RunDB.Format.H2) {
      connectionPath = "mem:";
    } else {
      connectionPath = ":memory:";
    }
  }

  public static DatabasePath createNew(File pth, RunDB.Format fmt, boolean overwrite)
          throws IOException {
    DatabasePath obj = new DatabasePath(pth, fmt);
    if (obj.fsPath.exists()) {
      if (!overwrite) {
        throw new IOException("File already exists: " + obj.fsPath);
      } else {
        FileUtils.deleteQuietly(obj.fsPath);
      }
    }
    return obj;
  }

  /**
   * Creates a database path for memory.
   * @param fmt
   * @return a new DatabasePath object.
   */
  public static DatabasePath createMemory(RunDB.Format fmt) {
    return new DatabasePath(fmt);
  }

  public static DatabasePath createExisting(File pth) throws IOException {
    DatabasePath obj = new DatabasePath(pth);
    if (!obj.fsPath.exists()) {
      throw new FileNotFoundException(obj.fsPath.toString());
    }
    return obj;
  }

  public static DatabasePath createExisting(String str) throws IOException {
    return createExisting(new File(str));
  }
}
