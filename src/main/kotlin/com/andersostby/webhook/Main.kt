package com.andersostby.webhook

import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("MainKt")

fun main() {
    log.info("Starting webhook")

    val webhook = Webhook()
    val rabbitMQProducer = RabbitMQProducer(
            username = "test",
            password = "test",
            host = "192.168.1.40"
    )

    webhook.addListener(testListener)
    webhook.addListener(rabbitMQProducer::send)

    val server = embeddedServer(Netty) {
        routing {
            healthCheck()
            webhook.apply { webhook() }
        }
    }.start()

    Runtime.getRuntime().addShutdownHook(Thread {
        server.stop(10, 10)
        rabbitMQProducer.close()
    })
}

private val testListener: WebhookListener
    get() = { log.info("Mottatt ny event: $it") }
