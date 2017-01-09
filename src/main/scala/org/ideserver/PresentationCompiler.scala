package org.ideserver

import java.io._
import java.lang.reflect.Member

import scala.collection.mutable
import scala.concurrent.Promise
import scala.io.Source
import scala.reflect.internal.util.{BatchSourceFile, OffsetPosition, Position}
import scala.tools.nsc.Settings
import scala.tools.nsc.interactive.{Global, Response}
import scala.tools.nsc.interpreter.AbstractFileClassLoader
import scala.tools.nsc.io.{AbstractFile, VirtualDirectory}
import scala.tools.nsc.reporters.{AbstractReporter, ConsoleReporter, Reporter}
import scala.util.{Failure, Success}
import scala.util.matching.Regex

class PresentationCompiler(classpaths: Seq[File]) {

  val target = new VirtualDirectory("", None)

  val settings = new Settings()
  settings.outputDirs.setSingleOutput(target)
  settings.classpath.value = classpaths.mkString(File.pathSeparator)

  val reporter = new ConsoleReporter(settings)
  val compiler = new Global(settings, reporter)

  def getCompletions(code: String, p: Int, prefix: String): List[Completion] = {
    val start = System.currentTimeMillis()

    val source = new BatchSourceFile("<virtual>", code)
    val response = new Response[Unit]

    compiler.askReload(List(source), response)
    response.get

    val tcompletion = new Response[List[compiler.Member]]
    val pos = new OffsetPosition(source, p)

    compiler.askTypeCompletion(pos, tcompletion)
    tcompletion.get

    val result = compiler.ask{ () =>
      tcompletion.get match {
         case Left(members) =>
            members
           .filter(_.accessible)
           .filter(!_.sym.isConstructor)
            .filter(_.sym.name.decoded.startsWith(prefix))
           .flatMap { x =>
            x.sym.paramLists.map { pl =>
              Completion(
                name = x.sym.name.decoded,
                definition = x.sym.defString,
                owner = x.sym.owner.fullName,
                fullName = x.sym.fullName,
                typeParams = x.sym.typeParams.map(_.name.decoded),
                params = pl.map(_.name.decoded)
              )
            }

           }
         case Right(e) => {
          println("Ah snap");
           e.printStackTrace
           List.empty
         }
       }
    }

    //compiler.askShutdown()
    val end = System.currentTimeMillis()
    println(s"Result in : %${end-start}")
    result
  }

  // def getCompletions(code: String, p: Int, prefix: String): List[Completion] = {
  //   val start = System.currentTimeMillis()

  //   val source = new BatchSourceFile("<virtual>", code)
  //   val response = new Response[Unit]

  //   compiler.askReload(List(source), response)
  //   response.get

  //   val tcompletion = new Response[List[compiler.Member]]
  //   val pos = new OffsetPosition(source, p)

  //   compiler.askScopeCompletion(pos, tcompletion)
  //   tcompletion.get

  //   val result = compiler.ask{ () =>
  //     tcompletion.get match {
  //        case Left(members) =>
  //           members
  //          .filter(_.accessible)
  //          .filter(!_.sym.isConstructor)
  //           .filter(_.sym.name.decoded.startsWith(prefix))
  //          .flatMap { x =>
  //           x.sym.paramLists.map { pl =>
  //             Completion(
  //               name = x.sym.name.decoded,
  //               definition = x.sym.defString,
  //               owner = x.sym.owner.fullName,
  //               fullName = x.sym.fullName,
  //               typeParams = x.sym.typeParams.map(_.name.decoded),
  //               params = pl.map(_.name.decoded)
  //             )
  //           }

  //          }
  //        case Right(e) => {
  //         println("Ah snap");
  //          e.printStackTrace
  //          List.empty
  //        }
  //      }
  //   }

  //   //compiler.askShutdown()
  //   val end = System.currentTimeMillis()
  //   println(s"Result in : %${end-start}")
  //   result
  // }


  // def showType(code: String, p: Int): String = {
  //   val source = new BatchSourceFile("<virtual>", code)
  //   val response = new Response[Unit]
  //   compiler.askReload(List(source), response)
  //   response.get
  //  // Thread.sleep(1000)

  //  val r = new Response[compiler.Tree]
  //  val pos = new OffsetPosition(source, p)

  // //  println(s"'${code.substring(0, p)}'")
  //  compiler.askTypeAt(pos, r)

  //  val tree = r.get.fold(x => x, e => throw e)

  // //  // println(s"Tree: ${tree.getClass}")
  //  tree match {
  //     // case t @ compiler.Select(qualifier, name) =>s"${qualifier}, ${name}"
  //     // case t @ compiler.Ident(name) => s"Name: ${name}"
  //     case t @ compiler.ValDef(mods, name, tpt, rhs) => s"${name.decoded}:${tpt}"
  //     case _ => "Unknown"
  //  }
  // }

}
