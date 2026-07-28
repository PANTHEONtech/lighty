# Lighty Netty RESTCONF/NETCONF Application
This application provides a RESTCONF north-bound interface built on the native Netty-based RESTCONF transport
(as opposed to the legacy Jetty/JAX-RS stack used by `lighty-community-restconf-netconf-app`), and utilizes the
NETCONF south-bound plugin to manage NETCONF devices on the network. Application works as a standalone SDN
controller. It is capable of connecting to NETCONF devices and exposing connected devices over RESTCONF
north-bound APIs.

Because it is built on the native Netty transport, this application is also able to serve
[RFC 8639](https://www.rfc-editor.org/rfc/rfc8639) / [RFC 8650](https://www.rfc-editor.org/rfc/rfc8650) dynamic
subscriptions (`establish-subscription`, `modify-subscription`, `delete-subscription`, `kill-subscription`) —
something the legacy Jetty/JAX-RS stack cannot do, since RFC 8639 dynamic subscriptions require a transport
session that only the Netty stack provides.

This application starts:
* Lighty Controller
* Netty-based RESTCONF plugin (RFC 8040 + RFC 8639/8650 dynamic subscriptions)
* AAA (Shiro-based authentication — **mandatory** for this application)
* NETCONF south-bound plugin

## Build and Run
build the project: ```mvn clean install```

### Start this demo example
* build the project using ```mvn clean install```
* go to target directory ```cd lighty-examples/lighty-community-netty-restconf-app/target```
* unzip example application bundle ```unzip lighty-community-netty-restconf-app-24.0.0-SNAPSHOT-bin.zip```
* go to unzipped application directory ```cd lighty-community-netty-restconf-app-24.0.0-SNAPSHOT```
* start the example controller application ```java -jar lighty-community-netty-restconf-app-24.0.0-SNAPSHOT.jar```

### Test example application
Once the example application has been started using the command
```java -jar lighty-community-netty-restconf-app-24.0.0-SNAPSHOT.jar```
RESTCONF web interface is available at URL ```http://localhost:8888/restconf/*```

Unlike `lighty-community-restconf-netconf-app`, this application requires HTTP Basic authentication on every
RESTCONF request (AAA is mandatory for the Netty transport). Default credentials are:
```
username: admin
password: admin
```

##### URLs to start with
* __GET__ ```http://localhost:8888/restconf/operations```
* __GET__ ```http://localhost:8888/restconf/data/network-topology:network-topology?content=config```
* __GET__ ```http://localhost:8888/restconf/data/network-topology:network-topology?content=nonconfig```

(all requests need `-u admin:admin`, or an `Authorization: Basic YWRtaW46YWRtaW4=` header)

## Streams and subscriptions (RFC 8639)
This application implements the subscription workflow described in the
[OpenDaylight NETCONF user guide](https://docs.opendaylight.org/projects/netconf/en/latest/user-guide.html#establishing-a-subscription).
Two use cases are covered below: subscribing to the built-in `NETCONF` stream, and subscribing to live datastore
changes on an arbitrary path (the more commonly useful real-world scenario).

### Basic subscription lifecycle
```bash
# 1. Establish a subscription
curl -sS -u admin:admin --request POST \
  --url http://127.0.0.1:8888/restconf/operations/ietf-subscribed-notifications:establish-subscription \
  --header 'Content-Type: application/xml' \
  --data '<input xmlns="urn:ietf:params:xml:ns:yang:ietf-subscribed-notifications">
    <stream>NETCONF</stream>
    <encoding>encode-xml</encoding>
  </input>'
# -> {"ietf-subscribed-notifications:output": {"id": <subscription-id>}}

# 2. Listen for notifications (SSE) — run in its own terminal/tab, leave it open
curl -N -u admin:admin --url http://127.0.0.1:8888/subscriptions/<subscription-id> \
  -H "Accept: text/event-stream"
# -> HTTP 200, text/event-stream, stays open waiting for matching events

# 3. modify/delete-subscription: must reuse the SAME connection as step 1
#    (curl's --next reuses the connection; a fresh curl call won't work — dynamic
#    subscriptions are tied to the transport session that established them, and
#    that session ends the moment its connection closes)
curl -u admin:admin --url .../establish-subscription --data '...' \
  --next \
  -u admin:admin --url .../modify-subscription --data '<input xmlns="..."><id>THE_ID</id>...</input>'

# 4. kill-subscription: fine from any separate connection
#    (unlike modify/delete-subscription, kill-subscription is designed for
#    cross-session/admin use and has no session-affinity check)
curl -u admin:admin --request POST \
  --url http://127.0.0.1:8888/restconf/operations/ietf-subscribed-notifications:kill-subscription \
  --header 'Content-Type: application/xml' \
  --data '<input xmlns="urn:ietf:params:xml:ns:yang:ietf-subscribed-notifications"><id>THE_ID</id></input>'
```

### Real-world use case: watch NETCONF devices being mounted/reconfigured/removed
The `NETCONF` stream above only carries actual YANG `notification` events, not generic datastore writes. To get
notified live whenever a NETCONF device is mounted, reconfigured, or removed from the controller's topology,
create a data-change-event stream scoped to the path you care about, then establish an RFC 8639 subscription on
top of it.

```bash
# 1. Create a data-change stream scoped to the specific device's node in the topology
#    (scoping down to the exact node gives clean per-entry operation labels;
#     scoping to the whole /network-topology:network-topology container also works,
#     but deletions then show up as "operation":"updated" on the parent instead of "deleted")
curl -u admin:admin --request POST \
  --url http://127.0.0.1:8888/restconf/operations/sal-remote:create-data-change-event-subscription \
  --header 'Content-Type: application/xml' \
  --data '<input xmlns="urn:opendaylight:params:xml:ns:yang:controller:md:sal:remote">
    <path xmlns:nt="urn:TBD:params:xml:ns:yang:network-topology">/nt:network-topology/nt:topology[nt:topology-id=&apos;topology-netconf&apos;]/nt:node[nt:node-id=&apos;new-netconf-device&apos;]</path>
  </input>'
# -> {"sal-remote:output": {"stream-name": "urn:uuid:<generated>"}}

# 2. Establish an RFC 8639 subscription on that stream
curl -u admin:admin --request POST \
  --url http://127.0.0.1:8888/restconf/operations/ietf-subscribed-notifications:establish-subscription \
  --header 'Content-Type: application/xml' \
  --data '<input xmlns="urn:ietf:params:xml:ns:yang:ietf-subscribed-notifications">
    <stream>urn:uuid:<generated-from-step-1></stream>
    <encoding>encode-json</encoding>
  </input>'
# -> {"ietf-subscribed-notifications:output": {"id": <subscription-id>}}

# 3. Listen — run this in its own terminal/tab, leave it open
curl -N -u admin:admin --url http://127.0.0.1:8888/subscriptions/<subscription-id> \
  -H "Accept: text/event-stream"
# -> HTTP 200, text/event-stream, stays open waiting for matching events

# 4. Mount the device — from ANY other connection
curl --request PUT -u admin:admin \
  --url http://127.0.0.1:8888/restconf/data/network-topology:network-topology/topology=topology-netconf/node=new-netconf-device \
  --header 'Content-Type: application/json' \
  --data '{
    "network-topology:node": [{
      "node-id": "new-netconf-device",
      "netconf-node-topology:netconf-node": {
        "port": 17830,
        "host": "127.0.0.1",
        "login-password-unencrypted": {"username": "admin", "password": "admin"},
        "keepalive-delay": 120
      }
    }]
  }'
```

What actually streams through to step 3 (real captured output):
```
data: {"ietf-restconf:notification":{"event-time":"2026-07-28T09:26:10...",
  "sal-remote:data-changed-notification":{"data-change-event":[{
    "path":"/network-topology:network-topology/topology[topology-id='topology-netconf']/node[node-id='new-netconf-device']",
    "operation":"created",
    "data":{"network-topology:node":[{"node-id":"new-netconf-device",
      "netconf-node-topology:netconf-node":{"port":17830,"host":"127.0.0.1","keepalive-delay":120,...}}]}
  }]}}}

# ...then PUT just the keepalive-delay leaf to 200...

data: {"ietf-restconf:notification":{"event-time":"2026-07-28T09:26:12...",
  "sal-remote:data-changed-notification":{"data-change-event":[{
    "path":"/network-topology:network-topology/topology[topology-id='topology-netconf']/node[node-id='new-netconf-device']",
    "operation":"updated",
    "data":{"network-topology:node":[{...,"keepalive-delay":200,...}]}
  }]}}}

# ...then DELETE the node...

data: {"ietf-restconf:notification":{"event-time":"2026-07-28T09:26:14...",
  "sal-remote:data-changed-notification":{"data-change-event":[{
    "path":"/network-topology:network-topology/topology[topology-id='topology-netconf']/node[node-id='new-netconf-device']",
    "operation":"deleted"
  }]}}}
```
Notice `deleted` carries no `data` field (there's nothing left to show), while `created`/`updated` carry the full
resulting node contents.

### Common pitfalls
* **Path mismatch**: the stream's `path` in step 1 must match what you're changing. Subscribing to
  `ietf-interfaces` and then editing `network-topology` gets you silence, not an error — the subscription is
  working perfectly, just watching the wrong place.
* **Unqualified top-level JSON key**: PUT/POST bodies need the module-qualified name at the top level —
  `"network-topology:node"`, not bare `"node"` — or you get `400 malformed-message`.
* **PUT vs POST**: once your URL names a specific list entry (e.g. `node=new-netconf-device`), use `PUT`
  (create-or-replace at that exact URI), not `POST`.
* **Listener must be running before the change happens** — the listening connection has to be open first; a
  `GET` you already closed sees nothing.
* **`encoding` is an identityref**: use `"ietf-subscribed-notifications:encode-json"` in JSON payloads, or plain
  `encode-json`/`encode-xml` in XML payloads — not `"json"`.
* **Self-check without watching the stream**: `GET /restconf/data/ietf-subscribed-notifications:subscriptions/subscription=<id>`
  and look at `sent-event-records` under `receivers/receiver` — a rising counter proves delivery even if you're
  not staring at raw SSE output.

## Setup Logging
Default logging configuration may be overwritten by JVM option
```-Dlog4j.configurationFile=path/to/log4j2.xml```

Content of ```log4j2.xml``` is described [here](https://logging.apache.org/log4j/2.x/manual/configuration.html).
