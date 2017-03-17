package com.gruchalski.consul

import com.gruchalski.consul.models._
import org.scalatest.{Inside, Matchers, WordSpec}
import play.api.libs.json.Json

class AgentResponseTests extends WordSpec
  with Matchers
  with AgentKeyResponseParser
  with AgentNodeResponseParser
  with ServiceWatchParser
  with AgentEventResponseParser
  with Inside {

  "Agent response" must {
    "parse" when {

      "given a GET /v1/catalog/services response" ignore {

      }

      "given a GET /v1/health/service/:service response" in {

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
            list.head.node shouldBe AgentNodeResponse("foobar", "10.1.10.12")
            list.head.service shouldBe ServiceWatchService("redis", "redis", None, 8000)
            list.head.checks.get.size shouldBe 2
        }

      }

      "given a GET /v1/event/list response" in {

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

        inside(Json.parse(jsonData).asOpt[List[AgentEventResponse]]) {
          case Some(list) =>
            list.head.id shouldBe "f07f3fcc-4b7d-3a7c-6d1e-cf414039fcee"
            list.head.ltime shouldBe 18L
        }

      }

      "given a GET /v1/catalog/nodes response" in {

        val jsonData =
          """
            |[
            |  {
            |    "Node": "nyc1-consul-1",
            |    "Address": "192.241.159.115"
            |  },
            |  {
            |    "Node": "nyc1-consul-2",
            |    "Address": "192.241.158.205"
            |  },
            |  {
            |    "Node": "nyc1-consul-3",
            |    "Address": "198.199.77.133"
            |  },
            |  {
            |    "Node": "nyc1-worker-1",
            |    "Address": "162.243.162.228"
            |  },
            |  {
            |    "Node": "nyc1-worker-2",
            |    "Address": "162.243.162.226"
            |  },
            |  {
            |    "Node": "nyc1-worker-3",
            |    "Address": "162.243.162.229"
            |  }
            |]
          """.stripMargin

        inside(Json.parse(jsonData).asOpt[List[AgentNodeResponse]]) {
          case Some(list) =>
            list.head.node shouldBe "nyc1-consul-1"
            list.last.node shouldBe "nyc1-worker-3"
            list.last.address shouldBe "162.243.162.229"
        }

      }

      "given a GET /v1/health/state/ reponse" in {

        val jsonData =
          """
            |[
            |  {
            |    "Node": "foobar",
            |    "CheckID": "service:redis",
            |    "Name": "Service 'redis' check",
            |    "Status": "passing",
            |    "Notes": "",
            |    "Output": "",
            |    "ServiceID": "redis",
            |    "ServiceName": "redis"
            |  }
            |]
          """.stripMargin

        inside(Json.parse(jsonData).asOpt[List[AgentCheckResponse]]) {
          case Some(list) =>
            list.head.node shouldBe "foobar"
            list.last.name shouldBe "Service 'redis' check"
            list.last.status shouldBe "passing"
        }

      }

      "given a GET /v1/kv/ response" in {

        val jsonData =
          """
            |[
            |  {
            |    "Key": "foo/bar",
            |    "CreateIndex": 1796,
            |    "ModifyIndex": 1796,
            |    "LockIndex": 0,
            |    "Flags": 0,
            |    "Value": "TU9BUg==",
            |    "Session": ""
            |  },
            |  {
            |    "Key": "foo/baz",
            |    "CreateIndex": 1795,
            |    "ModifyIndex": 1795,
            |    "LockIndex": 0,
            |    "Flags": 0,
            |    "Value": "YXNkZg==",
            |    "Session": ""
            |  },
            |  {
            |    "Key": "foo/test",
            |    "CreateIndex": 1793,
            |    "ModifyIndex": 1793,
            |    "LockIndex": 0,
            |    "Flags": 0,
            |    "Value": "aGV5",
            |    "Session": ""
            |  }
            |]
          """.stripMargin

        inside(Json.parse(jsonData).asOpt[List[AgentKeyResponse]]) {
          case Some(list) =>
            list.head.createIndex shouldBe 1796L
            list.last.value shouldBe "aGV5"
            list.last.key shouldBe "foo/test"
        }

      }

    }
  }

}
