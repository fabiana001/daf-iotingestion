package it.teamDigitale.kafkaProducers.eventConverters

import java.net.{HttpURLConnection, URL}
import java.text.SimpleDateFormat
import java.util.zip.GZIPInputStream

import com.typesafe.config.ConfigFactory
import it.teamDigitale.avro.{AvroConverter, DataPoint}
import it.teamDigitale.util.InfoBluDecoder

import scala.util.Try
import scala.xml.{NodeSeq, XML}

/**
  * Created by fabiana on 20/04/17.
  */
class InfoBluTrafficConverter extends EventConverter{
  import InfoBluTrafficConverter._

  val (srcMap, dstMap) = InfoBluDecoder.run()

  override def convert(time: Map[String, Long] = Map()): (Map[String, Long], Option[Seq[Array[Byte]]]) = {
    val xml = XML.load(url)
    val traffic_data: NodeSeq = xml \\ "TRAFFIC"
    val generationTime = (traffic_data \\ "TIMESTAMP").text

    val ts = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").parse(generationTime).getTime
    val oldTime = time.getOrElse(url, -1L)

    if (oldTime < ts) {
      val data = traffic_data \\ "SP"
      val dp = for {
        d <- data
      } yield convertDataPoint(d, ts)

      val avro = dp.map(x => AvroConverter.convertDataPoint(x))
      (Map(url -> ts), Some(avro))
    } else {
      (time, None)
    }


  }

  private def convertDataPoint(n: NodeSeq, generationTimestamp: Long): DataPoint = {
    val source =  (n \ sourceCode).text
    val end = (n \ endCode).text
    val offs = (n \ offset).text
    val speed = (n \ velocity).text

    val tags = Map("sourceCode" -> source,
      "endCode" -> end,
      "offset" -> offs,
      "srcCoordinates" -> srcMap.getOrDefault(source, "-1"),
      "dstCoordinates" -> dstMap.getOrDefault(end, "-1")
    )
    val values = Map("speed" -> speed.toDouble)
    val latLon = srcMap.getOrDefault(source, "")

    val point = new DataPoint(
      version = 0L,
      id = Some(measure),
      ts = generationTimestamp,
      event_type_id = measure.hashCode,
      location = latLon,
      host = url,
      service = url,
      body = Some(n.toString().getBytes()),
      tags = tags,
      values = values
    )
    point
  }

}

object InfoBluTrafficConverter {
  val url = ConfigFactory.load().getString("spark-dataIngestion-example.urls.infobluTraffic")
  val sourceCode = "@S"
  val endCode = "@E"
  val offset = "@P"
  val velocity = "@V"

  val measure = "opendata.infoblu.speed"
}