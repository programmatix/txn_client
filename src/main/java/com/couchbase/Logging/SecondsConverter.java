/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.couchbase.Logging;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

/**
 *
 * @author mnunberg
 */
public class SecondsConverter extends ClassicConverter {
  long start = System.currentTimeMillis();

  @Override
  public String convert(ILoggingEvent event) {
    float duration = System.currentTimeMillis() - start;
    duration /= 1000;
    return String.format("%.2f", duration);
  }

}
