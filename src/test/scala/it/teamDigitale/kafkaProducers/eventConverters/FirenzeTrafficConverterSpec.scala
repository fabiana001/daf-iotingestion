package it.teamDigitale.kafkaProducers.eventConverters

import java.util.concurrent.TimeUnit
import com.twitter.bijection.avro.SpecificAvroCodecs
import it.teamDigitale.avro.{ AvroConverter, DataPoint }
import it.teamDigitale.kafkaProducers.eventConverters.FirenzeTrafficConverter.{ FirenzeSensor, SensorFormat }
import org.scalatest.FunSuite
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ Await, Future }

/**
 * Created by fabiana on 28/03/17.
 */
class FirenzeTrafficConverterSpec extends FunSuite {

  val fakeSensor = FirenzeSensor(url = "http://thisISaFakeUrl.it", name = "Fake sensor", lat = -1, lon = -1)
  val urls = FirenzeTrafficConverter.urls :+ fakeSensor

  val firenzeConverter = new FirenzeTrafficConverter
  val data: (Map[String, Long], Option[Seq[Array[Byte]]]) = firenzeConverter.convert()

  val specificAvroBinaryInjection = SpecificAvroCodecs.toBinary[DataPoint]
  val dataPoint = data._2.getOrElse(Seq()).map { x =>
    specificAvroBinaryInjection.invert(x)
  }

  test("Http requests which fail should not block the whole application") {
    val data = Await.result(FirenzeTrafficConverter.collectSuccessHttpRequest(urls), FiniteDuration(60, TimeUnit.SECONDS))
    assert(data.size == 9)
  }

  test("Json data from sensors should be correctly converted into DataPoint") {

    assert(data._1.nonEmpty)
    assert(data._2.nonEmpty)
    val failedDP = dataPoint.filter(x => x.isFailure)

    assert(failedDP.isEmpty)

  }

  test { "All datapoint fields should be populated" } {
    val point = dataPoint.head.get
    assert(point.ts != -1)
    assert(point.values.nonEmpty)
    assert(point.host.length > 1)
    assert(point.service.contains(point.host))
    assert(point.body.nonEmpty)
    assert(point.body.get.length > 1)
    assert(point.location.length > 1)
    assert(point.tags.nonEmpty)

    //println(point)

  }

  test { "Running two consecutive times convert method no update should be returned" } {
    val firstRun = firenzeConverter.convert()
    val secondRun = firenzeConverter.convert(firstRun._1)
    assert(secondRun._2.isEmpty)
    val map1 = firstRun._1
    val map2 = secondRun._1

    map1.keys.foreach(k => assert(map1.get(k) == map2.get(k)))

  }
}