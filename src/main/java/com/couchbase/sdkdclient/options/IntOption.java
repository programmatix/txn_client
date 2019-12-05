/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.couchbase.sdkdclient.options;

/**
 * Option which coerces its value into an int.
 */
public class IntOption extends GenericOption<Integer> {

  @Override
  protected Integer coerce(String input) {
    return Integer.parseInt(input);
  }

  public IntOption(String name, String desc, Integer defaultValue) {
    super(name, desc, defaultValue.toString());
  }

  public IntOption(String longname, String desc) {
    super(longname, desc, "0");
  }

  public IntOption(String longname) {
    super(longname);
  }

  @Override
  protected String getDefaultArgname() {
    return "INT";
  }


}
