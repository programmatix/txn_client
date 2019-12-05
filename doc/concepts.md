<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->
**Table of Contents**  *generated with [DocToc](http://doctoc.herokuapp.com/)*

- [Couchbase SDKD Framework](#couchbase-sdkd-framework)
  - [Introduction](#introduction)
    - [Clients and Servers](#clients-and-servers)
  - [Terminology/Concepts](#terminologyconcepts)
    - [General Couchbase Terminology](#general-couchbase-terminology)
    - [vBucket Mappings](#vbucket-mappings)
    - [SDKD Terminology/Concepts](#sdkd-terminologyconcepts)
    - [_stester_ Terminology/Concepts](#_stester_-terminologyconcepts)
- [Getting Started](#getting-started)
  - [Client Installation](#client-installation)
    - [Dependencies](#dependencies)
  - [SDKD (Server) Installation](#sdkd-server-installation)
  - [Executables](#executables)
  - [](#)
  - [Workloads](#workloads)
    - [`GetSetWorkloadGroup`](#getsetworkloadgroup)
      - [Options (`--kv-*`)](#options---kv-)
      - [Examples](#examples)
        - [5 Worker threads setting values of 256 bytes in size without a delay](#5-worker-threads-setting-values-of-256-bytes-in-size-without-a-delay)
        - [Large value sizes with a delay between 100 and 200 milliseconds](#large-value-sizes-with-a-delay-between-100-and-200-milliseconds)
    - [`ViewWorkloadGroup`](#viewworkloadgroup)
      - [Options (`--views-*`)](#options---views-)
      - [Examples](#examples-1)
        - [Create a view with 100 documents](#create-a-view-with-100-documents)
        - [Create a view and have the worker threads use `stale=false`](#create-a-view-and-have-the-worker-threads-use-stale=false)
    - [Hybrid Workload (`HybridWorkloadGroup`)](#hybrid-workload-hybridworkloadgroup)
      - [Options](#options)
  - [](#-1)
  - [Cluster Setup](#cluster-setup)
    - [Cluster Setup Process](#cluster-setup-process)
      - [Sanity Check](#sanity-check)
      - [Setup](#setup)
    - [Options](#options-1)
  - [Configuring via Files](#configuring-via-files)
  - [SDKD Selection](#sdkd-selection)
    - [Diagnosing SDKD Issues](#diagnosing-sdkd-issues)
  - [Network Setup and Access](#network-setup-and-access)
    - [Memcached Protocol](#memcached-protocol)
    - [SDKD Protocol](#sdkd-protocol)
    - [Protocol Discussion Summary](#protocol-discussion-summary)
    - [Permissions and Privileges](#permissions-and-privileges)
      - [SSH Access](#ssh-access)
      - [Administrative Credentials](#administrative-credentials)
      - [Bucket Credentials](#bucket-credentials)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

# Couchbase SDKD Framework

SDKD is a framework for testing and instrumenting Couchbase client libraries.
It is designed to be client neutral and portable, so that scenarios need to
be designed and defined once, and then may be reproduced with any client
library having an SDKD server implementation.

## Introduction

SDKD is a powerful and versatile client-server architecture which allows
testing and duplication of scenarios across multiple client libraries (SDKs).
It allows to set up complex cluster scenarios with client behavior patterns
and execute the scenario to determine client behavior. It provides facilities
by which to report client behavior and help determine success or failure of
the client.

The SDKD framework was designed and written to establish a common platform for
SDKs to perform and execute various couchbase data store operations against a
cluster and report on their successes. Rather than write many individual
scripts to perform individual operations and report results in an ad-hoc
fashion, the SDKD formalizes the way operations are executed and their
results reported.

The SDKD employs its own protocol which is used to convey information about
the types of operations an individual SDK will perform and how the status
and performance metrics of these operations will be reported.

The SDKD Server itself is a server written with various SDKs in different
languages; and is responsible for converting the SDKD protocol commands to 
native SDK operations, for example `client.get(key)`.

The primary Couchbase SDKs have their own SDKD Server implementation which
map the SDKD protocol commands into native client methods; currently there
are implementations in:

* [C++](github.com/couchbase/sdkd-cpp) (for libcouchbase).
* [Java](github.com/couchbase/sdkd-java) (for _couchbase-java-client_).
* [C#](github.com/couchbase/sdkd-net) (for couchbase-net-client).
* [Python](github.com/couchbaselabs/sdkd-python)
* [Perl](github.com/couchbase/sdkd-perl)

In this sense, one may think of the SDKD as a form of proxy sitting in 
between the SDKD Client and the Couchbase cluster. The client tells the 
SDKD which datastore commands to execute against the cluster, and the SDKD 
Server actually executes said commands.

### Clients and Servers

>Throughout this document we will be using the term _client_ and _server_.
These terms may refer to either the relationship between the *Couchbase* client
(i.e. the client library) and server (i.e. the thing which listens on port 8091)
or they may refer to the *SDKD* client (i.e. the various executables shipped with
the `sdkdclient-ng` distribution) and the SDKD server (i.e. an SDK-specific implementation
of an executable which implements the SDKD protocol).
>
It should normally be simple to infer the type of client-server being discussed


## Terminology/Concepts

The following will list some terms used in this document and their general
meaning

### General Couchbase Terminology

This section will outline terminology and concepts which are common for
Couchbase. Most of these are documented elsewhere on the Couchbase website

* **Couchbase Cluster**, **Couchbase Server**.

    This means the server product published by Couchbase. Couchbase is a distributed
    and scalable key-value and document based high performance database. This term
    refers to the product as a whole.

* **Node**, **Cluster Node**.

    Refers to a single host which is part of a Couchbase cluster. Since the Couchbase
    cluster can contain more than a single host, the term _node_ refers to one of
    possibly many participants in the cluster

* **Key-Value Entry**, **Document**

    Refers to an entry within the Couchbase database. As Couchbase is a key-value
    database, accessing data within the database is performed by associating a key
    with a value. These terms refer to the key and its associated value.

* **Data Store**

    Refers to operations and commands which retrieve or manipulate data within the
    couchbase server; for example, getting a key-value, setting a key-value, deleting
    a key-value.

* **Bucket**

    This may be thought of as a namespace within the Couchbase server. Each bucket
    contains its own set of key-value databases, disk and memory quotas, and
    replication settings. For those with an RDBMS/SQL background, a Bucket is similar
    to a _database_

* **vBucket**

    *Not related to _Bucket_ (!!!)*. A vBucket may be thought of as a logical
    partition for a group of entries. Each entry is _mapped_ to a vBucket, but
    a vBucket typically contains many entries (this mapping is done by using a
    hashing algorithm on the key). Each couchbase bucket will typically contain
    about 1024 vBuckets (regardless of the number of entries). Clusteing is attained
    in couchbase by having each node within the cluster assigned a set of vBuckets
    (e.g. if the total number of vBuckets within the cluster is 1024, and there are
    four nodes, then each node will host 1024/4 (256) vBuckets). This allows
    scalable _sharding_ or partioning of data, such that on average, each node is
    contacted only 1/4th of the time for data store operations. (This gets a bit
    more complex when replication is enabled, but is outside the scope of this
    discussion).

* **SDK**, **Client**, **Couchbase Client**

    This is a library which communicates directly with the Couchbase cluster to
    perform data store operations. There are multiple clients available written
    in multiple languages. They communicate with the Couchbase cluster using a
    set of common protocols

* **Memcached Protocol**

    This is the primary protocol used by Clients to perform data store operations
    on the Couchbase cluster. The protocol is designed to communicate data about
    key-value entries.

* **Memcached Server**, **Memcached**

    This is the component with the Couchbase cluster which handles the Memcached
    protocol. Typically a client will connect to one or more Memcached servers and
    request data store operations using the Memcached protocol. Each cluster node
    contains a running instance of memcached which interacts with more internal
    components within the cluster.


### vBucket Mappings
>Clients communicate with memcached for most data store operations, and each
data store operation affects a single entry. Because there may be multiple
memcached instances within the cluster, the client must know which particular
memcached instance should be used for which particular entry. The client makes
this decision by doing the following steps:
>
1.  Determines which _vBucket_ the entry's key is mapped to
2.  Looks up which memcached instance is the assignee for the vBucket
>
This second stage is performed through obtaining a _vBucket Map_. The vBucket
is obtained periodically from the network. It contains information such as
the total number of vBuckets available as well as which vBuckets are assigned
to which nodes.

* **Configuration Change, Configuration Update**

    This refers to the event where the Client receives a vBucket map. This lets the
    client know which nodes are assigned which vBuckets. Clients will typically cache
    the vBucket map after each configuration update in order to improve performance
    when performing data store operations. However when a new configuration update
    is received, its vBucket map may be different from the map the client has cached.
    At this point, the client must promptly replaced the cached version with the
    updated version. Failure to do so may result in the client selecting the wrong
    node for data store operations, thus making them fail.

* **Couchbase REST API, Couchbase Administrative API**

    This is an administrative interface which allows a user to manipulate the
    cluster, doing things such as creating buckets, deleting buckets, modifying
    bucket properties, adding nodes, removing nodes, and declaring nodes as dead.
    This is acheived by communicating with the REST API (over HTTP). It is important
    to note that the REST API and administrative operations are distinct from the
    data store APIs and operations: The former affects the cluster as a whole, and
    does not alter or access entries within the database, whereas the latter
    is able to access individual entries, but not perform administrative duties
    like adding nodes or creating buckets.

* **Cluster Topology**

    Refers to the overall configuration of a Couchbase Cluster; e.g. how many nodes
    comprise this cluster, how many buckets are configured within the cluster, and
    how many replicas are available for each bucket. This term also includes the
    status of each node (i.e. whether the node is "up" and is functioning properly
    or whether the node is "down" (and is unavailable or malfunctioning)).

* **Topology Changes**

    Refers to a change in the cluster topology. Changes happen when a node is added,
    a node is removed, a node becomes unavailable, or a node becomes available.
    A topology change may or may not affect a _configuration change_.

* **Rebalance**

    This is a specific administrative operation which _applies_ any pending topology
    changes. This is a lengthy process (usually several minutes). For the GUI-oriented
    this is typically equivalent to clicking _Apply_ on an options dialog box after
    changing several settings.
    A typical use of rebalance may be to add multiple nodes to the cluster and then
    apply the changes. This is significantly quicker than rebalancing after adding
    each individual node.
    Clients may receive multiple _configuration updates_ during a rebalance operation.


### SDKD Terminology/Concepts


This section builds upon the concepts listed in the _General Concepts_ section.

* **Dataset**

  Refers to a collection of _Entries_. A dataset may theoretically look like so:

  ```json
  dataset = {
      "key1" : "value1",
      "key2" : "value2",
      "key3" : "value3",
      ...
  }
  ```

* **Dataset Seed, Dataset Specification**
   Refers to a means by which a _dataset_ can be obtained and/or generated. Having
   defined a _dataset_ itself as a collection of key-value entries, the
   _specification_ describes how to obtain and/or generate the entries themselves.
   Typically this will be done by using a _seed_, which provides properties for
   generating it.

      ```javascript
      var seed = {
        count : 3,
        key_prefix : "key",
        value_prefix : "value"
      };

      var dataset = {}

      for (var i = 0; i < seed.count; i++) {
        var currentKey = seed.key_prefix + i;
        var currentValue = seed.value_prefix + i;
        dataset[currentKey] = currentValue;
      }
      console.dir(dataset);
      // {
      //   key_prefix0: value_prefix0,
      //   key_prefix1: value_prefix1,
      //   key_prefix2: value_prefix2
      // }
      ```

* **Batched Command, SDKD Command**
  Refers to a means by which to specify multiple data store operations within a
  compact format. This is typically acheived by employing a dataset seed.
  A command typically consists of the following:
  1. The data store command to perform
  2. The dataset which will furnish the keys and values for the data store command

  Thus for each entry within the dataset, the specified data store command is used
  to access it on the Couchbase server. The actual data store command is performed
  by the _SDK_

* **SDKD Protocol**
  This is a network protocol designed to express _batched_ operations
  and receive their status summaries. The SDKD protocol is typically used to
  transmit batched commands

* **SDKD, SDKD Server, SDKD Implementation**
  A program which utilizes an _SDK_ to perform multiple data store operations,
  according to settings specified in the SDKD batched command.
  The SDKD is compiled with/against the SDK (employing the SDK as a dependency)
  and interacts with the SDK to perform these operations. After multiple data
  store operations have been performed, the SDKD generates a status summary
  for these data store operations.
  As there are multiple SDKs available, so too are there multiple SDKDs available,
  with the primary SDKs having an SDKD implementation which interacts with the
  specific SDK.

* **SDK Handle, Client Object**
  Refers to an instance of the SDK library. This may be thought of as the
  ``connection"" from the SDK to the Couchbase Cluster.

* **SDKD Handle, SDKD Thread**
  An SDKD handle may be thought of as an ephemeral thread which interacts with
  an SDK handle. The SDKD interacts with the SDK by first creating an SDKD handle
  which is associated with a client object. The SDKD handle receives an SDKD command
  as input and then interprets the SDKD command into multiple data store commands.
  Multiple concurrent SDKD handles can be running, allowing the SDKD to perform
  multiple operations in parallel.

* **SDKD Client**
  Refers to a program which communicates with the SDKD; the SDKD client will
  send commands to and receive responses from the SDKD server. The SDKD client
  communicates with the server using the SDKD protocol.


### _stester_ Terminology/Concepts
This section builds upon the previous two sections.

`stester` is an SDKD client as well as a cluster manipulator.

* **Workload**

    A workload refers to the process of creating one or more SDKD handles, and
    sending certain SDKD commands to these handles. Thus a workload may be thought
    of as a configuration construct which determines:

    * How many SDKD handles to create
    * What SDKD commands should these handles perform
    * Which dataset specifications should be passed for the SDKD commands


* **Scenario**, **Testcase**

    Refers to configuration options determining how to manipulate the cluster. This
    is typically done by performing one or more _toplogy changes_ using the Couchbase
    _REST API_. The scenario is executed concurrently with the _workload_.
    
* **Driver**

    This component indicates how to connect to the _SDKD_ component (where the SDK
    is run). This will establish a socket connection to the _SDKD_ and allow
    the _Workload_ component to deliver commands to the _SDKD_. Multiple helper
    classes for the _Driver_ are provided which perform tasks such as invoking
    a remote process with arguments.
 
# Getting Started

Using the SDKD framework requires both the SDKD Client and SDKD Server

## Client Installation

### Dependencies

If simply wanting to run the framework itself.

* JRE 1.6
* Download the release archive; e.g. http://sdk-testresults.couchbase.com.s3.amazonaws.com/sdkdclient-1.0-SNAPSHOT-366508a.zip

To build from source, check out the git project and run `mvn package`


## SDKD (Server) Installation

See documentation for the individual SDKD for further instructions. This should
really be in an appendix eventually


## Executables

The client distribution (i.e. _sdkdclient_) ships with a number of executables which
perform various functions. Here is a brief overview of them

* SDKD Clients
  These components communicate with an 'SDKD' and have it perform data store
  operations against a Couchbase cluster. All 'SDKD' clients communicate using the
  SDKD '
  * [`simpleclient`](simpleclient.md)
  Written more as a simple entry point into the SDKD itself, this can be
  mainly for demonstrative purposes. We'll show `simpleclient` a
  This does not manipulate the 
  
  * [`stester`](stester.md)
  This client sets up the cluster and possibly spawns an SDKD. It sets up the
  Couchbase cluster into a given configuration, creating the necessary buckets
  and adding the required nodes. Then it effects a *scenario* on the cluster
  (e.g. rebalance, failover, node addition, etc).
  While the scenario is being executed, it also commands a *workload* pattern
  to the SDKD itself; instructing what data store operations to perform, and
  the rate at which they should be performed.
  Finally, it writes a log file in a special format (if logging was requested).
  
	 `stester` thus requires the following inputs:
	   * Cluster Node Information
	   This is a text file containing information about the hosts which are to be used
	   as part of the Couchbase cluster
	   * SDKD Information
	   This is a special directive specifying which SDKD implementation to use. Depending
	   on the value of this specifier, the SDKD may be downloaded, compiled, and executed
	   * Scenario Selection
	   This specifies what `stester` will do to manipulate the cluster. This is one of
	   several pre-defined scenarios (listed elsewhere); these scenarios will typically
	   do a rebalance, failover, node addition, node removal, or combination thereof.
	   * Workload Selection
	   Specifies the type of commands and the command frequencies the SDKD will perform
	   _while_ the specified 'scenario' is performed against the cluster


* Result Analysis
  These executables analyze various aspects of the log output or SDKD error codes

    * [`logview`](logview.md)
      Parses and displays the `stester` log output in a human-readable format. This
      program may be run with several options controlling the level of detail.

    * `strerror`
      Simple tool which attempts to analyze a numeric SDKD error code


* Batched Execution
These executables form tools in executing multiple invocations of `stester`.
They help in storing and managing test execution.

    * [`brun`](running_suites.md) - Batch Executor
    Invokes a series of tests using `stester`, based on selections of various
    definitions (stored via `dbmanage`)

    * `report` - Batched Run Viewer
    Generates reports from the test database

--------
## Workloads

This section will outline the various pre-defined workloads available to `stester`.

As mentioned previously, a workload is a class which defines how the SDKD
should perform operations. Selection of the workload and its options may
be used to create various stress and performance tests by changing the
frequency of the operations and the sizes of the key-value data which the
SDKD (and SDK) send to the cluster.

The workloads themselves are defined in the `com.couchbase.sdkdclient.workloads`
package. You may add more workloads assuming it conforms/derives to/from the `c.c.s.workloads.WorkloadGroup` and `c.c.s.workloads.Workload` abstract classess.

Workloads may be supplied to `stester` using the `-W` option. The `-W` option expects a class name which is located within the `com.couchbase.sdkdclient.workloads` namespace. You may also pass a fully qualified class name which can be loaded as a JAR as well.


### `GetSetWorkloadGroup`

*Use `-W GetSetWorkloadGroup`*

This is the "standard" workload which is used for basic key-value testing. It spins up several SDKD handles which act as _setters_, setting key-value entries to the cluster; it also spins up _getter_ handles which retrieve those key-value entries from the cluster. Both
_setter_ and _getter_ handles operate on the same dataset concurrently.


#### Options (`--kv-*`)

These options are available to modify the `GetSetWorkloadGroup` and are
prefixed with `--kv`


<table>
<tr><th>Option</th><th>Description</th><th>Value</th></tr>
<tr>
	<td><code>--kv-nthreads</code></td>
	<td>Set the number of thread pairs to perform gets and sets. If the default <code>opmode</code> is applied, the number of threads will be double this value
	</td>
	<td>Number of thread (pairs). The default is <code>10</code></td
</tr>
<tr>
	<td><code>--kv-opmode</code></td>
	<td>Determine if this workload will set, get, or both set and get items</td>
	<td>The string <code>GETTER</code> to only get items, the string <code>SETTER</code> to only set items, or the string <code>BOTH</code> to use both getters and setters. The default is <code>BOTH</code></td>
</tr>
<tr>
	<td><code>--kv-delay-min</code>, <code>--kv-delay-max</code>, <code>--kv-delay</code></td>
	<td>Set the delay that each thread should use between successive key operations. If <code>--kv-delay-min</code> and <code>--kv-delay-max</code> are specified then the wait value will be a random number generated ranged between the former and latter. If <code>--kv-delay</code> is specified, then the delay will be fixed to that length of time.</td>
	<td>A value in milliseconds. The default places the lower range (<code>--kv-delay-min</code>) at 1 and the upper range (<code>--kv-delay-max</code> at 10)</td>
</tr>
<tr>
	<td><code>--kv-ksize</code>, <code>--kv-vsize</code></td>
	<td>Set the sizes for the generated keys and values to be manipulated. These two values affect the size of the keys and values (respectively) in bytes. The resultant size is not guaranteed to be exactly this amount but is guaranteed to be <i>at least</i> the given amount. Note that there is a cluster-side limitation in which keys may not exceed 150 bytes and values may not exceed 20MB</td>
	<td>Size in bytes. The default key size is 32 and the default value size is 128</td>
</tr>
<tr>
	<td><code>--kv-timeres</code></td>
	<td>Set the output resolution for sampling error and performance information. This determines the interval at which a new TimeWindow will be created to collect statistics. If a test is being run for a long time (many hours) it may be advisable to set this number higher as otherwise the underlying SDKD may run out of memory.</td>
	<td>The resolution in seconds. The default is 1</td>
</tr>
<tr>
	<td><code>--kv-batchsize</code></td>
	<td>Set how many operations the setter and getter threads will perform in a single pass. Setting this value to a number higher than one will enable <i>bulk</i> or <i>multi</i> mode on the SDKD.</td>
	<td>The number of operations to batch. The default is 1</td>
</tr>
<tr>
	<td><code>--kv-kvcount</code></td>
	<td>Set the total number of items that all threads will operate on. This will determine the total sie of the dataset inside the cluster. Setting this number will affect disk storage and rebalance times on the cluster</td>
	<td>Number of items to be stored/retrievedd to/from the cluster. The default is 15,000</td>
</tr>
<tr>
	<td><code>--kv-preload</code></td>
	<td>Preload the dataset into the cluster before starting the worker threads. Enabling this option helps eliminate spurious <code>ENOENT</code> errors which may arise later during the testing. Items will be loaded into the cluster (using a single SDKD thread and batching 100 items per call). This will guarantee that any subsequent GET on any item within the dataset will not yield a not-found error under normal circumstances</td>
	<td>Boolean. The default is false</td>
</tr>
</table>

#### Examples

##### 5 Worker threads setting values of 256 bytes in size without a delay

```
-W GetSetWorkloadGroup --kv-nthreads 5 --kv-opmode SETTER --kv-delay 0 --kv-vsize 256
```

##### Large value sizes with a delay between 100 and 200 milliseconds

```
-W GetSetWorkloadGroup --kv-delay-min 100 --kv-delay-max 200 --kv-vsize 16384
```

### `ViewWorkloadGroup`

*Use `-W ViewWorkloadGroup`*

This workload uses the MapReduce view query feature of the Couchbase server.
It creates a pre-defined JavaScript view and instructs the SDKD to query the
view continually.

The order of operation is something like:

1. Create the MapReduce query function (i.e. upload it to the server by `PUT` ting it there)
2. Load the server with keys and values which will constitute the results of
   these queries (i.e. create values with well-formed JSON and store them on the server)
3. Create SDKD handles to query the view

Conceptually speaking, the mechanics of the View workload is quite similar to the
`GetSetWorkloadGroup`

#### Options (`--views-*`)

<table><tr><th>Option</th><th>Description</th><th>Value</th></tr>
<tr>
	<td><code>--views-dvname</code></td>
	<td>Set the name for the design doc and the enclosed view. Typically not needed</td>
	<td>A string in the format of <code>DESIGN/VIEW</code>. Default is <code>test_design/test_view</code></td>
</tr>
<tr>
	<td><code>--views-qdelay</code></td>
	<td>Set the number of milliseconds to wait between successive view queries</td>
	<td>Value in milliseconds. By default there is no delay</td>
</tr>
<tr>
	<td><code>--views-noload</code></td>
	<td>Do not populate the bucket with documents for the view. Should be used if the documents have already been populated during a previous run</td>
	<td>Boolean. Default is false</td>
</tr>
<tr>
	<td><code>--views-nodefined</code></td>
	<td>Do not create the design doc and view. Useful if this has been created in a previous run</td>
	<td>Boolean. Default is false</td>
</tr>
<tr>
	<td><code>--views-total-rows</code></td>
	<td>Set the number of documents which should be loaded into the bucket</td>
	<td>Number of documents. Default is 1000</td>
</tr>
<tr>
	<td><code>--views-limit</code></td>
	<td>Set how many rows should be returned (in the worker thread) for each query. Modifies the <code>limit</code> view parameter</td>
	<td>Set to <code>-1</code> to disallow a limit. Otherwise the value will be passed as the limit. The default is <code>-1</code></td>
</tr>
<tr>
	<td><code>--views-nthreads</code></td>
	<td>Set the number of threads to be querying views</td>
	<td>Number of threads. Default is 10</td>
</tr>
<tr>
	<td><code>--views-timeres</code></td>
	<td>Time resolution for aggregation. Similar to the <code>--kv-timeres</code></option>
	<td>Default is 1 second</td>
</tr>
<tr>
	<td><code>--views-stalefalse</code></td>
	<td>Specify <code>stale=false</code> to the view query on each iteration to force a reindex</td>
	<td>Boolean. Default is false</td>
</tr>
</table>

#### Examples

##### Create a view with 100 documents

```
-W ViewWorkloadGroup --views-total-rows 100
```

##### Create a view and have the worker threads use `stale=false`

```
-W ViewWorkloadGroup --views-stalefalse
```

### Hybrid Workload (`HybridWorkloadGroup`)

*Use `-W HybridWorkloadGroup`*

This encapsulates both the `GetSetWorkloadGroup` and `QueryWorkloadGroup` in a single
invocation. Using this workload will result in having the SDKD run both KV/Get/Set operations and View query operations in a single instance (though in different threads of course). It will create _named_ workloads which will need to be analyzed using the `-W` option to `logview`.

Specifically, it creates these groups of handles

* Worklaod performing the `GetSetWorkloadGroup`. These are called `mc` as they test the _memcached_ interaction facility.
* Handles performing view queries which return results. These are called `cb`
* Handles performing view queries which return no results. These are called `http` since they test HTTP connect and header parsing.

#### Options

Options for this workload are prefixed with `--wlhybrid-`. As the workload itself is a grouping of three different workloads, it is generally useful to affect each workload directly.

Each underlying workload may be accessed by prefixing the `--wlhybrid-$WLNAME-$WLOPTION` format where:

* `$WLNAME` is the name of the workload (either `mc`, `cb`, or `ht`)
* `$WLOPTION` is the _non-prefixed_ option to control, i.e. `nthreads`, `noload`, `batchsize`.

Thus for example, to control the delay parameter (`--kv-delay`) in the `mc` workload, you would specify
`--wlhybrid-mc-delay 10`.


-------------

## Cluster Setup

These options affect the way the cluster is set up before any workload
or scenario is run. We'll first outline exactly what happens when `stester` sets up the cluster
and then present options which manipulate it.

### Cluster Setup Process

Cluster Setup is divided into several phases:

#### Sanity Check

This really just ensures we have the basic requirements needed to interact with
a Couchbase cluster:

* The nodes located within the _cluster configuration file_ (the `-i` or `-I` parameter)
are gathered.
* A remote `SSH` connection is established to each of the nodes listed in the
file. `stester` will fail with "authentication errors" if bad credentials were
provided.
* All ports on all the nodes' firewalls are unblocked. Ports may be blocked due to
previous runs of certain scenarios
* All couchbase server processes are resumed by sending them a `SIGCONT`. Processes
may have been suspended during a previous run
* `couchbase-server start` is invoked on each of the nodes. If the server process was
somehow stopped or crashed previously, this command ensures that it is running again.

#### Setup

This phase ensures that we have the proper node count and the proper buckets
needed to invoke the actual workload and scenario.

* Proper version of cluster is installed (if specified by options)
* Any existing buckets on the cluster are deleted. This makes the setup quicker.
* Any pending rebalance operations are stopped. This avoids some problems.
* If an existing cluster is present (i.e. multiple nodes are joined to a single
cluster) then all nodes in that cluster are removed; so that each node is in a
"blank" state (i.e. if one were to access the node using the web UI, they would
be greeted with the initial setup screen).
* One node is chosen at random to be the "master" node. The master node is the
node which all other nodes will join (this concept of "master" should be taken
lightly; it has no significance during vBucket operations or data storage, but
is merely the first node we use).
* Other nodes are joined to the "master" node. The number of nodes to be joined
depends on the scenario. In scenarios where nodes are added during the cluster
manipulation phase, the initial number of nodes is `total_nodes - nodes_to_add`
where _nodes_to_add_ is the amount of nodes which are added during the scenario
manipulation phase.
* After all the nodes have been joined, data bucket(s) are created. At this point
`stester` will poll each node to ensure that it has a ready vBucket map for the
specific data bucket.
* Scenario begins its _Ramp_ phase; this is when the SDKD is invoked.

### Options

The full set of cluster options are documented inside the [stester](stester.md#cluster_configuration) manual.

## Configuring via Files

While most of the options specified are in command line format, they may also be specified using _INI_-style configuration files. INI files are structured like so:

```
[section]
option1 = value1
option2 = value2 
; This is one type of comment
; This is another type of comment
```

Command line arguments may be converted into INI files by using the _prefix_ as the _section name_, and then placing the option name (without the prefix) as the option. An exception are top level arguments which have no prefix. These options are placed in a special section called `[main]`.

Here is an INI file followed by its equivalent on the command-line

```
[main]
# -C is an alias for --sdkd-config
sdkd-config = RemoteExecutingDriver

[cluster]
disable-ssh = true
node = 127.0.0.1:9000
node = 127.0.0.1:9001
node = 127.0.0.1:9002
node = 127.0.0.1:9003
username = Administrator
password = password

# For RemoteExecutingDriver
[exec]
path=mnunberg@localhost:/home/mnunberg/src/sdkd-cpp/build/sdkd_lcb
arg=--listen
arg=4444
port=4444

[scenario]
change-only = true
```

And the equivalent command line (line breaks inserted for readability)

```
bin/stester --sdkd-config RemoteExecutingDriver --cluster-disable-ssh \
	--cluster-node 127.0.0.1:9000 --cluster-node 127.0.0.1:9001 \
	--cluster-node 127.0.0.1:9002 --cluster-node 127.0.0.1:9003 \
	--cluster-username Administrator --cluster-password password \
	--exec-path mnunberg@localhost:/home/mnunberg/src/sdkd-cpp/build/sdkd_lcb \
	--exec-arg=--listen --exec-arg=444 --exec-port 4444 --scenario-change-only
```

You may specify _multiple_ configuration files to _stester_. They will be merged into a single configuration. If multiple files contain conflicting options, the one most recently mentioned in the command line will win, but a warning will be printed. Configuration files may be specified using the `-I` parameter, thus:

```
stester -I config1 -I config2
```

## SDKD Selection

This section discusses how one can use various SDKDs with `stester` effectively.
It also provides a rationale for some of the options available within the
`stester` SDKD selection framework.

As discussed previously, SDKD is the intermediary between the scenario/workload and
the SDK which performs the data store operations. SDKD conforms to a common
protocol interface, and it is therefore to use different SDKDs interchangeably
with `stester`.

That being said, different SDKDs have different methods of invocation and
compilation, since their SDK components differ. Building and running the C SDKD
may be different than building and running the Java SDKD.

At the most common level, _all_ SDKD implementations will listen on a TCP port
and accept connections made by the client (e.g. `stester`). The _stester_ client
in this case must know the port number to connect to; this is typically done
in one of two ways:

* An already-running SDKD is running on a predefined port. This port is passed
  directly (using `host:port` notation) as the `-C` option to `tester`.
* A _path_ to an SDKD executable (or wrapper thereof) is given to `stester`,
  (`--exec-path`) along
  with _additional arguments to pass to the SDKD itself_ (`--exec-args`) which
  indicate the port it should listen on (the format of these specific arguments
  is specific to the implementation of the SDKD itselft).
  This port is then passed, as another argument (`--exec-port`) to `stester`. The
  `--exec-*` family of arguments require that the `-C LocalExecutingDriver` be
  passed (a similar configuration, `RemoteExecutingDriver`, is also capable
  of launching this SDKD over SSH).




### Diagnosing SDKD Issues

For normal routine tests, it is recommended to launch the SDKD as a process
via one of the `ExecutingDriver` drivers, as this will allow _stester_ to
log the output of the SDKD (such as SDK logging output), as well as be able
to detect various sorts of process crashes.

It is important you specify the full path to the SDKD executable to be invoked,
otherwise _stester_ will fail and report that it could not launch the SDKD.
Launching the SDKD may also fail as well if the SDKD could not load all its
dependencies. In short, if there are issues launching the SDKD, ensure that:

1. The path (`--exec-path`) exists, and points to an executable
2. The executable can be launched properly
3. The arguments specified as `--exec-arg` are valid for the SDKD itself.
   Note that each individual argument should be specified as a _separate_
   `--exec-arg` value. _stester_ will internally quote each argument value
   passed to the executable.


If you have taken the steps above, and are still experiencing issues with
SDKD, then attempt to invoke the SDKD manually (if necessary, specifying the
port to listen on), and then providing the appropriate argument to _stester_;
thus for example, in one terminal you would do:

```
mnunberg@mbp15 ~/Source/sdkd-cpp/build $ ./sdkd_lcb -l 4444
[cbskd-test] runServer:198 Listening on port 4444
```

And in another:
```
./stester -C localhost:4444 -N <node1> ...
```

If further problems are still experienced, it may be the result of bugs in
the SDKD itself, or in the SDK. Since the _SDKD_ is an executable native
to the client platform being tested it may be debugged using the appropriate
debugger/IDE you prefer. For implementations which support command line "headless"
debugging (for example, C with Valgrind), you may make a simple shell script which
wraps the SDKD with the appropriate arguments and invokes it under the debugger.

## Network Setup and Access


As mentioned previously, the SDKD framework is network-oriented; the prime
components interact via TCP socket, and therefore it is not required that they
reside on the same machine. This section will detail how the SDKD components
communicate over the network, and what sort of user setup may be necessary
to ensure smooth operation.

As a preliminary, let us demonstrate the workings of the two primary protocols
used throughout the test framework; this will demonstrate the relation between
each of the protocols and help expose the bandwidth and networking considerations
needed for proper operation.

[NOTE]
The term _PDU_ will be used in the following sections. _PDU_ means _Protocol
Data Unit_ and serves in simple words to describe "the contents of the message"
or the "contents of the command"; roughly speaking.

> What follows is a mock-up of the protocols; these descriptions are not definitive
> by any means

### Memcached Protocol

The `memcached` protocol is the protocol used for key-value operations. In each
_memcached_ PDU, information about a single key is conveyed. For requests such as
GET, the request PDU contains the key information, and the response PDU contains
both the key and the value information.

If one wished to store the key `my_user_id` with the value `mnunberg`, the
following would be an approximation of the the network interaction between
client and server

>
The actual memcached protocol is _binary_. We are using text here for clarity.
Additionally, all memcached protocol headers also require the placing of the
vBucket ID for the included key.


Some examples:

**Request setting the key `my_user_id` with value `mnunberg`**:

    Command: STORE
    Key Length: 10
    Key: my_user_id
    Value Length: 8
    Value: munberg
    Expiration: 0

The contents of this command should be fairly simple to understand.

This request PDU is sent over from the SDK to the Couchbase cluster.
If the operation succeeds, the key is stored to the server,
and the server responds like so:

**Response for setting the key**

    Command: STORE
    Status: OK
    Key Length: 10
    Key: my_user_id
    CAS: 00000003

The response from the server indicates that we've successfully stored the key
with its value, and gives us the CAS in case we need it for future use.

Now, if we wish to retrieve the key, the interaction looks like the following

**Request getting the key `my_user_id`**

    Command: GET
    Key Length: 10
    Key: my_user_id

The SDK client sends this command to the couchbase cluster. If the key exists,
the cluster responds like so:

**Response to getting the key `my_user_id`**

    Command: GET
    Status: OK
    Key Length: 10
    Key: my_user_id
    Value Length: 8
    Value: mnunberg
    CAS: 000003

If we try to get a key that does not exist, the response will look a bit different

**Response to getting the key `non-existing-key`**

    Command: GET
    Status: NOT FOUND
    Key Length: 16
    Key: non-existing-key

Note the protocol overhead for each key, and that in our examples, the keys and
values themselves are smaller than the size of the other data (like the command,
CAS, and length properties). In reality the size of each memcached PDU is _at least_
24 bytes. This does not include the key or value themselves.

At Couchbase, one of our main selling points is the low latency and high speed
at which users can store and retrieve data, to and from the database. We often
claim sub-millisecond response time;
so let's see what we actually need bandwidth-wise to acheive these numbers.

Note that most production environments expect _at least_ a throughput of several
thousand ops/sec.


Assuming 12 byte keys and 128 byte values; the formulae for simple storage
operations looks like this:

    operation size = (12 * 2) + 128 + (24 * 2);
    operation size = 200

We estimate 200 bytes per operation; the base size for each PDU (request and
response) is 24 bytes. Each PDU will also have the key, which is 12 bytes.
The request itself will also have the value, which is 128 bytes.

Without even taking network _latency_ into consideration, to get 1000 operations
per second, the link required would be 200KB/s (1.5Mb/s). Conversely, at a link
speed of 200KB/s (1.5 Mb/s), each operation would take 1 millisecond to complete.

Considering that 1k ops/sec is well below the average, we'd need significantly
higher speeds in order to achieve the desired throughput. As such, operating
the memcached protocol over a slow link such as a VPN would not very much
simulate a production environment.


### SDKD Protocol

The SDKD protocol is a batch and control protocol. It instructs the SDKD (and
by extension, the SDK) to perform a series of operations on _multiple_ keys
and multiple values. The SDKD protocol does not actually transmit the keys
and values themselves, but simply tells the SDKD how to generate them.
The following demonstrates how this works.

Here is an example of an SDKD client instructing the SDKD to store 500 key-value
pairs. The keys will be approximately 12 bytes in length, and the values will
be approximately 128 bytes in length.

**Request setting 500 key-value pairs via SDKD**

	```javascript
		{
	    "Command" : "MC_DS_MUTATE_SET",
	    "Handle" : 1,
	    "ReqID" : 42,
	    "CommandData" : {
	        "DSType" : "SEEDED",
	        "DS" : {
	            "KSize" : 12
	            "KSeed" : "key_prefix_",
	            "VSize" : 128,
	            "VSeed" : "value_prefix_",
	            "Repeat" : "filler_text",
	            "Count" : 500
		        }
		    }
		}


	The above PDU is sent from the SDKD client (e.g. `stester`, `cbsdk_client`, etc.)
to the SDKD. Once the SDKD has received this command, it generates the keys and
values itself. We can use the `dsemo` app to see approximately how each of the
keys and values will look like

**Using `dsdemo` to see the key-value pairs which would be generated**

Note that I've only specified a count of 5. Showing all 500 key-value pairs
would take up a lot of space.

	$ python thedsdemo -c 5 \
	    --kseed "key_prefix_" \
	    --vseed "value_prefix_" \
	    --repeat "filler_text_" \
	    --ksize 12 \
	    --vsize 128

Outputs:

	key_prefix_filler_text_0 : value_prefix_filler_text_0filler_text_0filler_text_0filler_text_0filler_text_0filler_text_0filler_text_0filler_text_0filler_text_0
	key_prefix_filler_text_1 : value_prefix_filler_text_1filler_text_1filler_text_1filler_text_1filler_text_1filler_text_1filler_text_1filler_text_1filler_text_1
	key_prefix_filler_text_2 : value_prefix_filler_text_2filler_text_2filler_text_2filler_text_2filler_text_2filler_text_2filler_text_2filler_text_2filler_text_2
	key_prefix_filler_text_3 : value_prefix_filler_text_3filler_text_3filler_text_3filler_text_3filler_text_3filler_text_3filler_text_3filler_text_3filler_text_3
	key_prefix_filler_text_4 : value_prefix_filler_text_4filler_text_4filler_text_4filler_text_4filler_text_4filler_text_4filler_text_4filler_text_4filler_text_4


For each of the generated key-value pairs, the SDKD will instruct the encapsulated
SDK to store them to the (Couchbase) server. As we explained above in the _Memcached_
section, this would involve 500 individual memcached operations encompassing 1000
individual memcached PDUs (2 PDUs, a request and a response for each operation).

Once all memcached operations have been completed, the SDKD command is considered
done. At this point, the SDKD sends back a response to the (SDKD) client indicating
the status of all the operations.

**Response for the setting of 500 key-value pairs via SDKD**

	```javascript
	{
	    "Command" : "MC_DS_MUTATE_SET",
	    "Handle" : 1,
	    "ReqID" : 42,
	    "Status" : 0,
	    "ResponseData" : {
	        "Summary" : {
	            "0" : 500
	        }
	    }
	}
	```

Considering that the SDKD's PDU size is fairly small, and considering that
each SDKD PDU corresponds to a fairly long wait time (because all the related
memcached operations need to complete), the bandwidth requirements between the
SDKD and the client are minimal. It is perfectly OK for them to communicate
over the VPN.

### Protocol Discussion Summary

According to the above information, we can summarize the network interaction in
this table.

The meaning of the columns are as follows:

* Endpoint 1, Endpoint 2
These are two endpoints communicating over the network
* Protocol
This describes the protocol being used
* Function - describes the high level functionality and purpose that the protocol
serves between the two endpoints
* Performance Considerations
Notes about the frequency and network requirements for the protocol. It is assumed
that any two endpoints should also be able to communicate with each other :)


<table>
<tr>
<th>Endpoint 1</th>
<th>Endpoint 2</th>
<th>Protocol</th>
<th>Function</th>
<th>Performance Considerations</th>
</tr>

<tr>
<td>`stester` (harness, scenario)</td>
<td>Couchbase Cluster</td>
<td>REST API (HTTP); port 8091</td>
<td>Used to initialized, setup, and manipulate the cluster</td>
<td>Minimal performance considerations (any working link (e.g. VPN) is sufficient)</td>
</tr>

<tr>
<td>SDKD/SDK</td>
<td>Couchbase Cluster</td>
<td>Memcached Protocol (usually port 11210)</td>
<td>Used to store and retrieve individual key-values to and from the cluster</td>
<td>High performance, high frequency. The link between these components
should be local</td>
</tr>


<tr>
<td>SDKD/SDK</td>
<td>Couchbase Cluster</td>
<td>HTTP Protocol (Port 8092)</td>
<td>Used for view queries</td>
High performance, high frequency. The link between these components
should be local</td>
</tr>

<tr>
<td>SDKD/_stester_</td>
<td>SDKD Protocol (Port configurable)</td>
<td>Used to control the SDKD</td>
<td>Minimal performance considerations, any working link (e.g. VPN) is OK</td>
</tr>
</table>


Note that the connection to the `memcached` server is done internally by the SDK
and the SDK only needs to know the "REST API" port. The SDK will then obtain
specific `memcached` node/port information.


### Permissions and Privileges

In order for the SDKD system to function properly, it requires certain permissions
and credentials to be able to access systems. Note that it is still possible to
gain some functionality of the framework without escalated privileges.

#### SSH Access

By default, _stester_ will log into each of the nodes specified (as `-N` or `--node`)
and start the `couchbase-server` script (if not already running) and clear the System's
firewall. These commands require root privileges and thus need `sudo` as well as a login
with `sudo` permissions to be passed (see `--cluster-ssh-username`).

Note that it is possible to disable this behavior by passing `--cluster-ssh-disable`, which
will disable SSH access.

#### Administrative Credentials

If an existing Couchbase installation is present on the nodes, the administrative password
should be specified to _stester_, via the `--cluster-password` option.

#### Bucket Credentials

_stester_ by default will clear the cluster and delete all buckets. If the
`--cluster-noinit` option is passed, the cluster's configuration will remain untouched,
and the  `--bucket-password` option must specify the SASL password, if any, which
the bucket is secured with. Note that when _stester_ creates a bucket, it will also
use this value (if present) as the bucket password, thus making the bucket a SASL
bucket.