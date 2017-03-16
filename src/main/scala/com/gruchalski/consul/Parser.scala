package com.gruchalski.consul

import java.io.InputStream

import com.gruchalski.consul.cdf.{CdfVisitor, ConsulWatchIntegrationLexer, ConsulWatchIntegrationParser}
import org.antlr.v4.runtime.{ANTLRInputStream, CommonTokenStream}

object Parser {

  type Vars = Map[String, String]

  def emptyVars(): Vars = {
    Map.empty[String, String]
  }

  def parse(is: InputStream, localVars: Vars = emptyVars(), environment: Vars = emptyVars()): Any =
    (new CdfVisitor(localVars = localVars, environment = environment)).visit(new ConsulWatchIntegrationParser(
      new CommonTokenStream(
        new ConsulWatchIntegrationLexer(
          new ANTLRInputStream(is)))).prog())

}
