package org.ideserver

import org.specs2.mutable.Specification

class ParseSpec extends Specification with BaseTest
{
  def sourceCode = s"""
      package test

      case class TestClass(key:String, value:String)
      object Test {
        def apply():TestClass = {
          val temp = TestClass("hello", "world")
          tem
        }
      }
      """

  "Parse" should {
    "Find errror 1" in {
      val results = completer.parse("ImportCompletionSpec.scala", sourceCode)
      println(s"Val ${results}")
      results must have length 1
    }
  }
}
