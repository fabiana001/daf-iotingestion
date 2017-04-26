package it.teamDigitale.util

import java.io.File
import java.lang.Double
import java.util.concurrent.ConcurrentMap

import org.mapdb.{DB, DBMaker, HTreeMap, Serializer}

/**
  * Created by fabiana on 20/04/17.
  */
object InfoBluDecoder {

  val classLoader = getClass().getClassLoader()
  val file = classLoader.getResource("teamdigitale-Coordinate.csv").getPath
  val srcFile = file.replace(".csv", "-Source.db")
  val dstFile = file.replace(".csv", "-End.db")
  val (mapSource, dbSource) = getMap(srcFile)
  val (mapEnd, dbEnd) = getMap(dstFile)

  def getMap(file: String) : (ConcurrentMap[String, String], DB)= {

    val f = new File(file)
    if(f.exists())
      f.delete()

    val db: DB = DBMaker
      .fileDB(file)
      .fileMmapEnable()
      .make()

    val map: ConcurrentMap[String, String] = db
      .hashMap("map", Serializer.STRING, Serializer.STRING)
      .createOrOpen()
    (map, db)
  }


  def closeAll(): Unit = {
    dbSource.close()
    dbEnd.close()
  }

  def run(): (ConcurrentMap[String, String], ConcurrentMap[String, String]) = {

    val lines = scala.io.Source.fromFile(file).getLines().drop(3)
    lines.foreach{l =>
      val tokens = l.split(";")
      val sourceKey = tokens(1)
      val sourceLat= tokens(2).replace(",", ".").toDouble
      val sourceLon = tokens(3).replace(",", ".").toDouble
      val sourceLatLon = s"$sourceLat-$sourceLon"

      val endKey = tokens(4)
      val endLat = tokens(5).replace(",", ".").toDouble
      val endLon = tokens(6).replace(",", ".").toDouble
      val endLatLon = s"$endLat-$endLon"
      mapSource.put(sourceKey, sourceLatLon)
      mapEnd.put(endKey, endLatLon)
    }

    //dbSource.close()
    //dbEnd.close()
    println(s"DBs generated into \n \t $srcFile \n \t $dstFile")
    (mapSource, mapEnd)
  }

  def main(args: Array[String]): Unit = {
   val (a,b) = run()
  }
}
