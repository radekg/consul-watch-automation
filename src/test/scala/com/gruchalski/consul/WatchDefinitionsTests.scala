package com.gruchalski.consul

import com.gruchalski.consul.models._
import org.scalatest.{Inside, Matchers, WordSpec}
import play.api.libs.json.Json

class WatchDefinitionsTests extends WordSpec with Matchers with Inside with JsonSupport {

  "Watch JSON" must {
    "parse correctly to an instance" when {
      "given a key watch declaration" in {

        val jsonData =
          """
            |[
            |  {
            |    "type": "key",
            |    "key": "foo/bar/baz",
            |    "handler": "/usr/bin/my-key-handler.sh"
            |  },
            |  {
            |    "type": "keyprefix",
            |    "prefix": "foo/",
            |    "handler": "/usr/bin/my-prefix-handler.sh"
            |  },
            |  {
            |    "type": "services",
            |    "handler": "/usr/bin/my-services-handler.sh"
            |  },
            |  {
            |    "type": "nodes",
            |    "handler": "/usr/bin/my-nodes-handler.sh"
            |  },
            |  {
            |    "type": "service",
            |    "service": "redis",
            |    "handler": "/usr/bin/my-service-handler.sh"
            |  },
            |  {
            |    "type": "checks",
            |    "handler": "/usr/bin/my-checks-handler.sh"
            |  },
            |  {
            |    "type": "event",
            |    "name": "web-deploy",
            |    "handler": "/usr/bin/my-deploy-handler.sh"
            |  }
            |]
          """.stripMargin

        inside(Json.parse(jsonData).asOpt[List[WatchDefinition]]) {
          case Some(list) =>
            list shouldBe List(
              WatchDefinitionKey(
                key = "foo/bar/baz",
                handler = "/usr/bin/my-key-handler.sh"),
              WatchDefinitionKeyprefix(
                prefix = "foo/",
                handler = "/usr/bin/my-prefix-handler.sh"),
              WatchDefinitionServices(
                handler = "/usr/bin/my-services-handler.sh"),
              WatchDefinitionNodes(
                handler = "/usr/bin/my-nodes-handler.sh"),
              WatchDefinitionService(
                service = "redis",
                handler = "/usr/bin/my-service-handler.sh"),
              WatchDefinitionChecks(
                handler = "/usr/bin/my-checks-handler.sh"),
              WatchDefinitionEvent(
                name = "web-deploy",
                handler = "/usr/bin/my-deploy-handler.sh")
            )
        }

      }
    }
  }

}
