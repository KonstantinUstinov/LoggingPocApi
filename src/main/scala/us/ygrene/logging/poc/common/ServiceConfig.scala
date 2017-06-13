package us.ygrene.logging.poc.common

import com.typesafe.config.{Config, ConfigFactory}

trait ServiceConfig {

  val config: Config = ConfigFactory.load()

}
