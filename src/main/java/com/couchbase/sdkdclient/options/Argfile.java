/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.couchbase.sdkdclient.options;

import com.couchbase.sdkdclient.options.ini.IniParser;
import com.couchbase.sdkdclient.options.ini.IniSection;
import com.couchbase.sdkdclient.util.StringArgv;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An argfile is a configuration file that contains command line arguments. The
 * syntax follows normal (Unix) command line conventions. Whitespace is
 * considered a delimiter (unless quoted) and backslashes may be used to escape
 * newlines.
 *
 * Argfiles may include other argfiles; the inclusion pattern can be relative to
 * the current file, or an absolute path (which follows the conventions of the C
 * preprocessor).
 *
 * Argfiles may come in two formats:
 *
 * <ol>
 *   <li>
 *     "old"-style argiles which are simply snippets of command line options.
 *     Thus, an argfile with the contents of:
 *     <pre>{@code
 *       --foo foo_value \
 *       --bar bar_value \
 *       --baz baz_value
 *     }</pre>
 *     would be equivalent to passing {@code --foo foo_value --bar bar_value --baz baz_value}
 *     on the commandline.
 *   </li>
 *   <li>
 *     New-style argfiles. These follow the conventions of the <i>INI</i> file
 *     and do not require a leading {@code --} for options.
 *
 *     New-style argfiles require that the first line be the text {@code #ARGFILE:INI}
 *     and that the file be aware of the various {@link OptionPrefix}es for each
 *     individual option.
 *
 *     Thus, an argfile with the contents of :
 *     <pre>{@code
 *     #ARGFILE:INI
 *     [cluster]
 *     ssh-disable=true
 *     no-init=true
 *     username=user
 *     password=secret
 *
 *     [bucket]
 *     ram=256
 *     type=memcached
 *     }
 *     </pre>
 *
 *     Is equivalent to passing:
 *     {@code --cluster-ssh-disable --cluster-no-init --cluster-username user --cluster-password secret --bucket-ram 256 --bucket-type memcached}
 *   </li>
 * </ol>
 *
 * As seen, the <i>INI</i> format is far more concise for multiple options as
 * it eliminates the need to repeat the prefix multiple times.
 *
 * The syntax for both sections is very similar in that the <i>INI</i> syntax
 * is just sugar: For every ini option, the containing section name is prefixed
 * to the option during processing.
 */
public class Argfile {
  private ArrayList<String> argv = new ArrayList<String>();
  private final static Logger logger = LoggerFactory.getLogger(Argfile.class);

  /**
   * Returns the array of parsed argfiles
   *
   * @return A string array to be parsed
   */
  public String[] getArgv() {
    return argv.toArray(new String[argv.size()]);
  }

  private static File locateArgfile(File relPath, File path) {
    if (relPath == null) {
      return path;
    }

    File combined = new File(relPath, path.toString());
    if (combined.exists()) {
      return combined;
    }
    return path;
  }

  private void maybeRecurse(List<String> curArgv, File curPath) throws IOException {
    StringOption optInclude = new StringOption("include");
    optInclude.setShortName("-I");

    OptionLookup lu = new OptionLookup();
    OptionTree coll = new OptionTree(OptionPrefix.ROOT);
    lu.addGroup(coll);

    coll.addOption(optInclude);
    OptionParser parser = new OptionParser(curArgv);
    parser.addTarget(coll);
    parser.apply();

    if (!optInclude.wasPassed()) {
      return;
    }

    File nextFile = new File(optInclude.getValue());
    nextFile = locateArgfile(curPath, nextFile);

    Argfile obj = new Argfile(nextFile);
    argv.addAll(obj.argv);
  }

  /**
   * Parse a single argfile buffer
   *
   * @param curCmdline The raw string to parse
   * @param curPath The path of the current file being parsed (used to resolve
   * recursive includes)
   * @throws FileNotFoundException
   * @throws IOException
   */
  private void parse(String curCmdline, File curPath) throws IOException {
    List<String> curArgv = createArgv(curCmdline);
    argv.addAll(curArgv);
    maybeRecurse(curArgv, curPath);
  }

  /**
   * @param path The path to open as the entry point
   * @throws FileNotFoundException
   * @throws IOException
   */
  public Argfile(File path) throws IOException {
    String s = FileUtils.readFileToString(path);
    parse(s, path);
  }

  /**
   * @param buf An argfile buffer
   * @throws FileNotFoundException
   * @throws IOException
   */
  public Argfile(String buf) throws IOException {
    parse(buf, null);
  }
  /**
   * Flattens multiple lines of an argfile into a single line
   *
   * @param buf
   * @return
   */
  static private final String eolRegex =  "\\\\" + "\\s*" + "$";

  private static void scanPrefs(IniSection section, List<String> args, String prefix)
          throws IOException {

    for (Entry<String,String> ent : section.entrySet()) {
      String arg = String.format("--%s%s=%s", prefix, ent.getKey(), ent.getValue());
      args.add(arg);
    }
  }

  private static List<String> preparseIni(String buf) throws IOException {
    // Convert from INI format to a single command line
    List<String> ll = new ArrayList<String>(100);
    IniParser parser = IniParser.create(buf);
    parser.parse();
    if (parser.hasSection("main")) {
      IniSection mainPrefs = parser.getSection("main");
      scanPrefs(mainPrefs, ll, "");
    }

    for (String name : parser.getSectionNames()) {
      if (name.equals("main")) {
        continue;
      }
      IniSection section = parser.getSection(name);
      scanPrefs(section, ll, name + "_");
    }
    return ll;
  }

  private final static Pattern ptrnIniHeader = Pattern.compile("\\s*[#;]\\s*ARGFILE\\s*:\\s*INI\\s*");
  private final static Pattern ptrnIniSection = Pattern.compile("^\\s*\\[");
  private enum ArgfileType { COMPAT, INI }


  private static List<String> argFromCompatArgfile(String[] lines) {
    // Otherwise, parse compat.
    StringBuilder sb = new StringBuilder();
    for (String line : lines) {
      if (line.startsWith("#")) {
        // Comment
        continue;
      }

      line = line.replaceAll(eolRegex, " ");
      sb.append(line).append(' ');
    }

    logger.debug("Normalized argfile as {}", sb);
    return new StringArgv().parse(sb.toString()).getList();
  }

  /**
   * Converts a multi-line argfile into a set of tokens. Handles both old and
   * new-style argument files.
   *
   * @param buf The raw buffer to parse
   * @return A list of tokens.
   */
  private static List<String> createArgv(String buf) {
    String[] lines = buf.split("\n");
    if (lines.length == 0) {
      return Collections.emptyList();
    }

    ArgfileType type = null;
    String header = lines[0];

    Matcher headerMatcher = ptrnIniHeader.matcher(header);
    if (headerMatcher.matches()) {
      type = ArgfileType.INI;
    } else {
      for (String line : lines) {
        Matcher sectionMatcher = ptrnIniSection.matcher(line);
        if (sectionMatcher.find()) {
          type = ArgfileType.INI;
          break;
        }
      }
    }
    if (type == ArgfileType.INI) {
      try {
        return preparseIni(buf);
      } catch (IOException ex) {
        throw new RuntimeException(ex);
      }
    } else {
      return argFromCompatArgfile(lines);
    }
  }
}
