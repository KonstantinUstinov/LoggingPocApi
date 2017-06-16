package us.ygrene.logging.poc.routes

import io.swagger.annotations._
import akka.http.scaladsl.server.Directives
import akka.http.javadsl.model.StatusCodes._
import us.ygrene.logging.poc.dao.{CriteriaHistory, CriteriaHistoryDao}
import javax.ws.rs.Path

import reactivemongo.bson.BSONDocument
import us.ygrene.logging.poc.common.LazyLogging
import us.ygrene.logging.poc.utils.BaseResponse

import scala.util.control.NonFatal


@Api(value = "/criteriahistory", description = "manage CriteriaHistory", produces = "application/json")
@Path("/criteriahistory")
trait CriteriaHistoryService extends CriteriaHistoryDao with Directives with LazyLogging {

  import us.ygrene.logging.poc.dao.CriteriaHistoryDao._

  def serviceRoute = pathPrefix("criteriahistory") {
    get{
      listOfCriteriaHistory
    } /* ~ post {
      addNewCriteriaHistory
    }*/
  }

  val searchParams = parameters(
    'search.as[BSONDocument],
    'sort.as[BSONDocument].?,
    'limit.as[Int].?
  )

  def listOfCriteriaHistory = searchParams {
    (search, projection, sort, limit) =>
      complete {
        dao.find(search, sort, limit) map {
          case ls if ls.nonEmpty => OK -> BaseResponse(OK.intValue(), None, Some(ls))
          case ls if ls.isEmpty => NOT_FOUND -> BaseResponse[CriteriaHistory](NOT_FOUND.intValue, Some("CriteriaHistory was not found"), None)
        } recover {
          case NonFatal(e) =>
            logger.error(e.getMessage, e.getCause)
            INTERNAL_SERVER_ERROR -> BaseResponse[CriteriaHistory](INTERNAL_SERVER_ERROR.intValue(), Some(e.getMessage), None)
        }
      }
  }

}
