#!/usr/bin/env bash

base="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

CONSUL_CONFIG_DIR=$base/consul-config-dir

rm -rf $base/consul/raft
rm -rf $base/consul/serf
rm -rf $base/consul/checkpoint-signature
rm -rf $base/consul/node-id
rm -rf $base/passthru-watch/output.log

rm -rf $CONSUL_CONFIG_DIR/check-*
rm -rf $CONSUL_CONFIG_DIR/service-*
rm -rf $CONSUL_CONFIG_DIR/watch-*