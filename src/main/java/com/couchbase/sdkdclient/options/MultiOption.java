/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.couchbase.sdkdclient.options;

import java.util.LinkedList;
import java.util.List;

/**
 * Option which adds each occurrence found into a list of strings.
 * To retrieve the values, use the {@link #getRawValues()}  } method.
 */
public class MultiOption extends RawOption {
  private final List<String> rawValues = new LinkedList<String>();

  public MultiOption(String longname) {
    super(longname);
  }

  @Override
  boolean isMultiOption() {
    return true;
  }

  @Override
  public void parse(String input) {
    ensureEnabled();
    rawValues.add(input);
  }

  /**
   * Get the raw values specified for this option.
   * @return A list of 0 or more strings.
   */
  public List<String> getRawValues() {
    return rawValues;
  }

  /**
   * Adds a value to the list of values.
   * @param val the value to add
   */
  public void addRawValue(String val) {
    ensureEnabled();
    rawValues.add(val);
  }

  /**
   * Add multiple values to the value list.
   * @param vals
   */
  public void addRawValues(String[] vals) {
    for (String val : vals) {
      addRawValue(val);
    }
  }
}