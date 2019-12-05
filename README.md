# SDKD Client

This is the next-generation SDK test harness, written in Java. It comes with
no dependencies (other than those retrievable via Maven) and can be
distributed in binary form on all major testing platforms.

It mostly retains command linlie compatibility and has full protocol-level
compatibility with existing SDKDs.

The `stester` script is also provided as a wrapper around the JAR.

See [Overview](doc/overview.md) for more information on the SDKD architecture
in general.


# Building
To build, simply run `mvn package`


# Running

To run the SDKD itself, you must first obtain a suitable client for it. You
can use the [C](https://github.com/couchbase/sdkd-cpp),
[Java](https://github.com/couchbase/sdkd-java),
or [C#](https://github.com/couchbase/sdkd-net) SDKDs. There may be other SDKD
versions available as well.

If you just want to see what this client can do, you can download an existing
compiled SDKD Java Jar from [http://sdk-testresults.couchbase.com.s3.amazonaws.com/share/sdkd-sample.jar](http://sdk-testresults.couchbase.com.s3.amazonaws.com/share/sdkd-sample.jar)


We'll use the Java version as it's obviously the most simple to get started with
(considering the client itself is in Java).

## Configuring your cluster

The simplest way to let the client know about your cluster is by giving it
an _configuration file_ telling it where it is. I'll show how I've set up my local
cluster using the `cluster_run` script (bundled with the Couchbase source code)

```ini
#ARGFILE:INI
[cluster]

; you might want to change these for your own cluster
node = 127.0.0.1:9000
node = 127.0.0.1:9001
node = 127.0.0.1:9002
node = 127.0.0.1:9003

; Don't try to connect via SSH since we're running on localhost.
disable-ssh = true

; This tells sdkdclient that all these hostnames are the same. Sometimes the
; cluster will choose a random listening address and it won't correspond with
; what we've specified in "node"
ip-aliases = 127.0.0.1/localhost/10.0.0.99
username = Administrator
password = password
```

Save this file anywhere you'd like. I've called it `cluster\_run.ini`. Be sure the
`#ARGFILE:INI` is the first line of the file.

## Configuring the SDKD

Since the SDKD is already built, this step only involves telling the sdkdlcient
about where your SDKD is listening on the network. For the Java SDKD, simply open
up a new terminal and run it like so:

```
shell> java -jar sdkd-sample.jar -listen 8050 -share 100 -sync true
```

Note that the `-share` and `-sync` parameters are there just to make the
connection quicker.

You should not see any console output thus far from the SDKD which is normal,
because it is waiting for a connection from the client side.

## Running the SDKD Client

Now that we've configured the server and the SDKD is listening, it's time
to start up the sdkdclient. To do so, run the following:

```
shell> ./stester -I conf/cluster_run.ini -C localhost:8050
+ exec java -ea -jar target/sdkdclient-1.0-SNAPSHOT.jar -I conf/cluster_run.ini -C localhost:8050 -c BasicScenario
[0.76 INFO] (RunContext run:215) Preparing and validating configuration
[1.10 INFO] (RunContext run:220) Configure the cluster and run the workload for 20 seconds. This scenario does not change the cluster
[1.11 INFO] (RunContext run:234) Starting cluster and driver
[7.52 INFO] (CBCluster setupNewCluster:369) All nodes added. Will rebalance
[8.55 INFO] (RebalanceWaiter sweepOnce:33) Rebalance complete
[8.56 INFO] (CBCluster setupMainBucket:325) Creating bucket default
[8.57 INFO] (CBCluster setupMainBucket:327) Bucket creation submitted
[15.76 INFO] (CBCluster bucketPostSetup:319) Bucket creation done
[15.79 INFO] (RunContext run:253) Driver and cluster initialized
[15.81 INFO] (RunContext call:266) Running scenario..
[37.04 INFO] (RunContext run:322) Closing SDKD Handles
```

Here, the most basic testcase is run which simply connects the SDKD to the
cluster and makes it run `get` and `set` operations for 20 seconds.

## Analyzing the results

By default, `stester` outputs to a file called `sdkdclient.logdb.h2.db` (the
`h2.db` suffix is mandatory and will always be present). To analyze this file
you can use the `logview` program. The most basic usage will simply show a quick
summary of the run:

```
shell> ./logview -f sdkdclient.logdb.h2.db -F p
Phase timings for RAMP
Ops/Sec: 6156
 [134788] OK: 129278, ERR: 0
  {[OK]: 129278, [MEMD:MEMD_ENOENT]: 5510}
  MIN: 0, MAX: 5, AVG: 0

Phase timings for CHANGE
-- No Info --
Phase timings for REBOUND
-- No Info --
```

You can also get an HTML formatted report showing performance metrics by running

```
shell> ./logview -f sdkdclient.logdb.h2.db -O html > report.html
```

which will write a timings report to stdout.

# More Reading

**General Concepts**:

  * [Concepts](doc/concepts.md)
  * [FAQ](doc/faq.md)

**Command Line Tools**:

  * [Simple Client](doc/simpleclient.md) is a application which
    demonstrates the basic workings of the system
  * [STester](doc/stester.md) Runs the tests and workloads
  * [Scenarios](doc/scenarios.md) Specifies the fault injection behavior
  * [Log Analysis](doc/logview.md) describes how to analyze logs once a test has been completed
  * [Logging](doc/logging.md) shows how to show more detailed information for _stester_.

**Protocol/Implementation Documentation**:

  * [Protocol Specifications](doc/sdkd-protocol.md)
  * [Timings Overview](doc/timings.md)
  * [Log Format](doc/logdb.md)


# Supported Features

The following features are currently supported

* Cluster "ini" files (`-i`)
* Argfiles (`-I`)
* Host:Port SDKD (`-C`)
* Key-Value Workload
* Basic Scenario
* Rebalance Scenario (add, remove swap)
* Logging (via `logback` for messages, SQLite/ORMLite for persistence)
* Logging analysis (via `logview`)
* Invocation of SDKD
* SSH Targets
* View Workload
* Hybrid Workload
* Automatic generation of config files
* Automatic grading of reports
* Spreadsheet generation

# TODO

I'd like to have the following features:

* GUI Configuration (i.e. Swing)
* More log output formats
I