package us.ygrene.logging.poc.dao

import org.specs2.mutable.Specification
import reactivemongo.bson.BSONDocument
import us.ygrene.logging.poc.common.ServiceConfig
import us.ygrene.logging.poc.test_util._


class CriteriaHistoryDaoSpec extends Specification  with CriteriaHistoryDao with ServiceConfig {

  sequential
  stopOnFail

  import CriteriaHistoryDao._

  "CriteriaHistoryDao" should {
    "save CriteriaHistory to MongoDB" in {
      cleanup()
      success
    }

  }

  def cleanup(): Unit = {
    dao.removeAll().force
    (dao ensure CriteriaHistoryDao.requiredIndexes).force
    dao.find(BSONDocument()).force.size === 0
  }

}
