/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.couchbase.sdkdclient.options;

/**
 * Option subclass encapsulating a string.
 */
public class StringOption extends GenericOption<String> {

  public StringOption(String longname, String desc, String dfl) {
    super(longname, desc, dfl);
  }

  public StringOption(String longname, String desc) {
    super(longname, desc, "");
  }

  public StringOption(String longname) {
    super(longname);
  }

  @Override
  protected String coerce(String input) {
    return input;
  }
}
