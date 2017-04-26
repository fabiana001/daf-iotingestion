package it.teamDigitale.kafkaProducers.eventConverters

import java.util.concurrent.TimeUnit

import com.twitter.bijection.Injection
import com.twitter.bijection.avro.SpecificAvroCodecs
import it.teamDigitale.avro.DataPoint
import org.scalatest.FunSuite

import scala.concurrent.Await
import scala.concurrent.duration.FiniteDuration
import scala.util.Success

/**
 * Created by fabiana on 30/03/17.
 */
class TorinoTrafficConverterSpec extends FunSuite {
  val torinoTrafficConverter = new TorinoTrafficConverter
  val data = torinoTrafficConverter.convert()
  test("Xml data from Torino sensor should be correctly converted into DataPoint") {
    assert(data._1.nonEmpty)
    assert(data._2.nonEmpty)

    val specificAvroBinaryInjection = SpecificAvroCodecs.toBinary[DataPoint]

    val dataPoints = data._2.get.map { x =>
      val Success(data) = specificAvroBinaryInjection.invert(x)
      data
    }

    val point = dataPoints.head
    assert(point.ts != -1)
    assert(point.values.nonEmpty)
    assert(point.host.length > 1)
    assert(point.service.contains(point.host))
    assert(point.body.nonEmpty)
    assert(point.body.get.length > 1)
    assert(point.location.length > 1)
    assert(point.tags.nonEmpty)

  }

  test { "Running two consecutive times convert method no update should be returned" } {
    val firstRun = torinoTrafficConverter.convert()
    val secondRun = torinoTrafficConverter.convert(firstRun._1)
    assert(secondRun._2.isEmpty)
    val map1 = firstRun._1
    val map2 = secondRun._1

    map1.keys.foreach(k => assert(map1.get(k) == map2.get(k)))
  }

}
