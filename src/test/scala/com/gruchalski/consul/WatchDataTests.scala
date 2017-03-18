package com.gruchalski.consul

import com.gruchalski.consul.models._
import org.scalatest.{Inside, Matchers, WordSpec}
import play.api.libs.json.Json

class WatchDataTests extends WordSpec
  with Matchers
  with Inside
  with ConsulModelNodeParser
  with ConsulModelServiceHealthCheckParser
  with ConsulModelEventParser
  with ConsulModelKvParser
  with ConsulModelHealthCheckParser {

  "Watch data" must {
    "parse watch event info" when {

      "given nodes watch input" in {
        val jsonData =
          """
            |[
            |    {
            |        "Meta": {},
            |        "Node": "support-node",
            |        "Address": "127.0.0.1",
            |        "TaggedAddresses": {
            |            "wan": "127.0.0.1",
            |            "lan": "127.0.0.1"
            |        },
            |        "ID": "622d2470-c34d-31f9-a62b-6aa9a3c6a3cd"
            |    }
            |]
          """.stripMargin

        inside(Json.parse(jsonData).asOpt[List[ConsulModelNode]]) {
          case Some(list) =>
            list.head shouldBe ConsulModelNode(
              node = "support-node",
              address = "127.0.0.1",
              taggedAddresses = ConsulModelTaggedAddresses(
                lan = "127.0.0.1",
                wan = "127.0.0.1"
              ),
              id = "622d2470-c34d-31f9-a62b-6aa9a3c6a3cd"
            )
        }

      }

      "given services watch input" in {
        val jsonData =
          """
            |{
            |  "consul": [],
            |  "support-service": []
            |}
          """.stripMargin
        inside(Json.parse(jsonData).asOpt[Map[String, List[String]]]) {
          case Some(data) =>
            data shouldBe Map(
              "consul" -> List.empty[String],
              "support-service" -> List.empty[String]
            )
        }
      }

      "given service watch input" in {
        val jsonData =
          """
            |[
            |    {
            |        "Service": {
            |            "Port": 0,
            |            "Service": "support-service",
            |            "Tags": [],
            |            "Address": "127.0.0.1",
            |            "EnableTagOverride": false,
            |            "ID": "support-service.1"
            |        },
            |        "Node": {
            |            "TaggedAddresses": {
            |                "lan": "127.0.0.1",
            |                "wan": "127.0.0.1"
            |            },
            |            "Node": "support-node",
            |            "Address": "127.0.0.1",
            |            "Meta": {},
            |            "ID": "622d2470-c34d-31f9-a62b-6aa9a3c6a3cd"
            |        },
            |        "Checks": [
            |            {
            |                "Node": "support-node",
            |                "ServiceID": "",
            |                "CheckID": "serfHealth",
            |                "Status": "passing",
            |                "Output": "Agent alive and reachable",
            |                "Name": "Serf Health Status",
            |                "Notes": "",
            |                "ServiceName": ""
            |            }
            |        ]
            |    }
            |]
          """.stripMargin

        inside(Json.parse(jsonData).asOpt[List[ConsulModelServiceHealthCheck]]) {
          case Some(list) =>
            list.head shouldBe ConsulModelServiceHealthCheck(
              node = ConsulModelNode(
                id = "622d2470-c34d-31f9-a62b-6aa9a3c6a3cd",
                node = "support-node",
                address = "127.0.0.1",
                taggedAddresses = ConsulModelTaggedAddresses(
                  lan = "127.0.0.1",
                  wan = "127.0.0.1"
                )
              ),
              service = ConsulModelHealthCheckService(
                id = "support-service.1",
                service = "support-service",
                tags = List.empty[String],
                address = "127.0.0.1",
                port = 0,
                enableTagOverride = false
              ),
              checks = List(ConsulModelHealthCheck(
                node = "support-node",
                checkId = "serfHealth",
                name = "Serf Health Status",
                status = "passing",
                notes = "",
                output = "Agent alive and reachable",
                serviceId = "",
                serviceName = ""
              ))
            )
        }
      }

      "given key watch input" in {
        val jsonData =
          """
            |{
            |    "LockIndex": 0,
            |    "Flags": 0,
            |    "Value": "c29tZS1kYXRh",
            |    "CreateIndex": 28,
            |    "Key": "some-key",
            |    "ModifyIndex": 28,
            |    "Session": ""
            |}
          """.stripMargin
        inside(Json.parse(jsonData).asOpt[ConsulModelKv]) {
          case Some(data) =>
            data shouldBe ConsulModelKv(
              lockIndex = 0,
              key = "some-key",
              flags = 0,
              value = "c29tZS1kYXRh",
              createIndex = 28,
              modifyIndex = 28,
              session = Some("")
            )
        }
      }

      "given keyprefix watch input" in {
        val jsonData =
          """
            |[
            |    {
            |        "Value": "c29tZS1kYXRh",
            |        "Flags": 0,
            |        "LockIndex": 0,
            |        "CreateIndex": 41,
            |        "Key": "prefix/some-key",
            |        "Session": "",
            |        "ModifyIndex": 41
            |    }
            |]
          """.stripMargin
        inside(Json.parse(jsonData).asOpt[List[ConsulModelKv]]) {
          case Some(list) =>
            list.head shouldBe ConsulModelKv(
              lockIndex = 0,
              key = "prefix/some-key",
              flags = 0,
              value = "c29tZS1kYXRh",
              createIndex = 41,
              modifyIndex = 41,
              session = Some("")
            )
        }
      }

      "given checks watch input" in {
        val jsonData =
          """
            |[
            |  {
            |    "Node": "support-node",
            |    "CheckID": "serfHealth",
            |    "Name": "Serf Health Status",
            |    "Status": "passing",
            |    "Notes": "",
            |    "Output": "Agent alive and reachable",
            |    "ServiceID": "",
            |    "ServiceName": ""
            |  }
            |]
          """.stripMargin

        inside(Json.parse(jsonData).asOpt[List[ConsulModelHealthCheck]]) {
          case Some(list) =>
            list.head shouldBe ConsulModelHealthCheck(
              node = "support-node",
              checkId = "serfHealth",
              name = "Serf Health Status",
              status = "passing",
              notes = "",
              output = "Agent alive and reachable",
              serviceId = "",
              serviceName = ""
            )
        }
      }

      "given event watch input" in {
        val jsonData =
          """
            |[
            |    {
            |        "ServiceFilter": "",
            |        "Version": 1,
            |        "LTime": 2,
            |        "TagFilter": "",
            |        "NodeFilter": "",
            |        "Name": "event-name",
            |        "Payload": "c29tZS1kYXRh",
            |        "ID": "41a95054-3db7-2d0b-64c3-32a82995a089"
            |    }
            |]
          """.stripMargin

        inside(Json.parse(jsonData).asOpt[List[ConsulModelEvent]]) {
          case Some(list) =>
            list shouldBe List(
              ConsulModelEvent(
                id = "41a95054-3db7-2d0b-64c3-32a82995a089",
                name = "event-name",
                nodeFilter = "",
                serviceFilter = "",
                tagFilter = "",
                version = 1,
                ltime = 2,
                payload = Some("c29tZS1kYXRh")
              )
            )
        }
      }

    }
  }

}
