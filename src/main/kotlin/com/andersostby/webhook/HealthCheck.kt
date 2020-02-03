package com.andersostby.webhook

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.routing.Route
import io.ktor.routing.options
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("HealthCheck")

internal fun Route.healthCheck() {
    options("/isAlive") {
        log.info("Health check performed")
        call.response.status(HttpStatusCode.OK)
    }
}
