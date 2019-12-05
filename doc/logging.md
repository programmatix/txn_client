<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->
**Table of Contents**  *generated with [DocToc](http://doctoc.herokuapp.com/)*

- [`sdkdlient` Logging](#sdkdlient-logging)
  - [Logging Subsystems](#logging-subsystems)
      - [Enabling debugging information](#enabling-debugging-information)
      - [Enabling more verbose information](#enabling-more-verbose-information)
      - [Enabling *all* subsystems](#enabling-all-subsystems)
      - [Disable color output](#disable-color-output)
      - [Viewing a list of subsystems](#viewing-a-list-of-subsystems)
  - [SDKD Logging](#sdkd-logging)
  - [Retrieving log messages](#retrieving-log-messages)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

# `sdkdlient` Logging

Because this is a test and instrumentation framework, logging merits its own
special section.

On the harness side, logging is acheived using the popular [slf4j](www.slf4j.org)
interface together with the [logback](logback.qos.ch) backend.

Logback itself is configured via an XML file. The XML file used for `sdkdclient`
may be located in `src/main/resources/logback.xml` and can be modified according
to your needs.


Besides the XML file, you can also configure logging from the commandline.
All entry points offer a `debug` option which can be passed multiple times
with the format `--debug <logger>:<level>` where `logger` is a named logger
and `level` is the minimum output level.

In addition to displaying the logs to the console, they are also added to the
database. Note that the output level to the database is fixed to `TRACE` and is
not affected by the `debug` option.

<a name="logging_options"></a>
## Logging Subsystems

Here's a full list of logging options:

<table><tr><th>Category</th><th>Description</th></tr>
<tr>
  <td><code>all</code></td>
  <td>The most common categories to provide information. This omits categories
  that are either third party, or are likely to supply a lot of irrelevant
  information
  </td>
</tr>
<tr>
  <td><code>spam</code></td>
  <td>Unlike <i>all</i>, this unconditionally affects <i>all</i> categories.
  Be ready with a fire extinguisher.</td>
</tr>
<tr>
  <td><code>options</code></td>
  <td>This category handles the configuration parsing and validation. Since
  this has little to do with the actual test logic, it is normally disabled
  with <i>all</i>, but may be helpful if there is a possible bug in the
  configuration handling, or to figure out why your option cannot be found/parsed
  </td>
</tr>
<tr>
  <td><code>cbadmin</code></td>
  <td>This category affects the raw REST API handling. The REST API handling
  is performed in a different package</td>
</tr>
</table>



Here are some common tasks you might need to perform

#### Enabling debugging information

```
shell> bin/stester --debug all:debug
```

#### Enabling more verbose information

```
shell> bin/stester --debug all:trace
```

#### Enabling *all* subsystems

The `all` logger doesn't really include all the loggers. It just includes those
loggers which participate in the actual SDKD/Cluster mechanics. Other systems
which perform logging or configuration validation and parsing are not included
in the `all` logger. To enable them you can either specify them by name
(e.g. `orm`, `db`, `options`) or specify the `spam` logger which logs
_everything_

```
shell> bin/stester --debug spam:trace
```

#### Disable color output

Some consoles don't like color output

```
shell> bin/stester --disable-colors
```

#### Viewing a list of subsystems

```
shell> bin/stester --debug-help
```


## SDKD Logging

Technically speaking, SDKD logging is out of scope of the client since the client
cannot control the output of the SDKD itself (they are two different programs
potentially running on different hosts).

However when the SDKD is launched as a subprocess under the harness (either via
a local executable, or via remote SSH execution), all its output is logged
to the database as well as to the screen. The fixed severity level of SDKD
messages is `WARN`.

## Retrieving log messages

Retrieving log messages is documented in the [logview](logview.md) documentation,
but here's a quick recipe:

```
shell> bin/logview -f log.h2.db -F L --loglevel DEBUG
```

to print debug messages from the console to the screen