package us.ygrene.logging.poc.utils

import us.ygrene.logging.poc.common.LazyLogging
import reactivemongo.api.indexes.Index
import reactivemongo.api.commands.{UpdateWriteResult, WriteResult}
import reactivemongo.bson._
import scala.concurrent._
import scala.util.Try
import scala.concurrent.duration._
import scala.util.control.NonFatal

class GenericDAO[T: BSONDocumentReader : BSONDocumentWriter](implicit ctx: MongoCollectionContext[T]) extends LazyLogging {

  /**
    * Insert a new document.
    */
  def save(doc: T): Future[WriteResult] = {
    ctx.collection.flatMap(_.insert(doc))
  }

  /**
    * Insert/Update document.
    */
  def upsert(query: BSONDocument, doc: T, upsert: Boolean = true): Future[UpdateWriteResult] = {
    ctx.collection.flatMap(_.update(query, doc, upsert = upsert))
  }

  def update(query: BSONDocument, updates: BSONDocument, upsert: Boolean = true): Future[UpdateWriteResult] = {
    ctx.collection.flatMap(_.update(query, updates, upsert = upsert))
  }

  /**
    * Delete a document.
    */
  def remove(query: BSONDocument): Future[WriteResult] = {
    ctx.collection.flatMap(_.remove(query))
  }

  def remove(doc: T): Future[WriteResult] = {
    ctx.collection.flatMap(_.remove(doc))
  }

  def removeAll(): Future[WriteResult] = {
    ctx.collection.flatMap(_.remove(BSONDocument()))
  }

  /**
    * Find a document by its _id field.
    */
  def findById(id: BSONObjectID): Future[Option[T]] = {
    val query = BSONDocument("_id" -> id)
    ctx.collection.flatMap(_.find(query).one[T](readPreference = ctx.readPreference))
  }

  /**
    * Find documents matching a query provided as BSONDocument.
    */
  def find(query: BSONDocument, sort: BSONDocument = BSONDocument(), limit: Int = Int.MaxValue): Future[List[T]] = {
    ctx.collection.flatMap(_.find(query).sort(sort).cursor[T](readPreference = ctx.readPreference).collect[List](limit))
  }

  /**
    * Find document matching a query provided as tuples.  The resulting query will and the tuples together.
    */
  def filter(filters: (String, String)*)(sort: BSONDocument = BSONDocument(), limit: Int = Int.MaxValue): Future[List[T]] = {
    val query = filters.map { t => (t._1, BSONString(t._2)) }
    find(BSONDocument(query), sort, limit)
  }

  def headOption(query: BSONDocument, sort: BSONDocument = BSONDocument()): Future[Option[T]] = {
    ctx.collection.flatMap(_.find(query).sort(sort).one[T](readPreference = ctx.readPreference))
  }

  def bulkInsert(docs: Stream[BSONDocument], ordered: Boolean = false): Future[Int] = {
    ctx.collection.flatMap(_.bulkInsert(docs, ordered) map { result => result.n })
  }

  def count(query: BSONDocument): Future[Int] = {
    ctx.collection.flatMap(_.count(Some(query)))
  }

  case class MongoIndexData(keys: Seq[(String, BSONValue)],
                            unique: Boolean = false, dropDups: Boolean = false, sparse: Boolean = false) {
    override def toString = {
      val keysStr = keys.map { case (k, v) => s""""$k": $v""" }.mkString(", ")

      s"""{$keysStr}, {"unique" : $unique, "dropDups" : $dropDups, "sparse" : $sparse } """
    }
  }

  def check(required: Seq[Index]): Boolean = (Try {

    def simplify(ind: Index): MongoIndexData = MongoIndexData(ind.key.map { case (k, v) => k -> v.value },
      unique = ind.unique, dropDups = ind.dropDups, sparse = ind.sparse)

    val indexes = Await.result({
      ctx.collection.flatMap(_.indexesManager.list())
    }, 10.seconds).map(simplify)

    required.map(simplify).filterNot(indexes.contains) match {
      case Nil => true
      case indexesList: Seq[MongoIndexData] =>
        logger.warn(s"required indexes not found in [${ctx.dbName}/${ctx.collectionName}]: " + indexesList.mkString(", "))
        false
    }
  } recover {
    case NonFatal(ex) =>
      logger.error("Error while listing indexes", ex)
      false
  }).getOrElse(false)

  def ensure(required: Seq[Index]): Future[Seq[Boolean]] = {
    Future.sequence(required.map(index => ctx.collection.flatMap(_.indexesManager.ensure(index))))
  }
}
