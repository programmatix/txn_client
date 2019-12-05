/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.couchbase.sdkdclient.options;

/**
 * Subclass of RawOption allowing generic types. You typically only need
 * to override the @link{coerce} method in order to use a custom option
 * with your type.
 *
 * @param <T> Inner type to use for options
 */
public abstract class GenericOption<T> extends RawOption {

  protected T innerValue = null;

  public GenericOption(String name, String description, String defaultValue) {
    super(name, description, defaultValue);
  }

  public GenericOption(String name) {
    super(name);
  }

  /**
   * Called whenever the value changes. You can inspect 'innerValue' to see.
   */
  protected void onChange() {
  }

  /**
   * Gets the coerced value
   *
   * @return The coerced value
   */
  public T getValue() {
    if (isSealed() == false && innerValue == null) {
      throw new IllegalStateException("Option not sealed");
    }
    return innerValue;
  }

  /**
   * Coerces the input string @code{s} into the return value.
   *
   * @param input The input string to coerce
   * @return the coerced value
   */
  protected abstract T coerce(String input);

  @Override
  public final void parse(String input) {
    try {
      innerValue = coerce(input);
      onChange();
    } catch (Exception exc) {
      throw new IllegalArgumentException("Error for option '" + getName() + "'", exc);
    }
  }

  @Override
  public final void reset() {
    super.reset();
    innerValue = null;
    onChange();
  }

  public void set(T value) {
    if (isSealed()) {
      throw new IllegalStateException("Tried to set a sealed option");
    }
    setFound();
    setArgFound();
    innerValue = value;
    onChange();
    seal();
  }

  @Override
  public void setRawDefault(String defaultValue) {
    coerce(defaultValue);
    super.setRawDefault(defaultValue);
  }
}