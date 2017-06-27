package us.ygrene.logging.poc.routes

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.github.swagger.akka.model.Info
import com.github.swagger.akka.{HasActorSystem, SwaggerHttpService}
import us.ygrene.logging.poc.common.ServiceConfig

import scala.reflect.runtime.universe._


class SwaggerRoute(system: ActorSystem) extends SwaggerHttpService with HasActorSystem with ServiceConfig {
  override implicit val actorSystem: ActorSystem = system
  override implicit val materializer: ActorMaterializer = ActorMaterializer()
  override val apiTypes = Seq(typeOf[CriteriaHistoryService])
  override val host =  s"${config.getString("server.host")}:${config.getString("server.port")}" //the url of your api, not swagger's json endpoint
  override val basePath = "/"    //the basePath for the API you are exposing
  override val apiDocsPath = "api-docs" //where you want the swagger-json endpoint exposed
  override val info = Info("Desc", "1.0", "title", "terms") //provides license and other description details
}
