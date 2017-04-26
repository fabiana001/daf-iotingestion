package it.teamDigitale.kafkaProducers.main

import java.util.Properties
import java.util.concurrent.{Executors, ScheduledFuture, TimeUnit}

import com.typesafe.config.ConfigFactory
import it.teamDigitale.kafkaProducers.KafkaEventProducer
import it.teamDigitale.kafkaProducers.eventConverters._
import org.apache.kafka.clients.producer.ProducerConfig
import org.slf4j.LoggerFactory

import scala.reflect.{ClassTag, classTag}

/**
 * Created by fabiana on 30/03/17.
 */
object MainProducer {
  val logger = LoggerFactory.getLogger(this.getClass)

  var config = ConfigFactory.load()
  val serializer = config.getString("spark-dataIngestion-example.kafka.serializer")
  val brokers = config.getString("spark-dataIngestion-example.kafka.bootstrapServers")
  val topic = config.getString("spark-dataIngestion-example.kafka.topic")
  val zookeepers = config.getString("spark-dataIngestion-example.kafka.zookeeperServers")

  val props: Properties = new Properties()

  props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, brokers)
  props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, serializer)
  props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, serializer)
  //props.put("enable.auto.commit", "true")
  props.put("enable.zookeeper", "true")
  props.put("zookeeper.connect", zookeepers)
  //props.put("auto.offset.reset", "earliest")

  logger.info(s"Kafka Bootstrap Servers $brokers, topic $topic")

  val ex = Executors.newScheduledThreadPool(1)
  var lastGeneratedTimeMap: Option[Map[String, Long]] = None

  def main(args: Array[String]): Unit = {

    args match {

      case Array(producerType: String, period: String) =>
        val producer = Producer.withName(producerType)
        val kafkaEventProducer: KafkaEventProducer[_  <: EventConverter] = producer match {
          case Producer.FirenzeTraffic => new KafkaEventProducer[FirenzeTrafficConverter](props, topic)
          case Producer.TorinoParking => new KafkaEventProducer[TorinoParkingConverter](props, topic)
          case Producer.TorinoTraffic => new KafkaEventProducer[TorinoTrafficConverter](props, topic)
          case Producer.InfoBluEvent => new KafkaEventProducer[InfoBluEventConverter](props, topic)
          case Producer.InfoBluTraffic => new KafkaEventProducer[InfoBluTrafficConverter](props, topic)
        }

        val timeStamp = System.currentTimeMillis()
        val thread = run(period.toLong, kafkaEventProducer)
        logger.debug(s"Execution at $timeStamp isFinished: ${thread.isDone}")

      case _ =>
        Console.err.println(s"wrong parameters for: ${args.mkString(" ")}")

        val string = """to run the jar do: java -Dconfig.file=./application.conf -cp "./iotingestion-1.0/iotingestion_2.11-1.0.jar:./iotingestion-1.0/lib/*" it.teamDigitale.kafkaProducers.main.MainProducer <producerType> <period>
                       | where:
                       | <producerType> : should be TorinoTraffic, FirenzeTraffic or TorinoParking
                       | <period> : period in seconds between two successive executions
                     """

        Console.err.println(string)

    }

  }

  def run[T <: EventConverter](period: Long, kafkaEventClient: KafkaEventProducer[T]): ScheduledFuture[_] = {
    val task = new Runnable {
      def run(): Unit = {

        val times = lastGeneratedTimeMap match {
          case None =>
            kafkaEventClient.run()
          case Some(t) =>
            kafkaEventClient.run(t)
        }

        times.keys.foreach(url => logger.info(s"Data analyzed for the url $url at time ${times(url)}"))

        lastGeneratedTimeMap = Some(times)
      }
    }
    ex.scheduleAtFixedRate(task, 0, period, TimeUnit.SECONDS)
  }
}

object Producer extends Enumeration {
  type Producer = Value
  val TorinoTraffic, FirenzeTraffic, TorinoParking, InfoBluEvent, InfoBluTraffic = Value
}

