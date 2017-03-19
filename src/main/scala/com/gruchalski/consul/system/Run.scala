package com.gruchalski.consul.system

object Run {

  def main(args: Array[String]): Unit = {
    Server().withShutdownHook().start()
  }

}
