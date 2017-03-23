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

import java.io.FileInputStream

import com.gruchalski.consul.parser.Exceptions.{NoRolesException, UseOfUndefinedVariableException}
import com.gruchalski.consul.cdf.{ConsulWatchIntegrationLexer, ConsulWatchIntegrationParser}
import com.gruchalski.consul.parser._
import org.antlr.v4.runtime.{ANTLRInputStream, CommonTokenStream}
import org.scalatest.{Matchers, WordSpec}

class ParserTests extends WordSpec with Matchers {

  "The parser" must {

    "parse a definition" when {

      "given an object with all variables declared" in {

        val localVars = Parser.emptyVars()
        val environment = Map(
          "A_VARIABLE" -> "anEnvVariable",
          "VARIABLE_IN_ARRAY" -> "variable expansion in an array"
        )

        val is = new FileInputStream(getClass.getResource("/inputs/valid-object.data").getPath)
        val obj = new ConsulWatchIntegrationParser(
          new CommonTokenStream(
            new ConsulWatchIntegrationLexer(
              new ANTLRInputStream(is)
            )
          )
        ).obj()
        val result = new Parser(localVars, environment).parseObject(obj)
        result shouldBe Map(
          "simpleString" -> "this is a string",
          "aVariable" -> environment("A_VARIABLE"),
          "numericValue" -> -1.234,
          "withInteger" -> 1,
          "anArray" -> List(1, 2, environment("VARIABLE_IN_ARRAY")),
          "anObject" -> Map(
            "aNestedObjectKey" -> "with-an-id"
          )
        )

      }

      "given a complex valid example with all variables defined" in {

        val localVars = Parser.emptyVars()
        val environment = Map(
          "EXPECTED_CONSENSUS_SERVERS" -> "3",
          "SVC_NAME_ZOOKEEPER" -> "zookeeper",
          "SVC_NAME_MESOS_MASTER_ZK" -> "mesos-master-zk-address",
          "ZOOKEEPER_TICK_TIME" -> "2000",
          "ZOOKEEPER_DATA_DIR" -> "/opt/zookeeper/data",
          "ZOOKEEPER_CLIENT_PORT" -> "2181",
          "ZOOKEEPER_INIT_LIMIT" -> "30000",
          "ZOOKEEPER_SYNC_LIMIT" -> "30000",
          "SERVER_ID" -> "0"
        )

        val is = new FileInputStream(getClass.getResource("/inputs/valid-complex-example.data").getPath)
        val prog = new ConsulWatchIntegrationParser(
          new CommonTokenStream(
            new ConsulWatchIntegrationLexer(
              new ANTLRInputStream(is)
            )
          )
        ).prog()
        val result = new Parser(localVars, environment).parseProg(prog)
        result.roles shouldBe List("zookeeper", "mesos-master")
        result.log should matchPattern { case Some(_) => }
        result.consulWatchTriggers shouldBe Map(
          (3, "zookeeper") -> ConsulServiceWatch(3, "zookeeper",
            Map(Some("zookeeper") ->
              ConsulServiceWatchRestriction(Some("zookeeper"), List(
                ExecAction("/bin/bash -c 'ping google.com'"),
                ExecAction("/bin/bash -c 'ping google.com'", Some("stat /etc/some/action")),
                TemplateAction("$env.PROGRAMS_DIR/zookeeper/templates/tmpl.zoo.cfg", "/etc/zookeeper/zoo.cfg", Map(
                  "tickTime" -> "2000",
                  "dataDir" -> "/opt/zookeeper/data",
                  "clientPort" -> "2181",
                  "initLimit" -> "30000",
                  "syncLimit" -> "30000",
                  "ports" -> List(1, 2, 3)
                ), Some(ExecAction("$env.PROGRAMS_DIR/mesos-master/helpers/append-server-info"))),
                TemplateAction("$env.PROGRAMS_DIR/zookeeper/templates/tmpl.myid", "/etc/zookeeper/myid", Map(
                  "serverId" -> "0"
                )),
                SystemServiceAction(SystemServiceActions("system_service_start"), environment("SVC_NAME_ZOOKEEPER")),
                CreateConsulServiceAction(environment("SVC_NAME_MESOS_MASTER_ZK"), Map(
                  "id" -> "Escaped quotes \" must work",
                  "address" -> "zk://$consul.service-adresses-list$env.MESOS_ZK_PATH"
                ))
              ))))
        )

      }

    }

    "fail to parse a definition" when {

      "an undeclared variable is used" in {
        val localVars = Parser.emptyVars()
        val environment = Parser.emptyVars()
        val is = new FileInputStream(getClass.getResource("/inputs/valid-object.data").getPath)
        val obj = new ConsulWatchIntegrationParser(
          new CommonTokenStream(
            new ConsulWatchIntegrationLexer(
              new ANTLRInputStream(is)
            )
          )
        ).obj()
        intercept[UseOfUndefinedVariableException] { new Parser(localVars, environment).parseObject(obj) }
      }

      "no roles defined" in {
        val localVars = Parser.emptyVars()
        val environment = Parser.emptyVars()
        val is = new FileInputStream(getClass.getResource("/inputs/definition-without-roles.data").getPath)
        val prog = new ConsulWatchIntegrationParser(
          new CommonTokenStream(
            new ConsulWatchIntegrationLexer(
              new ANTLRInputStream(is)
            )
          )
        ).prog()
        intercept[NoRolesException] { new Parser(localVars, environment).parseProg(prog) }
      }

    }

  }

}
