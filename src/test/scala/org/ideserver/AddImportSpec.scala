package org.ideserver

import scala.concurrent.Await
import scala.concurrent.duration._

import org.specs2.mutable.Specification

class AddImportCompletionSpec extends Specification with BaseTest
{

  def sourceCode(snippet:String):(Int,String) = {
      val source = s"""

      case class TestClass(key:String, value:String)
      object Test {
        def apply() = {
          val temp = TestClass("hello", "world")
          val d = new Date(0)
        }
      }
      """
      (173, source)
  }

  "Add Import completions" should {
    "Find import at Dat" in {
      val (index,source) = sourceCode("")
      val results = completer.addImport("AddImportCompletionSpec.scala", source, index)
      results must have length 2 //scala package
    }
}
}
