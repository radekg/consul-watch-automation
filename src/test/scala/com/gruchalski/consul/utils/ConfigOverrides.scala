package com.gruchalski.consul.utils

import com.typesafe.config.ConfigFactory
import scala.collection.JavaConverters._

object ConfigDefaults {
  def apply(defaults: Map[String, _]) =
    ConfigFactory.parseMap(defaults.asJava).withFallback(ConfigFactory.load().resolve())
}
