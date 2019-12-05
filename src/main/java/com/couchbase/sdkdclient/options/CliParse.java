/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.couchbase.sdkdclient.options;

import com.couchbase.sdkdclient.logging.LogUtil;
import org.slf4j.Logger;

import java.util.ArrayList;

/**
 * Static class for handling commandline parsing.
 */
public class CliParse {
  private static Logger logger = LogUtil.getLogger(CliParse.class);

  private static void assignValueToOption(RawOption opt, String value) {
    if (opt.isMultiOption()) {
      ((MultiOption) opt).addRawValue(value);
    } else {
      if (opt.wasProvidedValue()) {
        logger.warn("Option \"{}\" passed multiple times "
                + "Overriding with \"{}\" (was \"{}\")",
                    opt.getName(), value, opt.getCurrentRawValue());
      }
     // System.out.println(opt.getName());
      opt.setRawValue(value);
    }

    opt.setArgFound();
    logger.trace("Setting value {} for option {}", value, opt.getName());
  }

  private static boolean shouldAssignDashValue(RawOption lastOption) {
    if (lastOption == null) {
      return false;
    }
    if (!lastOption.mustHaveArgument()) {
      return false;
    }
    if (lastOption.wasProvidedValue()) {
      return false;
    }
    return true;
  }

  static String[] parseArgv(String[] argv, OptionLookup coll) throws CommandLineException {
    ArrayList<String> remaining = new ArrayList<String>();
    RawOption lastOption = null;
    for (String s : argv) {
      if (s.startsWith("-") == false || shouldAssignDashValue(lastOption)) {
        if (lastOption == null) {
          // Nothing to do here
          remaining.add(s);
          continue;
        }

        assignValueToOption(lastOption, s);

      } else {
        final String orig = s;
        // Start new option..
        if (s.startsWith("--")) {
          s = s.substring(2);
        } else {
          // == "-"
          s = s.substring(1);
        }

        String embeddedVal = null;

        // It's an option. See if it's foo=bar form.
        if (s.contains("=")) {
          embeddedVal = s.substring(s.indexOf("=") + 1);
          s = s.substring(0, s.indexOf("="));
        }

        // Find the option
        if (s.length() > 1) {
          lastOption = coll.findOption(null, s);
        } else {
          lastOption = coll.findOption(s, null);
        }

        if (lastOption == null) {
          logger.trace("Couldn't find option with base {}", orig);
          remaining.add(orig);
          continue;

        } else {
          lastOption.setFound();
          logger.trace("Mapped option with base {}", orig);
        }

        if (embeddedVal != null) {
          assignValueToOption(lastOption, embeddedVal);
        }
      }
    }

    return remaining.toArray(new String[remaining.size()]);
  }

}
