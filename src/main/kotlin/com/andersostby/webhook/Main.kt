package com.andersostby.webhook

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.routing.post
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("MainKt")

fun main() {
    log.info("Starting webhook")

    embeddedServer(Netty) {
        routing {
            post("/webhook") {
                log.info("Mottatt varsel")
                log.info("Payload:\n${call.receive<String>()}")
                call.response.status(HttpStatusCode.NoContent)
            }
        }
    }.start()
}
