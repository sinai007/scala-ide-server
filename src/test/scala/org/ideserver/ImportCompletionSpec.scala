package org.ideserver

import scala.concurrent.Await
import scala.concurrent.duration._

import org.specs2.mutable.Specification

class ImportCompletionSpec extends Specification with BaseTest
{

  def sourceCode(snippet:String):(Int,String) = {
      val source = s"""
      package test

      ${snippet}
      case class TestClass(key:String, value:String)
      object Test {
        def apply() = {
          val temp = TestClass("hello", "world")
        }
      }
      """
      (source.indexOf(TAB), source)
  }

  "Import completions" should {
    "Find import at scal" in {
      val (index,source) = sourceCode(s"import scal${TAB}")
      val results = completer.complete("ImportCompletionSpec.scala", source, index)
      results must have length 1 //scala package
    }
    "Find import at jav" in {
      val (index,source) = sourceCode(s"import ja${TAB}")
      val results = completer.complete("ImportCompletionSpec.scala", source, index)
      results must have length 3 //java,javax,javafx package
    }
  "Find import at java." in {
      val (index,source) = sourceCode(s"import java.${TAB}")
      val results = completer.complete("ImportCompletionSpec.scala", source, index)
      results must have length 14 //java.*
    }

    "Find import at java.n" in {
      val (index,source) = sourceCode(s"import java.n${TAB}")
      val results = completer.complete("ImportCompletionSpec.scala", source, index)
      results must have length 2 //java.*
    }


    "Find import at scala.con" in {
      val (index,source) = sourceCode(s"import scala.con${TAB}")
      val results = completer.complete("ImportCompletionSpec.scala", source, index)
      results must have length 1 //scala package
    }

    "Find import at scala.util.F" in {
      val (index,source) = sourceCode(s"import scala.util.F${TAB}")
      val results = completer.complete("ImportCompletionSpec.scala", source, index)

      results.length must be equalTo 2 //scala package
    }
    "Find import at scala.util." in {
      val (index,source) = sourceCode(s"import scala.util.${TAB}")
      val results = completer.complete("ImportCompletionSpec.scala", source, index)

      results.length must be equalTo 27 //scala package
    }

    "Find import at scala.util.{," in {
     val (index,source) = sourceCode(s"import scala.util.{${TAB}")
      val results = completer.complete("ImportCompletionSpec.scala", source, index)

      results.length must be equalTo 27 //scala package
    }

    //THis always causes FresRunReq?

   //  "Find import at scala.util.{T," in {
   //   val (index,source) = sourceCode(s"import scala.util.{T${TAB}")
   //    val results = completer.complete("ImportCompletionSpec.scala", source, index)

   //    results.length must be equalTo 2 //scala package
   // }
  }
}
