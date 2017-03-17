package com.gruchalski.consul

import com.gruchalski.consul.models.{ServiceWatch, ServiceWatchNode, ServiceWatchParser, ServiceWatchService}
import org.scalatest.{Inside, Matchers, WordSpec}
import play.api.libs.json.Json

class AgentResponseTests extends WordSpec
  with Matchers
  with ServiceWatchParser
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

    }
  }

}
