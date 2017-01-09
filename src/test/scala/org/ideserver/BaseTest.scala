package org.ideserver

import scala.concurrent.duration._

trait BaseTest {

  val waitDuration = 2 seconds
  val classpaths = scala.io.Source.fromFile("./target/.main-classpath").getLines().toSeq

  val TAB = "/*!*/"

  def completer = new DefaultCompletionService(classpaths)

}
