package com.andersostby.webhook

import com.andersostby.webhook.crypto.Hmac
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

internal typealias WebhookListener = (message: String) -> Unit

internal class Webhook(private val secret: String) {
    private val listeners = mutableListOf<WebhookListener>()
    private val objectMapper = jacksonObjectMapper()

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
                            secret = secret,
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
                val hook = objectMapper.readTree(call.attributes[bodyKey])

                val registry = hook.requiredAt("/package/registry/url").textValue()
                val app = hook.requiredAt("/package/name").textValue()
                val version = hook.requiredAt("/package/package_version/version").textValue()
                val partialTag = registry.replace("^https?://(.+)".toRegex()) { it.groupValues[1] }
                val tag = "$partialTag/$app:$version"

                val json = objectMapper.writeValueAsString(mapOf(
                        "registry" to registry,
                        "app" to app,
                        "version" to version,
                        "tag" to tag
                ))

                log.info("Ny versjon: $json")

                listeners.forEach { it(json) }

                call.response.status(HttpStatusCode.OK)
            }
        }
    }
}
