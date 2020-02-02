package com.andersostby.webhook

import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("MainKt")

fun main() {
    log.info("Starting webhook")

    val webhook = Webhook()

    webhook.addListener(testListener)

    embeddedServer(Netty) {
        routing {
            webhook.apply { webhook() }
        }
    }.start()
}

private val testListener: WebhookListener
    get() = { log.info("Mottatt ny event: $it") }
