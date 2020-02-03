package com.andersostby.webhook

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.routing.Route
import io.ktor.routing.options

internal fun Route.healthCheck() {
    options("/isAlive") {
        call.response.status(HttpStatusCode.OK)
    }
}
