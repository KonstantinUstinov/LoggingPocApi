package us.ygrene.logging.poc.dao

import org.specs2.mutable.Specification
import reactivemongo.bson.BSONDocument
import us.ygrene.logging.poc.common.ServiceConfig
import us.ygrene.logging.poc.test_util._
import java.util.Date


class CriteriaHistoryDaoSpec extends Specification  with CriteriaHistoryDao with ServiceConfig {

  sequential
  stopOnFail

  import CriteriaHistoryDao._

  "CriteriaHistoryDao" should {

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

    "save CriteriaHistory to MongoDB" in {
      cleanup()
      dao.save(ch).force.ok === true
      dao.find(BSONDocument()).force.size === 1
    }

    "find by projectId" in {
      dao.find(BSONDocument("projectId" -> "projectId")).force.head === ch
    }

  }

  def cleanup(): Unit = {
    dao.removeAll().force
    (dao ensure CriteriaHistoryDao.requiredIndexes).force
    dao.find(BSONDocument()).force.size === 0
  }

}
