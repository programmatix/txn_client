package com.couchbase.Logging;/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;

public class DedupeFilter extends Filter<ILoggingEvent> {
  private int maxDelay = 2000;
  private int maxRepeat = 1;
  private volatile String lastMessage = "";
  private volatile int curFilterCount = 0;
  private volatile long lastTimestamp = Long.MAX_VALUE;

  public void setSuppressionTime(int interval) {
    maxDelay = interval;
  }

  public void setMaxRepeat(int val) {
    maxRepeat = val;
  }

  private synchronized FilterReply decideImpl(String s) {
    FilterReply ret;
    if (!s.equals(lastMessage)) {
      // Reset
      curFilterCount = 0;
      ret = FilterReply.NEUTRAL;

    } else if (curFilterCount > maxRepeat) {
      // Matches..
      long now = System.currentTimeMillis();

      if (now - lastTimestamp > maxDelay) {
        lastTimestamp = now;
        curFilterCount = 0;
        ret = FilterReply.NEUTRAL;

      } else {
        return FilterReply.DENY;
      }
    } else {
      ret = FilterReply.NEUTRAL;
    }

    curFilterCount++;
    lastMessage = s;
    return ret;
  }

  @Override
  public FilterReply decide(ILoggingEvent event) {
    String s = event.getFormattedMessage();
    return decideImpl(s);
  }
}
