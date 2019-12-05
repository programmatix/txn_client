/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.couchbase.sdkdclient.options;

import org.apache.commons.lang.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Option which coerces its value into an enum constant
 * @param <T> The enum the value should be coerced into
 */
public class EnumOption<T extends Enum<T>> extends GenericOption<T> {
  private final Class<T> enm;
  private final Map<String,T> aliasMap = new HashMap<String, T>();

  /**
   * Constructs a new enum option
   * @param longname the canonical name for the option
   * @param cls The enum Class object.
   */
  public EnumOption(String longname, Class<T> cls) {
    super(longname);
    enm = cls;
  }

  @Override
  protected T coerce(String input) {
    T exists = aliasMap.get(input.toLowerCase());
    if (exists != null) {
      return exists;
    }

    input = input.toUpperCase();
    try{
      return Enum.valueOf(enm, input);
    }catch (Exception e)
    {
      System.out.println("Exception:"+e);
      System.exit(-1);
      return null;
    }

  }

  public EnumOption<T> addChoice(String k, T v) {
    aliasMap.put(k.toLowerCase(), v);
    return this;
  }

  @Override
  protected String getDefaultArgname() {
    return '(' + StringUtils.join(enm.getEnumConstants(), '|') + ')';
  }
}
