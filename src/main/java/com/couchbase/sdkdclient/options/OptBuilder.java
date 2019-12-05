package com.couchbase.sdkdclient.options;

import com.couchbase.sdkdclient.options.RawOption.OptionAttribute;

/**
 * Builder class to construct an option
 * @param <T> The option class to build.
 */
public class OptBuilder<T extends RawOption> {

  private final T inner;

  private OptBuilder(T obj) {
    inner = obj;
  }

  /**
   * Indicate that this option is required.
   * @see {@link com.couchbase.sdkdclient.options.RawOption.OptionAttribute#REQUIRED}  }
   */
  public OptBuilder<T> required() {
    inner.setAttribute(OptionAttribute.REQUIRED, true);
    return this;
  }

  /**
   * @param c The canonical short name.
   * @see {@link RawOption#setShortName(String)}  }
   */
  public OptBuilder<T> shortName(String c) {
    inner.setShortName(c);
    return this;
  }

  /**
   * @param s The default value for the option
   * @see {@link RawOption#setRawDefault(String)}  }.
   *
   * Note that this argument is always a String. It will be coerced depending
   * on the option type.
   */
  public OptBuilder<T> defl(String s) {
    inner.setRawDefault(s);
    return this;
  }

  /**
   * The description string for ths option
   * @param s The description text.
   * @see {@link RawOption#setDescription(String)}  }
   */
  public OptBuilder<T> help(String s) {
    inner.setDescription(s);
    return this;
  }

  /**
   * Sets this option to be hidden.
   * @see {@link com.couchbase.sdkdclient.options.RawOption.OptionAttribute#HIDDEN }
   */
  public OptBuilder<T> hidden() {
    inner.setAttribute(OptionAttribute.HIDDEN, true);
    return this;
  }

  /**
   * Adds a long alias. This may be specified multiple times
   * @param s The long alias to add
   * @see {@link RawOption#addLongAlias(String)}  }
   */
  public OptBuilder<T> alias(String s) {
    inner.addLongAlias(s);
    return this;
  }

  public OptBuilder<T> globalAlias(String s) {
    inner.addAbsoluteAlias(s);
    return this;
  }

  public OptBuilder<T> argName(String s) {
    inner.setArgName(s);
    return this;
  }

  /**
   * Adds a short alias. This may be specified multiple times
   * @param s The short alias to add
   * @see {@link RawOption#addShortAlias(String)}  }
   */
  public OptBuilder<T> shortAlias(String s) {
    inner.addShortAlias(s);
    return this;
  }

  public OptBuilder<T> subsystem(String s) {
    inner.setSubsystem(s);
    return this;
  }

  public OptBuilder<T> aliases(String... al) {
    for (String s : al) {
      if (s.length() == 1) {
        inner.addShortAlias(s);
      } else {
        inner.addAbsoluteAlias(s);
      }
    }
    return this;
  }

  /**
   * Constructs the option. This should be the last call in the method
   * chain.
   * @return The built option.
   */
  public T build() {
    return inner;
  }

  public static <U extends RawOption> OptBuilder<U> start(U obj) {
    return new OptBuilder<U>(obj);
  }

  public static OptBuilder<StringOption> startString(String optname) {
    return new OptBuilder<StringOption>(new StringOption(optname));
  }

  public static OptBuilder<IntOption> startInt(String optname) {
    return new OptBuilder<IntOption>(new IntOption(optname));
  }

  public static OptBuilder<BoolOption> startBool(String optname) {
    return new OptBuilder<BoolOption>(new BoolOption(optname));
  }

  public static OptBuilder<MultiOption> startMulti(String optname) {
    return new OptBuilder<MultiOption>(new MultiOption(optname));
  }

  public static <U extends Enum<U>> OptBuilder<EnumOption<U>> start(String optname, Class<U> cls) {
    return new OptBuilder<EnumOption<U>>(new EnumOption<U>(optname, cls));
  }

  public static OptBuilder<FileOption> startExistingFile(String longname) {
    return new OptBuilder<FileOption>(new FileOption(longname, FileOption.Policy.EXISTING));
  }

  public static OptBuilder<FileOption> startNewFile(String longname) {
    return new OptBuilder<FileOption>(new FileOption(longname,
                                                     FileOption.Policy.NEW));
  }

  public static OptBuilder<FileOption> startFile(String longname) {
    return new OptBuilder<FileOption>(new FileOption(longname));
  }

}