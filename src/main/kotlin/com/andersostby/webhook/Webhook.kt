package com.andersostby.webhook

import com.andersostby.webhook.crypto.Hmac
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.header
import io.ktor.request.receiveText
import io.ktor.routing.Route
import io.ktor.routing.post
import io.ktor.routing.route
import io.ktor.util.AttributeKey
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("Webhook")

internal typealias WebhookListener = (String) -> Unit

internal class Webhook {
    private val listeners = mutableListOf<WebhookListener>()

    internal fun addListener(listener: WebhookListener) {
        listeners.add(listener)
    }

    internal fun Route.webhook() {
        route("/webhook") {
            val signatureKey = AttributeKey<String>("signature")
            val bodyKey = AttributeKey<String>("body")
            intercept(ApplicationCallPipeline.Setup) {
                val signature = call.request.header("X-Hub-Signature") ?: run {
                    call.response.status(HttpStatusCode.BadRequest)
                    finish()
                    return@intercept
                }

                call.attributes.put(signatureKey, signature)
                call.attributes.put(bodyKey, call.receiveText())
            }
            intercept(ApplicationCallPipeline.Features) {
                try {
                    Hmac(
                            secret = "Hemmelig",
                            signature = call.attributes[signatureKey],
                            payload = call.attributes[bodyKey]
                    ).verify()
                } catch (e: IllegalArgumentException) {
                    call.response.status(HttpStatusCode.Unauthorized)
                    finish()
                    return@intercept
                } catch (e: IllegalStateException) {
                    call.response.status(HttpStatusCode.Unauthorized)
                    finish()
                    return@intercept
                }
            }
            post {
                log.info("Mottatt varsel")
                val hook = jacksonObjectMapper()
                        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                        .readTree(call.attributes[bodyKey])

                val dockerTag = hook.requiredAt("/package/registry/url").textValue()
                        .replace("^https?://(.+)".toRegex()) { it.groupValues[1] } + "/" +
                        hook.requiredAt("/package/name").textValue() + ":" +
                        hook.requiredAt("/package/package_version/version").textValue()

                log.info("Ny versjon: $dockerTag")

                listeners.forEach { it(dockerTag) }

                call.response.status(HttpStatusCode.OK)
            }
        }
    }
}
