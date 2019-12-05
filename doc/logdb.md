<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->
**Table of Contents**  *generated with [DocToc](http://doctoc.herokuapp.com/)*

- [Logging Database](#logging-database)
  - [`RunEntry`](#runentry)
  - [`ConfigEntry`](#configentry)
  - [`MiscEntry`](#miscentry)
  - [`LogEntry`](#logentry)
  - [`WorkloadEntry`](#workloadentry)
  - [`HandleEntry`](#handleentry)
  - [`RequestEntry`](#requestentry)
  - [`ResponseEntry`](#responseentry)
- [How entries are used.](#how-entries-are-used)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

# Logging Database

The logging database contains information about a single run. It is stored as
an [H2](h2database.com) database.

The database itself contains logging messages which happened during the run
as well as raw timing information. When analyzing the database, log messages
are correlated with time intervals and output is produced.

This document aims to describe some of the datatypes that are stored inside
the database and how they are used.

Most of the entries are defined in the `com.couchbase.sdkdclient.rundb`
package and are wrappers around the actual objects they aim to represent.

## `RunEntry`


The `RunEntry` is a singleton entry representing a single test run -
usually a single invocation of `stester`. It is identified by a _UUID_. A
run entry is provided in the case of allowing multiple simultaneous
`RunEntries` in the future.

Currently the `RunEntry` is created when the `RunDB` object itself is instantiated.


## `ConfigEntry`

The `ConfigEntry` represents a single configuration entry (either
via a configuration file or the commandline). There are multiple `ConfigEntry`
objects and each is a child of a `RunEntry`.

`ConfigEntry` objects are created inside the `STester` class.

## `MiscEntry`

Like the `ConfigEntry`, this is a simple configuration datum which is tied
to a `RunEntry`; however a `MiscEntry` does not represent a user configuration
item but rather a miscellaneous statistic (such as version information)
gathered during runtime.

`MiscEntry` objects are created by various components throughout the run.

Notable to mention are _Phase_ information and timings which are created via
the `ScenarioListener` implementation in `RunContext`.

## `LogEntry`

This is an entry for a single logging message received. It is created via a
custom `Appender` added to _logback_. By default all log messages are stored
to the database and these consume most of the databases' file size. Log entries
retain the logger name, severity level, timestamp, and the actual message.

## `WorkloadEntry`

A `WorkloadEntry` represents a single `Workload` class executed during a run.
A workload entry is a child of a `RunEntry`.

Workload entries are created via the `WorkloadListener` object as defined in
the `RunContext` class.

## `HandleEntry`
A `HandleEntry` is a child of a `WorkloadEntry` and represents a single
`Handle` created by a `Workload`. This corresponds directly to an _SDKD_
_Handle_.

`HandleEntries` are created within the `WorkloadListener` implementation.

## `RequestEntry`
A `RequestEntry` is a child of a `HandleEntry` and represents a request issued
to the handle. The `RequestEntry` contains the request ID as well as the time
the request was issued. The timestamp is important as it helps adjust for time
skews between the _sdkdclient_ and the _sdkd_ itself if running on different
systems.

## `ResponseEntry`
A `ResponseEntry` is a child of a `RequestEntry` and contains the timings and
summary of the payload for the SDKD command.

# How entries are used.

The database is opened from within the `DbAnalyzer` class (which is the Java
class for the `logview`) executable. The sequence goes as follows:

1. Load the `RunDB` object.
2. Locate the `RunEntry` object.
3. Load all its `ConfigEntry` objects
4. Locate all its `MiscEntry` objects.
5. Load the workloads.
    Since `logview` can only display one workload at a time, an exception will
    be thrown if there is more than one workload and none was explicitly passed
    on the commandline. Once a `WorkloadEntry` is located, a new
    `WorkloadAnalyzer` object is created.
6. The `WorkloadAnalyzer` locates all `HandleEntries` created for the specified
    workload.
7. For each `HandleEntry` created, it locates its `RequestEntry` and
    `ResponseEntry` objects.
8. The `WorkloadAnalyzer` is now ready to be queried about various statistics.
    Each query on this object involves essentially returning the same data. Key
    is the return of a _Combined_ handle and _individual_ handles.