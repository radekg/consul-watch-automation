package com.gruchalski.consul

import java.io.FileInputStream

object Runner {

  def main(args: Array[String]): Unit = {

    val res = Parser.parse(
      is = new FileInputStream("/Users/rad/dev/my/antlr-playground/test-files/test.cdf"),
      environment = Map(
        "EXPECTED_CONSENSUS_SERVERS" -> "3",
        "SVC_NAME_ZOOKEEPER" -> "zookeeper"))
    println(s" ================> $res")

  }

}
