package com.gruchalski.consul

import com.gruchalski.consul.models._
import org.scalatest.{Inside, Matchers, WordSpec}
import play.api.libs.json.Json

class AgentResponseTests extends WordSpec
  with Matchers
  with ServiceWatchParser
  with EventWatchParser
  with Inside {

  "Agent response" must {
    "parse" when {

      "given valid service check declaration response" in {

        val jsonData =
          """
            |[
            |  {
            |    "Node": {
            |      "Node": "foobar",
            |      "Address": "10.1.10.12"
            |    },
            |    "Service": {
            |      "ID": "redis",
            |      "Service": "redis",
            |      "Tags": null,
            |      "Port": 8000
            |    },
            |    "Checks": [
            |      {
            |        "Node": "foobar",
            |        "CheckID": "service:redis",
            |        "Name": "Service 'redis' check",
            |        "Status": "passing",
            |        "Notes": "",
            |        "Output": "",
            |        "ServiceID": "redis",
            |        "ServiceName": "redis"
            |      },
            |      {
            |        "Node": "foobar",
            |        "CheckID": "serfHealth",
            |        "Name": "Serf Health Status",
            |        "Status": "passing",
            |        "Notes": "",
            |        "Output": "",
            |        "ServiceID": "",
            |        "ServiceName": ""
            |      }
            |    ]
            |  }
            |]
          """.stripMargin

        inside(Json.parse(jsonData).asOpt[List[ServiceWatch]]) {
          case Some(list) =>
            list.head.node shouldBe ServiceWatchNode("foobar", "10.1.10.12")
            list.head.service shouldBe ServiceWatchService("redis", "redis", None, 8000)
            list.head.checks.get.size shouldBe 2
        }

      }

      "given valid event declaration response" in {

        val jsonData =
          """
            |[
            |  {
            |    "ID": "f07f3fcc-4b7d-3a7c-6d1e-cf414039fcee",
            |    "Name": "web-deploy",
            |    "Payload": "MTYwOTAzMA==",
            |    "NodeFilter": "",
            |    "ServiceFilter": "",
            |    "TagFilter": "",
            |    "Version": 1,
            |    "LTime": 18
            |  }
            |]
          """.stripMargin

        inside(Json.parse(jsonData).asOpt[List[EventWatch]]) {
          case Some(list) =>
            list.head.id shouldBe "f07f3fcc-4b7d-3a7c-6d1e-cf414039fcee"
            list.head.ltime shouldBe 18L
        }

      }

    }
  }

}
