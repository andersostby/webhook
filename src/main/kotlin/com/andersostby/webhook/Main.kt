package com.andersostby.webhook

import com.rabbitmq.client.ConnectionFactory
import org.slf4j.LoggerFactory
import java.io.ByteArrayOutputStream
import java.util.zip.DeflaterOutputStream
import java.util.zip.GZIPOutputStream
import kotlin.system.exitProcess

private val log = LoggerFactory.getLogger("MainKt")

fun main(vararg args: String) {
    log.info("Starting webhook")
    exitProcess(0)
    //    if(true){
//        log.info(UUID.randomUUID().toString())
//        return@runBlocking
//    }
    ConnectionFactory().apply {
        username = "test"
        password = "test"
        host = "192.168.1.134"
        port = args.getOrNull(0)?.toInt() ?: port
    }.newConnection().use { connection ->
        connection.createChannel().use { channel ->
            //            channel.exchangeDeclare("broadcast", "fanout")

            val queueName = "hello-world"
            val message = if (args.getOrNull(0) == null) {
//                "Hello from Boat!"
                """{
  "id": "ca9e3449-4287-4245-a0f0-9db93473464b",
  "type": "power",
  "volt": 12.34,
  "current": 21.54
}"""
            } else {
//                "Hello from House!"
                """{"shuntName":"FORBRUK","maxCurrent":534.0,"ampsPerVolt":6676,"shuntVoltage":-0.43,"ampHour":-0.78,"busVoltage":13.35,"ampere":-2927.24,"updated":"2019-06-02T01:11:11.004"}"""
            }

            val otherMessage = """{"shuntName":"START","maxCurrent":534.0,"ampsPerVolt":6676,"shuntVoltage":-0.12,"ampHour":-0.12,"busVoltage":11.1,"ampere":-34.2,"updated":"2019-06-02T01:11:11.004"}"""
//            channel.queueDeclare(queueName, false, false, false, null)

            repeat(10000) { big ->
                //                repeat(2) {small->
                val newMessage = message// + "$big-$small" + (1..big % 3 + 1).joinToString(separator = "") { "." }
                val deflated = newMessage.deflate()
                val gzipped = newMessage.gzip()
//                channel.basicPublish("rapid", "", null, "før ${now()}".toByteArray())
//                channel.basicPublish("rapid", "", null, "122: $newMessage".substring(0..122).toByteArray())
//                channel.basicPublish("rapid", "", null, "123: $newMessage".substring(0..123).toByteArray())
//                channel.basicPublish("rapid", "", null, "124: $newMessage".substring(0..124).toByteArray())
//                channel.basicPublish("rapid", "", null, "2040: $newMessage $newMessage $newMessage $newMessage $newMessage $newMessage $newMessage $newMessage $newMessage $newMessage $newMessage $newMessage $newMessage $newMessage $newMessage $newMessage $newMessage $newMessage $newMessage $newMessage".substring(0..2040).toByteArray())
//                channel.basicPublish("rapid", "", null, "2041: $newMessage $newMessage $newMessage $newMessage $newMessage $newMessage $newMessage $newMessage $newMessage $newMessage $newMessage $newMessage $newMessage $newMessage $newMessage $newMessage $newMessage $newMessage $newMessage $newMessage".substring(0..2041).toByteArray())
//                channel.basicPublish("rapid", "", null, "2042: $newMessage $newMessage $newMessage $newMessage $newMessage $newMessage $newMessage $newMessage $newMessage $newMessage $newMessage $newMessage $newMessage $newMessage $newMessage $newMessage $newMessage $newMessage $newMessage $newMessage".substring(0..2042).toByteArray())
//                channel.basicPublish("rapid", "", null, "2043: $newMessage $newMessage $newMessage $newMessage $newMessage $newMessage $newMessage $newMessage $newMessage $newMessage $newMessage $newMessage $newMessage $newMessage $newMessage $newMessage $newMessage $newMessage $newMessage $newMessage".substring(0..2043).toByteArray())
//                channel.basicPublish("rapid", "", null, "2044: $newMessage $newMessage $newMessage $newMessage $newMessage $newMessage $newMessage $newMessage $newMessage $newMessage $newMessage $newMessage $newMessage $newMessage $newMessage $newMessage $newMessage $newMessage $newMessage $newMessage".substring(0..2044).toByteArray())
//                channel.basicPublish("rapid", "", null, "etter ${now()}".toByteArray())
//                channel.basicPublish("rapid", "", null, newMessage.toByteArray())
                channel.basicPublish("rapid", "", null, "hi".toByteArray())
//                channel.basicPublish("rapid", "", AMQP.BasicProperties().builder()
//                        .contentType("application/json")
//                        .contentEncoding("deflate")
//                        .build(), deflated)
//                channel.basicPublish("rapid", "", AMQP.BasicProperties()
//                        .builder()
//                        .contentType("application/json")
//                        .contentEncoding("gzip")
//                        .build(), gzipped)
//                log.info(" [x] Sent '$newMessage'")
//                }
                Thread.sleep(1000)
            }
        }
    }

    Unit
}

private fun String.gzip(): ByteArray =
        ByteArrayOutputStream().use { baos ->
            GZIPOutputStream(baos).bufferedWriter().use { it.write(this) }
            baos.toByteArray()
        }

private fun String.deflate(): ByteArray =
        ByteArrayOutputStream().use { baos ->
            DeflaterOutputStream(baos).bufferedWriter().use { it.write(this) }
            baos.toByteArray()
        }
