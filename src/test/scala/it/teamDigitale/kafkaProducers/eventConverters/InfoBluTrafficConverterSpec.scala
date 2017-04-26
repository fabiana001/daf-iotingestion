package it.teamDigitale.kafkaProducers.eventConverters

import com.twitter.bijection.Injection
import com.twitter.bijection.avro.SpecificAvroCodecs
import it.teamDigitale.avro.DataPoint
import org.scalatest.FunSuite

import scala.util.Success

/**
  * Created by fabiana on 20/04/17.
  */
class InfoBluTrafficConverterSpec extends FunSuite {
  val specificAvroBinaryInjection: Injection[DataPoint, Array[Byte]] = SpecificAvroCodecs.toBinary[DataPoint]
  val infoBlu = new InfoBluTrafficConverter()

  test("the first execution should extract some data") {

    val data = infoBlu.convert()._2.getOrElse(Seq()).map { x =>
      specificAvroBinaryInjection.invert(x)
    }
    val Success(head) = data.head
    assert(data.nonEmpty)

    assert(head.ts != -1)
    assert(head.values.nonEmpty)
    assert(head.host.length > 1)
    assert(head.body.nonEmpty)
    val stringBody = new String(head.body.get, "UTF-8")

    assert(stringBody.length > 1)
    assert(head.location.split("-").size == 2)
    assert(head.tags.size == 5)
    println(head)
  }

  test {
    "Running two consecutive times convert method no update should be returned"
  } {
    val firstRun = infoBlu.convert()
    val secondRun = infoBlu.convert(firstRun._1)
    assert(secondRun._2.isEmpty)
    val map1 = firstRun._1
    val map2 = secondRun._1

    assert(secondRun._2.isEmpty)
    assert(firstRun._2.nonEmpty)
    map1.keys.foreach(k => assert(map1.get(k) == map2.get(k)))

  }
}
