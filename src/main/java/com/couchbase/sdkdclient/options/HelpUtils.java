/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.couchbase.sdkdclient.options;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class HelpUtils {
  static final String INDENT = " ";
  static final int LINE_WIDTH = 74;
  static final int DESC_WIDTH = 40;
  static final int DESC_MARGIN = LINE_WIDTH - DESC_WIDTH;

  static List<String> getFormattedAliases(RawOption opt) {
    List<String> ll = new ArrayList<String>();
    for (String s : opt.getShortAliases()) {
      ll.add("-"+s);
    }

    for (String s : opt.getLongAliases()) {
      ll.add("--" + s);
    }
    return ll;
  }

  static void dumpSingleOption(PrintWriter pw, OptionInfo info) {
    StringBuilder sb = new StringBuilder();
    pw.write(INDENT);
    RawOption opt = info.getOption();
    String name = prettyOption(info);

    // Dump short name first, if it exists
    if (opt.getShortName() != null) {
      sb.append("-").append(opt.getShortName());
    }

    // Dump long name
    if (name != null) {
      if (opt.getShortName() != null) {
        sb.append(",");
      }
      sb.append(name);
    }

    // The beginning margin
    if (sb.length() < DESC_MARGIN) {
      while (sb.length() < DESC_MARGIN - 1) {
        sb.append(" ");
      }
    } else {
      sb.append(" ");
    }

    StringBuilder descSb = new StringBuilder();
    String description = opt.getDescription();

    if (description == null) {
      description = "";
    }

    descSb.append(prettyArgname(info)).append(". ");
    descSb.append(description);
    if (opt.getCurrentRawValue() != null) {
      descSb.append(String.format(" [default=%s]", opt.getCurrentRawValue()));
    }

    List<String> aliases = getFormattedAliases(opt);
    if (! aliases.isEmpty()) {
      descSb.append(" [aliases: ")
              .append(StringUtils.join(aliases, ", "))
              .append("]");
    }

    String descTxt = WordUtils.wrap(descSb.toString(), DESC_WIDTH);
    String[] lines = descTxt.split("\n");

    // First line should be appended directly
    sb.append(lines[0]).append("\n");
    for (int i = 1; i < lines.length; i++) {
      sb.append(StringUtils.repeat(" ", DESC_MARGIN)).append(lines[i]);
      sb.append("\n");
    }

    pw.write(sb.toString());
  }

  static String prettyOption(OptionInfo info) {
    String s = info.getCanonicalName();
    s = s.replaceAll("^/", "");
    s = s.replaceAll("[._/]", "-");
    return "--"+s;
  }

  static String prettyArgname(OptionInfo info) {
    String s = info.getOption().getArgName();
    return s.replaceAll("[/.-]", "_");
  }
}
