<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->
**Table of Contents**  *generated with [DocToc](http://doctoc.herokuapp.com/)*

- [STester. The SDKD Harness](#stester-the-sdkd-harness)
    - [Primary Differences from `simpleclient`](#primary-differences-from-simpleclient)
  - [Basic Run](#basic-run)
    - [Cluster Configuration](#cluster-configuration)
    - [Running](#running)
  - [Manipulating the cluster](#manipulating-the-cluster)
  - [Configuration](#configuration)
    - [Option Naming](#option-naming)
    - [Passing configuration via a file](#passing-configuration-via-a-file)
    - [Passing options via commandline](#passing-options-via-commandline)
    - [Validation](#validation)
  - [Workload Configuration](#workload-configuration)
      - [Prefix: kv](#prefix-kv)
  - [Cluster Configuration](#cluster-configuration-1)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

# STester. The SDKD Harness

`stester` is a program which sets up a cluster and performs manipulations on it,
concurrent with an SDK and SDKD running. It then collects status results and
saves them to a logfile.


### Primary Differences from `simpleclient`

While conceptually speaking, `stester` builds upon `simpleclient` there are quite
a few differences in how `stester` interacts with the user in terms of required
inputs and outputs.

* Cluster Configuration
    `stester` requires a special file to tell it about which nodes are available
    for the cluster. While `simpleclient` connects to an already-running and set up
    cluster, `stester` performs much of the setup itself (you still need to have the
    base cluster package installed though).
    Additionally, `stester` must be able to connect to each node in the cluster
    using the `SSH` protocol (in order to perform some configuration steps).

* Workload Specification
    `stester` dispatches data store operations based on _Workloads_. Workloads
    define things such as which data store operations to perform, how often they
    should be performed, and how many threads should perform them. This offers
    more options than `simpleclient`, but the options interface is different.
    Also, workload specifications support Couchbase MapReduce views.


* SDKD Specification
    `simpleclient` requires the user to have an already-running SDKD listening.
    `stester` is typically able to construct the SDKD for you, and spawn it -
    though how this is done generally depends on the SDKD itself. Typically,
    options will be provided which allow the user to specify the location
    of the SDKD itself (both the actual directory it resides in, as well as the host
    machine the SDKD will be invoked on); as well as dependency/linkage information
    (which SDK version should the SDKD employ)

* Cluster Manipulation Configuration
    `stester` requires for user input the name of a _Scenario_ that contains
    code which instructs `stester` how to manipulate/change cluster topology.
    `simpleclient` does _not_ change cluster topology

* Detailed Logging
    `stester` offers a detailed logging format which provides much information
    regarding how the `SDKD` is invoked.

## Basic Run

We'll demonstrate the basic `stester` run. This will not do anything special
except demonstrate what `stester` does and the options it accepts.

First, we need to configure some files:

### Cluster Configuration
`stester` operates and manipulates a Couchbase cluster. As such, it must know
about all nodes in the cluster. `stester` requires a configuration file
describing the cluster topology as well as the credentials needed to access
all nodes in the cluster.


```ini
#ARGFILE:INI

[cluster]
node = 10.3.121.207
node = 10.3.121.208
node = 10.3.121.209
node = 10.3.121.212
username = Administrator
password = 123456

# For now, disable SSH
disable-ssh = true
```

Save this file as `cluster.conf`

By default `stester` will attempt to establish an SSH connection to each of the
cluster nodes and perform some sanity checks on it. The requirements for an
SSH connection are that you provide SSH login information within the configuration
file. Since this syntax is a bit more complex, we'll leave it for later.

The rest of the file contains several `node` entries which expects as its value
the hostname or IP of each node.

Finally there are the `username` and `password` entries which contain the
login information for the cluster's administrative interface.

Since we're not going to show you yet how to launch the SDKD automatically, you
should launch it manually in a different shell. Refer to the [simpleclient](simpleclient.md)
documentation for more information on that.


### Running

To run, invoke `stester` like so:

```
shell> bin/stester -C localhost:8050 -I cluster.conf
[1.26 INFO] (RunContext run:215) Preparing and validating configuration
[1.66 INFO] (RunContext run:220) Configure the cluster and run the workload for 20 seconds. This scenario does not change the cluster
[1.67 INFO] (RunContext run:234) Starting cluster and driver
[9.60 INFO] (CBCluster setupNewCluster:369) All nodes added. Will rebalance
[10.63 INFO] (RebalanceWaiter sweepOnce:33) Rebalance complete
[10.63 INFO] (CBCluster setupMainBucket:325) Creating bucket default
[10.64 INFO] (CBCluster setupMainBucket:327) Bucket creation submitted
[17.74 INFO] (CBCluster bucketPostSetup:319) Bucket creation done
[17.78 INFO] (RunContext run:253) Driver and cluster initialized
[17.79 INFO] (RunContext call:266) Running scenario..
[38.87 INFO] (RunContext run:322) Closing SDKD Handles
```

I'll explain the output here:

* `1.26`: Configuration is loaded and validated. Configuration includes the
    `cluster.conf` file and the other arguments passed

* `9.60`-`17.74`

    The cluster nodes are all reset to their original state and a new cluster
    is created with all the nodes as members. A new default bucket is created
    as well.

* `17.78`

    At this point, the initial _Control_ connection to the _SDKD_ has been made
    and the cluster has been set up. The actual workload is about to begin

* `38.87`

    The workload is complete and the handles are closed.


Once the run has completed, it will be written to a custom log format. The
output file will have the name `sdkdclient.logdb.h2.db` by default.

To inspect the contents of this file, use the `logview` executable.
We'll use several options to ensure the output is small and concise:

```
shell> bin/logview -f sdkdclient.logdb.h2.db -F d -i 5
+0s
 [28233] OK: 26049, ERR: 0
  {[OK]: 26049, [MEMD:MEMD_ENOENT]: 2184}
  MIN: 0, MAX: 20, AVG: 0

+5s
 [33936] OK: 29219, ERR: 0
  {[OK]: 29219, [MEMD:MEMD_ENOENT]: 4717}
  MIN: 0, MAX: 1, AVG: 0

+10s
 [34378] OK: 32685, ERR: 0
  {[OK]: 32685, [MEMD:MEMD_ENOENT]: 1693}
  MIN: 0, MAX: 1, AVG: 0

+15s
 [34196] OK: 31132, ERR: 0
  {[OK]: 31132, [MEMD:MEMD_ENOENT]: 3064}
  MIN: 0, MAX: 1, AVG: 0
```

This dumps timing and status information to the screen. On the commandline
the interval granularity was specified as `5` (i.e. `-i 5`) so we receive
timing information for each 5 second window of the run. Since the run was
only 20 seconds, we only have 5 windows.

Each of the windows contains status and timing information.

## Manipulating the cluster

`stester` relies on a set of plugins called _Scenarios_ to manipulate the cluster.
Since there are many ways to manipulate the cluster and each method may have
multiple options, you must first select a scenario and then pass it specific
options.

In order to view a scenario's available options, you must first specify it
on the commandline and then use the `--plugin-help` option to display the
available options.

In the previous run, the default scenario was used. It did not manipulate the
cluster but was still a loaded plugin.

Some of the other scenarios available are:

<ul>
  <li><b>rebalance</b>. Add or remove nodes from the cluster and rebalance</li>
  <li><b>failover</b>. Trigger a failover for one or more nodes</li>
  <li><b>servicefailure</b>. Disrupt/terminate various services for the server</li>
</ul>

In order to select a scenario on the commandline, you should pass it as an
argument to the `-c` option. For example to select the `rebalance` scenario,
pass:

```
shell> bin/stester -c rebalance -C localhost:8050 -I cluster.conf
```

## Configuration

`stester` can be configured using multiple means. It is advised that for fixed
scenarios or configurations, you use a configuration file, and for ad-hoc
configuration to specify the parameters on the commandline.


Configuration properties are outlined in a hierarchy, with each option being
of the form `prefix/option`. This way what an option effects can easily be
identified by looking at its name, and also avoids name clashes with other
components attempting to define the same option (e.g. `timeout` or `count`).

An exception to this rule are the top level options for `stester` which have
no prefix.

The relevant top level options will be documented here

<table><tr><th>Name</th><th>Description</th></tr>
<tr>
  <td><code>include</code></td>
  <td>Include additional configuration files. This option may be specified
  multiple times</td>
</tr>
<tr>
  <td><code>sdkd-config</code></td>
  <td>Specification for connecting to the SDKD. This can be a string in
  the format of <code>host:port</code>, but can also be the name of a plugin
  as will be shown later</td>
</tr>
<tr>
  <td><code>workload</code></td>
  <td>Workload group plugin name to use. The default is
  <code>GetSetWorkloadGroup</code>.
  See later for more information on workloads</td>
</tr>
<tr>
  <td><code>output</code></td>
  <td>Logfile location. Note that if the value does not have a suffix of
  <code>.h2.db</code> then it <b>will be appended</b>. This is a limitation
  of the underlying <i>H2</i> database format</td>
</tr>
<tr>
  <td><code>version</code></td>
  <td>Print version and exit</td>
</tr>
<tr>
  <td><code>debug</code></td>
  <td>This option can be specified multiple times. The format for this value
  is <code>category:level</code> where <code>category</code> is one of several
  predefined categories or a classname, and <code>level</code> is a verbosity
  level ranging from <code>TRACE</code>, <code>DEBUG</code>, <code>INFO</code>,
  <code>WARN</code>, and <code>ERROR</code>
  See [logging options](logging.md) for more details.</td>
</tr>
<tr>
  <td><code>disable-colors</code></td>
  <td>By default log information is disaplayed with console coloring. For some
  consoles this may be problematic. This option turns off coloring
</tr>
</table>

### Option Naming

Option names are varied and aliased in many places. In most places, an option
of `foo/bar/baz` is equivalent to `foo-bar-baz` and to `foo_bar_baz`
and to `foo.bar.baz`. When passing an option on the commandline, the option
must be prefixed via a double-dash (`--`).

### Passing configuration via a file

You may employ multiple configuration files. Files follow the normal *INI*
style key-value format. Note the following though

* The first line **must** be `#ARGFILE:INI`
* Section names are significant and represent the option prefix. In the file
    above (`cluster.conf`), there was a single section in the file (`cluster`)
    and multiple options under that section. The fully qualified option names
    were actually something similar to this:

    `cluster-node`, `cluster-username`, `cluster-password`, cluster-disable-ssh`.

* An exception to the above rule is for top-level `stester` options. In such
  a case, the section name should be `main` (i.e. `[main]`) even though there
  is no `main` prefix otherwise.


### Passing options via commandline

Any option can be passed via commandline. The commandline is also the best
place to obtain reference for an option.

If an option name is `cluster/username` then you may specify it on the commandline
as `--cluster-username`.

### Validation

Arguments and options are validated at a very early stage. This is by design
to enable errors to be detected early on.

## Workload Configuration

To configure a workload, specify the `-W` or `--workload`. Workloads themselves
are contained within _groups_, the idea being that a single _group_ may run
one or more workloads (and this is in fact the case with the _Hybrid_ workload).

This section will document the `GetSetWorkloadGroup` options.

The `GetSetWorkloadGroup` will spawn a number of SDKD handles and have them
perform similar operations on a given dataset. The parameters included can
modify the types of operations performed, the frequency of the operations,
and the number of handles created.

<h4>Prefix: <code>kv</code></h4>

<table><tr><th>Option</th><th>Description</th></tr>
<tr>
  <td><code>kv-opmode</code></td>
  <td>Determines whether the workload should get, set, or both set and get
  keys and values.
  <table><tr><th>Mode</th><th>Description</th></tr>
  <tr>
    <td><code>SETTER</code></td>
    <td>SDKD handles will be setting items in the cluster</td>
  </tr>
  <tr>
    <td><code>GETTER</code></td>
    <td>SDKD handles will be getting items from the cluster</td>
  </tr>
  <tr>
    <td><code>BOTH</code></td>
    <td>SDKD Handles will be both getting and setting the same dataset. This
    is the default. When this mode is selected, the <code>nthreads</code> value
    is effectively doubled as two handles are spawned for each thread</td>
  </tr>
  </table>
  </td>
</tr>
<tr>
  <td><code>kv-delay-min, kv-delay-max</code>
  <td>Set the minimum and maximum delay between each operation within the
  SDKD. This value is specified in milliseconds. See the related
  <code>DelayMin</code> field in the <a href="sdkd-protocol.md">protocol</a> for more
  information</td>
</tr>
<tr>
  <td><code>kv-delay</code></td>
  <td>Equivalent to setting <code>delay-min</code> and <code>delay-max</code>
  to the same value</td>
</tr>
<tr>
  <td><code>kv-ksize</code></td>
  <td>Sets the key size to use for the operations</td>
</tr>
<tr>
  <td><code>kv-vsize</code></td>
  <td>Sets the value size to use for the operations</td>
</tr>
<tr>
  <td><code>kv-kseed</code></td>
  <td>Sets the base string for the key</td>
</tr>
<tr>
  <td><code>kv-vseed</code></td>
  <td>Sets the base string for the value</td>
</tr>
<tr>
  <td><code>kv-kvcount</code></td>
  <td>Sets the absolute number of items within the dataset. This will
  dictate how many total items are stored (or retrieved) from the cluster.
  Since the workload will iterate over all the items as many times as needed, this
  number should only be raised if you wish to load a lot of data into the cluster
  </td>
</tr>
<tr>
  <td><code>kv-nthreads</code></td>
  <td>Number of SDKD handles to create. This may also mean how many threads
  to spawn, so be careful with this number and don't set it too high</td>
<tr>
  <td><code>kv-batchsize</code</td>
  <td>Set the number of items to be batched for each operation. Making this number
  more than <code>1</code> will make the SDKD perform a <code>multi</code>
  operation</td>
</tr>
</table>

<a id="cluster_configuration"/>

## Cluster Configuration

This section details the input parameters for the cluster and bucket setup.

In addition to a workload, `sdkdclient` needs to know about the cluster that
the SDK will connect to. In the best case scenario, the cluster should satisfy
the following criteria:

* Each node should be on a different host
* Each node should be running the same version of Couchbase Server
* Each node should have an SSH login.
* You should have 4 or more nodes.

Nevertheless, the harness can function in many different environments - so the
points above are recommendations and not requirements.

<table><tr><th>Option</th><th>Description</th></tr>
<tr>
  <td><code>cluster-node</code></td>
  <td>Specify a cluster node. The node may be specified in one of the following
  two manners:
  <ul>
    <li><b><code>1.2.3.4:8091</code></b>.<br>This is a simple address for the REST API
    entry point. If using SSH, it will assume that the current user (i.e.
    the user on which the `stester` application is running) is authorized
    for private-key login as the <code>root</code> user on this node. If this
    is not the case and you still desire SSH access, please use the next
    form.</li>
    <li><b><code>http://user:pass@1.2.3.4:8091,ssh://sshUser:sshPass@1.2.3.4:22</code></b><br>
    This indicates a combined HTTP+SSH URL to use. In this case,
    <code>user</code> is your <i>administrative</i> username for the cluster
    and <code>sshUser</code> is your <i>SSH Login</i> for the specific operating
    system within that node.</li>
  </ul>

  <b>Specify this option for as many nodes as there are in the cluster.</b>
  </td>
</tr>
<tr>
  <td><code>cluster-disable-ssh</code></td>
  <td>Disable any SSH access. Even if an SSH URI is not specified in the
  <code>cluster-node</code> list. This option will specifically disable any
  SSH logins.</td>
</tr>
<tr>
  <td><code>cluster-noinit</code></td>
  <td>Do <i>not</i> reinitialize the cluster. Reinitializing the cluster means
  destroying any existing buckets and resetting the nodes. This is usually
  necessary if the cluster is not in a known state. It may be useful to use this
  option however for quick runs. On average this shaves off about 30-40 seconds
  from startup time. Note that all other functionality will remain within the
  cluster.</td>
</tr>
<tr>
  <td><code>cluster-username</code></td>
  <td>This is short-hand for specifying the REST username for each node in the
  <code>cluster-node</code> list. You will want to use this option if you've
  specified the simpler (i.e. <code>1.2.3.4:8091</code>) form for the
  <code>cluster-node</code> option. The value is typically <code>Administrator</code>
  </td>
</tr>
<tr>
  <td><code>cluster-password</code></td>
  <td>This is the cluster password. It has the same semantics as <code>cluster-username</code>
  </td>
</tr>
<tr>
  <td><code>bucket-ram</code></td>
  <td>Number of MB to allocated to the bucket</td>
</tr>
<tr>
  <td><code>bucket-password</code></td>
  <td>If the bucket should be a SASL-authenticated bucket with a password then
  specify it here. If this value is non-empty it will be used</td>
</tr>
<tr>
  <td><code>bucket-name</code></td>
  <td>The name to provide for the new bucket. If not specified, the name will
  be <code>default</code></td>
</tr>
<tr>
  <td><code>bucket-add-default</code></td>
  <td>Whether in the case of creating a non-<code>default</code> bucket, the
  cluster should still retain a (second) bucket called <code>default</code></td>
</tr>
<tr>
  <td><code>bucket-type</code></td>
  <td>What the storage type of the created bucket should be. Choices are
  <code>COUCHBASE</code> or <code>MEMCACHED</code></td>
</tr>
<tr>
  <td><code>bucket-replicas</code></td>
  <td>Number of replicas the bucket should be configured with</td>
</tr>
</table>
