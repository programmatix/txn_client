/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.couchbase.sdkdclient.options.ini;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Simple INI Parser.
 * @author mnunberg
 */
public class IniParser {
  private IniSection globalSection = new IniSection();
  private final Map<String,IniSection> sections = new HashMap<String, IniSection>();
  private final InputStream strm;

  private IniParser(InputStream ss) {
    strm = ss;
  }

  public static IniParser create(File f) throws IOException {
    return new IniParser(new FileInputStream(f));
  }

  public static IniParser create(String s) {
    return new IniParser(new ByteArrayInputStream(s.getBytes()));
  }

  static private final Pattern sectionPattern = Pattern.compile("\\[([^]]+)\\]");

  public void parse() throws IOException {
    BufferedReader rdr = new BufferedReader(new InputStreamReader(strm));
    IniSection currentSection = globalSection;

    String ln;
    while ( (ln = rdr.readLine()) != null) {
      // Strip leading whitespace
      ln = ln.replaceAll("^\\s*", "");

      // Strip trailing whitespace
      ln = ln.replaceAll("\\s*$", "");


      if (ln.startsWith(";") || ln.startsWith("#") || ln.isEmpty()) {
        // Comment
        continue;
      }

      Matcher matcher = sectionPattern.matcher(ln);
      if (matcher.matches()) {
        String sectionName = matcher.group(1);
        currentSection.seal();
        currentSection = new IniSection();
        sections.put(sectionName, currentSection);
        continue;
      }

      final String kv[] = ln.split("\\s*=\\s*", 2);
      if (kv.length != 2) {
        throw new IOException("Invalid key-value spec: " + ln + " Key " + kv[0]);
      }
      currentSection.kvPairs.add(new AbstractMap.SimpleImmutableEntry(kv[0], kv[1]));
    }

    currentSection.seal();
  }

  public IniSection getSection(String name) throws IOException {
    IniSection ret = sections.get(name);
    if (ret == null) {
      throw new IOException("No such section: " + name);
    }
    return ret;
  }

  public boolean hasSection(String name) {
    return sections.containsKey(name);
  }

  public Collection<String> getSectionNames() {
    return sections.keySet();
  }
}
