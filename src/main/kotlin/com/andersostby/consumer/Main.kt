package com.andersostby.consumer

import com.rabbitmq.client.CancelCallback
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.DeliverCallback
import com.rabbitmq.client.Delivery
import org.slf4j.LoggerFactory
import java.util.zip.GZIPInputStream
import java.util.zip.InflaterInputStream

private val log = LoggerFactory.getLogger("com.andersostby.consumer.MainKt")

fun main(vararg args: String) {
    log.info("Consumer starting")
    ConnectionFactory().apply {
        username = "test"
        password = "test"
        host = "192.168.1.134"
        port = args.getOrNull(0)?.toInt() ?: port
    }.newConnection().also { connection ->
        connection.createChannel().also { channel ->
            //            val queueName = "hello-world"
            val queueName = channel.queueDeclare().queue
//            channel.exchangeDeclare("broadcast", "fanout")
            channel.queueBind(queueName, "rapid", "")
            channel.basicQos(10)

            log.info(" [*] Waiting for messages")

            val deliverCallback = DeliverCallback { _, delivery ->
                val message = measureNanoTime { delivery.parse() }
                log.info(" [x] Received '$message'")

//                log.info("Method: ${delivery.properties.contentEncoding}, Body size: ${delivery.properties.bodySize}, Msg size: ${message.length}, Ratio: ${delivery.properties.bodySize.toDouble() / message.length}")

                try {
                    doWork(message)
                } finally {
//                    log.info(" [x] Done '$message'")
                    channel.basicAck(delivery.envelope.deliveryTag, false)
                }
            }
            channel.basicConsume(queueName, false, deliverCallback, CancelCallback {})
        }
    }
}

inline fun <T> measureNanoTime(block: () -> T): T {
    val start = System.nanoTime()
    return try {
        block()
    } finally {
//        println("Parsetime: ${(System.nanoTime() - start)/1000_000.0} ms")
    }
}

fun doWork(message: String) {
    //        message.forEach { if (it == '.') delay(100) }
}

fun Delivery.parse(): String =
        when (properties.contentEncoding) {
            "gzip" -> body.ungzip()
            "deflate" -> body.inflate()
            else -> String(body)
        }

private fun ByteArray.ungzip(): String =
        GZIPInputStream(inputStream()).bufferedReader().use { it.readText() }

private fun ByteArray.inflate(): String =
        InflaterInputStream(inputStream()).bufferedReader().use { it.readText() }
