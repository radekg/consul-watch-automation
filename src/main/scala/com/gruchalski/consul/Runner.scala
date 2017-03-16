package com.gruchalski.consul

import java.io.FileInputStream

object Runner {

  def main(args: Array[String]): Unit = {

    val res = Parser.parse(
      is = new FileInputStream("/Users/rad/dev/my/antlr-playground/test-files/test.cdf"),
      environment = Map(
        "EXPECTED_CONSENSUS_SERVERS" -> "3",
        "SVC_NAME_ZOOKEEPER" -> "zookeeper",
        "SVC_NAME_MESOS_MASTER_ZK" -> "mesos-master-zk-address",
        "ZOOKEEPER_TICK_TIME" -> "2000",
        "ZOOKEEPER_DATA_DIR" -> "/opt/zookeeper/data",
        "ZOOKEEPER_CLIENT_PORT" -> "2181",
        "ZOOKEEPER_INIT_LIMIT" -> "30000",
        "ZOOKEEPER_SYNC_LIMIT" -> "30000",
        "SERVER_ID" -> "0"))
    println(s" ================> $res")

  }

}
