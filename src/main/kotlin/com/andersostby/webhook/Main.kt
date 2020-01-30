package com.andersostby.webhook

import com.andersostby.webhook.crypto.Hmac
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.application.ApplicationCallPipeline.ApplicationPhase.Features
import io.ktor.application.ApplicationCallPipeline.ApplicationPhase.Setup
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.header
import io.ktor.request.receiveText
import io.ktor.routing.Route
import io.ktor.routing.post
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.util.AttributeKey
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("MainKt")

fun main() {
    log.info("Starting webhook")

    embeddedServer(Netty) {
        routing {
            webhook()
        }
    }.start()
}

internal fun Route.webhook() {
    val signatureKey = AttributeKey<String>("signature")
    val bodyKey = AttributeKey<String>("body")
    intercept(Setup) {
        val signature = call.request.header("X-Hub-Signature") ?: run {
            call.response.status(HttpStatusCode.Unauthorized)
            finish()
            return@intercept
        }

        call.attributes.put(signatureKey, signature)
        call.attributes.put(bodyKey, call.receiveText())
    }
    intercept(Features) {
        val hmac = Hmac(
                secret = "Hemmelig",
                signature = call.attributes[signatureKey],
                payload = call.attributes[bodyKey]
        )
        try {
            hmac.verify()
        } catch (e: IllegalStateException) {
            call.response.status(HttpStatusCode.Unauthorized)
            finish()
            return@intercept
        }
    }
    post("/webhook") {
        log.info("Mottatt varsel")
        val hook = jacksonObjectMapper()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .readTree(call.attributes[bodyKey])

        val dockerTag = hook.requiredAt("/package/registry/url").textValue()
                .replace("^https?://(.+)".toRegex()) { it.groupValues[1] } + "/" +
                hook.requiredAt("/package/name").textValue() + ":" +
                hook.requiredAt("/package/package_version/version").textValue()

        log.info("Ny versjon: $dockerTag")

        call.response.status(HttpStatusCode.NoContent)
    }
}
