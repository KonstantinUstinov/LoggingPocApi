package us.ygrene.logging.poc.utils

import us.ygrene.logging.poc.common.ServiceConfig
import us.ygrene.logging.poc.common.StrictLogging
import com.typesafe.config.Config
import java.util.concurrent.Executors
import scala.collection.concurrent.TrieMap
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.api.commands.{GetLastError, WriteConcern}
import reactivemongo.api.{DefaultDB, MongoConnection, MongoConnectionOptions, ReadPreference}
import reactivemongo.core.nodeset.Authenticate


object MongoDriverContext extends ServiceConfig{

  import collection.JavaConverters._

  val channelsPerNode = config.getInt(s"mongo.channelsPerNode")
  val nodes = config.getStringList(s"mongo.nodes").asScala

  implicit lazy val mongoExecutionContext: ExecutionContext = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(channelsPerNode))
  lazy val driver = new reactivemongo.api.MongoDriver

  val connections = TrieMap.empty[Authenticate, MongoConnection]

  def connection(auth: Authenticate, wc: WriteConcern): MongoConnection = connections.getOrElseUpdate(auth,
    driver.connection(nodes = nodes, authentications = if(auth.password == "" && auth.user == "") Seq() else Seq(auth),
      options = MongoConnectionOptions(nbChannelsPerNode = channelsPerNode, writeConcern = wc)
    )
  )
}

case class MongoCollectionContext[A](config: Config,
                                     dbName: String,
                                     collName: String,
                                     ns: String = "mongo.db")(implicit executionContext: ExecutionContext) extends StrictLogging {

  val user: String = config.getString(s"$ns.$dbName.user")
  val password: String = config.getString(s"$ns.$dbName.password")
  val collectionName: String = config.getString(s"$ns.$dbName.$collName.name")

  val writeConcern: WriteConcern = toWriteConcern(
    w = config.getInt(s"$ns.$dbName.$collName.writeConcern.write"),
    j = config.getBoolean(s"$ns.$dbName.$collName.writeConcern.journal"),
    timeout = if(config.hasPath(s"$ns.$dbName.$collName.writeConcern.timeout")) Some(config.getInt(s"$ns.$dbName.$collName.writeConcern.timeout")) else None
  )

  def toWriteConcern(w: Int, j: Boolean, timeout: Option[Int]): GetLastError = {
    (w, j) match {
      case (0, false) => GetLastError.Unacknowledged
      case (1, false) => GetLastError.Acknowledged
      case (1, true) => GetLastError.Journaled
      case (n, _) if n > 1 => GetLastError.ReplicaAcknowledged(n, timeout.getOrElse(0), j)
      case _ =>
        logger.warn(s"Unknown write concern: write=$w, journal=$j, using 'Acknowledged'")
        GetLastError.Default
    }
  }

  val authTimeout = 10.seconds

  // primary | primaryPreferred | secondary | secondaryPreferred | nearest
  val readPreference: ReadPreference = config.getString(s"$ns.$dbName.$collName.readPreference.type") match {
    case "nearest" => ReadPreference.nearest
    case "primary" => ReadPreference.primary
    case "primaryPreferred" => ReadPreference.primaryPreferred
    case "secondary" => ReadPreference.secondary
    case "secondaryPreferred" => ReadPreference.secondaryPreferred
    case unknown =>
      logger.warn(s"Unknown read preference '$unknown' in $ns.$dbName.$collName.readPreference.type, using 'nearest'")
      ReadPreference.nearest
  }

  val auth = Authenticate(dbName, user, password)

  lazy val collection: Future[BSONCollection] =
    MongoDriverContext.connection(auth, writeConcern).database(dbName).map(_.apply(collectionName))
}
