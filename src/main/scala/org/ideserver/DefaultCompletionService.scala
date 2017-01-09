package org.ideserver

import java.io.File

import scala.reflect.internal.util.{BatchSourceFile, OffsetPosition, Position}
import scala.reflect.io.VirtualDirectory
import scala.tools.nsc.Settings
import scala.tools.nsc.interactive.Global
import scala.tools.nsc.reporters.StoreReporter

class DefaultCompletionService(
  classpaths: Seq[String]
) extends CompletionServiceImpl {

  val target = new VirtualDirectory("", None)

  val settings = new Settings()
  settings.outputDirs.setSingleOutput(target)
  settings.classpath.value = classpaths.mkString(File.pathSeparator)

  // val reporter = new ConsoleReporter(settings)
  val reporter = new StoreReporter() {
    override def info0(pos: Position, msg: String, severity: Severity, force: Boolean): Unit = {
      super.info0(pos, msg, severity, force)
      println(s"Messages: ${msg}, ${severity}, ${pos}, ${force}")
    }
  }
  override val compiler = new Global(settings, reporter) {
    // override val verboseIDE = true
    // override val debugIDE = true
  }
}
