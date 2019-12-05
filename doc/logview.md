<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->
**Table of Contents**  *generated with [DocToc](http://doctoc.herokuapp.com/)*

- [Effective Log Analysis - `logview`](#effective-log-analysis---logview)
  - [Anatomy of a logfile](#anatomy-of-a-logfile)
  - [Default Invocation](#default-invocation)
      - [Reproducing the run](#reproducing-the-run)
      - [Quickly checking for pass/failure](#quickly-checking-for-passfailure)
      - [Replaying log messages](#replaying-log-messages)
      - [Generating an HTML Log](#generating-an-html-log)
  - [Interpreting Output](#interpreting-output)
    - [Ramp Baseline](#ramp-baseline)
      - [`MEMD_ENOENT` errors](#memd_enoent-errors)
    - [Cluster Change Behavior](#cluster-change-behavior)
      - [Error Messages and Stack Traces](#error-messages-and-stack-traces)
    - [Rebound Behavior](#rebound-behavior)
  - [Additional Options](#additional-options)
    - [Formatters](#formatters)
    - [Format Strings](#format-strings)
      - [`screen` Formatter Options](#screen-formatter-options)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

# Effective Log Analysis - `logview`

This section will discuss the usage of the `logview.py` program; explain the
logging format of `stester`, and provide insight on how to best examine
the output

## Anatomy of a logfile

This section will describe the possible output of a logfile:

* Configuration Information
  This contains various configuration options as well as some environmental
  information. Currently displayed are:

    * Option values
    * STester version
    * SDKD `INFO` output

* Status Information
  This contains the actual performance and error information for the entire run.
  Here you can see the timeline of the SDK's performance and optionally
  correlate it with cluster events and logging messages.


## Default Invocation

The default invocation of `logview` requires the `-f` option to be supplied with
the filename to be analyzed. The filename may have either a `.zip` or `.h2.db`
suffix.

```
shell> logview -f log.h2.db
Cluster Version: 2.2.1r_206_g24d13cb
SDKD Client: add489f10542b2907238fafb064c62b72566e8cd
Scenario: Ramp for 30 seconds. Cluster Modification: remove 2 nodes and rebalance. Rebound for 90 seconds
Will dump configuration settings. [M] Modified [-] Default
<Section: Cluster>
  [-] bucket/add_default   false
  ...
<Section: Main>
  [M] comment              EXTRA_ARGS
  ...
<Section: SDKD>
  [M] exec/args            -jar
  ...
<Section: Scenario>
  [M] rebalance/count      2
  ...

<Section: Workload>
  [-] kv/delay             0
  ...

Phase timings for RAMP
Ops/Sec: 6663
 [207568] OK: 206580, ERR: 0
  {[OK]: 206580, [MEMD:MEMD_ENOENT]: 988}
  MIN: 0, MAX: 1, AVG: 0

Phase timings for CHANGE
Ops/Sec: 6757
 [287668] OK: 283831, ERR: 0
  {[OK]: 283831, [MEMD:MEMD_ENOENT]: 3837}
  MIN: 0, MAX: 1, AVG: 0

Phase timings for REBOUND
Ops/Sec: 6997
 [629770] OK: 629770, ERR: 0
  {[OK]: 629770}
  MIN: 0, MAX: 16, AVG: 0

  ...

+0s
 [11058] OK: 10660, ERR: 0
  {[OK]: 10660, [MEMD:MEMD_ENOENT]: 398}
  MIN: 0, MAX: 1, AVG: 0
[WARN] Nov 29, 2013 11:47:19 AM com.couchbase.client.ViewConnection createConnections
...

+160s
 [13842] OK: 13842, ERR: 0
  {[OK]: 13842}
  MIN: 0, MAX: 1, AVG: 0

```

The full output has been abbreviated in order to display the basic sections
more concisely.

1. The cluster version.
    This is the cluster version against which the test was run

2. The _sdkdclient_ version. This is shown as a git commit number, and allows
    to re-run the test using the same version it was run with originally

3. Scenario Description
    This provides a human-readable string of which manipulations and changes were
    applied to the cluster.

4. Configuration Entries
    This shows all of the configuration inputs the test was run with. Each
    configuration entry is prefixed with either a `[-]` or a `[M]`. The
    `[-]` means the configuration value was left at its default value and `[M]`
    means it was overidden by a user.

5. Per-Phase timing information
    This provides output of the SDK's performance during the various phases.
    Typically this is quicker to examine as it is much shorter than the
    output which follows. For each phase the following information is displayed:

      * `Ops/Sec`. How many successful operations per second were performed
          during this phase

      * `[nnnn]`. Total number of operations executed.

      * `{ [Error]: count, [Error]: count, ... }`
          Detailed breakdown of which errors took place and how many times
          they were encountered

      * `MIN`, `MAX`, and `AVG` latency for the phase.

6. Per-Interval timing information.
    This follows the same format as the phase breakdown, except that it shows
    detailed information for every 2-second window of the test run.
    Additionally, any logging events which took place during the interval are
    displayed inline.


#### Reproducing the run

Often it is deisrable to rerun the test. In order to do this, `logview`
can dump a configuration file to the console. You may wish to redirect the
output to a file on your disk.


#### Quickly checking for pass/failure

You can easily obtain pass/fail information using `logview`. This will output
a grade number (on the scale of 0 to 100) and a reason for the grade.

```
shell> logview -f log.h2.db -O score
Grade: 100. OK
```

#### Replaying log messages

Since the underlying database contains the logging information of the run itself,
you can view all the log messages at any level.

```
shell> logview -f log.h2.db -F L --loglevel debug
[0.00] [INFO] Preparing and validating configuration
[0.00] [DEBUG] Spec: 127.0.0.1:9000
[0.03] [DEBUG] Spec: 127.0.0.1:9001
[0.03] [DEBUG] Spec: 127.0.0.1:9002
[0.04] [DEBUG] Spec: 127.0.0.1:9003
...
```

#### Generating an HTML Log

This generates an HTML log with a graph displaying the error and latency information
of the handles. The final format of the page is still in progress.

```
logview -f log.h2.db -O html > log.html
```

And then open `log.html` with your browser.


## Interpreting Output

`logview` comes with a variety of means by which to output results, however at
the end of the day it is the user who must ultimately decide whether a run has
succeeded or not.

In a quick paragraph, interpreting the output involves estimating the _baseline_
from the `RAMP` phase and comparing it to the throughput of the `REBOUND` phase.
At the very least, the last 20 seconds or so of the `REBOUND` phase should resemble
the `RAMP` phase in both throughput and error information.

### Ramp Baseline

Most scenarios have a _Ramp_ phase. The ramp phase exists to establish a baseline
metric - or in other words, to provide a base throughput number that the client
is able to execute under _known working conditions_. Since the cluster is empty
and is not being manipulated it is assumed that the client will be at its best
behavior and deliver optimal throughput for the given environment.

As a corollary, it is assumed that the _Ramp_ phase must <b>not have any errors</b>
at all. In some cases there may be some initial timeout errors due to the
environment - but these should only be acceptable in the first few seconds.

#### `MEMD_ENOENT` errors
>
A common point of confusion are the `ENOENT` errors. These errors mean that
the key for the operation was not found, and is natural for the key-value workload.
>
Since the workload executes <i>set</i> and <i>get</i> operations concurrently,
it is possible for one thread to attempt to fetch the key before it is set.
>
Most of the analysis utilities are aware of this condition and thus will not
consider `ENOENT` as an actual error (but will not count it as a success either).


### Cluster Change Behavior

Once the _Ramp_ phase has completed (by default this is 30 seconds) the selected
scenario will manipulate the cluster. Depending on the SDK and the cluster change
taking place there will potentially be a lot of logging output and failed operations.
Nevertheless failed operations during the _Change_ state are acceptable since
network conditions may change; in some cases failures may even be _expected_.

Typically points should be deducted for a heavy rate of failures during a
cluster change - if the failure rate is deemed beyond reasonable.

Also worth looking at is the maximum latency of operations during this time -
the maximum latency should be within range of the SDK's defined timeout for
the given operations and not span many hundreds of seconds.

#### Error Messages and Stack Traces
>
The SDKD may have a *lot* of output during a change scenario. However
**it should not factor into the grading of the test** - log output may be
diagnostic and is typically just the SDK informing the user of what's happening
with the cluster. **If there are errors they will always be reflected in the
interval and phase summaries** - the stack traces are just **possible hints**
of what may have caused these errors.


### Rebound Behavior

Once the cluster change has been completed, the scenario will enter the _Rebound_
phase. The _Rebound_ phase indicates that no more cluster changes will take place
and that the SDK should resume normal functionality.

It is common for the first few intervals of the _Rebound_ phase to contain many
errors. This is typically because:

* Various SDK-level timeouts and watchdogs have not yet fired.
    Some SDKs have backoff and retry intervals. A retry timer may have been
    scheduled during the `CHANGE` phase and will not fire until several seconds
    into then `REBOUND` phase.

* Timeline correlations are inexact.
    The performance and status data are collected using the SDKD protocol
    whereas phase boundaries are created using the sdkdclient. When the SDKD
    and the harness are not running on the same host it is possible that their
    clocks will be out of sync. The included analysis utilities contain workarounds
    for these conditions but it may still result in phases being off by several
    seconds.


Notwithstanding the comments about potential errors in the *beginning* of the
rebound phase, the default length of the rebound phase (90 seconds) is there
to ensure ample sample intervals at which we can certainly expect for the SDK
to function normally. Normal functionality is defined as follows:

* Operations per second are similar or within range to the `RAMP` phase.
    Since the cluster is "back to normal", we also expect the SDK to be
    "back to normal".

    Note that in cases where nodes are removed, the throughput may be understandably
    less because of a loss of server resources. However even a 50% drop in
    performance is usually not an indicator of an error, and most times when
    there are SDK problems, the `REBOUND` phase will show a very low throughput.

* Absolutely no errors (other than the occasional `ENOENT`) should be seen
    in the final few intervals of Rebound.


## Additional Options

### Formatters

`logview` is the wrapper script around the `DbAnalyzer` class. The analyzer
works by examining the logfile, extracting various information and then
notifying various plugins about statistics. These plugins are known as
`Formatter` objects. Here is a list of some formatters

* `screen`
    This formatter prints the results to the screen in a pretty format

* `html`
    Generates a report in HTML form and dumps it to standard output

* `score`
    Attempts to automatically determine whether the test passed or not; outputting
    its grade and reason to the screen.

* `xunit`
    Like `score`, but will output _XUnit_ XML rather than simple text.

To select a formatter for `logview`, use the `-O` option. Examples of this
can be seen above where the `html` and `score` formatters were used.


### Format Strings

In addition to `Formatter` selection, `logview` also accepts a _format string_.
A _format string_ is a sequence of items which instruct the analyzer about which
statistics to process and deliver to its attached formatters. Format strings
are specified as a sequence of characters and are values to the `-F` option.

Possible values are

<table><tr><th>Character</th><th>Description</th></tr>
<tr>
  <td><code>c</code></td>
  <td>Show configuration information</td>
</tr>
<tr>
  <td><code>p</code></td>
  <td>Show phase information. This shows the timings and summaries on a per-phase
  breakdown</td>
</tr>
<tr>
  <td><code>d</code></td>
  <td>Show timings and status summary for each interval</td>
</tr>
<tr>
  <td><code>A</code></td>
  <td>Format the configuration as a configuration file and dump it to
  standard output</td>
</tr>
<tr>
  <td><code>L</code></td>
  <td>Display the messages logged during the run</td>
</tr>
</table>

The default format specified is <code>cpd</code> which means to do the
following in this order:

1. Show configuration information
2. Show phase information
3. Show interval information.

#### `screen` Formatter Options

<table><tr><th>Option</th><th>Description</th></tr>
<tr>
  <td><code>interval</code></td>
  <td>Specifies the length of each `TimeWindow` to be printed. The value is in
  seconds. Setting this to a higher number will cause output to be more
  concise and setting this to a lower number will cause output to be more
  detailed and verbose</td>
</tr>
<tr>
  <td><code>threads</code></td>
  <td>For each `TimeWindow`, in addition to printing out the _Combined_
  window, also print the `TimeWindow` for each individual _Handle_. See
  [timings](timings.md) for more information on combined and handle windows</td>
</tr>
<tr>
  <td><code>loglevel</code></td>
  <td>Specify the minimum log level to output. This affects both the
  detailed interval output where log messages are interleaved with statistics;
  as well as the log dump mode where only log messages are displayed</td>
</tr>
</table>