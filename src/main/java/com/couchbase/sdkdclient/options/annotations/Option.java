/*
 * Copyright (c) 2013 Couchbase, Inc.
 */

package com.couchbase.sdkdclient.options.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Option {
  public String name();
  public String argName() default "";
  public String help() default "";
  public String defl() default "";
  public boolean required() default false;
  public String[] aliases() default {};
  public String[] shortAliases() default {};
}
