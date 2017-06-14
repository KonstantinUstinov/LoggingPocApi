package us.ygrene.logging.poc

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.{Duration, _}


package object test_util {

  implicit val AwaitTimeout = 5.seconds

  implicit class futureToResult[T](f: Future[T]) {
    def force(implicit atMost: Duration): T = Await.result(f, atMost)
  }
}
