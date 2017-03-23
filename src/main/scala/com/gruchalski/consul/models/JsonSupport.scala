package com.gruchalski.consul.models

trait JsonSupport extends ConsulModelNodeParser
  with ConsulModelServiceHealthCheckParser
  with ConsulModelEventParser
  with ConsulModelKvParser
  with ConsulModelHealthCheckParser
  with WatchDefinitionsParser
