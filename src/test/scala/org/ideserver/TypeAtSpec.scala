package org.ideserver

import scala.concurrent.Await
import scala.concurrent.duration._

import org.specs2.mutable.Specification

class TypeAtSpec extends Specification with BaseTest {

  def sourceCode = s"""
      package test

      case class TestClass(key:String, value:String)
      object Test {
        def apply():TestClass = {
          val temp = TestClass("hello", "world")
          temp
        }
      }
      """

  "TypeAt" should {
    "Find val type" in {
      val results = completer.typeAt("ImportCompletionSpec.scala", sourceCode, 144)
      println(s"Val ${results}")
      results must have length 1
    }
    "Find class type" in {
      val results = completer.typeAt("ImportCompletionSpec.scala", sourceCode, 153)
      println(s"Class ${results}")
      results must have length 1
    }

    "Find ident type" in {
      val results = completer.typeAt("ImportCompletionSpec.scala", sourceCode, 190)
      println(s"ident ${results}")
      results must have length 1
    }

  }
}
