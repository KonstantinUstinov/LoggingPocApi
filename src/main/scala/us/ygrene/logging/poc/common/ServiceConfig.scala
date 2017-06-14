package us.ygrene.logging.poc.common

import com.typesafe.config.{Config, ConfigFactory}

trait ServiceConfig {

  lazy val config: Config = ConfigFactory.load()

}
