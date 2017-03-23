package com.gruchalski.consul.system.config

import com.typesafe.config.Config

import scala.util.Try

case class Configuration(val underlying: Config) {

  lazy val `com.gruchalski.consul.bind.host` = Try { underlying.getString("com.gruchalski.consul.bind.host") }
    .getOrElse("localhost")
  lazy val `com.gruchalski.consul.bind.port` = Try { underlying.getInt("com.gruchalski.consul.bind.port") }
    .getOrElse(9000)
  lazy val `com.gruchalski.consul.access-token` = Try { underlying.getString("com.gruchalski.consul.access-token") }
    .getOrElse("please-set-your-own-token")
  lazy val `com.gruchalski.consul.config-file` = Try { underlying.getString("com.gruchalski.consul.config-file") }
    .getOrElse("resource:///default.data")

}
