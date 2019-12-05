/*
 * Copyright (c) 2013 Couchbase, Inc.
 */

package com.couchbase.sdkdclient.options.annotations;

import com.couchbase.sdkdclient.options.BoolOption;
import com.couchbase.sdkdclient.options.EnumOption;
import com.couchbase.sdkdclient.options.FileOption;
import com.couchbase.sdkdclient.options.FileOption.Policy;
import com.couchbase.sdkdclient.options.IntOption;
import com.couchbase.sdkdclient.options.OptionPrefix;
import com.couchbase.sdkdclient.options.OptionTree;
import com.couchbase.sdkdclient.options.OptionTreeBuilder;
import com.couchbase.sdkdclient.options.RawOption;
import com.couchbase.sdkdclient.options.RawOption.OptionAttribute;
import com.couchbase.sdkdclient.options.StringOption;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class AnnotatedOptions {
  private AnnotatedOptions() {}

  private static class ActionContext {
    final Field field;
    final Object target;

    private ActionContext(Field field, Object obj) {
      this.field = field;
      target = obj;
    }

    void assign(Object src) {
      try {
        field.set(target, src);
      } catch (IllegalAccessException ex) {
        throw new RuntimeException(ex);
      }
    }

    void initDefault(RawOption option) {
      try {
        Object src = field.get(target);
        if (src != null) {
          option.setRawDefault(src.toString());
        }
      } catch (IllegalAccessException ex) {
        throw new RuntimeException(ex);
      }
    }
  }

  private static RawOption setupField(Field field,
                                      Object target) throws IllegalAccessException {
    if ((field.getModifiers() & Modifier.FINAL) != 0) {
      throw new IllegalArgumentException("Field cannot be final");
    }

    Option param = field.getAnnotation(Option.class);

    if (param == null) {
      return null;
    }

    RawOption curOption;
    Class fieldType = field.getType();
    final ActionContext aContext = new ActionContext(field, target);
    field.setAccessible(true);

    if (fieldType.isEnum()) {
      //noinspection unchecked
      curOption  = new EnumOption(param.name(), fieldType) {
        @Override protected void onChange() {
          aContext.assign(innerValue);
        }
      };

    } else if (File.class.isAssignableFrom(fieldType)) {
      FileParam fileOptions = field.getAnnotation(FileParam.class);
      Policy filePolicy = fileOptions == null ? Policy.ANY : fileOptions.policy();
      curOption = new FileOption(param.name(), filePolicy) {
        @Override protected void onChange() { aContext.assign(innerValue); }
      };

    } else if (Boolean.TYPE.isAssignableFrom(fieldType)) {
      curOption = new BoolOption(param.name()) {
        @Override protected void onChange() { aContext.assign(innerValue); }
      };

    } else if (Integer.TYPE.isAssignableFrom(fieldType)) {
      curOption = new IntOption(param.name()) {
        @Override protected void onChange() { aContext.assign(innerValue); }
      };

    } else if (String.class.isAssignableFrom(fieldType)) {
      curOption = new StringOption(param.name()) {
        @Override protected void onChange() { aContext.assign(innerValue); }
      };

    } else {
      throw new IllegalArgumentException("Unsupported option type: " + field.toString());
    }

    curOption.setDescription(param.help());
    curOption.setAttribute(OptionAttribute.REQUIRED, true);

    for (String s : param.shortAliases()) {
      curOption.addShortAlias(s);
    }

    for (String s : param.aliases()) {
      curOption.addLongAlias(s);
    }

    if (!param.argName().isEmpty()) {
      curOption.setArgName(param.argName());
    }

    aContext.initDefault(curOption);
    return curOption;
  }

  public static OptionTree createTree(final Object o,
                                      Class cls) throws IllegalAccessException {
    HasOptions containerProps =
            (HasOptions) cls.getAnnotation(HasOptions.class);

    if (containerProps == null) {
      throw new IllegalArgumentException(
              "Class must be annotated with @HasOptions");
    }

    OptionTreeBuilder treeBuilder = new OptionTreeBuilder();
    treeBuilder.group(containerProps.group());
    treeBuilder.description(containerProps.description());
    String[] prefixes = containerProps.prefix();
    if (prefixes != null) {
      treeBuilder.prefix(new OptionPrefix(prefixes));
    }

    OptionTree tree = treeBuilder.build();

    for (final Field field : cls.getDeclaredFields()) {
      RawOption option = setupField(field, o);
      if (option != null) {
        tree.addOption(option);
      }
    }

    return tree;
  }
}
