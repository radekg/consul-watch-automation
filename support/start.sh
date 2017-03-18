#!/usr/bin/env bash

base="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

SERVICE_NAME="support-service"
CONSUL_CONFIG_DIR=$base/consul-config-dir

INSTALL_CHECKS=false
INSTALL_SERVICES=true

WATCH_SERVICE=true
WATCH_KEY=false
WATCH_KEYPREFIX=false
WATCH_SERVICES=true
WATCH_NODES=true
WATCH_CHECKS=false
WATCH_EVENT=true

if [ "$WATCH_SERVICE" = "true" ]; then
    tee $CONSUL_CONFIG_DIR/watch-service.json <<EOF
    {
      "watches": [{
        "type": "service",
        "service": "$SERVICE_NAME",
        "handler": "$base/passthru-watch/watch.py --service"
      }]
    }
EOF
fi

if [ "$WATCH_KEY" = "true" ]; then
    tee $CONSUL_CONFIG_DIR/watch-key.json <<EOF
    {
      "watches": [{
        "type": "key",
        "key": "some-key",
        "handler": "$base/passthru-watch/watch.py --key"
      }]
    }
EOF
fi

if [ "$WATCH_KEYPREFIX" = "true" ]; then
    tee $CONSUL_CONFIG_DIR/watch-keyprefix.json <<EOF
    {
      "watches": [{
        "type": "keyprefix",
        "prefix": "prefix/",
        "handler": "$base/passthru-watch/watch.py --keyprefix"
      }]
    }
EOF
fi

if [ "$WATCH_SERVICES" = "true" ]; then
    tee $CONSUL_CONFIG_DIR/watch-services.json <<EOF
    {
      "watches": [{
        "type": "services",
        "handler": "$base/passthru-watch/watch.py --services"
      }]
    }
EOF
fi

if [ "$WATCH_NODES" = "true" ]; then
    tee $CONSUL_CONFIG_DIR/watch-nodes.json <<EOF
    {
      "watches": [{
        "type": "nodes",
        "handler": "$base/passthru-watch/watch.py --nodes"
      }]
    }
EOF
fi

if [ "$WATCH_CHECKS" = "true" ]; then
    tee $CONSUL_CONFIG_DIR/watch-checks.json <<EOF
    {
      "watches": [{
        "type": "checks",
        "handler": "$base/passthru-watch/watch.py --checks"
      }]
    }
EOF
fi

if [ "$WATCH_EVENT" = "true" ]; then
    tee $CONSUL_CONFIG_DIR/watch-event.json <<EOF
    {
      "watches": [{
        "type": "event",
        "name": "event-name",
        "handler": "$base/passthru-watch/watch.py --event"
      }]
    }
EOF
fi

if [ "$INSTALL_CHECKS" = "true" ]; then
    tee $base/check-simple.json <<EOF
    {
      "checks": [{
        "id": "simple",
        "name": "Simple test check",
        "script": "echo 1",
        "interval": "1m",
        "timeout": "1s"
      }]
    }
EOF
fi

if [ "$INSTALL_SERVICES" = "true" ]; then
    tee $CONSUL_CONFIG_DIR/service-support-service.json <<EOF
    {
      "service": {
        "id": "$SERVICE_NAME.1",
        "name": "$SERVICE_NAME",
        "port": 0,
        "address": "127.0.0.1",
        "tags": [],
        "checks": [
          {
            "script": "echo 1",
            "interval": "1m"
          }
        ]
      }
    }
EOF
fi

$base/consul/consul agent --config-dir $CONSUL_CONFIG_DIR/consul-config-dir