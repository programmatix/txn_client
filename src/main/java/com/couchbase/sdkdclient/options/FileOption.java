/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.couchbase.sdkdclient.options;

import java.io.File;

public class FileOption extends GenericOption<File> {

  /**
   * A Policy is a constraint on the file path provided.
   */
  public enum Policy {

    /**
     * The file selected <i>must not</i> already exist. Use this if you wish
     * to create a new file
     */
    NEW,

    /**
     * The file selected <i>must</i> already exist. Use this if you wish to
     * read an existing file.
     */
    EXISTING,

    /**
     * No checks are performed.
     */
    ANY
  }

  final private Policy policy;

  @Override
  protected File coerce(String input) {
    if (input == null) {
      return null;
    }

    File f = new File(input);
    switch (policy) {
      case ANY:
        return f;
      case NEW:
        if (f.exists()) {
          throw new IllegalStateException("File " + input + " already exists");
        }
        return f;
      case EXISTING:
        if (!f.exists()) {
          throw new IllegalStateException("File " + input + " does not exist");
        }
        return f;
      default:
        throw new IllegalArgumentException("eh?");
    }
  }

  /**
   * Constructs a new FileOption
   * @param longname The option name
   * @param constraints Constraints on the state of the path selected.
   */
  public FileOption(String longname, Policy constraints) {
    super(longname);
    policy = constraints;
  }

  /**
   * Constructs a new file option with the {@link Policy#ANY} policy.
   * @param longname
   */
  public FileOption(String longname) {
    this(longname, Policy.ANY);
  }

  /**
   * Convenience method to get the underlying {@link File} object's path
   * as a String.
   * @return The path.
   */
  public String getPath() {
    return getValue().toString();
  }

  @Override
  protected String getDefaultArgname() {
    switch (policy) {
      case ANY:
        return "PATH";
      case EXISTING:
        return "EXISTING_PATH";
      case NEW:
        return "NEW_PATH";
      default:
        return super.getDefaultArgname();
    }
  }


}
