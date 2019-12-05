/*
 * Copyright (c) 2013 Couchbase, Inc.
 */

package com.couchbase.sdkdclient.options;

import com.couchbase.sdkdclient.logging.LogUtil;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class OptionTreeBuilder {

  public static class NoOptionsException extends IllegalArgumentException {
    NoOptionsException() {
      super("'source' parameter yielded no options. Refactored options?");
    }
  }

  private static class Source {
    final Object obj;
    final Class cls;
    Source(Object o, Class c) {
      obj = o;
      cls = c;
    }
  }

  private final static Logger logger = LogUtil.getLogger(OptionTreeBuilder.class);

  private String sGroup = null;
  private String description = null;
  private final List<Source> sources = new ArrayList<Source>();
  private OptionPrefix prefix = null;

  public OptionTreeBuilder() {}

  public OptionTreeBuilder(String groupName, String desc) {
    sGroup = groupName;
    description = desc;
  }



  public OptionTreeBuilder source(@Nonnull Object o, @Nonnull Class cls) {
    Source src = new Source(o, cls);
    sources.add(src);
    return this;
  }

  public OptionTreeBuilder description(@Nonnull String desc) {
    description = desc;
    return this;
  }

  public OptionTreeBuilder group(@Nonnull String grp) {
    sGroup = grp;
    return this;
  }

  public OptionTreeBuilder prefix(OptionPrefix pfx) {
    prefix = pfx;
    return this;
  }

  public OptionTree build() {
    if ((sGroup == null || sGroup.isEmpty()) || (description == null || description.isEmpty())) {
      throw new IllegalArgumentException("group and description cannot be null");
    }

    if (prefix == null) {
      logger.trace("{}: Applying ROOT prefix to empty prefix", sGroup);
      prefix = OptionPrefix.ROOT;
    }

    OptionTree ret = new OptionTree(null, prefix, sGroup, description);

    for (Source src : sources) {
      Collection<RawOption> coll = OptionTree.extract(src.obj, src.cls);
      if (coll.isEmpty()) {
        throw new NoOptionsException();
      }

      for (RawOption opt : coll) {
        ret.addOption(opt);
      }
    }
    return ret;
  }
}
