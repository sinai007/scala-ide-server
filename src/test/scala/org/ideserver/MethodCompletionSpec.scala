package org.ideserver

import scala.concurrent.Await
import scala.concurrent.duration._

import org.specs2.mutable.Specification

class MethodCompletionSpec extends Specification with BaseTest
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


  "Method Completions" should {

    List("temp.k", "temp.ke") map { term =>
      s"Find method completions at ${term}" in {
        val (index,source) = sourceCode(s"${term}${TAB}")
        val results = completer.complete("MethodCompletionSpec.scala", source, index)
        results must have length 1
      }
    }

// //    Block completion...
//     List("temp.key", "temp.key(") map { term =>
//       s"Not find method completions at ${term}" in {
//         val (index,source) = sourceCode(s"${term}${TAB}")
//         val results = completer.complete("MethodCompletionSpec.scala",source, index)
//         results must have length 0
//       }
//     }
    "Find method completions at ." in {
      val (index,source) = sourceCode(s"temp.${TAB}")
      val results = completer.complete("MethodCompletionSpec.scala",source, index)
      results must have length 33
    }
  }

}
