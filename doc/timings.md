<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->
**Table of Contents**  *generated with [DocToc](http://doctoc.herokuapp.com/)*

- [Timings Protocol](#timings-protocol)
  - [How Timings are Gathered](#how-timings-are-gathered)
  - [How timings are analyzed.](#how-timings-are-analyzed)
    - [The `Timings` class](#the-timings-class)
    - [The `TimeWindow` object.](#the-timewindow-object)
      - [Combining Windows](#combining-windows)
    - [Getting `TimeWindow` objects from a `Timings` object](#getting-timewindow-objects-from-a-timings-object)
  - [IntervalInfo object](#intervalinfo-object)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

# Timings Protocol

Timings is at the heart of the SDKD. In essence it provides the true analytic
strength.

Timings allow us to do three things:

1. Gather absolute performance metrics
2. Gather status information according to specific time periods
3. Correlate these time periods with cluster topology changes

Timings also provide the foundation by which to draw graphical charts, as
each _Time Quantum_ represents a group of data points for a single interval.

## How Timings are Gathered

The actual aggregation of timings is done by the SDKD implementation. Typically
the SDKD will have some kind of timer and time each operation. For each
operation it will evaluate the return code (whether it was a success or failure)
and then gather timing statistics.

Timing statistics are rather trivial for the SDKD to gather as most commands
are executed within a loop. Error statistics are a bit more complex as the SDKD
must determine which SDK status code correlates to which SDK error code.

Once a command has completed, the SDKD formats its internal representation
of the timing information into a JSON object and sends it back to the client.
The client, in turn, stores this information into the database for later
analysis.

## How timings are analyzed.

Timing analysis is done by the `WorkloadAnalyzer` class and the `Timings` class.
These two classes work in conjunction to filter and format the data in a
meaningful fashion.

### The `Timings` class

The `Timings` class represents a single timings response from a single SDKD
handle. It knows about how long each time interval is and when it started.
Each time interval (or time quantum) is represented as a `TimeWindow` object
which contains fields for error statistics and latency measurement.

### The `TimeWindow` object.
The `Timings` object itself is comprised of multiple `TimeWindow` objects which
appear as a sequence. Each `TimeWindow` object contains information about the
averaged latency information and the total error information which occured
during measurement.

Here's some sample usage:

```java
TimeWindow tw;
System.out.printf("Minimum latency during this interval: %d%n",
                  tw.getMinLatency());
System.out.printf("Maximum latency during this interval: %d%n",
                  tw.getMaxLatency());
System.out.printf("Average latency during this interval: %d%n",
                  tw.getAvgLatency());

OperationSummary errorDetails = tw.getSummary();
for (Entry<Error,Integer> ent : errorDetails.getDetails().entrySet()) {
  System.out.println("Error %s was received %d times during this interval%n",
                     ent.getKey().toString(),
                     ent.getValue());
}
```

As can be seen, the `TimeWindow` contains accessors for the latency information
as well as an accessor for the `OperationSummary` object which itself contains
the status information. One can iterate over the status information to receive
the number of times each error occurred.

#### Combining Windows
>
`TimeWindow` objects may be _combined_ with each other to form an aggregate
window. The combination works through creating a new `TimeWindow` object whose
minimum latency is the lowest of all the windows; whose maximum latency is
the highest of all the windows, whose average latency is the mean of all
windows' averages, and with the error details for each window merged together.
>
`TimeWindows` can be combined either by combining intervals before and after it,
or by combining it with a `TimeWindow` from a different handle from the same
interval.

### Getting `TimeWindow` objects from a `Timings` object

A raw list of all `TimeWindow` objects can be extracted from a `Timings`
object by calling its `getWindows()` method. However typically you will
want to display or gather timing information based on some other criteria -
for example, to correlate it with some specific interval in time.

The following for example may be used to get the first five seconds of the
`Timings` object as a single `TimeWindow`

```java
// Get the start time
Timings timings;
CBTime start = timings.getBaseTime();
CBTimeSpan range = new CBTimeSpan(start, start.incSeconds(5));
TimeWindow combinedWindow = timings.atRange(range, true);
```

Or to get the window at 10 seconds into the run:

```java
TimeWindow interval = timings.atOffset(10);
```

The `WorkloadAnalyzer` has more tools for dealing with `TimeWindow` objects.


## IntervalInfo object

The `IntervalInfo` class is defined in the analytics package - while the previous
`TimeWindow` and `Timings` classes were defined in the `protocol` package. This
is because the analytics routines simply build upon the core objects mentioned
above.

The `IntervalInfo` object is returned by the `WorkloadAnalyzer` instance for
various such queries and provides several methods for retrieving `TimeWindow`
objects for a specified interval.

The `IntervalInfo` always correlates to a single fixed time quantum whose upper
and lower bound are determined at the time of the query. The implementation of
the object is bound to several internal classes which enable the retrieval of
more special information not found within the protocol itself - in this case,
logging messages.

This enables a code like the following:


```java
WorkloadAnalyzer wa;
for (IntervalInfo info : wa.getSeries(5)) {
  System.out.printf("Have new 5 second interval%n");
  System.out.printf("Combined latency: Min %d, Max %d, Avg %d%n",
                    info.getCombinedWindow().getMinLatency(),
                    info.getCombinedWindow().getMaxLatency(),
                    info.getCombinedWindow().getAvgLatency());
  // If you wanted to, you could print the error details for the combined window
  // itself.
  System.out.printf("Combined window has %d operations per second",
                    info.getOpsPerSecond(info.getCombinedWindow());

  // Now we can print out information for each invidual window.
  for (Entry<Integer,TimeWindow> ent : info.getWindows().entrySet()) {
    System.out.printf("Printing information for handle ID %d%n", ent.getKey());
    // ...
  }
}
```