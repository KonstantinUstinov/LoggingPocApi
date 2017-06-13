package us.ygrene.logging.poc.utils


case class BaseResponse[A](code: Int, message: Option[String], results: Option[List[A]])
