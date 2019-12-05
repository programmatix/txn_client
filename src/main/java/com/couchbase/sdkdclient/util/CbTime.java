package com.couchbase.sdkdclient.util;


import javax.annotation.Nonnull;

/**
 * Convenience class for dealing with a timestamp. I decided to write my own
 * as the problems I face are mainly about simple seconds arithmetic. Since
 * I suck at that and there wasn't any alternative, I decided to stick with this.
 *
 * The {@link CbTime} class uses a single internal field which is a
 * {@code long} for the <i>millisecond</i> time.
 *
 * For simplicity and performance this object is immutable and its class is
 * {@code final}.
 *
 * This class may be used in conjunction with the {@link} class
 * to deal with ranges and intervals of time.
 */
@SuppressWarnings("unused")
public final class CbTime implements Comparable<CbTime> {
  final long msTime;
  final static CbTime DUMMY = new CbTime(0);

  /**
   * Create a new object
   * @param msTime The epoch timestamp in milliseconds. This must not be negative.
   */
  private CbTime(long msTime) {
    if (msTime < 0) {
      throw new IllegalArgumentException("Time cannot be negative");
    }
    this.msTime = msTime;
  }

  /**
   * @return A new object representing the current system time
   */
  public static CbTime now() {
    return new CbTime(System.currentTimeMillis());
  }

  /**
   * @return A new object representing the current system time. The internal
   * value is rounded to the nearest second, rather than millisecond.
   */
  public static CbTime nowRounded() {
    return CbTime.fromSeconds(System.currentTimeMillis()/1000);
  }

  /**
   *
   * @param seconds Epoch timestamp in <b>seconds</b>
   * @return A new time object
   */
  public static CbTime fromSeconds(long seconds) {
    return new CbTime(seconds*1000);
  }

  /**
   * @param ms Epoch timestamp in <b>milliseconds</b>
   * @return A new time object
   */
  public static CbTime fromMillis(long ms) {
    return new CbTime(ms);
  }

  /**
   * @see #fromFutureMillis(long)
   * @param seconds The number of seconds from now.
   * @return A new CBTime object.
   */
  public static CbTime fromFutureSeconds(long seconds) {
    return new CbTime(System.currentTimeMillis() + (seconds * 1000));
  }

  /**
   * Create an object which represents a time at the future specified by an
   * offset. This does the equivalent of calling
   * {@code CbTime.fromMillis(System.currentTimeMillis() + millis) }
   * @param millis The offset in <b>milliseconds</b>
   * @return A new time object
   */
  public static CbTime fromFutureMillis(long millis) {
    return new CbTime(System.currentTimeMillis() + millis);
  }

  /**
   * Get a dummy object.
   * @return an object whose internal field is 0
   */
  public static CbTime dummy() {
    return DUMMY;
  }

  /**
   * Get the "Maximum" time.
   * @return an object representing the most distant time
   */
  public static CbTime endOfTime() {
    return new CbTime(Long.MAX_VALUE);
  }

  /**
   * Get the epoch in milliseconds.
   * @return The epoch timestamp expressed in <b>milliseconds</b>
   */
  public final long getEpochMillis() {
    return msTime;
  }

  /**
   * Get the epoch in seconds.
   * @return The epoch timestamp expressed in <b>seconds</b>
   */
  public final int getEpochSeconds() {
    return (int)(msTime / 1000);
  }

  /**
   * Checks whether this object's time is <i>after</i> the time of {@code other}
   * @param other The object to compare to
   * @return true if after, false otherwise.
   */
  public final boolean isAfter(CbTime other) {
    return msTime > other.msTime;
  }

  /**
   * Checks whether this object's time is <i>before</i> the time of {@code other}
   * @param other The object to compare to
   * @return true if before, false otherwise.
   */
  public final boolean isBefore(CbTime other) {
    return msTime < other.msTime;
  }

