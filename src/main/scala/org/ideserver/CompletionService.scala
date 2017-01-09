package org.ideserver

import java.io._

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.{Duration, _}
import scala.reflect.internal.util.{BatchSourceFile, OffsetPosition, Position}
import scala.reflect.runtime.universe._
import scala.tools.nsc.Settings
import scala.tools.nsc.interactive.{Global, Response}
import scala.tools.nsc.interpreter.AbstractFileClassLoader
import scala.tools.nsc.io.{AbstractFile, VirtualDirectory}
import scala.tools.nsc.reporters._
import scala.util.{Failure, Success, Try}

trait CompletionService {
  def parse(filename:String, source:String): List[ParseNote]
  def typeAt(filename:String, source:String, position: Int): List[Completion]
  def addImport(filename: String, source: String, position: Int): List[Completion]
  def complete(filename: String, source: String, position: Int): List[Completion]
}

trait CompletionServiceImpl extends CompletionService {
  val compiler: Global
  val reporter: StoreReporter

  private def label(severity: reporter.Severity): String = severity match {
    case reporter.ERROR   => "error"
    case reporter.WARNING => "warning"
    case reporter.INFO    => "info"
    case _       => "unknown"
  }

  def getFile(filename:String, source:String): BatchSourceFile = {
    new BatchSourceFile(filename, source)
  }
  def parse(filename:String, source:String): List[ParseNote] = {

    val compileSource = getFile(filename,source)

    reporter.reset()
    LoadSource(compileSource, compiler).execute()
    getTypeTree(compileSource, new OffsetPosition(compileSource, 0))
    reporter.infos.toList.map { info =>
        println(s"Found ${info}")
        ParseNote(msg = info.msg, start = info.pos.start, end = info.pos.end, severity = label(info.severity) )
    }
  }

  def waitForResponse[T: Manifest](func: Response[T] => Unit): T = {
    val response = new Response[T]
    func(response)
    response.get.fold(x => x, e => {
      println("Failure waiting for a response, retrying...")
      val response2 = new Response[T]
      func(response2)
      response2.get.fold(x => x, e => throw e)
    })
  }

  /** Refreshes the source for the compiler */
  def refreshSource(source: BatchSourceFile): Unit = waitForResponse[Unit] {
    compiler.askReload(List(source), _)
  }

  /**
   * Returns the type tree at the given position in the file.  This is used
   * to determine the completion type needed, eg Method, import, scope, etc.
   */
  def getTypeTree(source: BatchSourceFile, position: OffsetPosition): compiler.Tree = waitForResponse[compiler.Tree] {
    compiler.askTypeAt(position, _)
  }

  def getTypeCompletion(position: Position): List[compiler.Member] = waitForResponse[List[compiler.Member]] {
    compiler.askTypeCompletion(position, _)
  }

  def getScopeCompletion(position: Position): List[compiler.Member] = waitForResponse[List[compiler.Member]] {
    compiler.askScopeCompletion(position, _)
  }

  def getCompletionResults(members: List[compiler.Member], prefix: String): List[Completion] = compiler.ask { () =>
    members
      .filter(_.accessible)
      .map{x => x.sym}
      .filter(!_.isConstructor)
      .filter(_.name.decoded.startsWith(prefix))
      .map{ x => getCompletionForMember(x) }
  }

  def cleanup(source: BatchSourceFile) = waitForResponse[Unit] {
    compiler.askFilesDeleted(List(source), _)
  }

  // /** Returns a list of method completions at the give o
  def getMethodCompletions(position: OffsetPosition, qualifier: compiler.Tree, name: String): List[Completion] = {
    //set the prefix to empty if it is an <error> tree
    val prefix = if (name == "<error>") "" else name

    println(Position.formatMessage(position, s"Completing Method '${prefix}':", true))

    val response = getTypeCompletion(position)
    getCompletionResults(response, prefix)
  }

  def getScopeCompletions(position: OffsetPosition, prefix: String): List[Completion] = {
    println(Position.formatMessage(position, s"Completing Scope '${prefix}':", true))
    val response = getScopeCompletion(position)
    getCompletionResults(response, prefix)
  }

  def getCompletionForMember(symbol: compiler.Symbol): Completion = {
    Completion(
      name = symbol.name.decoded,
      definition = symbol.defString,
      owner = symbol.owner.fullName,
      fullName = symbol.fullName,
      typeParams = symbol.typeParams.map(_.name.decoded),
      params = symbol.paramLists.headOption.toList.flatMap { l => l.map(_.name.decoded) }
    )
  }

  def extractImport(t: compiler.Import, compileSource: BatchSourceFile) = {
    // println(s"Import: ${showRaw(t)}")
    val dotPosition = new OffsetPosition(compileSource, t.expr.pos.end + 1)
    t match {
      // import ja<pos>
      case compiler.Import(qualifier @ compiler.Ident(qname), _) if qualifier.tpe.toString == "<error>" => ScopeCompletion(dotPosition, qname.decoded)
      // import <pos>
      case compiler.Import(qualifier @ compiler.Ident(qname), _) if qname.decoded == "<error>"          => ScopeCompletion(dotPosition, "")
      // import java.
      case compiler.Import(qualifier, List(compiler.ImportSelector(name, pos, mappedName, mappedPos))) if name.decoded == "<error>" =>
        MethodCompletion(dotPosition, qualifier, name.decoded)
      case compiler.Import(qualifier, List(compiler.ImportSelector(name, pos, mappedName, mappedPos))) =>
        MethodCompletion(dotPosition, qualifier, name.decoded)
      case _ =>
        UnknownCompletion()
    }

  }

