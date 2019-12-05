/*
 * Copyright (c) 2013 Couchbase, Inc.
 */

package com.couchbase.sdkdclient.options;
public class MissingArgumentException extends OptionException {
  public MissingArgumentException(RawOption o) {
    super("Required option '" + o.getName() + "' not found");
  }
  public MissingArgumentException(OptionInfo info) {
    super("Required option '" + HelpUtils.prettyOption(info) + "' not found");
  }
}
