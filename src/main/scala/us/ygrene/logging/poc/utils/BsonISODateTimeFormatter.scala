package us.ygrene.logging.poc.utils

import org.joda.time.format.{ISODateTimeFormat, DateTimeFormatterBuilder}

object BsonISODateTimeFormatter {

  val dateTime = new DateTimeFormatterBuilder().
    append(ISODateTimeFormat.date()).
    appendLiteral('T').
    append(ISODateTimeFormat.hourMinuteSecondFraction()).
    appendTimeZoneOffset(null, true, 2, 2).
    toFormatter
}