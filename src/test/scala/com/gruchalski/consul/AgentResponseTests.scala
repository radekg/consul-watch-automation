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

package com.gruchalski.consul

import com.gruchalski.consul.models._
import org.scalatest.{Inside, Matchers, WordSpec}
import play.api.libs.json.Json

class AgentResponseTests extends WordSpec
    with Matchers
    with ConsulModelEventParser
    with ConsulModelKvParser
    with ConsulModelHealthCheckParser
    with ConsulModelServiceHealthCheckParser
    with ConsulModelNodeParser
    with ConsulModelNodeExtendedParser
    with ConsulModelServiceParser
    with Inside {

  "Agent response" must {

    "parse event response" when {

      "given a GET /v1/event/list" in {

        val jsonData =
          """
            |[
            |  {
            |    "ID": "a53d85eb-b4d7-0d17-fff5-05925c6ac0ec",
            |    "Name": "event-name",
            |    "Payload": null,
            |    "NodeFilter": "",
            |    "ServiceFilter": "",
            |    "TagFilter": "",
            |    "Version": 1,
            |    "LTime": 2
            |  },
            |  {
            |    "ID": "a53d85eb-b4d7-0d17-fff5-05925c6ac0ee",
            |    "Name": "event-name",
            |    "Payload": "with-payload",
            |    "NodeFilter": "",
            |    "ServiceFilter": "",
            |    "TagFilter": "",
            |    "Version": 1,
            |    "LTime": 3
            |  }
            |]
          """.stripMargin

        inside(Json.parse(jsonData).asOpt[List[ConsulModelEvent]]) {
          case Some(list) =>
            list shouldBe List(
              ConsulModelEvent(
                id = "a53d85eb-b4d7-0d17-fff5-05925c6ac0ec",
                name = "event-name",
                nodeFilter = "",
                serviceFilter = "",
                tagFilter = "",
                version = 1,
                ltime = 2
              ),
              ConsulModelEvent(
                id = "a53d85eb-b4d7-0d17-fff5-05925c6ac0ee",
                name = "event-name",
                nodeFilter = "",
                serviceFilter = "",
                tagFilter = "",
                version = 1,
                ltime = 3,
                payload = Some("with-payload")
              )
            )
        }

      }

    }

    "parse catalog response" when {

      "given a GET /v1/catalog/datacenters" in {
        val jsonData =
          """
            |[
            |  "support"
            |]
          """.stripMargin
        inside(Json.parse(jsonData).asOpt[List[String]]) {
          case Some(data) =>
            data shouldBe List("support")
        }
      }

      "given a GET /v1/catalog/nodes" in {

        val jsonData =
          """
            |[
            |  {
            |    "ID": "622d2470-c34d-31f9-a62b-6aa9a3c6a3cd",
            |    "Node": "support-node",
            |    "Address": "127.0.0.1",
            |    "TaggedAddresses": {
            |      "lan": "127.0.0.1",
            |      "wan": "127.0.0.1"
            |    },
            |    "Meta": {},
            |    "CreateIndex": 4,
            |    "ModifyIndex": 5
            |  }
            |]
          """.stripMargin

        inside(Json.parse(jsonData).asOpt[List[ConsulModelNode]]) {
          case Some(list) =>
            list.head shouldBe ConsulModelNode(
              id = "622d2470-c34d-31f9-a62b-6aa9a3c6a3cd",
              node = "support-node",
              address = "127.0.0.1",
              taggedAddresses = ConsulModelTaggedAddresses(
                lan = "127.0.0.1",
                wan = "127.0.0.1"
              ),
              createIndex = Some(4),
              modifyIndex = Some(5)
            )
        }

      }

      "given a GET /v1/catalog/services" in {
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

      "given a GET /v1/catalog/service/:service" in {
        val jsonData =
          """
            |[
            |  {
            |    "ID": "622d2470-c34d-31f9-a62b-6aa9a3c6a3cd",
            |    "Node": "support-node",
            |    "Address": "127.0.0.1",
            |    "TaggedAddresses": {
            |      "lan": "127.0.0.1",
            |      "wan": "127.0.0.1"
            |    },
            |    "NodeMeta": {},
            |    "ServiceID": "support-service.1",
            |    "ServiceName": "support-service",
            |    "ServiceTags": [],
            |    "ServiceAddress": "127.0.0.1",
            |    "ServicePort": 0,
            |    "ServiceEnableTagOverride": false,
            |    "CreateIndex": 6,
            |    "ModifyIndex": 6
            |  }
            |]
          """.stripMargin

        inside(Json.parse(jsonData).asOpt[List[ConsulModelService]]) {
          case Some(list) =>
            list.head shouldBe ConsulModelService(
              id = "622d2470-c34d-31f9-a62b-6aa9a3c6a3cd",
              node = "support-node",
              address = "127.0.0.1",
              taggedAddresses = ConsulModelTaggedAddresses(
                lan = "127.0.0.1",
                wan = "127.0.0.1"
              ),
              serviceId = "support-service.1",
              serviceName = "support-service",
              serviceTags = List.empty[String],
              serviceAddress = "127.0.0.1",
              servicePort = 0,
              serviceEnableTagOverride = false,
              createIndex = Some(6),
              modifyIndex = Some(6)
            )
        }
      }

      "given a GET /v1/catalog/node/:node" in {
        val jsonData =
          """
            |{
            |  "Node": {
            |    "ID": "622d2470-c34d-31f9-a62b-6aa9a3c6a3cd",
            |    "Node": "support-node",
            |    "Address": "127.0.0.1",
            |    "TaggedAddresses": {
            |      "lan": "127.0.0.1",
            |      "wan": "127.0.0.1"
            |    },
            |    "Meta": {},
            |    "CreateIndex": 4,
            |    "ModifyIndex": 5
            |  },
            |  "Services": {
            |    "consul": {
            |      "ID": "consul",
            |      "Service": "consul",
            |      "Tags": [],
            |      "Address": "",
            |      "Port": 8300,
            |      "EnableTagOverride": false,
            |      "CreateIndex": 4,
            |      "ModifyIndex": 5
            |    },
            |    "support-service.1": {
            |      "ID": "support-service.1",
            |      "Service": "support-service",
            |      "Tags": [],
            |      "Address": "127.0.0.1",
            |      "Port": 0,
            |      "EnableTagOverride": false,
            |      "CreateIndex": 6,
            |      "ModifyIndex": 6
            |    }
            |  }
            |}
          """.stripMargin

        inside(Json.parse(jsonData).asOpt[ConsulModelNodeExtended]) {
          case Some(data) =>
            data shouldBe ConsulModelNodeExtended(
              node = ConsulModelNode(
                id = "622d2470-c34d-31f9-a62b-6aa9a3c6a3cd",
                node = "support-node",
                address = "127.0.0.1",
                taggedAddresses = ConsulModelTaggedAddresses(
                  lan = "127.0.0.1",
                  wan = "127.0.0.1"
                ),
                createIndex = Some(4),
                modifyIndex = Some(5)
              ),
              services = Map(
                "consul" -> ConsulModelHealthCheckService(
                  id = "consul",
                  service = "consul",
                  tags = List.empty[String],
                  address = "",
                  port = 8300,
                  enableTagOverride = false,
                  createIndex = Some(4),
                  modifyIndex = Some(5)
                ),
                "support-service.1" -> ConsulModelHealthCheckService(
                  id = "support-service.1",
                  service = "support-service",
                  tags = List.empty[String],
                  address = "127.0.0.1",
                  port = 0,
                  enableTagOverride = false,
                  createIndex = Some(6),
                  modifyIndex = Some(6)
                )
              )
            )
        }
      }

    }

    "parse k/v response" when {

      "given a GET /v1/kv/:key" in {

        val jsonData =
          """
            |[
            |  {
            |    "LockIndex": 0,
            |    "Key": "some-key",
            |    "Flags": 0,
            |    "Value": "c29tZS1kYXRh",
            |    "CreateIndex": 256,
            |    "ModifyIndex": 256
            |  }
            |]
          """.stripMargin

        inside(Json.parse(jsonData).asOpt[List[ConsulModelKv]]) {
          case Some(list) =>
            list shouldBe List(
              ConsulModelKv(
                lockIndex = 0,
                key = "some-key",
                flags = 0,
                value = "c29tZS1kYXRh",
                createIndex = 256,
                modifyIndex = 256
              )
            )
        }

      }

    }

    "parse health check response" when {

      "given a GET /v1/health/node/:node" in {

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
            |    "ServiceName": "",
            |    "CreateIndex": 4,
            |    "ModifyIndex": 4
            |  },
            |  {
            |    "Node": "support-node",
            |    "CheckID": "service:support-service.1",
            |    "Name": "Service 'support-service' check",
            |    "Status": "passing",
            |    "Notes": "",
            |    "Output": "1\n",
            |    "ServiceID": "support-service.1",
            |    "ServiceName": "support-service",
            |    "CreateIndex": 6,
            |    "ModifyIndex": 9
            |  },
            |  {
            |    "Node": "support-node",
            |    "CheckID": "simple",
            |    "Name": "Simple test check",
            |    "Status": "passing",
            |    "Notes": "",
            |    "Output": "1\n",
            |    "ServiceID": "",
            |    "ServiceName": "",
            |    "CreateIndex": 7,
            |    "ModifyIndex": 11
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
              serviceName = "",
              createIndex = Some(4),
              modifyIndex = Some(4)
            )
        }

      }

      "given a GET /v1/health/checks/:service" in {

        val jsonData =
          """
            |[
            |  {
            |    "Node": "support-node",
            |    "CheckID": "service:support-service.1",
            |    "Name": "Service 'support-service' check",
            |    "Status": "passing",
            |    "Notes": "",
            |    "Output": "1",
            |    "ServiceID": "support-service.1",
            |    "ServiceName": "support-service",
            |    "CreateIndex": 6,
            |    "ModifyIndex": 9
            |  }
            |]
          """.stripMargin

        inside(Json.parse(jsonData).asOpt[List[ConsulModelHealthCheck]]) {
          case Some(list) =>
            list.head shouldBe ConsulModelHealthCheck(
              node = "support-node",
              checkId = "service:support-service.1",
              name = "Service 'support-service' check",
              status = "passing",
              notes = "",
              output = "1",
              serviceId = "support-service.1",
              serviceName = "support-service",
              createIndex = Some(6),
              modifyIndex = Some(9)
            )
        }
      }

      "given a GET /v1/health/service/:service" in {

        val jsonData =
          """
            |[
            |  {
            |    "Node": {
            |      "ID": "622d2470-c34d-31f9-a62b-6aa9a3c6a3cd",
            |      "Node": "support-node",
            |      "Address": "127.0.0.1",
            |      "TaggedAddresses": {
            |        "lan": "127.0.0.1",
            |        "wan": "127.0.0.1"
            |      },
            |      "Meta": {},
            |      "CreateIndex": 4,
            |      "ModifyIndex": 5
            |    },
            |    "Service": {
            |      "ID": "support-service.1",
            |      "Service": "support-service",
            |      "Tags": [],
            |      "Address": "127.0.0.1",
            |      "Port": 0,
            |      "EnableTagOverride": false,
            |      "CreateIndex": 6,
            |      "ModifyIndex": 6
            |    },
            |    "Checks": [
            |      {
            |        "Node": "support-node",
            |        "CheckID": "serfHealth",
            |        "Name": "Serf Health Status",
            |        "Status": "passing",
            |        "Notes": "",
            |        "Output": "Agent alive and reachable",
            |        "ServiceID": "",
            |        "ServiceName": "",
            |        "CreateIndex": 4,
            |        "ModifyIndex": 4
            |      }
            |    ]
            |  }
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
                ),
                createIndex = Some(4),
                modifyIndex = Some(5)
              ),
              service = ConsulModelHealthCheckService(
                id = "support-service.1",
                service = "support-service",
                tags = List.empty[String],
                address = "127.0.0.1",
                port = 0,
                enableTagOverride = false,
                createIndex = Some(6),
                modifyIndex = Some(6)
              ),
              checks = List(ConsulModelHealthCheck(
                node = "support-node",
                checkId = "serfHealth",
                name = "Serf Health Status",
                status = "passing",
                notes = "",
                output = "Agent alive and reachable",
                serviceId = "",
                serviceName = "",
                createIndex = Some(4),
                modifyIndex = Some(4)
              ))
            )
        }

      }

      "given a GET /v1/health/state/:state" in {

        val jsonData =
          """
            |[
            |  {
            |    "Node": "support-node",
            |    "CheckID": "service:support-service.1",
            |    "Name": "Service 'support-service' check",
            |    "Status": "passing",
            |    "Notes": "",
            |    "Output": "1",
            |    "ServiceID": "support-service.1",
            |    "ServiceName": "support-service",
            |    "CreateIndex": 6,
            |    "ModifyIndex": 9
            |  }
            |]
          """.stripMargin

        inside(Json.parse(jsonData).asOpt[List[ConsulModelHealthCheck]]) {
          case Some(list) =>
            list.head shouldBe ConsulModelHealthCheck(
              node = "support-node",
              checkId = "service:support-service.1",
              name = "Service 'support-service' check",
              status = "passing",
              notes = "",
              output = "1",
              serviceId = "support-service.1",
              serviceName = "support-service",
              createIndex = Some(6),
              modifyIndex = Some(9)
            )
        }
      }

    }
  }

}
