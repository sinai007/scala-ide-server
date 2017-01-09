package org.ideserver


import scala.reflect.internal.util.{BatchSourceFile, OffsetPosition, Position}
import scala.reflect.runtime.universe._
import scala.tools.nsc.Settings
import scala.tools.nsc.interactive.{Global, Response}
import scala.tools.nsc.interpreter.AbstractFileClassLoader
import scala.tools.nsc.io.{AbstractFile, VirtualDirectory}

trait TaskResult[T] {
  val results:T
}

trait Task[T] {
  val compiler: Global
  def execute(): TaskResult[T]

  def demand(func: Response[T] => Unit): T = {
    val response = new Response[T]
    func(response)
    response.get.fold(x => x, e => throw e)
  }
}

case class LoadSource(source: BatchSourceFile, compiler: Global) extends Task[Unit] {
  override def execute():TaskResult[Unit] = new TaskResult[Unit] {
    override val results = demand {
      compiler.askReload(List(source), _)
    }
  }
}


