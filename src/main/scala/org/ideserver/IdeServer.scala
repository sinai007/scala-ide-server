package org.ideserver

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}

import akka.actor.{Actor, ActorSystem, Props}
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import spray.can.Http
import spray.http.MediaTypes._
import spray.http._
import spray.httpx.SprayJsonSupport
import spray.json.{DefaultJsonProtocol, JsonFormat}
import spray.routing._

case class CompletionRequest(
  filename: String,
  source: String,
  position: Int,
  classpath: List[String]
)
case class Completion(
  name: String,
  fullName: String,
  definition: String,
  owner: String,
  typeParams: List[String],
  params: List[String]
)
case class CompletionResponse(
  completions: List[Completion]
)
case class ParseNote(
  start: Int,
  end: Int,
  severity: String,
  msg: String
)

object CompletionJsonSupport extends DefaultJsonProtocol with SprayJsonSupport {
   implicit val CompletionFormats = jsonFormat6(Completion)
   implicit val ParseFormats = jsonFormat4(ParseNote)
   implicit val CompletionRequestFormats = jsonFormat4(CompletionRequest)
   implicit val CompletionResponseFormats = jsonFormat1(CompletionResponse)
}

object IdeServer extends App with SimpleRoutingApp {

  import CompletionJsonSupport._

  val serverPort:Int = sys.props.get("ideServerPort").map(_.toInt).getOrElse(8080)

  implicit val system = ActorSystem("ide-server")

  startServer(interface="localhost", port=serverPort) {
    path("parse") {
      post {
        entity(as[CompletionRequest]) { request =>
          val service = CompletionService(request.classpath)
          val result = Future {
            service.parse(request.filename, request.source)
          }
          onComplete(result) {
              case Success(value) => {
                // println(s"Result: ${value}")
                complete(value)
              }
              case Failure(ex) => {
                ex.printStackTrace()
                complete(ex.getMessage())
              }
            // complete("Hello")
          }
        }
      }
    } ~ path("type") {
      post {
        entity(as[CompletionRequest]) { request =>
          val service = CompletionService(request.classpath)
          val result = Future {
            service.typeAt(request.filename, request.source, request.position)
          }
          onComplete(result) {
              case Success(value) => {
                // println(s"Result: ${value}")
                complete(CompletionResponse(value))
              }
              case Failure(ex) => {
                ex.printStackTrace()
                complete(ex.getMessage())
              }
            // complete("Hello")
          }
        }
      }
    } ~path("imports") {
      post {
        entity(as[CompletionRequest]) { request =>
          val service = CompletionService(request.classpath)
          val result = Future {
            service.addImport(request.filename, request.source, request.position)
          }
          onComplete(result) {
              case Success(value) => {
                // println(s"Result: ${value}")
                complete(CompletionResponse(value))
              }
              case Failure(ex) => {
                ex.printStackTrace()
                complete(ex.getMessage())
              }
            // complete("Hello")
          }
        }
      }
    } ~
    path("completions") {
      post {
        entity(as[CompletionRequest]) { request =>
          val service = CompletionService(request.classpath)
          val result = Future {
            service.complete(request.filename, request.source, request.position)
          }
          onComplete(result) {
              case Success(value) => {
                // println(s"Result: ${value}")
                complete(CompletionResponse(value))
              }
              case Failure(ex) => {
                ex.printStackTrace()
                complete(ex.getMessage())
              }
            // complete("Hello")
          }
        }
      } ~
      get {
        complete("GET: lost")
      }
    }
  }
}
