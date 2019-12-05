/*
 * Copyright (c) 2013 Couchbase, Inc.
 */

package com.couchbase.sdkdclient.util;

import java.io.File;
import java.io.IOException;

public class Symlinks {
  private Symlinks() {}

  /**
   * Creates a symbolic link {@code dst} pointing to {@code src}
   * @param src The file the link should target
   * @param dst The path of the link to create
   * @param removeExisting Whether to remove an existing link (if found)
   * @throws IOException
   */
  static public void createSymlink(File src,
                                   File dst,
                                   boolean removeExisting) throws IOException {
    String[] rmCmd = { "rm", "-f",
            dst.getAbsoluteFile().toString()
    };

    String[] lnCmd = { "ln", "-s",
            src.getAbsoluteFile().toString(),
            dst.getAbsoluteFile().toString()
    };
    Runtime runtime = Runtime.getRuntime();
    Process ps;
    try {
      if (removeExisting) {
        ps = runtime.exec(rmCmd);
        ps.waitFor();
        if (ps.exitValue() != 0) {
          throw new IOException("Couldn't remove existing file");
        }
      }

      ps = runtime.exec(lnCmd);
      ps.waitFor();
      if (ps.exitValue() != 0) {
        throw new IOException("Couldn't create symlink");
      }

    } catch (InterruptedException ex) {
      throw new IOException("Can't create symlink", ex);
    }
  }
}
