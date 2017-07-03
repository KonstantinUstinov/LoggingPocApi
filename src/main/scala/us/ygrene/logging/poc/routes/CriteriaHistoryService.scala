package us.ygrene.logging.poc.routes

import io.swagger.annotations._
import akka.http.scaladsl.server.Directives
import us.ygrene.logging.poc.dao.{CriteriaHistory, CriteriaHistoryDao}
import javax.ws.rs.Path
import spray.json.DefaultJsonProtocol
import us.ygrene.logging.poc.common.LazyLogging
import us.ygrene.logging.poc.utils.BaseResponse
import us.ygrene.logging.poc.utils.BSONDocumentConverters._
import scala.util.control.NonFatal
import akka.http.scaladsl.model.StatusCodes._
import reactivemongo.bson.BSONDocument
import scala.concurrent.ExecutionContext
import java.util.Date


@Api(value = "criteriahistory",  produces = "application/json")
@Path("/criteriahistory")
trait CriteriaHistoryService extends CriteriaHistoryDao with Directives with LazyLogging with DefaultJsonProtocol {

  import us.ygrene.logging.poc.dao.CriteriaHistoryDao._
  implicit val executor: ExecutionContext

  def serviceRoute = pathPrefix("criteriahistory") {
    get{
      listOfCriteriaHistory
    } ~ post {
      addNewCriteriaHistory
    }
  }

  @ApiOperation(httpMethod = "GET", value = "Returns list of CriteriaHistory", response = classOf[BaseResponse[CriteriaHistory]], produces = "application/json")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "search", required = true, dataType = "string", paramType = "query", value = "search json") ,
    new ApiImplicitParam(name = "sort", required = false, dataType = "string", paramType = "query", value = "sort json", defaultValue = "{}"),
    new ApiImplicitParam(name = "limit", required = false, dataType = "int", paramType = "query", value = "max number of documents, default = 100", defaultValue = "100")
  ))
  @ApiResponses(Array(new ApiResponse(code = 404, message = "The requested resource could not be found.")))
  def listOfCriteriaHistory = parameters('search.as[BSONDocument], 'sort.as[BSONDocument].?, 'limit.as[Int].?) {
    (search, sort, limit) =>
      complete {
        dao.find(search, sort.getOrElse(BSONDocument.empty), limit.getOrElse(100)) map {
          case ls if ls.nonEmpty => OK -> BaseResponse(OK.intValue, None, Some(ls))
          case ls if ls.isEmpty => NotFound -> BaseResponse[CriteriaHistory](NotFound.intValue, Some("CriteriaHistory was not found"), None)
        } recover {
          case NonFatal(e) =>
            logger.error(e.getMessage, e.getCause)
            InternalServerError -> BaseResponse[CriteriaHistory](InternalServerError.intValue, Some(e.getMessage), None)
        }
      }
  }

  @ApiOperation(httpMethod = "POST", value = "Save CriteriaHistory", produces = "application/json")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "projectId", required = true, dataType = "string", paramType = "form", value = "projectId"),
    new ApiImplicitParam(name = "service", required = true, dataType = "string", paramType = "form", value = "service"),
    new ApiImplicitParam(name = "name", required = true, dataType = "string", paramType = "form", value = "name"),
    new ApiImplicitParam(name = "change_type", required = true, dataType = "string", paramType = "form", value = "change_type"),
    new ApiImplicitParam(name = "decision_type", required = true, dataType = "string", paramType = "form", value = "decision_type"),
    new ApiImplicitParam(name = "user", required = true, dataType = "string", paramType = "form", value = "user"),
    new ApiImplicitParam(name = "timestamp", required = true, dataType = "string", paramType = "form", value = "timestamp 2013-07-29 06:35:40:622", defaultValue = "2013-07-29 06:35:40:622"),
    new ApiImplicitParam(name = "definition", required = true, dataType = "string", paramType = "form", value = "definition"),
    new ApiImplicitParam(name = "old_decision", required = false, dataType = "string", paramType = "form", value = "old_decision", defaultValue = ""),
    new ApiImplicitParam(name = "new_decision", required = false, dataType = "string", paramType = "form", value = "new_decision", defaultValue = ""),
    new ApiImplicitParam(name = "old_data", required = false, dataType = "string", paramType = "form", value = "old_data", defaultValue = ""),
    new ApiImplicitParam(name = "new_data", required = false, dataType = "string", paramType = "form", value = "new_data", defaultValue = "")
  ))
  @ApiResponses(Array(new ApiResponse(code = 404, message = "The requested resource could not be found.")))
  def addNewCriteriaHistory = formFields('projectId, 'service, 'name, 'change_type, 'decision_type, 'user, 'timestamp.as[Date], 'definition.as[BSONDocument], 'old_decision.?, 'new_decision.?, 'old_data.?, 'new_data.?) {
    (projectId, service, name, change_type, decision_type, user, timestamp, definition, old_decision, new_decision, old_data, new_data) =>
      complete {
        dao.save(CriteriaHistory(projectId,
                                 service,
                                 name,
                                 change_type,
                                 decision_type,
                                 old_decision,
                                 new_decision,
                                 old_data,
                                 new_data,
                                 user,
                                 timestamp,
                                 definition)) map {
          case le if le.ok => logger.debug(s"Successfully inserted CriteriaHistory projectId = $projectId")
            OK -> BaseResponse[CriteriaHistory](OK.intValue, None, None)
        } recover {
          case NonFatal(e) =>
            logger.error(e.getMessage, e.getCause)
            InternalServerError -> BaseResponse[CriteriaHistory](InternalServerError.intValue, Some(e.getMessage), None)
        }
      }
  }

}
