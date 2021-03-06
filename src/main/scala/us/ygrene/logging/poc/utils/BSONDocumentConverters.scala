package us.ygrene.logging.poc.utils

import java.text.SimpleDateFormat

import akka.http.scaladsl.unmarshalling.Unmarshaller
import org.joda.time.DateTimeZone
import org.joda.time.format.ISODateTimeFormat
import reactivemongo.bson._
import spray.json._
import java.util.Date

object BSONDocumentConverters extends DefaultJsonProtocol {

  implicit val BsonDocumentFormat: RootJsonFormat[BSONDocument] = new RootJsonFormat[BSONDocument] {

    override def read(json: JsValue): BSONDocument = JsBSONWriter.writeObject(json.asJsObject)

    override def write(doc: BSONDocument): JsValue = JsBSONReader.readObject(doc)
  }

  object JsBSONReader {

    def readObject(doc: BSONDocument): JsValue = JsObject(doc.stream.map(it => readElement(it.get)): _*)

    def readElement(e: BSONElement): JsField = e._1 -> (e._2 match {
      case BSONString(value) => JsString(value)
      case BSONInteger(value) => JsNumber(value)
      case BSONLong(value) => JsNumber(value)
      case BSONDouble(value) => JsNumber(value)
      case BSONBoolean(true) => JsTrue
      case BSONBoolean(false) => JsFalse
      case BSONNull => JsNull
      case doc: BSONDocument => readObject(doc)
      case arr: BSONArray => readArray(arr)
      case oid@BSONObjectID(value) => JsObject("$oid" -> JsString(oid.stringify))
      case BSONDateTime(value) => JsString(BsonISODateTimeFormatter.dateTime.withZone(DateTimeZone.UTC).print(value))
      case BSONRegex(value, flags) => JsObject("$regex" -> JsString(value), "$options" -> JsString(flags))
      case BSONTimestamp(value) => JsObject("$timestamp" -> JsObject(
        "t" -> JsNumber(value.toInt), "i" -> JsNumber((value >>> 32).toInt)))
      case BSONUndefined => JsObject("$undefined" -> JsTrue)
    })

    def readArray(array: BSONArray): JsValue = JsArray(array.stream.map(it => readElement(("", it.get))._2): _*)
  }

  object JsBSONWriter {

    def writeObject(obj: JsObject): BSONDocument =
      BSONDocument(obj.fields.map(writePair).toSeq)

    def writeArray(arr: JsArray): BSONArray =
      BSONArray(arr.elements.zipWithIndex.map(p => writePair(p._2.toString -> p._1)).map(_._2))

    def writePair(p: (String, JsValue)): (String, BSONValue) =
      (p._1, p._2 match {
        case JsString(str) => BSONString(str)
        case JsNumber(num) =>
          if (num.isValidInt) BSONInteger(num.intValue())
          else if (num.isValidLong) BSONLong(num.longValue())
          else BSONDouble(num.doubleValue())
        case obj: JsObject =>
          val value = obj.fields.find(entry => specialValues(entry._1))
          if (value.isDefined) {
            writeSpecialValue(value.get)
          }
          else writeObject(obj)
        case arr: JsArray => writeArray(arr)
        case JsTrue => BSONBoolean(value = true)
        case JsFalse => BSONBoolean(value = false)
        case JsNull => BSONNull
      })

    lazy val specialValues = Set("$date", "$oid")

    def writeSpecialValue(p: (String, JsValue)): BSONValue = p match {
      case ("$date", JsString(str)) => BSONDateTime(ISODateTimeFormat.dateTime.parseDateTime(str).getMillis)
      case ("$date", JsNumber(num)) =>
        if (num.isValidLong) BSONDateTime(num.longValue())
        else throw new IllegalArgumentException(s"Wrong $$date value ${p._2}")
      case ("$oid", JsString(str)) => BSONObjectID(str)
      case (_, _) => throw new IllegalArgumentException(s"Wrong special value $p")
    }
  }

  implicit val string2BSONDocument = Unmarshaller.strict[String, BSONDocument] { value =>
    try {
      val js = value.parseJson
      js.convertTo[BSONDocument]
    } catch {
      case ex: Throwable => throw new  IllegalArgumentException( s"Not a valid json: '$value'. ${ex.getMessage}" )
    }
  }

  implicit val string2Date = Unmarshaller.strict[String, Date] { value =>
    try {
      val formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS")
      formatter.parse(value)
    } catch {
      case ex: Throwable => throw new  IllegalArgumentException( s"Not a valid DateTime: '$value'. ${ex.getMessage}" )
    }
  }

}


