package us.ygrene.logging.poc.dao

import java.util.Date

import reactivemongo.bson.BSONDocument

case class CriteriaHistory(
                            projectId: String,
                            service: String,
                            name: String,
                            change_type: String,
                            decision_type: String,
                            old_decision: Option[String],
                            new_decision: Option[String],
                            old_data: Option[String],
                            new_data: Option[String],
                            user: String,
                            timestamp: Date,
                            definition: BSONDocument
                          )