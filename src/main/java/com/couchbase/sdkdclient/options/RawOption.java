/*
 * Copyright (c) 2013 Couchbase, Inc.
 */

package com.couchbase.sdkdclient.options;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * A class representing the common subset of functionality used by the Options
 * class.
 *
 * <h1>USAGE AND RATIONALE</h1>
 *
 * <h2>Parsing</h2>
 *
 * The user provided command line consists of simple options, without a complex
 * syntax; a typical commandline would look like this:
 * {@code
 *  [ program....] -c foo -b \
 *            --foo_username some_user --bar_timeout some_timeout \
 *            -W baz \
 *            --baz_debug
 * }
 *
 * The main entry point operates by loading a bunch of plugins, which are
 * specified as the '-c', '-b', and '-W' options. Each of these plugins define
 * their own command line options.
 *
 * Parsing occurs in several sweeps. First there are the top-level options which
 * specify the actual plugins to be loaded. These are processed in the first
 * pass. The parser initially scans the command line, searching <b>only</b>
 * for its top-level options, and placing any unrecognized options into a
 * different 'unknown' list.
 *
 * For each loaded plugin, the parser will do the following:
 *
 * <ol>
 * <li>Grab the option declarations from the plugin</li>
 * <li>Scan the remaining command line for any plugin-specific options</li>
 * <li>Remove the recognized options from the remaining commandline</li>
 * </ol>
 *
 * As such, the 'remaining' list is gradually trimmed by each plugin which
 * 'selects' only its options (conventionally, plugins will try to prefix their
 * options with something unique so it doesn't conflict with other plugin
 * options).
 *
 * Once all plugins have been parsed, the 'remaining' array should be empty. If
 * it's not empty, an error is displayed to the user.
 *
 * <h2>Option Definitions</h2>
 *
 * Option definitions are provided by each plugin. The way this normally works
 * is that a plugin exports a given type which is instantiated by the consumer
 * when loaded. The instance is then queried for its options by calling e.g.
 * {@link com.couchbase.sdkdclient.options.OptionConsumer#getOptionTree()} () }
 * which returns an {@link OptionTree} object.
 *
 * Because some plugins inherit from other plugins, the subclassed plugin may
 * wish to utilize the same options as the parent plugin, but perhaps modify
 * properties of individual options (for example, change the default of a
 * specific option, or force a specific option to be explicitly passed). To this
 * end, each individual option is addressed as a proper object. For this reason
 * we don't use JCommander, as JCommander does not export an explicit Options
 * object, but rather relies on annotations to modify existing fields.
 *
 * It's important to note that this options object is <b>never</b>
 * copied, though it must be processed by an {@link  OptionTree} object.
 */
public abstract class RawOption {
  public final static String DEFAULT_SUBSYSTEM = "GENERIC";
  final static int F_FOUND = 0x01;
  final static int F_HASARG = 0x02;
  final static int F_SEALED = 0x04;

  private final String name;
  private String description;
  private String defaultValue = null;
  private String shortName = null;
  private String value = null;
  private String subsystem = DEFAULT_SUBSYSTEM;
  private String argName = null;

  private final Set<String> longAliases = new HashSet<String>();
  private final Set<String> shortAliases = new HashSet<String>();
  private final Set<String> absAliases = new HashSet<String>();

  public enum OptionAttribute {
    /** Value is hidden. It will not appear in the help text */
    HIDDEN,

    /**
     * Value is disabled (implies {@link #HIDDEN}). An exception will be thrown
     *
     */
    DISABLED,

    /**
     * Option is a switch. It does not need to have a value
     */
    SWITCHARG,

    /**
     * Option value must be present when processing is complete.
     */
    REQUIRED
  }

  final Set<OptionAttribute> attributes = new HashSet<OptionAttribute>();
  private int flags = 0;

  public static final String DELIMITER = ".";
  final static Pattern xfrmPattern = Pattern.compile("[-_\\.-]");

  static public String transformName(String name) {
    return xfrmPattern.matcher(name).replaceAll(DELIMITER).toLowerCase();
  }

  /**
   * Constructs a new RawOption class
   * @param name The name of the option. This is a unique and <i>canonical</i>
   * name by which the option will be known. The format of the name is no
   * restrictions other than it may not begin with a hyphen (<code>-</code>).
   *
   * The name is then considered to be one of the command aliases, see
   * {@link #addLongAlias(String)} } for more details.
   *
   * @param description The description for the option. This should be a human readable
   * string.
   *
   * @param defaultValue The default value for the option.
   */
  public RawOption(String name, String description, String defaultValue) {
    this.defaultValue = defaultValue;
    this.name = transformName(name);
    this.description = description;
  }

  public RawOption(String name, String description) {
    defaultValue = "";
    this.name = transformName(name);
    this.description = description;
  }

  public RawOption(String name) {
    this.name = transformName(name);
  }

  final void setRawValue(String value) {
    this.value = value;
    parse(this.value);
  }

  public String getCurrentRawValue() {
    if (value == null) {
      return defaultValue;
    }
    return value;
  }

  boolean isMultiOption() {
    return false;
  }

  public void setAttribute(OptionAttribute attr, boolean value) {
    if (value) {
      attributes.add(attr);
    } else {
      attributes.remove(attr);
    }
  }

  public boolean getAttribute(OptionAttribute attr) {
    return attributes.contains(attr);
  }

  /**
   * Set the human-readable description explaining the meaning of this option
   * @param description
   */
  public void setDescription(String description) {
    this.description = description;
  }

  public void setArgName(String argName) {
    this.argName = argName;
  }

  /**
   * @return The canonical name of this option.
   */
  public String getName() {
    return name;
  }

  /**
   * gets the canonical 'short' name for this option. The canonical short name
   * @return The character for the canonical short alias, or null if there is
   * no short alias.
   */
  public String getShortName() {
    return shortName;
  }

  /**
   * Adds a short alias by which this option may be addressed. A short alias
   * is a single character and may be specified on the command line as
   * <code>-<i>c</i></code> where <i>c</i> is the short alias.
   * @param alias
   */
  public void addShortAlias(String alias) {
    shortAliases.add(alias);
  }

  /**
   * Adds a long alias by which the option may be addressed. A long alias
   * may be a string of one character or longer. The alias may be addressed
   * from the commandline using the <code>--<i>alias</i></code> syntax.
   * @param alias
   */
  public void addLongAlias(String alias) {
    longAliases.add(transformName(alias));
  }

  /**
   * Adds an absolute alias. While normal long aliases are prefixed
   * by a possible {@link OptionPrefix}, absolute aliases are not and are
   * simply a global alias lookup. As a result, they canot be found in the
   * tree either.
   * @param alias The alias to add
   */
  public void addAbsoluteAlias(String alias) {
    absAliases.add(alias);
  }


  /**
   * @return A collection of all the long aliases this option recognizes
   */
  public Collection<String> getLongAliases() {
    return longAliases;
  }

  /**
   * @return A collection of all the short aliases this option recognizes.
   */
  public Collection<String> getShortAliases() {
    return shortAliases;
  }

  public Collection<String> getAbsoluteAliases() {
    return absAliases;
  }

  /**
   * Sets the 'subsystem' of the option. A "Subsystem" is an identifier
   * showing the "Kind" of option. While an option may be part of an
   * OptionCollection showing the grouping of the option, the subsystem
   * is explicitly set.
   *
   * @param subsystem The subsystem string to assign.
   */
  public void setSubsystem(String subsystem) {
    this.subsystem = subsystem;
  }

  public String getSubsystem() {
    return subsystem;
  }

  public boolean hasCustomSubsystem() {
    return subsystem.equals(DEFAULT_SUBSYSTEM) == false;
  }


  /**
   * Set the raw string value which will act as the default if a suitable value
   * was not supplied on the command line
   * @param defaultValue The default value to use.
   */
  public void setRawDefault(String defaultValue) {
    this.defaultValue = defaultValue;
  }

  /**
   * Set the short one-character version of this option. This is only relevant
   * for command-line options so they may be specified as {@code -o} rather
   * than {@code --option}
   * @param shortName The short name to use
   */
  public void setShortName(String shortName) {
    this.shortName = shortName;
  }

  protected final void ensureEnabled() {
    if (attributes.contains(OptionAttribute.DISABLED)) {
      throw new IllegalStateException("Cannot modify disabled option");
    }
  }

  /**
   * Indicates that this option was found on the command line
   */
  void setFound() {
    ensureEnabled();
    flags |= F_FOUND;
  }

  void setArgFound() {
    ensureEnabled();
    flags |= F_HASARG | F_FOUND;
  }

  /**
   * Seals the option to indicate that a value is now available.
   */
  void seal() {
    flags |= F_SEALED;
    if (wasPassed() == false && isMultiOption() == false) {
      ensureEnabled();
      parse(defaultValue);
    }
  }

  /**
   * Ensure an option was passed
   */
  public void checkRequired() {
    seal();
    if (wasPassed()) {
      return;
    }
    if (!getAttribute(OptionAttribute.REQUIRED)) {
      return;
    }
    throw new MissingArgumentException(this);
  }

  /**
   * Whether this option is <i>sealed</i> or not. In order to prevent accidental
   * overwriting of configurations and to ensure multi-pass parsing, an option
   * may be <i>sealed</i> or <i>unsealed</i>.
   *
   * An <i>unsealed</i> option is one which is still open to configuration
   * changes and may be changed. Once an option is sealed it can no longer
   * be modified.
   *
   * Conversely, trying to read a value from an unsealed option will raise
   * an {@link IllegalStateException} because the option may yet change values
   * before it is sealed.
   * @return true if sealed, false otherwise.
   */
  public boolean isSealed() {
    return (flags & F_SEALED) != 0;
  }

  /**
   * Check whether the value for this option was found on the command line.
   * Note that if {@link #mustHaveArgument() } is false, this does not
   * indicate that this option has a value, and {@link #wasProvidedValue() }
   * should be used.
   *
   * @return true if this option was found, false otherwise
   */
  public boolean wasPassed() {
    return (flags & F_FOUND) != 0;
  }

  /**
   * Like {@link #wasPassed() } but also indicates whether a value for this
   * option was specified on the commandline. It is possible for a
   * <i>switch</i> option to have {@link #wasPassed() } to be true while this
   * method would return false
   *
   * @return Whether this option was provided a value or not.
   */
  public boolean wasProvidedValue() {
    return (flags & F_HASARG) != 0;
  }

  /**
   * @return true if this option must have an argument, i.e. it is not a boolean.
   */
  public boolean mustHaveArgument() {
    return !getAttribute(OptionAttribute.SWITCHARG);
  }

  /**
   * Get the textual description or help text of this item
   * @return The description
   */
  public String getDescription() {
    return description;
  }

  /**
   * Gets the argument name. This is for description only and shows what kind
   * of argument is expected.
   * @return The argument name. If the argument name was not set, this will
   * be the argument name in {@code UPPER_CASE}
   */
  public String getArgName() {
    if (argName != null) {
      return argName;
    }

    return getDefaultArgname();
  }

  /**
   * @see #getArgName()
   * @return The argument name.
   */
  protected String getDefaultArgname() {
    return name.toUpperCase();
  }


  /**
   * This is a hook to be used whenever a value is set. This may be called with
   * a value from the commandline, or a default value
   *
   * @param input The value to convert
   */
  public abstract void parse(String input);

  /**
   * Resets the value state.
   */
  public void reset() {
    flags = 0;
    value = null;
  }

  @Override
  public String toString() {
    return "Option: " + getName();
  }
}