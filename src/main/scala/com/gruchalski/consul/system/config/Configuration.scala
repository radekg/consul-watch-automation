/*
 * Copyright 2017 Rad Gruchalski
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
