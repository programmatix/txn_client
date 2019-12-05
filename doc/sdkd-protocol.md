<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->
**Table of Contents**  *generated with [DocToc](http://doctoc.herokuapp.com/)*

- [Couchbase SDKD Protocol](#couchbase-sdkd-protocol)
  - [Control Flow](#control-flow)
  - [Basic Message Format](#basic-message-format)
- [Commands](#commands)
  - [Creating a new handle](#creating-a-new-handle)
  - [Setting Items](#setting-items)
    - [Implementing Dataset](#implementing-dataset)
    - [Implementing control options:](#implementing-control-options)
  - [Getting Items](#getting-items)
  - [Item Command Results](#item-command-results)
  - [View/Map Reduce Queries](#viewmap-reduce-queries)
    - [Loading view JSON Documents.](#loading-view-json-documents)
    - [Querying View Documents](#querying-view-documents)
  - [Cancelling Commands](#cancelling-commands)
  - [Closing Handles](#closing-handles)
    - [Closing the Control Handle](#closing-the-control-handle)
  - [Status Codes](#status-codes)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

# Couchbase SDKD Protocol

This describes the message format and message types which will is used in the
_SDKD_ framework.

It is important to distinguish between `SDKD` as a framework and `SDKD`
as a specific component. In this document, we shall use the following terminology:

* <b>Harness/Client</b>.
  This is the component which contains the test logic. In the `SDKD` architecture
  it is the **client** (e.g. `sdkdclient`)


* <b>Server/SDKD</b>
  This is the component which accepts commands from the harness and dispatches
  them to the SDK. This is the server or driver (e.g. `sdkd-java`)


* <b>SDK</b>.
  This is the actual SDK which is invoked by the _Server_. This communicates
  with the cluster.


* <b>Cluster</b>.
  One or more nodes which comprises a Couchbase database cluster.


## Control Flow

When the client connects to the SDKD, the first connection is implicitly
known as the *Control Connection*. The control connection may be used
to perform commands related to all handles (as shall be demonstrated later on).

Once the control connection is established, the client may then establish
*Handle* connections. A _Handle_ is an abstract entity representing a single
SDK instance.

An example Python implementation for a *Handle** may look like this:

```python
from couchbase import Couchbase

class Handle(object):
  def __init__(self, bucket, password, host, handleId):
    self.client = Couchbase.connect(bucket=bucket, host=host, password=password)
    self.handle_id = handleId

  def handle_set(self, kv_dict):
    for k, v in kv_dict.items():
      self.client.set(k, v)
```

## Basic Message Format

The basic message format for requests and responses is a single JSON object.
The object shall be followed by a newline (`\n`). In this document we shall
show newlines in the message for clarity.

The message consists of a *Command*, *Request ID*, *Handle ID*, and an
optional *Body*.

Here is an example request.

```json
{ "Command" : "INFO",
  "Handle": 20,
  "ReqID": 35,
  "CommandData": {...}
}
```

<table>
<tr><th>Field</th><th>Description</th></tr>
<tr>
  <td>Command</td>
  <td>(<i>String</i>). The command string to send</td>
</tr>
<tr>
  <td>Handle</td>
  <td>(<i>Integer</i>).
  The unique Handle ID. A handle ID is first created with the <code>NEWHANDLE</code>
command</td>
</tr>
  <td>ReqID</td>
  <td>(<i>Integer</i>). The unique request ID to be used</td>
</tr>
<tr>
  <td>CommandData</td>
  <td>(<i>Object</i>). Command-specific data</td>
</tr>
</table>

The response will contain the same header, along with a `Status` field and
an optional body.

```json
{ "Command" : "INFO",
  "Handle": 20,
  "ReqID": 35,
  "Status": 0,
  "ResponseData": { ... }
}
```

The `Command`, `Handle`, and `ReqID` are always mirrored in the response.
Additionally, the following fields are present:

<table><tr><th>Field</th><th>Description</th></tr>
<tr>
  <td>Status</td>
  <td>(<i>Integer</i>). Response status. If this is anything other than 0
  then the response is an error. See later in this section for various error
  codes</td>
</tr>
<tr>
  <td>ResponseData</td>
  <td>(<i>Object</i>). Payload body. The fields here are dependent on the
  command</td>
</tr>
<tr>
  <td>ErrorString</td>
  <td>(<i>String</i>). If the status was non-successful, the server may include
  a reason in this field</td>
</tr>
</table>

# Commands

The following is a detailed description of the commands accepted by the
SDKD

## Creating a new handle

The `NEWHANDLE` command may be used to create a new handle. Typically, the
client will create a connection to the server and issue a `NEWHANDLE`
request. the `NEWHANDLE` request shall have a new `HandleID` field which is
assigned by the _client_. It is the client's responsibility to ensure that
Handle IDs are unique (within the application scope), though this can be
accomplished simply enough by maintaining a global counter.

A typical `NEWHANDLE` command may look like follows:

```json
{
   "Command" : "NEWHANDLE",
   "Handle" : 111
   "ReqID" : 17,
   "CommandData" : {
      "Hostname" : "127.0.0.1",
      "Port" : 8091,
      "Bucket" : "default",
      "Options" : {
        "Username" : "default",
        "Password" : "",
        "OtherNodes": [ ["127.0.0.1", 9001], ["127.0.0.1", 9002] ]
      }
   }
}
```

The fields for the `CommandData` body are explained as follows:

<table><tr><th>Field</th><th>Description</th></tr>
<tr>
  <td>Hostname</td>
  <td>The hostname for the cluster node</td>
</tr>

<tr>
  <td>Port</td>
  <td>The REST Port for the hostname</td>
</tr>

<tr>
  <td>Bucket</td>
  <td>The bucket to connect to </td>
</tr>

<tr>
  <td>Options</td>
  <td>Options specific to the SDKD being used:
  <table><tr><th>Option</th><th>Description</th></tr>
  <tr>
    <td>Username</td>
    <td>The username for the bucket. Currently this should always be the bucket
    name itself</td>
  </tr>

  <tr>
    <td>Password</td>
    <td>The SASL password for the bucket. May be an empty string if no password</td>
  </tr>
  <tr>
    <td>OtherNodes</td>
    <td>A JSON array of <i>additional</i> nodes to bootstrap from. This simulates
     the "other" nodes a user would pass in a "URI List".
     Each element should be an array of two elements; the first
    being the host and the second being the port</td>
  </tr>
  </table>
  </td>
</tr>
</table>

The response for the `NEWHANDLE` command looks like this:

```json
{
   "Command" : "NEWHANDLE",
   "Handle" : 117,
   "ReqID" : 31,
   "Status" : 0,
   "ResponseData" : {}
}
```

## Setting Items

There are quite a few commands for mutating items on the cluster, but they
are all variants of the `MC_DS_MUTATE_SET` which will be explained here. This
command must be sent to a handle created with `NEWHANDLE`:


```json
{
  "Command" : "MC_DS_MUTATE_SET",
  "Handle" : 111,
  "ReqID" : 30,
  "CommandData" : {
    "DSType" : "DSTYPE_SEEDED",
    "DS" : {
      "KSeed" : "SimpleKey",
      "VSeed" : "SimpleValue",
      "KSize" : 32,
      "VSize" : 128,
      "Count" : 15000,
      "Repeat" : "REP",
      "Continuous": true,
    },
    "Options" : {
      "DelayMin" : 1,
      "DelayMax" :10,
      "TimeRes" : 1,
      "IterWait" : 1
    }
  }
}
```

The heart of the `MC_DS_MUTATE_SET` command is the *Dataset* abstraction. A
dataset specifies a means by which the client can convey parameters to the
server about which data should be stored in the cluster and how it should be
stored. The main form of **Dataset** is the _Seeded_ dataset. The seeded
dataset takes several input parameters describing how to generate the keys
and values. The _seed_ is then passed on to the SDKD and translates it into
actual keys and values for the SDK.

Here are the paramters for the `CommandData` field.

<table><tr><th>Field</th><th>Description</th></tr>
<tr>
  <td>DSType</td>
  <td>The dataset type. This should always be set to <code>DSTYPE_SEEDED</code>.
  It is here primarily to allow the implementations of other sorts of datasets and
  SDK-specific workloads</td>
</tr>

<tr>
  <td>DS</td>
  <td>Dataset body. This contains the parameters for the seeded dataset
  <table><tr><th>Subfield</th><th>Description</th></tr>
  <tr>
    <td>KSeed</td>
    <td>The base text for the generated key. This text will be prefixed to each
    key generated and stored</td>
  </tr>
  <tr>
    <td>VSeed</td>
    <td>The base text for the generated value. This text will be prefixed to
    each generated value</td>
  </tr>
  <tr>
    <td>KSeed</td>
    <td>The approximate desired length for each key</td>
  </tr>
    <td>VSeed</td>
    <td>The approximate desired length for each value</td>
  </tr>
  <tr>
    <td>Count</td>
    <td>How many key-value pairs should be generated in total. For each sequence
    item in the generated key-value pair, the SDKD shall append the current number
    <i>n</i> (where <i>n</i> is a number between 0 and <i>Count</i>) to the generated
    item. Thus for a <code>KSeed</code> of <code>foo</code>, the first generated key
    shall be <code>foo0</code>, the second key shall be <code>foo1</code> and
    the last key shall be <code>foo15000</code></td>
  </tr>
  <tr>
    <td>Repeat</td>
    <td>This is a filler text appended to each key and value until they reach
    the desired size. This is used if the length of the current item and the
    current sequence number fall short of the desired generated length. For
    example if the desired generated size is <code>10</code> and the <code>KSeed</code>
    is <code>foo</code> and the <code>Repeat</code> string is <code>YO</code>
    then the first sequence item will be <code>foo0YO0YO0YO0</code>, this will
    be used since the base generated key (i.e. <code>foo0</code>) is only
    a length of 4.
  </tr>
  <tr>
    <td>Continuous</td>
    <td>If set to true, then when the <code>Count</code> is reached, it will
    reset to <code>0</code> and start over again, rather than ending the
    loop. This effectively sends the SDK into an infinite loop until the
    <code>CANCEL</code> command is sent. The client will normally set this parameter to
    <code>true</code> because it allows the SDK to perform many operations for
    an indefinite time without filling the cluster up with too much data.
    </td>
  </table>
  </td>
</tr>
<tr>
  <td>Options</td>
  <td>These options affect various control parameters for the SDKD while it
  iterates over the dataset. As these parameters are not specifically tied
  to the <i>seeded</i> dataset, they are specified separately<br>
  <table><tr><th>Subfield</th><th>Description</th></tr>
  <tr>
    <td>DelayMin, DelayMax</td>
    <td>Specify the minimum and maximum delay as a range in milliseconds. Normally
    the harness will request from the server to insert a delay so as not to overload
    the cluster and network with many operations, and also to simulate more realistic
    use cases. The minimum and maximum delay are provided as upper and lower
    bounds for e.g. a random number generator to operate upon
    </td>
  </tr>
  <tr>
    <td>IterWait</td>
    <td>If the underlying SDK supports "batched" or "multi"
    operations, this specifies the number of operations to batch at a time
    before waiting for the results to complete. Mainly useful for performance
    testing
    </td>
  </tr>
  <tr>
    <td>TimeRes</td>
    <td>Specify the sampling resolution (in seconds) for timing and error metrics
    This specifies how fine a granularity the SDKD should measure timing
    and performance metrics. This also places the upper bound on the
    resolution available from subsequent reporting tools.</td>
  </tr>
  </table>
</tr>
</table>

Is is noteworthy to mention that in all the SDKD implementations, the
dataset is implemented as an iterator which generates each sequence on its
equivalent `.next()` method/function.

### Implementing Dataset
```java
class CommandExecutor {
  List<Entry<String,String>> allEntries;
  void loadEntries() {
    for (int i = 0; i < count; i++) {
      Entry<String,String> currentEntry = new Entry<String,String>();
      String kBase = kseed; // KSeed parameter
      // Initialize the seed and count at least once:
      do {
        kBase += repeat + String.format("{0}", i);
      } while (kBase.length() < ksize);

      currentEntry.setKey(kBase);

      String vBase = vseed; // VSeed parameter
      do {
        vBase += repeat + String.format("%d", i);
      } while (vBase.length() < vsize);

      currentEntry.setValue(vBase);
      allEntries.append(currentEntry);
    }
  }

```

### Implementing control options:

```java
class CommandExecutor {
  // ...

  CouchbaseClient cb;
  SdkdCommand cmd;

  List<Future> curFutures = new ArrayList<>();

  for (Entry<String,String> curKv : allEntries) {
    Future ft = cb.set(curKv.getKey(), curKv.getValue());
    curFutures.add(ft);

    if (curFutures.size() == cmd.getIterWait()) {

      for (Future curFt : curFutures) {
        curFt.get();
      }

      int timeToSleep = (new Random()).nextInt(cmd.getDelayMax() - cmd.getDelayMin());
      timeToSleep += cmd.getDelayMin();

      if (timeToSleep > 0) {
        Thread.sleep(timeToSleep);
      }
    }
  }
```


## Getting Items

Getting items is done with the `MC_DS_GET` command which looks almost exactly
like the `MC_DS_MUTATE_SET` command:

```json
{
   "Command" : "MC_DS_GET",
   "ReqID" : 35,
   "Handle" : 116,

   "CommandData" : {
      "Options" : {
         "TimeRes" : 1,
         "DelayMin" : 1,
         "DelayMax" : 10,
         "IterWait" : 1
      },

      "DSType" : "DSTYPE_SEEDED",
      "DS" : {
         "VSize" : 128,
         "Count" : 15000,
         "VSeed" : "SimpleValue",
         "KSize" : 32,
         "KSeed" : "SimpleKey",
         "Repeat" : "REP",
         "Continuous" : true
      }
   }
}

```

The only thing to note here is that the value-related parameters are ignored
(e.g. `VSize`, `VSeed`).

## Item Command Results

```json
{
  "Command" : "MC_DS_GET",
  "ReqID" : 23,
  "Status" : 0,
  "Handle" : 108,

  "ResponseData" : {
    "Summary" : {
      "0" : 6621,
      "520" : 61
    },

    "Timings" : {
      "Base" : 1385760648
      "Step" : 1,
      "Windows" : [
        {
          "Max" : 1,
          "Min" : 0,
          "Count" : 266,
          "Avg" : 0,

          "Errors" : {
            "0" : 205,
            "520" : 61
          }
        },

        {
          "Max" : 2,
          "Min" : 0,
          "Count" : 313,
          "Avg" : 0
          "Errors" : {
            "0" : 313
          }
        }
      ]
    }
  }
}
```

The response format is rather complex and is a result if needing to convey a lot
of information in an efficient and concise manner. At the core of the response
is the per-item status information. Thus for each key-value pair set, the SDKD
will log whether the operation was successful, and if not, also log the error
which happened.

Additionally, if timings are enabled (via the `TimeRes` parameter), then timing
details will be provided for each interval.

The details of these fields will be explained here:

<table><tr><th>Field</th><th>Description</th></tr>
<tr>
  <td>Summary</td>
  <td>A mapping of error codes to the number of times they took place for the
  entire duration of the command. Int the sample above the SDK executed
  <code>6682</code> commands, of which <code>6621</code> were successes (i.e.
  error code </code>0</code>) and 61 were failures of some sort (in this case
  code <code>520</code>). The breakdown will contain as many error types as
  have taken place. See the section on error codes below to understand how
  to interpret the data.
</tr>
<tr>
  <td>Timings</td>
  <td>Per-interval timing statistics. This is only available if the <code>TimeRes</code>
  field was greater than 0 for the command. The details of this object are as
  follows:
  <table><tr><th>Subfield</th><th>Description</th></tr>
  <tr>
    <td>Base</td>
    <td>Base Unix (Epoch) timestamp in seconds. This is the point in time from
    which the first metric was collected.</td>
  </tr>
  <tr>
    <td>Step</td>
    <td>The resolution in seconds that each timing interval window (or <i>Time Quantum</i>)
    represents. This will be equal to the <code>TimeRes</code> field in the request)
    </td>
  </tr>
  <tr>
    <td>Windows</td>
    <td>An array of interval descriptions; each interval description describes a
    set of metrics over time. The length of time each interval covers is specified
    in the <code>Step</code> field below. Each interval is in direct succession
    to its predecessor and the first interval begins at the <code>Base</code> time.<br>
    The ordering of the intervals is thus that <code>Step * Windows.length</code>
    will yield the number of time in seconds for the duration of the run.<br><br>
    The description of the interval follows:
    <table><tr><th>Subfield</th><th>Description</th></tr>
    <tr>
      <td>Min</td>
      <td>The minimum latency in milliseconds for all operations in this interval</td>
    </tr>
    <tr>
      <td>Avg</td>
      <td>The mean latency in milliseconds for all operations in this interval</td>
    </tr>
    <tr>
      <td>Max</td>
      <td>The maximum latency of any operation in this interval</td>
    </tr>
    <tr>
      <td>Errors</td>
      <td>Follows the same format as the <code>Summary</code> field above,
      except that this represents only those statuses for operations which
      were executed in the current interval</td>
    </tr>
    </table>
    </td>
  </td>
  </tr>
  </table>
</td>
</tr>
</table>


## View/Map Reduce Queries

The SDKD protocol also supports handling views and ensuring the integrity of
view data. View operations are divided into two parts:

1. Loading the view JSON documents into the cluster via `CB_VIEW_LOAD`
2. Querying the view for the keys via `CB_VIEW_QUERY`

### Loading view JSON Documents.


```json
{
   "Command" : "CB_VIEW_LOAD",
   "Handle" : 101,
   "ReqID" : 2,
   "CommandData" : {
      "DSType" : "DSTYPE_SEEDED",
      "DS" : {
         "VSize" : 512,
         "Count" : 1000,
         "VSeed" : "ViewFillerSeed",
         "KSize" : 12,
         "KSeed" : "ViewFillerSeed",
         "Repeat" : "rep",
         "Continuous" : false
      },
      "Schema" : {
         "InflateLevel" : 40,
         "InflateContent" : "meh"
      },
      "Options" : {
         "TimeRes" : 0,
         "DelayMin" : 0,
         "DelayMax" : 0,
         "IterWait" : 1
      }
   }
}
```

The command looks similar to the previous _Dataset_-related commands
with key-value operations. In truth this operation is also key-value in that
it loads the documents to be returned within the database.

Worthy of note here is the the `Schema` field which is specific to
loading the views. The contents of the `Schema` field are explained
in light of the actual view function itself:

```javascript
function(doc, meta) {
  // key "Identity"
  if (!doc.kIdent) {
    return;
  }

  var repeated = "";
  // Repeat the output up until the inflate level
  for (var i = 0; i < doc.InflateLevel; i++) {
    repeated += doc.InflateContent;
  }

  emit([
    doc.KIdent,
    doc.InflateContent,
    doc.InflateLevel,
    repeated
  ], null);
}
```

Before we explain what the `Schema` is for, the format of each JSON
document itself will be explained:

<table><tr><th>Field</th><th>Description</th></tr>
<tr>
  <td>kIdent</td>
  <td>The <i>identity</i> of the document, i.e. the document ID itself</td>
</tr>
<tr>
  <td>InflateContent</td>
  <td>A string of text to be repeated and returned as part of the emitted key.
  This helps control the size of the emitted key and as such, also the size
  of the resultset; without loading too much into the key-value store
  itself. This can be thought of as similar to the <code>Repeat</code>
  field in the <code>DSTYPE_SEEDED</code> specification</td>
</tr>
<tr>
  <td>InflateLevel</td>
  <td>The number of times the <code>InflateContent</code> value is to
  be repeated before it is emitted as part of the key</td>
</tr>
</table>

As such, the `Schema` fields directly control what is placed in
the value's `InflateContent` and `InflateLevel` field.

The `DS` specification follows the same semantics as above, with the following
notes:

* Value-related (i.e. `VSeed`, `VSize`) parameters are ignored
* The SDKD should insert only `Count` number of documents into the database.
* Since the `CB_VIEW_LOAD` is only a variation on `MC_DS_MUTATE_SET`, the
  _client_ must ensure that delay/control parameters are optimally set.
  In this particular case, the quickest way to set 1000 keys in the SDK
  is to do so without any artificial inserted delay. Thus `DelayMin` and
  `DelayMax` are both set to `0`.
  Additionally, since we only want precisely 1000 keys, we do not use
  the `Continuous` option.


The response for the `CB_VIEW_LOAD` command looks something like this:

```json
{
   "Command" : "CB_VIEW_LOAD",
   "ReqID" : 5,
   "Status" : 0,
   "Handle" : 102,
   "ResponseData" : {
      "Summary" : {
         "0" : 1000
      }
   }
}
```

### Querying View Documents

```json
{
   "ReqID" : 24,
   "Command" : "CB_VIEW_QUERY",
   "Handle" : 111,
   "CommandData" : {
      "DesignName" : "test_design",
      "ViewName" : "test_view",'
      "ViewParameters" : {
         "limit": 10,
         "stale": false,
         ...
      },
      "Options" : {
         "TimeRes" : 1,
         "ViewQueryDelay" : 0,
         "ViewQueryCount" : -1
         "DelayMin" : 1,
         "DelayMax" : 10,
      },
   },
}
```

The fields are explained as follows:

<table><tr><th>Field</th><th>Description</th></tr>
<tr>
  <td>DesignName</td>
  <td>The design document in which the view is located</td>
</tr>
<tr>
  <td>ViewName</td>
  <td>The view name to query</td>
</tr>
<tr>
  <td>ViewParameters</td>
  <td>A JSON object containing other view parameters to be passed
  to the engine. These should be set either via a native SDK method
  (e.g. <code>view.setLimit(100)</code>) if possible, or directly
  into the URI (e.g. <code>?limit=100</code></td>
</tr>
<tr>
  <td>Options</td>
  <td>Control options for the execution. The subfields are as follows
  <table><tr><th>Subfield</th><th>Description</th></tr>
  <tr>
    <td>TimeRes</td>
    <td>Time resolution to sample on. This has the same meaning as in
    the key-value queries</td>
  </tr>
  <tr>
    <td>DelayMin, DelayMax</td>
    <td>Time to wait (in milliseconds) between fetching each <i>row</i>.
    This is especially useful if the SDK has a "streaming" option which
    fetches rows and results incrementally</td>
  </tr>
  <tr>
    <td>ViewQueryCount</td>
    <td>Number of times to query the view before stopping the command.
    If this is a negative number, the view is executed in a loop until
    the command is cancelled</td>
  </tr>
  <tr>
    <td>ViewQueryDelay</td>
    <td>Sleep interval in milliseconds to pause between each execution
    of the <i>view</i> itself. This is different than the <code>DelayMin</code>
    and <code>DelayMax</code> parameters which affect the delay between fetching
    <i>individual rows</i>. If this value is 0 then no delay is used.
  </tr>
  </table>
  </td>
</tr>
</table>

The response is similar to that of the key-value commands.

```json
{
   "Command" : "CB_VIEW_QUERY",
   "ReqID" : 26,
   "Status" : 0,
   "Handle" : 112,

   "ResponseData" : {
      "Timings" : {
         "Step" : 1,
         "Windows" : [
            {
               "Count" : 5,
               "Avg" : 50,
               "Max" : 142,
               "Min" : 13,
               "Errors" : {
                  "0" : 5
               },
            },
            ...
         ],
         "Base" : 1385844342
      },
      "Summary" : {
         "0" : 637
      }
   }
}
```

Note that it is not defined whether the success count is the number of
<i>rows</i> fetched, or the number of <i>queries</i> executed.


## Cancelling Commands

Most command sent by the client are <i>continuous</i> or <i>unbounded</i>,
meaning that they will continue to execute until explicitly stopped.

Stopping a command is done via the `CANCEL` command. This command however
is *not* sent to the executing _Handle_ but rather to the _Control_ handle.
This is because the executing handle is assumed to be running in a loop and
thus not being available to respond to socket requests.

In this case it is important for the `CANCEL` command to include the correct
`Handle` and `ReqID` fields of the command to be cancelled. In this case,
the SDKD maintains a mapping between each _Handle ID_ and its respective
internal _Handle_ object. The _Handle_ is expected to have a definition that
looks something like this

```cpp
class Handle {
  // ...
  private:
  volatile bool isCancelled;

  // ...
  void executeGet(....) {
    while (isCancelled == false || (cmd.continuous == false && curCount < cmd.count)) {
      // ... do stuff
    }
    sendCommandResponse();
  }

  public:
  int getId() const {
    return this.id;
  }

  void cancel() {
    isCancelled = true;
  }
}

class ControlHandle {
  // ...
  std::map<int,Handle*> handles;

  void cancelCommand(int handle_id, ....) {
    Handle *h = handles[handle_id];
    h->cancel();
  }
}
```


The `CANCEL` command in itself is fairly simple:

```json
{
  "Command": "CANCEL",
  "Handle":106,
  "ReqID":26,
  "CommandData":{}
}
```

And the response is simple as well:

```json
{
  "Status":0,
  "Handle":106,
  "ReqID":26,
  "Command":"CANCEL",
  "ResponseData":{}
}
```

`CANCEL` is essentially an asynchronous signal to the specific _Handle_
executing the command identified by the `ReqID`. This means that the
response for the `CANCEL` command should _not_ wait until the _Handle_ is
done executing the command, but simply return once the signal has been sent.

Depending on the command being executed, it may take several seconds until
the command is actually finished (this is usually the case of the handle
is stuck waiting on network I/O from the cluster). Taking an excessively
long time to respond from a `CANCEL` request is typically an indication
of a bug in the SDK itself.

Once a handle has been cancelled, it should compile the necessary result
data and send it back to the client.

`CANCEL`ling a handle does not close it, and a handle may be reused for several
commands (though a Handle can only ever execute one command at a time).


## Closing Handles

A handle should be _closed_ by sending it a `CLOSEHANDLE`. Graceful closing
of handles is necessary for the client to distinguish a graceful disconnect
from a possible crash in the SDK and/or SDKD. Since the _Handle_ is expected
to close its network connection upon a receipt of a `CLOSEHANDLE` command,
this command does not have a response.

```json
{
  "Command":"CLOSEHANDLE",
  "Handle":109,
  "ReqID":33,
  "CommandData":{}
}
```

### Closing the Control Handle

Closing the _Control_ handle requires a separate command as this may also
involve the SDKD application exiting. To close a _Control_ handle, send it
a `GOODBYE` request.

```json
{
  "Command":"GOODBYE",
  "Handle":0,
  "ReqID":37,
  "CommandData":{}
}
```

As with `CLOSEHANDLE`, the `GOODBYE` command has no response.

<a name="status_codes"></a>
## Status Codes

This is an integer representing an error code, or the number 0 in the case of no
error.

In the case of an error, the code is an integer with the following format:

The least significant 8 bits indicate an error _category_. This represents
a category of errors.

The additional bits indicate an error _detail_ code which is additional detail for
the error category. The detail code is left-shifted by 8 as well. To split
the error code into its components, do the following:

```c
unsigned int err = 520; /* Hex: 0x208 */
major = err & 0xff; /* == 8, 0x08 */
minor = (err & (~0xff)) >> 8; /* == 2, 0x02 */
```

In this case, the error class is `0x08`, or `SUBSYS_MEMD` - i.e. a direct
memcached error response; and the minor code is `0x02` which is `MEMD_ENOENT`,
or `Key not found`.

Note that some SDKDs will expose the detail constants as being left-shifted by
8, so that `MEMD_ENOENT` will actually be `0x200` rather than `0x02` - due to
it being simpler to `OR` the values together (i.e. `SUBSYS_MEMD|MEMD_ENOENT`).

Before going into additional details, there is a special error category called
the _SDK_ Category. Unlike the other error categories which are uniform across
all SDKD and SDK implementations, the `SDK` category defines its minor codes
as being specific to the SDK in use. This is helpful if the SDKD wishes to
convey an error condition which does not easily fit into any of the other
categories.

For example, in the _libcouchbase_ SDKD, the following code appears:

```cpp
static int mapError(lcb_error_t err) {
    // If it's a success, return 0.
    if (err == LCB_SUCCESS) {
        return 0;
    }
    // If the SDK error fits nicely into the SDKD error, return the SDKD error
    if (Errmap.find(err) != Errmap.end()) {
        return Errmap[err];
    }

    // Otherwise, encode the SDK error into the minor bits of the
    // SUBSYS_SDK category.
    int ret = Error::SUBSYSf_SDK;
    ret |= err << 8;
    return ret;
}
```

Here's a common list of errors and their components. You can refer to
the source code to get a comprehensive list of categories

<table><tr><th>Category</th><th>Value</th><th>Description</th></tr>
<tr>
  <td>Cluster</td>
  <td><code>0x02</code></td>
  <td>Cluster-related errors, for example bad authentication or a missing bucket.
  Known minor codes include:
  <table><tr><th>Detail</th><th>Code</th><th>Description</th></tr>
  <tr>
    <td>Auth</td>
    <td><code>0x202</code></td>
    <td>Authentication to the cluster or bucket failed</td>
  </tr>
  <tr>
    <td>Not Found</td>
    <td><code>0x302</code></td>
    <td>Bucket does not exist</td>
  </tr>
  </table>
  </td>
</tr>
<tr>
  <td>Client</td>
  <td><code>0x04</code></td>
  <td>Client-related errors, such as timeouts, vBucket mapping errors, orinvalid states
  <table><tr><th>Detail</th><th>Code</th><th>Description</th></tr>
  <tr>
    <td>Timeout</td>
    <td><code>0x204</code></td>
    <td>An operation timed out while waiting for completion</td>
  </tr>
  <tr>
    <td>Scheduling</td>
    <td><code>0x304</code></td>
    <td>An error occurred during the scheduling phase. Either the vBucket
    map was not available or the server it mapped to was offline</td>
  </tr>
  </table>
  </td>
</tr>
<tr>
  <td>Memcached</td>
  <td><code>0x08</code></td>
  <td><code>memcached</code> errors. These errors are those exposed to the user
  from the client side and can have a one-to-one correlation with a server
  error. These include `Not Found`, `Not Stored`, and `Not My VBucket` errors.
  <table><tr><th>Detail</th><th>Code</th><th>Description</th></tr>
  <tr>
    <td>Key Not Found, <code>ENOENT</code></td>
    <td><code>0x208</code></td>
    <td>Key does not exist</td>
  </tr>
  <tr>
    <td>Not My Vbucket</td>
    <td><code>0x508</code></td>
    <td>Key was sent to wrong server</td>
  </tr>
  </table>
  </td>
</tr>
<tr>
  <td>Network</td>
  <td><code>0x10</code></td>
  <td>Network-related errors. This includes things such as socket timeouts,
  socket closes, and malformed data. Specifically this means the SDK has delivered
  an I/O or network error to the API</td>
</tr>
<tr>
  <td>SDKD</td>
  <td><code>0x20</code></td>
  <td>Errors related to the SDKD protocol itself. This includes things like
  missing a `Handle` field or sending an unknown command</td>
</tr>
<tr>
  <td>Views</td>
  <td><code>0x41</code></td>
  <td>Errros related to views. This includes HTTP errors and JSON errors</td>
</tr>
<tr>
  <td>SDK</td>
  <td><code>0x80</code></td>
  <td>SDK Specific errors. The SDKD-specific number is encoded as an integer
  starting from the second byte upwards. This can be used to express SDK-specific
  error conditions without specifying new SDKD errors</td>
</tr>
<tr>
  <td>Unknown</td>
  <td><code>0x01</code></td>
  <td>Any unknown error takes place here</td>
</tr>
</table>