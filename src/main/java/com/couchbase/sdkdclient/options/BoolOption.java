/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.couchbase.sdkdclient.options;

/**
 * Option which coerces its argument into a boolean.
 */
public class BoolOption extends GenericOption<Boolean> {

  public BoolOption(String ll, String desc, Boolean dfl) {
    super(ll, desc, dfl ? "true" : "false");
    setAttribute(OptionAttribute.SWITCHARG, true);
  }

  public BoolOption(String longname) {
    super(longname);
    setRawDefault("false");
    setAttribute(OptionAttribute.SWITCHARG, true);
  }

  @Override
  public Boolean coerce(String input) {
    if (input == null) {
      // No argument:
      return true;
    }

    input = input.toLowerCase();
    if (input.equals("0") || input.equals("false") || input.equals("off")) {
      return false;
    } else if (input.equals("1") || input.equals("true") || input.equals("on")) {
      return true;
    } else {
      throw new IllegalArgumentException("Bad value for boolean " + input);
    }
  }

  @Override
  public String getCurrentRawValue() {
    if (wasProvidedValue() || wasPassed() == false) {
      return super.getCurrentRawValue();
    }

    // wasPassed
    return "true";
  }

  @Override
  void setFound() {
    super.setFound();
    innerValue = true;
  }

  @Override
  protected String getDefaultArgname() {
    return "BOOLEAN";
  }


}