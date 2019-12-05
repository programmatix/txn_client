<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->
**Table of Contents**  *generated with [DocToc](http://doctoc.herokuapp.com/)*

- [The Ad-Hoc Client (`simpleclient`)](#the-ad-hoc-client-simpleclient)
      - [Be sure to start your SDKD](#be-sure-to-start-your-sdkd)
  - [Examining Output](#examining-output)
      - [General Output Format](#general-output-format)
      - [Creating the SDK Handle/Object](#creating-the-sdk-handleobject)
      - [Dispatching Storage Operations](#dispatching-storage-operations)
    - [Storage Operation Results](#storage-operation-results)
      - [Error Codes](#error-codes)
  - [Extra Options](#extra-options)
      - [Run additional threads](#run-additional-threads)
      - [Increase the number of keys inserted](#increase-the-number-of-keys-inserted)
      - [Increase the value size](#increase-the-value-size)
      - [Store as many keys as possible in a 10 second interval](#store-as-many-keys-as-possible-in-a-10-second-interval)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

# The Ad-Hoc Client (`simpleclient`)

The `simpleclient` is a simple program which may be used to test the basic
functionality of the SDKD

This script is not part of the actual testing framework (though it can be useful
for certain types of load testing) but its invocation and setup is simpler.

Specifically, the `simpleclient` script is more ad-hoc.

The basic usage looks like this:

```
shell> bin/simpleclient -C 127.0.0.1:8050 --hostname localhost --port 8091

Response @101
   OK: 84
   UNKNOWN:GENERIC: 16
Response @102
   OK: 84
   UNKNOWN:GENERIC: 16
```

<a name="launching_sdkd"></a>
Note that this example relies on an SDKD running on port 8050. If you've
been following along and using the sample SDKD, simply start it in a new
console like so:

```
shell> java -jar share/sdkd-java-0.5-SNAPSHOT.jar -l 8050
```


#### Be sure to start your SDKD
> SDKDs by default are configured to exit when the client application exits.
Thus it is necessary to start the SDKD before each invocation of the client.
How to spawn the SDKD is dependent on the SDKD itself and may be explained
later on.
Otherwise, you might end up seeing this:
>
```
Exception in thread "main" java.net.ConnectException: Connection refused
	at java.net.PlainSocketImpl.socketConnect(Native Method)
	at java.net.AbstractPlainSocketImpl.doConnect(AbstractPlainSocketImpl.java:339)
	at java.net.AbstractPlainSocketImpl.connectToAddress(AbstractPlainSocketImpl.java:200)
	at java.net.AbstractPlainSocketImpl.connect(AbstractPlainSocketImpl.java:182)
	at java.net.SocksSocketImpl.connect(SocksSocketImpl.java:392)
	at java.net.Socket.connect(Socket.java:579)
	at java.net.Socket.connect(Socket.java:528)
	at com.couchbase.sdkdclient.driver.HostPortDriver.start(HostPortDriver.java:41)
	at com.couchbase.sdkdclient.simpleclient.SimpleClient.run(SimpleClient.java:191)
	at com.couchbase.sdkdclient.simpleclient.SimpleClient.main(SimpleClient.java:201)
```


## Examining Output

We'll run this again with verbose output to see some more information.

```
[0.05 DEBUG] (Driver start:39) Establishing SDKD Control connection /127.0.0.1:8050
[0.07 DEBUG] (CBCluster prepare:552) Spec: localhost:9000
[0.45 DEBUG] (Handle sendMessageAsync:180) > NEWHANDLE@102.1 => {Port=9000, Password=, Username=default, Bucket=default, Options={}, Hostname=localhost}
[0.45 DEBUG] (Handle sendMessageAsync:180) > NEWHANDLE@101.0 => {Port=9000, Password=, Username=default, Bucket=default, Options={}, Hostname=localhost}
[1.04 DEBUG] (Handle receiveMessage:155) < NEWHANDLE@101.0
[1.04 DEBUG] (Handle receiveMessage:155) < NEWHANDLE@102.1
[1.04 DEBUG] (Handle sendMessageAsync:180) > MC_DS_GET@101.2 => {DSType=DSTYPE_SEEDED, DS={KSeed=GENERATED, VSeed=GENERATED, Count=100, Continuous=false, VSize=128, Repeat=REP, KSize=12}, Options={DelayMax=0, DelayMin=0, IterWait=1, TimeRes=1}}
[1.04 DEBUG] (Handle sendMessageAsync:180) > MC_DS_MUTATE_SET@102.3 => {DSType=DSTYPE_SEEDED, DS={KSeed=GENERATED, VSeed=GENERATED, Count=100, Continuous=false, VSize=128, Repeat=REP, KSize=12}, Options={DelayMax=0, DelayMin=0, IterWait=1, TimeRes=1}}
[1.04 DEBUG] (Workload cancel:114) Non-continuous handles. Not invoking CANCEL
[1.21 DEBUG] (Handle receiveMessage:155) < MC_DS_GET@101.2 => {[OK]: 84, [UNKNOWN:GENERIC]: 16} (tms: 2s)
Response @101
   OK: 84
   UNKNOWN:GENERIC: 16
[1.22 DEBUG] (Handle receiveMessage:155) < MC_DS_MUTATE_SET@102.3 => {[OK]: 84, [UNKNOWN:GENERIC]: 16} (tms: 2s)
Response @102
   OK: 84
   UNKNOWN:GENERIC: 16
[1.22 DEBUG] (Handle sendMessageAsync:180) > CLOSEHANDLE@101.4
[1.22 DEBUG] (Handle sendMessageAsync:180) > CLOSEHANDLE@102.5
```


* `0.05`: the initial connection to the SDKD is established.
* `0.45`: Two new SDKD handles are created
* `1.04`: One handle receives a request to set items. The other receives a request
  to retrieve them.
* `1.21`, `1.22`: Both handles are done and deliver their results back to the client
* `1.22`: Both handles are shut down.

#### General Output Format
>
This describes the general output format for `simpleclient`.
>
It normally looks like
>
```
[ Seconds.MS Severity ] (Class method:line) Content
```
>
<table><tr><th>Field</th><th>Description</th></tr>
<tr>
  <td>Seconds</td>
  <td>The number of seconds since the beginning of the application</td>
</tr>
<tr>
  <td>Severity</td>
  <td>A string like <code>WARN</code> or <code>INFO</code></td>
</tr>
<tr>
  <td>Class, Method, Line</td>
  <td>Source code information showing where this message was logged from</td>
</tr>
</table>



#### Creating the SDK Handle/Object

>
SDKDs work by creating internal library objects which communicate with
the server. In other words, they create the same kinds of object a normal
user of the library would; this means that the C SDKD creates several
`lcb_t` s, the Java SDKD creates a `CouchbaseClient` object, and the C#
SDKD creates a `Couchbase` Object

Here the underlying SDK handle is created:
```
NEWHANDLE@102.1 => {Port=9000, Password=, Username=default, Bucket=default, Options={}, Hostname=localhost}
NEWHANDLE@101.0 => {Port=9000, Password=, Username=default, Bucket=default, Options={}, Hostname=localhost}
```

Each handle created by the SDKD is given its own thread (within the SDKD)
and its own control channel (via a socket).

The SDKD command `NEWHANDLE` is sent to the SDKD, with various options stating
which Couchbase cluster to connect to (`10.0.0.99`), which bucket to connect to
(`default`), and various fields for the bucket (all empty in this case).

Once a handle is created, it is assigned a _Handle ID_ (HID), in the logs
the HID will be displayed for each command and will typically be prefixed
by a `@`-sign. In this case, the Handle ID is `1`. The number after the decimal
point is the `ReqID` field in the protocol.

#### Dispatching Storage Operations

```
[1.04 DEBUG] (Handle sendMessageAsync:180) > MC_DS_MUTATE_SET@102.3 => {DSType=DSTYPE_SEEDED, DS={KSeed=GENERATED, VSeed=GENERATED, Count=100, Continuous=false, VSize=128, Repeat=REP, KSize=12}, Options={DelayMax=0, DelayMin=0, IterWait=1, TimeRes=1}}
```


Here we request the SDKD to set 100 items. We define the format of these items
by passing along a _Dataset_ to the SDKD as part of the command arguments to
the mutation request (which if you haven't figured out yet, is called
`MC_DS_MUTATE_SET`: `MC` because it is a memcached operation, and `DS` because
it accepts a dataset as an argument).

The dataset is a definition about _what_ to use as keys and values for a given
operation.

Refer to the [protocol documentation](sdkd-protocol.md) for more information.

### Storage Operation Results

```
[1.22 DEBUG] (Handle receiveMessage:155) < MC_DS_MUTATE_SET@102.3 => {[OK]: 84, [UNKNOWN:GENERIC]: 16} (tms: 2s)
```


Here we see the operation result. In this case we have two status codes. The
first is for the overall command (_OK_), which indicates that the SDKD
was able to _schedule_ the operation successfuly. This operation could have
failed, if for example, the Dataset was invalid (in which case the error code
would have been `SDKD_EINVAL`) or the command was unrecognized (`SDKD_ENOIMPL`).

The second set of status codes is expressed as `'OK': 84, 'UNKNOWN:GENERIC': 16`
which is broken down
as the following:

`OK` is the error code, and `84` is the amount of times the error code itself was received.
`UNKNOWN:GENERIC` is another error code, and it was received `16` times. The
total number of operations is the sum of all the number of times any error code
was received. In this case, `84 + 16 == 100`.

Thus the response may be interpreted as:

``Of all the datastore operations restored, 100 of them were completed successfuly''.

Depending on the environment the cluster and SDK are running in, you may get
different errors. Success and failure is a complex topic and is discussed
in [Understanding Failures](understanding-failures.md).

#### Error Codes
>
The codes presented in the output are actually numeric. Other than `OK`, each
status code string is delimited by a colon (`:`), the first part is the _Major_
code, and the second part is the _Detail_ code. See
[Status Codes](sdkd-protocol.md#status_codes) for details.

## Extra Options

The `simpleclient` is provided as a gateway to the `stester` executable.
You can play with it using some of the following examples:

#### Run additional threads

```
shell> bin/simpleclient -C localhost:8050 -M 20
```

This will spawn 20 threads, rather than a single thread.


#### Increase the number of keys inserted

```
shell> bin/simpleclient -C localhost:8050 --count 5000
```

#### Increase the value size

```
shell> bin/simpleclient -C localhost:8050 --vsize 4096
```

Will store keys with 4kb values.


#### Store as many keys as possible in a 10 second interval

```
shell> bin/simpleclient -C localhost:8050 --duration 10
```
