package com.andersostby.webhook

import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.gson.gson
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.request.receiveText
import io.ktor.routing.post
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.util.toMap
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("MainKt")

fun main() {
    log.info("Starting webhook")

    embeddedServer(Netty) {
        install(ContentNegotiation) {
            gson {}
        }
        routing {
            post("/webhook") {
                log.info("Mottatt varsel")

                log.info("Headers:\n${call.request.headers.toMap().entries.joinToString("\n")}")
                //log.info("Ny versjon:\n${call.receive<Hook>()}")
                log.info("Ny versjon:\n${call.receiveText()}")
                call.response.status(HttpStatusCode.NoContent)
            }
        }
    }.start()
}

data class Hook (
        val `package`: Package
){
    override fun toString() =
            "${`package`.registry.url}/${`package`.name}:${`package`.package_version.version}"
}

data class Package(
        val name: String,
        val package_version: PackageVersion,
        val registry: Registry
)

data class PackageVersion(
        val version: String
)

data class Registry(
        val url: String
)
