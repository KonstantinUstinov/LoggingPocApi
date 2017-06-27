package us.ygrene.logging.poc

import us.ygrene.logging.poc.common.{LazyLogging, ServiceConfig}
import akka.Done
import akka.actor.ActorSystem
import akka.http.scaladsl.server.HttpApp
import akka.pattern
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.settings.ServerSettings
import us.ygrene.logging.poc.routes.{CriteriaHistoryService, SwaggerRoute}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}
import scala.language.postfixOps
import scala.util.Try

// Server definition
class WebServer(system: ActorSystem) extends HttpApp with CriteriaHistoryService {
  override def routes: Route = serviceRoute ~ new SwaggerRoute(system).routes

  override implicit val executor: ExecutionContextExecutor = system.dispatcher

  override def postServerShutdown(attempt: Try[Done], system: ActorSystem): Unit = {

  }
}

object Boot extends App with ServiceConfig with LazyLogging{

  val host = config.getString("server.host")
  val port = config.getInt("server.port")

  val system = ActorSystem("auth-server", config)

  logger.info(s"HTTPS service have started. See Rest API docs at: http://$host:$port")

  new WebServer(system).startServer(host, port, ServerSettings(config), system)
}
