<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->
**Table of Contents**  *generated with [DocToc](http://doctoc.herokuapp.com/)*

- [Scenarios](#scenarios)
- [Introduction](#introduction)
  - [Generic Scenario Options](#generic-scenario-options)
      - [Prefix: scenario](#prefix-scenario)
  - [`rebalance` - Add/Remove/Swap Nodes and Rebalance](#rebalance---addremoveswap-nodes-and-rebalance)
      - [Use -c rebalance](#use--c-rebalance)
      - [Prefix: rebalance](#prefix-rebalance)
      - [Add two nodes to the cluster](#add-two-nodes-to-the-cluster)
      - [Remove one node and add one node](#remove-one-node-and-add-one-node)
      - [Remove three nodes from the cluster](#remove-three-nodes-from-the-cluster)
  - [`failover` - Failover nodes.](#failover---failover-nodes)
      - [Use: -c failover](#use--c-failover)
      - [Prefix: service](#prefix-service)
      - [Prefix: failover](#prefix-failover)
      - [Failover two nodes (including EPT), sleep 10 seconds and rebalance](#failover-two-nodes-including-ept-sleep-10-seconds-and-rebalance)
      - [Failover and eject two nodes](#failover-and-eject-two-nodes)
      - [Failover three nodes, waiting 60 seconds before readding them](#failover-three-nodes-waiting-60-seconds-before-readding-them)
  - [`servicefailure` - Modify Couchbase Services](#servicefailure---modify-couchbase-services)
      - [Use -c servicefailure](#use--c-servicefailure)
      - [Suspend `memcached` for one minute on four nodes and resume](#suspend-memcached-for-one-minute-on-four-nodes-and-resume)
      - [Restart the server on three nodes](#restart-the-server-on-three-nodes)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

# Scenarios

# Introduction

This section discusses some of the built-in scenarios. Scenarios are components
of code which determine when and how a cluster should be manipulated. Each
scenario has its own options.

A scenario to some extent encapsulates a workload (though typically the scenario
does not care what type the workload is).

All pre-defined scenarios may be found in `com.couchbase.sdkdclient.scenarios`.
The scenario name is relative to this path and is referenced using either its
name relative to the package, or one of the aliases.

Timing is an important concept with scenarios, as it is often required to view
the SDKD behavior as it happens *before*, *during*, and *after* the cluster
manipulation takes place.

Typically what a scenario does is the following:

1. Start the specified workload. At this point in time the SDKD starts performing
data store operations

2. Sleep for a specified period of time. During this time, the SDKD continues
operations but the cluster remains unchanged. This is called the _ramp_ phase.

3. Perform any cluster manipulations requested. As you may have guessed, the SDKD
is still executing its workload

4. Sleep for a specified period of time. At this point, the cluster manipulation
has already taken place. The SDKD is still performing operations during this phase.
This is called the _rebound_ phase.

5. After the last sleep interval has passed, the workload is stopped. The scenario
is now finished.


## Generic Scenario Options

These options may be used in _most_ scenarios (some do not support them). They
primarily affect the scheduling and timing of the cluster manipulation.

<a name="phased_options"></a>

<h4>Prefix: <code>scenario</code></h4>

<table><tr><th>Name</th><th>Description</th></tr>
<tr>
  <td><code>scenario.ramp</code></td>
  <td>Number of seconds to sleep before performing the cluster change.</td>
</tr>
<tr>
  <td><code>scenario.rebound</code></td>
  <td>Number of seconds to sleep after performing the cluster change. This
  should be a time large enough for the SDK to recover internally and stop
  returning errors to the SDKD. SDK failures after the cluster has been
  modified are generally not acceptable and will cause the test to fail</td>
</tr>
<tr>
  <td><code>scenario.change_only</code></td>
  <td>Equivalent to setting both <i>ramp</i> and <i>rebound</i> intervals to 0.
  This is helpful for testing the scenario manipulation itself</td>
</tr>
</table>

## EPT Node

This is the entry point node of the cluster. All the nodes are added and bootstrapped
using the ept node. If the bootstrap node goes down/removed by user and other 
nodes need to reconfigure and agree upon a master node. It is worthwhile to test client
behavior when the EPT node is removed from the cluster.


## `rebalance` - Add/Remove/Swap Nodes and Rebalance

<h4>Use <code>-c rebalance</code></h4>
<h4>Prefix: <code>rebalance</code></h4>

This scenario will change the node configuration in the cluster by
either adding, removing, or swapping nodes. It will then rebalance
the cluster.

<table><tr><th>Option</th><th>Description</th></tr>
<tr>
  <td><code>rebalance.mode</code></td>
  <td>What kind of node changes to perform. The choices are <code>OUT</code> - where nodes
  are removed from the cluster; <code>IN</code> where nodes are added - where nodes
      are added to the cluster; and <code>SWAP</code> where some nodes are added
      while others are removed in the same operation</td>
</tr>
<tr>
  <td><code>rebalance.count</code></td>
  <td>How many nodes to add/remove from the cluster during a rebalance. Note
  that if <code>rebalance.mode=SWAP</code>, then this number is effectively
  doubled as it indicates the number of nodes to add <b>and</b> remove. <b>
  note that you cannot remove all nodes in the cluster.</td>
</tr>
<tr>
  <td><code>rebalance.ept</code></td>
  <td>When removing a node (in either <code>OUT</code> or <code>SWAP</code>),
  also include the <i>EPT</i> node.
</tr>
</table>

#### Add two nodes to the cluster

```ini
#ARGFILE:INI
[rebalance]
mode = IN
count = 2
```

#### Remove one node and add one node

```ini
#ARGFILE:INI
[rebalance]
mode = SWAP
count = 1
```

#### Remove three nodes from the cluster

```ini
#ARGFILE:INI
[rebalance]
mode = OUT
count = 3
```


## `failover` - Failover nodes.

<h4>Use: <code>-c failover</code></h4>
<h4>Prefix: <code>failover</code></h4>

The `failover` scenario may be used to failover one or more nodes and perform
subsequent actions on them. A _failover_ is when a node is temporarily removed
from the cluster and its replica node (if any) becomes active.

Once a node is failed over, it may be ejected from the cluster or readded to the
cluster. In any event, for the cluster to recover from a failover it will
eventually require a rebalance.

The following options are understood by the `failover` scenario.

<table><tr><th>Option</th><th>Description</th></tr>
<tr>
  <td><code>failover.count</code></td>
  <td>Number of nodes to fail over</td>
</tr>
<tr>
  <td><code>failover.ept</code></td>
  <td>Whether the EPT should be failed over</td>
</tr>
<tr>
  <td><code>failover.next_action</code></td>
  <td>What should the scenario do once the node(s) have been failed over.
  Possible options here are:
  <table><tr><th>Action</th><th>Description</th></tr>
  <tr>
    <td><code>FO_REBALANCE</code</td>
    <td>Rebalance the cluster</td>
  </tr>
  <tr>
    <td><code>FO_EJECT</code></td>
    <td>Permanently eject the failed over nodes from the cluster. This will cause
    the replica to become permanently active</td>
  </tr>
  <tr>
    <td><code>FO_EJECT_REBALANCE</code></td>
    <td>Eject the nodes <i>and</i> rebalance</td>
  </tr>
  <tr>
    <td><code>FO_NOACTION</code></td>
    <td>Do nothing</td>
  </tr>
  <tr>
    <td><code>FO_READD</code></td>
    <td>Re-add the nodes. This will cause them to become master nodes again</td>
  </tr>
  <tr>
    <td><code>FO_READD_REBALANCE</code></td>
    <td>Readd the nodes <i>and</i> rebalance</td>
  </tr>
  </table>
  </td>
</tr>
<tr>
  <td><code>failover.next_delay</code></td>
  <td>Number of seconds to sleep before performing the <code>failover.next_action</code></td>
</tr>
</table>


#### Failover two nodes (including EPT), sleep 10 seconds and rebalance

```ini
#ARGFILE:INI
[failover]
ept = true
count = 2
next-action = FO_REBALANCE
next-delay = 10
```

#### Failover and eject two nodes

```ini
#ARGFILE:INI
[failover]
count = 2
next-action = FO_EJECT
```


#### Failover three nodes, waiting 60 seconds before readding them
```ini
#ARGFILE:INI
[failover]
count = 3
next_action = FO_READD
next_delay = 60
```

## `servicefailure` - Modify Couchbase Services

<h4>Prefix: <code>service</code></h4>
<h4>Use <code>-c servicefailure</code></h4>

This scenario modifies couchbase services. To be specific, it logs into the
cluster nodes via SSH and manipulates various aspects of cluster reachability
typically by suspending, terminating, or restarting processes.

This scenario may then perform a failover for the nodes which were disrupted. In
this case the scenario would mimic a production case where a node is automatically
failed over after it is detected to be "dead".

Additionally, this scenario may also choose to re-add the node and rebalance
the cluster after the service has been enabled as well.

Here are the options

<table><tr><th>Option</th><th>Description</th></tr>
<tr>
  <td><code>service.name</code></td>
  <td>The name of the service to affect. The choices are:
  <table><tr><th>Service</th><th>Description</th></tr>
  <tr>
    <td><code>MEMCACHED</code></td>
    <td>The <code>memcached</code> daemon itself. This will affect key value
    operations but not change the cluster topology or configuration</td>
  </tr>
  <tr>
    <td><code>NS_SERVER</code></td>
    <td>This will affect the accessibility to the REST API and any clustering
    features</td>
  </tr>
  <tr>
    <td><code>SYSV_SERVICE</code></td>
    <td>Will affect the entire cluster process using the normal
    <code>couchbase-server</code> script</td>
  </tr>
  </table>
  </td>
</tr>
<tr>
  <td><code>service.action</code></td>
  <td>How to cause an interruption to the service itself. The choices are
  <table><th>Disruption</th><th>Description</th></tr>
  <tr>
    <td><code>KILL</code></td>
    <td>Terminates a service causing it to shut down all its resources.
    Depending on the service selected this termination may either be graceful
    or ungraceful. In both these cases the service will be unambiguously
    <i>off</i></td>
  </tr>
  <tr>
    <td><code>HANG</code></td>
    <td>Suspends the given service so that it no longer performs its basic
    functionality. The service will <i>not</i> shut down its resources
    and external communication will not be shut down; rather any attempted
    interaction with the service will wait indefinitely until the service responds
    (or until some kind of client-side timeout is triggered).</td>
  </tr>
  </table>
  </td>
</tr>
<tr>
  <td><code>service.count</code></td>
  <td>How many nodes to interrupt</td>
</tr>
<tr>
  <td><code>service.ept</code></td>
  <td>Whether the <i>EPT</i> node should be interrupted. In the case of the
  <code>NS_SERVER</code> service, this will mean that the REST API connection
  will hang or be terminated</td>
</tr>
<tr>
  <td><code>service.stagger</code></td>
  <td>When multiple nodes have their services interrupted, this indicates the
  period of time to wait between failing each node's service. This option is here
  in order to simulate a situation where resources gradually become unavailable
  </td>
</tr>
<tr>
  <td><code>service.failover_delay</code></td>
  <td>Number of seconds to wait until the node is failed over. The failover
  will take place once the service has been interrupted. If this value is
  negative then no failover takes place</td>
</tr>
<tr>
  <td><code>service.restore_delay</code></td>
  <td>Number of seconds to wait before the interrupted service is restored. The
  scenario will attempt to undo the action it performed previously (i.e. the
  <code>service.action</code>) to make the service functional once again.
  This value indicates how long to wait until this happens. Note that if a
  failover delay was selected, this is the number of seconds from the failover.
  If this value is negative then no restoration is performed</td>
</tr>
<tr>
  <td><code>service.readd_delay</code></td>
  <td>Once the service has been restored (and assuming the nodes have been
  failed over), wait this many seconds until the nodes should be readded to
  the cluster. This option only makes sense if <code>service.failover_delay</code>
  was not negative and the service was restored</td>
</tr>
</table>


#### Suspend `memcached` for one minute on four nodes and resume

```ini
#ARGFILE:INI
[service]
name=memcached
count=4
restore_delay=60
```

#### Restart the server on three nodes

```ini
#ARGFILE:INI
[service]
name=SYSV_SERVICE
action=KILL
restore_delay=0
count=3
```
