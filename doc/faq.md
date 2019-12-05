<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->
**Table of Contents**  *generated with [DocToc](http://doctoc.herokuapp.com/)*

- [Frequently Asked Questions](#frequently-asked-questions)
  - [Do I need to know Java to use _sdkdclient_?](#do-i-need-to-know-java-to-use-_sdkdclient_)
  - [What is the difference between `stester` and `brun`.](#what-is-the-difference-between-stester-and-brun)
  - [Can I use operations other than *Get*, *Set* and *Views*?](#can-i-use-operations-other-than-get-set-and-views)
  - [Why do I see `MEMD_ENOENT` errors in the reports?](#why-do-i-see-memd_enoent-errors-in-the-reports)
  - [Can I use `stester` together with `cluster_run`?](#can-i-use-stester-together-with-cluster_run)
  - [Do I need to specify `stester` arguments in a specific order?](#do-i-need-to-specify-stester-arguments-in-a-specific-order)
  - [I don't like specifying so many options on the command line all the time!](#i-dont-like-specifying-so-many-options-on-the-command-line-all-the-time!)
    - [Recommended Configuration File Layout](#recommended-configuration-file-layout)
  - [How do I run the SDK/SDKD under my favorite debugger?](#how-do-i-run-the-sdksdkd-under-my-favorite-debugger)
  - [How do I run a scenario without resetting my cluster?](#how-do-i-run-a-scenario-without-resetting-my-cluster)
  - [How do I get more verbose logging from the SDK or SDKD?](#how-do-i-get-more-verbose-logging-from-the-sdk-or-sdkd)
    - [Saving the logs](#saving-the-logs)
  - [How long are the tests supposed to take?](#how-long-are-the-tests-supposed-to-take)
  - [Can I run multiple tests (or instances of `stester`) concurrently?](#can-i-run-multiple-tests-or-instances-of-stester-concurrently)
  - [Can I specify or combine multiple scenarios in the same instance of `stester`?](#can-i-specify-or-combine-multiple-scenarios-in-the-same-instance-of-stester)
  - [Can I specify or combine multiple workloads in the same instance of `stester`?](#can-i-specify-or-combine-multiple-workloads-in-the-same-instance-of-stester)
  - [Can I run multiple SDKDs from within the same `stester` instance?](#can-i-run-multiple-sdkds-from-within-the-same-stester-instance)
  - [How can I parse analyze the logs from `stester`?](#how-can-i-parse-analyze-the-logs-from-stester)
  - [How do I know which SDK version a test was run with?](#how-do-i-know-which-sdk-version-a-test-was-run-with)
  - [How can I reproduce a test from a log file?](#how-can-i-reproduce-a-test-from-a-log-file)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

#Frequently Asked Questions

This section discusses frequently asked questions about SDKD

The questions are presented in no particular order, and it is recommended
to browse through them as well, simply to see what's available

## Do I need to know Java to use _sdkdclient_?

No. While the software itself is implemented in Java, it is entirely driven
by INI-file and command line based configurations.

It _is_ recommended however to have some knowledge of Java (to be able to
understand various stack trace exceptions) and Maven (if you wish to build
the package from source).

If you wish to add more scenarios then you will need to know Java.

## What is the difference between `stester` and `brun`.

_stester_ is the main application which combines the SDKD together with
the workload and scenario and invokes a single "pass" at a scenario.

_brun_ is designed for batch execution - it will run multiple scenarios in
sequence, invoking _stester_ multiple times. _brun_ also contains some
convenience functionality which will for example, automatically analyze
logs after each run, and generate them in proper HTML and text format.

Use _brun_ to run routine tests for execution, as a verification step
(for example, to verify a new release); but use _stester_ to repeat
a single test or to dig further into an issue. It is important to note
that **_brun_ doesn't do anything _stester_ can't**, and is just designed
to be used as an automated tool which aggregates multiple results.


## Can I use operations other than *Get*, *Set* and *Views*?

The SDKD has theoretical support for other operations such as append, prepend
and replace; but these are not really implemented in practice. The reason being
that `stester` was designed to test protocol performance and handling - the two
aspects which are most likely to be affected during a cluster change. Under the
hood in all SDKs, the ADD, REPLACE, APPEND, etc - are just special cases of
the simple `SET`. Their packet structure and packet handling is similar.

Note that it is always possible to implement an additional workload, or modify
an existing workload to perform more diverse operation types. Note that the
SDKD protocol itself is extensible, and its behavior may further be customized
by modification of a single _SDKD_ itself (for example, it is possible to
modify an _SDKD_ implementation to transparently perform durability constraints
operation alongside each mutation).

Take caution however when implementing new operations. The system is highly
depending on accurate error reporting to be able to make a decision on success
or failure, so ensure that SDKD implementations output the appropriate error
in case a command fails.

## Why do I see `MEMD_ENOENT` errors in the reports?

`MEMD_ENOENT` means that the key for the requested operation was not found.
It is emitted by the SDKD when the underlying SDK attempts to fetch an item
from the cluster that does not exist.

If running a failover scenario where data is lost, it is possible for clients
to perform a *Get* request on an item which is no longer in the cluster (since the
node hosting the data was removed from the cluster). This is also the case
if running any workload with a memcached bucket where a node is removed.


## Can I use `stester` together with `cluster_run`?

`cluster_run` refers to the wrapper script bundled with the Couchbase server source
code. It launches a full "cluster" within an isolated environment. This
lets one run a "multi node cluster" without requiring different hosts or
virtual machines.

It is possible to run _stester_ with `cluster_run`, however some additional arguments
should be passed. Specifically, you probably want to disable SSH access (since the
Couchbase server is not invoked from `/etc/init.d/couchbase-server`), and may also
wish to modify your `--cluster-node` parameters so that it points to the correct
port.

Note that the underlying SDK employed by the selected SDKD implementation must also
allow connecting to non-default memcached/RESTAPI ports as well.

> Don't test against `cluster_run` for production/release testing. While `cluster_run`
> provides a quick way to setup the cluster locally, it functions rather differently
> than a normal cluster would.

## Do I need to specify `stester` arguments in a specific order?

No. `stester` arguments don't need to be specified in a specific order. All
arguments are unique and are merged into a single namespace (which is why some
option names are so long). Sub-components check for any options they support
within this namespace and process them.

Internally, `stester` scans for top-level options first; these are the
options which control client logging, which SDKD to load, which cluster
layout to use, which workload to run, and which scenario to produce. Once
these options have been picked out of the command line, their specified
components are loaded, and scan the command line for their sub-options.
Specifically, these options must be present:

* `--sdkd-config`, or `-C` (or `sdkd-config` in the `[main]` section within the
  configuration file). This tells _stester_ what SDKD should be used.
  Note that sub-options may also be required depending on the value for this item.
* `--cluster-node`, or `-N` (or `node` in the `[cluster]` section within the
  configuration file). You should specify this option for as many nodes as
  the cluster will contain.
* `--workload`, `-W` (or `workload` in the `[main]` section within the configuration
  file). Specifies the workload to use. If not specified, will default to the key-value
  `GetSetWorkload`.
* `--scenario`, (or `scenario` in the `[main]` section within the configuration file).
  specifies the kind of fault injection to employ in the cluster. This will default to
  the `basic` scenario (which performs no kind of fault injection at all).
  

## I don't like specifying so many options on the command line all the time!

All options that are specified on the command line can also be included into
_configuration files_. Configuration files are in INI format and can be included
to _stester_ via the `-I` (capital I) option.

You can choose to either put your entire configuration in a single file, or
split them into multiple files. There is no limit on how many configuration files
can be passed; this means you can pass the `-I` option multiple times.

Note that you can also mix and match between command line options and
configuration file options. Options on the command line will _override_
options found in the configuration file.

### Recommended Configuration File Layout

As mentioned above, there are multiple ways to lay out the
configuration files. It is recommended that you have a specific configuration
file for:

* SDKD Location - this will contain information on how your SDKD is executed, so
  for example, an `sdkd.ini` file may contain:
  ```ini
  [main]
  sdkd-config = LocalExecutingDriver
  
  [exec]
  path = /path/to/sdkd
  args = --listen-on
  args = 8050
  port = 8050
  ```
  
* Cluster Configuration - this will contain information about your cluster.
  A `cluster.ini` file may contain:
  ```
  [cluster]
  node = 10.0.0.1
  node = 10.0.0.2
  node = 10.0.0.3
  node = 10.0.0.4
  password = adminPassword
  ssh-username = root
  ssh-password = v3rys3cur3
  ```
* Scenario Configuration - this will contain information and options relating to
  the fault injection scenario for your test. A `scenario.ini` file may contain:
  ```ini
  [main]
  scenario = RebalanceScenario
  
  # Rebalance specific options
  [rebalance]
  mode = swap
  count = 1
  
  # General Scenario Options
  [scenario]
  ramp = 10
  rebound = 15
  ```

To combine these all together, you would invoke `tester` as:
```
./stester -I sdkd.ini -I cluster.ini -I scenario.ini
```
   


## How do I run the SDK/SDKD under my favorite debugger?

This largely depends on the language in which the SDKD/SDK is implemented
and the debugger you wish to use.

Some debuggers can attach to already-running processes (like 'gdb'), in which
case you can simply examine the process table (using `ps` or `pgrep`) to obtain
the PID, and trace the PID.

Some other debuggers (like `valgrind`) require that the target executable be
passed as an argument to the debugger itself (this means the entry point is
the debugger). In this case you can make a small shell script which runs the
debugger with the target executable, and have it run via the `rexec` plugin.
i.e. create a shell script that looks like:

```
#!/bin/sh
exec valgrind --leak-check=full /home/mnunberg/src/sdkd-cpp/src/sdkd_lcb -l 4444
```

and save it to `vg-wrapper.sh`

Then:

```
./stester -C LocalExecutingDriver --exec-path vg-wrapper.sh --exec_port 4444 ...
```

Some other debuggers (specifically, debuggers within IDEs) tend to make you
launch the target executable from within the IDE.

In this case, the `stester` program would need to connect to a `host:port`
combination that the executable will be listening on; i.e. use `-C host:port`


## How do I run a scenario without resetting my cluster?

`stester` by default _resets_ your cluster. Specifically, it deletes all buckets
from the cluster, and then removes each node from the cluster - so that all
nodes are in an "unjoined" state. Then it creates the cluster again and forms
the proper buckets.

For most scenarios this is a necessary operation when considering the destructive
nature of `stester` scenarios. If a previous test caused a rebalance or a failover
then it is assumed that not all nodes are functioning properly. Thus the reset
of the cluster ensures that there are no current error/failure conditions leftd
behind from the previous test which may affect the current test.

Additionally, some scenarios require that only a subset of nodes initially be joined
to the cluster - for example, in tests where nodes are _added_ to the cluster
during the "change" phase, there must be sufficient number of nodes which are not
yet added. These nodes are added later on during the topology change.

Nevertheless, some scenarios (specifically `BasicScenario`) do not change the cluster
and thus do not need to have a newly-reset cluster each time. The default
for those tests is still to reset the cluster.

If you truly wish to use an existing cluster without resetting it, the
`--cluster-noinit` option may be used. This bypasses any initial
reset/bucket-creation stages and simply assumes that the cluster is already
in a preferred configuration.

It is recommended this option only be used when running a series of `BasicScenario`
scenarios, and that it only be used when the user is sure that all nodes are
properly joined to the cluster. You may check the nodes' web UI to see
their state.

## How do I get more verbose logging from the SDK or SDKD?

This largely depends on the SDK and SDKD in question. In general
if you are using the `RemoteExecutingDriver` or `LocalExecutingDriver`, then
any output by the _SDKD_ executable is captured by the _stester_ harness.

For SDKs and SDKDs which have logging, the SDKD executable will typically contain
one or more command line switches to control the verbosity of the logging levels. You
can activate these switches by employing the `--exec-arg` option to `tester`.

See [Logging](logging.md) for more information on how to control logging of the
_stester_ harness itself. Generally the _SDKD_ log messages will show information
from an SDK perspective about operations and errors being performed, while the
_stester_ log messages will show information about how the cluster is being
manipulated.

### Saving the logs

_stester_ will write by default to the `sdkdclient.logdb.h2.db` file. This file is a
special binary format of the H2 database (an embedded Java relational database)
which contains the log messages and other information. It is typically not necessary
to redirect output of _stester_ itself.

## How long are the tests supposed to take?

Each individual scenario should not take any longer than 10 minutes
(and this is a very generous upper limit - this means they are normally expected to take much less than this).
I
If the tests are taking significantly longer than the time specified, the following
may be at fault:

* Verbose Output Slowing Execution
  If the SDKD is being run with verbose output, the amount of output may actually
  flood the terminal - at this point, the high volume of output the terminal must
  display will actually *block* execution. The solution for this is to either
  decrease the verbosity level on the SDKD (the default levels are sane enough)
  or to redirect shell ouptut to a file

* Slow Cluster
  It may indicate a bug in the cluster, or issues with the underlying execution
  environment (disk, RAM, network) which slow things down. To see if this is the
  case, log into one (or all of the nodes) via the web UI and inspect the cluster
  status. This will indicate what is happening. The number of ops/second and the
  rebalance percentage progress (if a rebalance operation is underway) will help
  measure overall progress and the amount of remaining time.

* High sleep intervals specified
  Most scenarios have several sub-options which direct `stester` to sleep for
  specified period of time, while the SDKD performs its workload. The sleeps
  are employed to increase sample time for statistical collection as well as
  to avoid some cluster (server)-side bugs which may arise. The defaults for
  all these values are sane - but it is possible that you have manually specified
  a very long wait time.

* Network issues between `stester` and the cluster or SDKD
  If there are issues in the network link between `stester` and the SDKD, between
  the SDKD and the cluster, or betweeen `stester` and the cluster, delay times
  might take abnormally long. This may happen when the connection is _broken_
  (a slow link should not drastically affect the duration of the test). If the
  test is taking too long, trying to ping the involved hosts may shed light
  on whether this is the issue.

The breakdown for the estimated duration for each operation is listed below


1. Initial cluster setup (reset, join nodes, create buckets)
   	* 20-60 Seconds

2. Launching the SDKD (not all plugins do this)
   	* 0-5 seconds

3. Preloading all the items into the cluster (can be disabled via `--kv-preload false`)
   	* 5-120 seconds; depending on how well the SDK handles multi operations.
 
4. Connecting to the SDKD (`NEWHANDLE` commands).
   	* 2-80 seconds (depending on the SDKD and workload, and how many threads
   	  are being used. Use `--kv-nthreads` or `--wlhybrid-kv-nthreads` to modify.

5. Ramp Phase (invocation of workload before cluster change)
   	* 30 seconds default. May be longer or shorter as this option is
   	  user-configurable via the `--scenario-ramp` option
 
6. Rebalance (not all scenarios perform a rebalance)
   	* 60-400 seconds. This is variable depending on cluster configuration,
   	  version, and number of items in the cluster.
 
7. Failover (not all scenarios perform a failover)
    * 60 seconds. Note the failover itself is typically instant
	  the delay is there to ensure the SDK receives and responds to appropriate
	  vBucket configuration changes. You can adjust the failover sleep interval
	  using the `--failover-next-delay` option.

8. Rebound (continuation of workload after cluster change)
   	* 90 seconds default. May be longer or shorter as this option is
   	  user-configurable via the `--scenario-ramp` option.

9. Cleanup - when the `CANCEL` commands are sent
   	* 0-100 seconds. This depends on the SDK. This is usually instant,
   	 but long delays may be encountered, as the SDK may be in middle of an
   	 operation - the `CANCEL` command will only cancel each handle
   	 once it has finished performing its current data store operation

If one adds together all the seconds (using the maximum number), the total
duration ends up being a bit under 11 minutes. The test duration is typically
less.


## Can I run multiple tests (or instances of `stester`) concurrently?

The short answer is _no_. The long answer is _it depends_.

`stester` itself does not employ any form of global system settings which would prevent
multiple invocations of it, however it expects exclusive ownership over the cluster nodes
which it manipulates.

Additionally, it is not recommended to run multiple instances of the SDKD implementation
on the same host, due to load constraints.

Finally, the default output file, `sdkdclient.log.h2.db` will be clobbered by multiple
instances, unless you specify a different output file for each instance.


## Can I specify or combine multiple scenarios in the same instance of `stester`?

Not exactly.

Broadly put, a _scenario_ describes the _flow of execution_ regarding how
`stester` interacts with the cluster. Only one scenario can run at a time, since
it would not make sense to remove nodes in the cluster and failover other nodes
at the same time (only one or the other may happen).

It is however possible to perform a failover followed by a rebalance, and the
like, however most of these common situations are already provided as options
to existing scenarios.

You may always _extend_ `stester` by writing your own scenarios.

## Can I specify or combine multiple workloads in the same instance of `stester`?

Workloads are already "combined" when using the Hybrid workload class. This
essentially combines various other workloads, partitioning various SDKD threads
for various sub-workloads.

Workload options should be sufficient for most things.

You may also write your own workloads in Java

## Can I run multiple SDKDs from within the same `stester` instance?

While it is technically possible to write a `MultiExecutingDriver`, there would also
need to be the ability to determine which SDKD failed and such.

## How can I parse analyze the logs from `stester`?

See [logview](logview.md)

## How do I know which SDK version a test was run with?

This information is included in the beginning of the `logview` output.
This will typically contain the SDK version information (if available). Some
builds cannot provide the version information, unfortunately.

To quickly see the version information:

```
./logview -f sdkdclient.logdb.h2.db -F c
< SNIP ..>
SDKD information
  CONFIG => CONNCACHE:, andIO_PLUGIN:, RECONNECT:0
  HEADERS => CHANGESET:"f1a8d8bd81cf07f4215889b2075e25fe9154aefe", SDK:0x20403
  RUNTIME => SDK:0x20403
  TIME => 1415214201
  CAPS => CANCEL:true, CONTINUOUS:true, DS_SHARED:true, PREAMBLE:false, VIEWS:true

```

## How can I reproduce a test from a log file?

Often it is necessary to re-run a test after having made some code changes. The
information needed to reproduce the test may be found in the logfile.

You can simply use the `-F A` option for `logview`. This will output an
INI-style configuration file, suitable as a value to the `-I` argument
of `stester`.

The following command will create a file called `repro.ini` which contains
the configuration for the test which produced the file `first_run.log.h2.db`,
and then re-run the test, using `stester`

```
./logview -f first_run.log.h2.db -F A | grep -v '^;' > repro.ini
./stester -I repro.ini -o second_run.log
```

> You may wish to change the IPs of the nodes (in the `[cluster]` section)
> and the path to the SDKD (in the `[exec]` section) if you are running
> the test in a different environment.