  def findSymbolChild(sym: compiler.Symbol, name: String): Set[compiler.Symbol] = {
    if(sym.hasPackageFlag || sym.isPackageObject) {
        sym.typeSignature.members.filter(_ != sym).filter(_.isPublic).flatMap(findSymbolChild(_, name)).toSet
    } else if ( sym.name.decoded == name) {
        Set(sym)
    } else {
      Set.empty[compiler.Symbol]
    }
  }

  def getAddImports(pos: OffsetPosition, name: String): List[Completion] = compiler.ask{() =>
    val topSymbol = compiler.RootClass

    val allSymbols = findSymbolChild(topSymbol, name)

    val matches = allSymbols
      // .filter{ x=> !x.name.decoded.contains("$")}
      .filter{ x => x.moduleClass == compiler.NoSymbol}
      .filter{ x => x.name.decoded.endsWith(name)}
      .map(getCompletionForMember(_))

    matches.toList
  }

  def addImport(filename: String, source: String, position: Int): List[Completion] = {
    //ensure that only one completion is in progress to prevent compiler corrupted state.

    val compileSource = getFile(filename,source)
      val completeAt = new OffsetPosition(compileSource, position)

      LoadSource(compileSource, compiler).execute()

      val tree = getTypeTree(compileSource, completeAt)
      println(s"Import: ${showRaw(tree)}")
      val completion = compiler.ask{ () =>
        tree match {
          case t @ compiler.Ident(name) => AddImportCompletion(completeAt, name.decoded)
          case _ => UnknownCompletion()
        }
      }

      val result = completion match {
        case AddImportCompletion(position, name) => getAddImports(position, name)
        case UnknownCompletion() =>
          println("Unknown Completion")
          List.empty[Completion]
      }
      result

  }

  def typeAt(filename:String, source:String, position: Int): List[Completion] = {
     val compileSource = getFile(filename,source)
      val completeAt = new OffsetPosition(compileSource, position)

      LoadSource(compileSource, compiler).execute()

      val tree = getTypeTree(compileSource, completeAt)

      compiler.ask{ () => List(getCompletionForMember(tree.symbol))}
  }

  def complete(filename: String, source: String, position: Int): List[Completion] = {
    //ensure that only one completion is in progress to prevent compiler corrupted state.
    Try {
      val compileSource = getFile(filename,source)
      val completeAt = new OffsetPosition(compileSource, position)

      LoadSource(compileSource, compiler).execute()

      val tree = getTypeTree(compileSource, completeAt)

      //figure out what is trying to be completed...
      val completion = compiler.ask { () =>
        tree match {
          case t @ compiler.Select(qualifier, name) => {
            val dotPosition = new OffsetPosition(compileSource, qualifier.pos.end + 1)
            MethodCompletion(dotPosition, qualifier, name.decoded)
          }
          case t: compiler.Import =>
            extractImport(t, compileSource)
          case t @ compiler.Ident(name) =>
            ScopeCompletion(completeAt, name.decoded)
          case t @ compiler.Block(_, _) =>
            ScopeCompletion(completeAt, "")
          case t =>
            println(s"Unknown type ${showRaw(t)}")
            UnknownCompletion()
        }
      }
      val result = completion match {
        case MethodCompletion(position, qualifier, name) => getMethodCompletions(position, qualifier, name)
        case ScopeCompletion(position, name)             => getScopeCompletions(position, name)
        case UnknownCompletion() =>
          println("Unknown Completion")
          List.empty[Completion]
      }
      //discard the file
      cleanup(compileSource)
      result
    } match {
      case Failure(ex) =>
        ex.printStackTrace()
        // ex.getCause().printStackTrace()
        List.empty[Completion]
      case Success(s) =>
        s
    }
  }

  case class MethodCompletion(position: OffsetPosition, qualifier: compiler.Tree, name: String)
  case class ScopeCompletion(position: OffsetPosition, name: String)
  case class AddImportCompletion(position: OffsetPosition, name: String)
  case class UnknownCompletion()
}

object CompletionService {

  var fileCounter = new java.util.concurrent.atomic.AtomicInteger(0)

  def fileIndex: Int = fileCounter.incrementAndGet()

  val services: scala.collection.mutable.Map[String, CompletionService] = scala.collection.mutable.Map.empty

  def getService(classpaths: Seq[String]): CompletionService = {
    val key = classpaths.mkString(File.pathSeparator)
    val service: CompletionService = services.get(key).getOrElse {
      val s: CompletionService = new DefaultCompletionService(classpaths)
      services += (key -> s)
      s
    }
    service
  }

  def apply(classpaths: Seq[String]): CompletionService = {
    CompletionService.getService(classpaths)
  }
}
