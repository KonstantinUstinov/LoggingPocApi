package us.ygrene.logging.poc.routes

import java.util.Date

import org.specs2.mutable.Specification
import akka.http.scaladsl.testkit.Specs2RouteTest
import reactivemongo.bson.BSONDocument
import us.ygrene.logging.poc.common.ServiceConfig
import us.ygrene.logging.poc.dao.{CriteriaHistory, CriteriaHistoryDao}
import us.ygrene.logging.poc.test_util._
import akka.http.scaladsl.model.StatusCodes._

import scala.concurrent.ExecutionContextExecutor
import scala.util.parsing.json.JSON
import java.net.URLEncoder

import akka.http.scaladsl.model.FormData

class CriteriaHistoryServiceSpec extends Specification with Specs2RouteTest with CriteriaHistoryService with ServiceConfig  {

  sequential
  stopOnFail

  override implicit val executor: ExecutionContextExecutor = system.dispatcher

  "CriteriaHistoryService" should {

    "return list of Json" in {
      cleanup()
      addSomeDataToDB()

      Get("?search={}") ~> listOfCriteriaHistory ~> check {
        handled must beTrue
        status === OK
        parseResp()("results")
          .asInstanceOf[List[Map[String, String]]]
          .map(_("user")).toSet === Set("user", "user2")
      }

      val search = URLEncoder.encode("""{"user":"user2"}""", "UTF-8")

      Get(s"?search=$search") ~> listOfCriteriaHistory ~> check {
        handled must beTrue
        status === OK
        val result = parseResp()("results").asInstanceOf[List[Map[String, String]]]
        result.length === 1
        result.head.get("user") === Some("user2")
        result.head.get("definition") === Some(Map("Doc" -> "Doc", "Doc2" -> "Doc2"))
      }
    }

    "support limit and sort" in {
      Get(s"?search={}&limit=1") ~> listOfCriteriaHistory ~> check {
        handled must beTrue
        status === OK
        val result = parseResp()("results").asInstanceOf[List[Map[String, String]]]
        result.length === 1
      }

      val sort = URLEncoder.encode("""{"user":1}""", "UTF-8")
      Get(s"?search={}&sort=$sort") ~> listOfCriteriaHistory ~> check {
        handled must beTrue
        status === OK
        val result = parseResp()("results").asInstanceOf[List[Map[String, String]]]
        result.length === 2
      }
    }

    "save data" in {
      val search = URLEncoder.encode("""{"user":"user3"}""", "UTF-8")

      Post("/", FormData("projectId" -> "blue", "service" -> "68", "name" -> "name", "change_type" -> "type", "decision_type" -> "type", "user" -> "user3", "timestamp" -> "2013-07-29 06:35:40:622", "definition" -> """{"user":"user3"}""")) ~> addNewCriteriaHistory ~> check {
        handled must beTrue
        status === OK
      }

      Get(s"?search=$search") ~> listOfCriteriaHistory ~> check {
        handled must beTrue
        status === OK
        val result = parseResp()("results").asInstanceOf[List[Map[String, String]]]
        result.length === 1
        result.head.get("user") === Some("user3")
      }
    }
  }

  def addSomeDataToDB() = {
    val ch = CriteriaHistory("projectId",
      "service",
      "name",
      "change_type",
      "decision_type",
      Some("old_decision"),
      Some("new_decision"),
      Some("old_data"),
      Some("new_data"),
      "user",
      new Date,
      BSONDocument("Doc" -> "Doc"))

    dao.save(ch).force
    dao.save(ch.copy(definition = BSONDocument("Doc" -> "Doc", "Doc2" -> "Doc2"), user = "user2"))

  }


  def parseResp() = JSON.parseFull(responseAs[String]) match {
    case Some(v) => v.asInstanceOf[Map[String, Any]]
    case None => throw new IllegalArgumentException("Error in parse response")
  }

  def cleanup(): Unit = {
    dao.removeAll().force
    (dao ensure CriteriaHistoryDao.requiredIndexes).force
    dao.find(BSONDocument()).force.size === 0
  }
}
