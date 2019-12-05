/*
 * Copyright (c) 2014 Couchbase, Inc.
 */

package com.couchbase.sdkdclient.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by mnunberg on 5/19/14.
 *
 * This class tokenizes a string into an array of arguments, much like a shell
 * would
 */
public class StringArgv {
  private boolean escapeEnabled = true;
  private ArrayList<String> tokens = null;
  private String origString;
  private StringBuilder sb = new StringBuilder();

  private enum ParseState {RAW, ESCAPE, SINGLEQUOTE, DOUBLEQUOTE, WHITESPACE}

  private ParseState curState = ParseState.WHITESPACE;

  private void addToken(boolean allowEmpty) {
    String s = sb.toString();
    sb = new StringBuilder();
    if (s.isEmpty() && allowEmpty == false) {
      return;
    }
    tokens.add(s);
  }

  private boolean inQuote() {
    return curState == ParseState.DOUBLEQUOTE || curState == ParseState.SINGLEQUOTE;
  }

  /**
   * Enables or disables the use of the '\' as an escape characted. This is
   * common on Unix but not on Windows.
   *
   * @param val
   */
  public void setEscapeEnabled(boolean val) {
    escapeEnabled = val;
  }

  public String[] getArray() {
    return tokens.toArray(new String[tokens.size()]);
  }

  public List<String> getList() {
    return tokens;
  }


  /**
   * Parse a string into its constituent parts
   *
   * @param s the string to parse
   */
  public StringArgv parse(String s) {
    origString = s;
    tokens = new ArrayList<String>();

    for (int curIndex = 0; curIndex < origString.length(); curIndex++) {
      char c = origString.charAt(curIndex);
      if (curState == ParseState.ESCAPE) {
        sb.append(c);
        continue;
      }

      // Go into escape mode and blindly append the next char
      if (c == '\\' && curState != ParseState.SINGLEQUOTE && escapeEnabled) {
        curState = ParseState.ESCAPE;
        continue;
      }

      if (c == '\'') {
        if (curState == ParseState.SINGLEQUOTE) {
          curState = ParseState.RAW;
          continue;
        } else if (curState != ParseState.DOUBLEQUOTE) {
          curState = ParseState.SINGLEQUOTE;
          continue;
        }
      }

      if (c == '"') {
        if (curState == ParseState.DOUBLEQUOTE) {
          curState = ParseState.RAW;
          continue;
        } else if (curState != ParseState.SINGLEQUOTE) {
          curState = ParseState.DOUBLEQUOTE;
          continue;
        }
      }

      if (inQuote()) {
        sb.append(c);
        continue;
      }

      if (Character.isWhitespace(c)) {
        if (curState == ParseState.WHITESPACE) {
          continue;
        }
        // Create new token
        curState = ParseState.WHITESPACE;
        addToken(true);
        continue;
      }

      curState = ParseState.RAW;
      sb.append(c);
    }

    addToken(false);
    return this;
  }

}
