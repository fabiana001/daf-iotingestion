package it.teamDigitale.util

import it.teamDigitale.util.InfoBluDecoder.getClass
import org.scalatest.FunSuite

/**
  * Created by fabiana on 20/04/17.
  */
class InfoBluDecoderSpec extends FunSuite {

  test{"should generate two maps"}{
    InfoBluDecoder.run()
    val classLoader = getClass().getClassLoader()
    val fileSrc = classLoader.getResource("teamdigitale-Coordinate-Source.db").getPath
    val fileDst = classLoader.getResource("teamdigitale-Coordinate-End.db").getPath

    val map1 = InfoBluDecoder.getMap(fileSrc)
    assert(map1._1.get("1110") == "45.42805-9.24686")
    val map2 = InfoBluDecoder.getMap(fileDst)
    assert(map2._1.get("1112") == "45.40683-9.2613")
    map1._2.close()
    map2._2.close()

  }
}
