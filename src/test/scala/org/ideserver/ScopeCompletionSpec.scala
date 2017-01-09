package org.ideserver

import scala.concurrent.Await
import scala.concurrent.duration._

import org.specs2.mutable.Specification

class ScopeCompletionSpec extends Specification with BaseTest
{

  def sourceCode(snippet:String):(Int,String) = {
      val source = s"""
      case class TestClass(key:String, value:String)
      object Test {
        def apply() = {
          val temp = TestClass("hello", "world")
          ${snippet}
        }
      }
      """
      (source.indexOf(TAB), source)
  }

  "Scope Completions" should {
    "Find identifier completions at <empty>" in {
      val (index,source) = sourceCode(s"${TAB}")
      val results = completer.complete("ScopeCompletionSpec.scala",source, index)
      results must have length 680
    }

    "Find identifier completions at t" in {
      val (index,source) = sourceCode(s"t${TAB}")
      val results = completer.complete("ScopeCompletionSpec.scala",source, index)
      results must have length 10
    }
    "Find identifier completions at tem" in {
      val (index,source) = sourceCode(s"tem${TAB}")
      val results = completer.complete("ScopeCompletionSpec.scala",source, index)
      results must have length 1
    }
    "Find identifier class completions at tem" in {
      val (index,source) = sourceCode(s"Tes${TAB}")
      val results = completer.complete("ScopeCompletionSpec.scala",source, index)
      results must have length 3 //case class, companion, object
    }
    "Can find package scoped things" in {
     val (index,source) = sourceCode(s"val c = new java.uti${TAB}")
      val results = completer.complete("ScopeCompletionSpec.scala",source, index)
      results must have length 1 //case class, companion, object
    }
  }
}