  /**
   * Checks whether this object is no less than {@code ms} milliseconds
   * within range of {@code other}. This method does not care if {@code other}
   * is before or after this object.
   * @param other The object to compare to
   * @param ms The maximum difference between the two times.
   * @return true if within range, false otherwise.
   */
  public final boolean isWithinMillis(CbTime other, long ms) {
    return Math.abs(getEpochMillis()-other.getEpochMillis()) <= ms;
  }

  /**
   * @see #isWithinSeconds(com.couchbase.sdkdclient.util.CbTime, long)
   * @param other
   * @param seconds
   * @return true if within the range, false otherwise.
   */
  public final boolean isWithinSeconds(CbTime other, long seconds) {
    return Math.abs(getEpochSeconds()-other.getEpochSeconds()) <= seconds;
  }

  /**
   * Returns the <i>absolute</i> difference between this and the other object
   * in <i>milliseconds</i>
   * @param other The object to compare to
   * @return The absolute millisecond delta
   */
  public final long millisDiff(CbTime other) {
    return Math.abs(getEpochMillis() - other.getEpochMillis());
  }

  /**
   * Returns the <i>absolute</i> difference between this and the other object in
   * <i>seconds</i>
   * @see #millisDiff(com.couchbase.sdkdclient.util.CbTime)
   * @param other
   * @return Absolute second delta.
   */
  public final int secondsDiff(CbTime other) {
    return Math.abs(getEpochSeconds() - other.getEpochSeconds());
  }

  /**
   * Check if object is empty.
   * @return Whether this object's time field is 0
   */
  public final boolean isEmpty() {
    return msTime == 0;
  }

  /**
   * Create a new {@link CbTime} object which has its time field {@code millis}
   * milliseconds <i>after</i> this current object's value
   * @param millis The offset in milliseconds
   * @return A new {@link CbTime} object.
   */
  public final CbTime incMillis(long millis) {
    return new CbTime(msTime + millis);
  }

  /**
   * @see #incMillis(long)
   * @param seconds Offset in seconds
   * @return A new time object
   */
  public final CbTime incSeconds(int seconds) {
    return incMillis(seconds * 1000);
  }

  /**
   * Return a new {@link CbTime} object which has its time field {@code millis}
   * milliseconds <i>after</i> the current object's value
   * @param millis The offset in milliseconds
   * @return A new {@link CbTime object}
   */
  public final CbTime decMillis(long millis) {
    return incMillis(-millis);
  }

  public final CbTime decSeconds(int seconds) {
    return incSeconds(-seconds);
  }

  /**
   * Get the earliest time object.
   * @param a
   * @param b
   * @return the earliest point in time from {@code a} and {@code b}
   */
  public static CbTime min(CbTime a, CbTime b) {
    if (a.getEpochMillis() < b.getEpochMillis()) {
      return a;
    }
    return b;
  }

  /**
   * Get the latest time object.
   * @param a
   * @param b
   * @return The latest point in time from {@code a} and {@code b}
   */
  public static CbTime max(CbTime a, CbTime b) {
    if (a.getEpochMillis() > b.getEpochMillis()) {
      return a;
    }
    return b;
  }

  @Override
  public String toString() {
    return msTime + "ms";
  }

  /**
   * Check time objects for equality.
   * @param other
   * @return true if the internal timestamp matches.
   */
  @Override
  public boolean equals(Object other) {
    if (other == this) {
      return true;
    }

    if (! (other instanceof CbTime)) {
      return false;
    }

    return ((CbTime)other).msTime == msTime;
  }

  @Override
  public int hashCode() {
    int hash = 3;
    hash = 29 * hash + (int) (this.msTime ^ (this.msTime >>> 32));
    return hash;
  }

  @Override
  public int compareTo(@Nonnull CbTime o) {
    long x = msTime;
    long y = o.msTime;
    return (x < y) ? -1 : ((x == y) ? 0 : 1);
  }


}