package us.ygrene.logging.poc.dao

import reactivemongo.api.indexes.{Index, IndexType}
import us.ygrene.logging.poc.common.{LazyLogging, ServiceConfig}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import reactivemongo.bson.Macros
import spray.json._
import us.ygrene.logging.poc.utils._

object CriteriaHistoryDao extends ServiceConfig with LazyLogging with SprayJsonSupport with DefaultJsonProtocol {
  val ProjectId = "projectId"
  val Service = "service"
  val Timestamp = "timestamp"
  val User = "user"

  val requiredIndexes = Seq(
    Index(key = Seq(ProjectId -> IndexType.Ascending), unique = false),
    Index(key = Seq(Service -> IndexType.Ascending), unique = false),
    Index(key = Seq(Timestamp -> IndexType.Ascending), unique = false),
    Index(key = Seq(User -> IndexType.Ascending), unique = false)
  )

  implicit val criteriaHistoryFormat: RootJsonFormat[CriteriaHistory] = new RootJsonFormat[CriteriaHistory] {
    override def write(m: CriteriaHistory) : JsValue =
      JsObject(Map(
        ProjectId -> JsString(m.projectId),
        Service -> JsString(m.service),
        "name" -> JsString(m.name),
        "change_type" -> JsString(m.change_type),
        "old_decision" -> m.old_decision.map(JsString(_)).getOrElse(JsNull),
        "new_decision" -> m.new_decision.map(JsString(_)).getOrElse(JsNull),
        "old_data" -> m.old_data.map(JsString(_)).getOrElse(JsNull),
        "new_data" -> m.new_data.map(JsString(_)).getOrElse(JsNull),
        User -> JsString(m.user),
        Timestamp -> JsString(BsonISODateTimeFormatter.dateTime.withZoneUTC().print(m.timestamp.getTime)),
        "definition" -> JsBSONReader.readObject(m.definition)
      ))

    override def read(json: JsValue): CriteriaHistory = ???
  }

  implicit val criteriaHistoryRes = jsonFormat3(BaseResponse[CriteriaHistory])
  implicit val criteriaHandler = Macros.handler[CriteriaHistory]

  import us.ygrene.logging.poc.utils.MongoDriverContext.mongoExecutionContext

  implicit val criteriaHistoryContext =
    MongoCollectionContext[CriteriaHistory](config = config, dbName = "criteriaHistorydb", collName = "criteriaHistory")

  class CriteriaHistoryDao extends GenericDAO[CriteriaHistory] {
    check(requiredIndexes)

  }

  lazy val criteriaHistoryDao = new CriteriaHistoryDao
}

trait CriteriaHistoryDao {
  def dao = CriteriaHistoryDao.criteriaHistoryDao
}
