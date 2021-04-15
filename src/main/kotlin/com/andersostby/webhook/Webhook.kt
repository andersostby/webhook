package com.andersostby.webhook

import com.andersostby.webhook.crypto.Hmac
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.routing.*
import io.ktor.util.*
import io.ktor.util.pipeline.*
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("Webhook")

internal typealias WebhookListener = (message: String) -> Unit

internal class Webhook(private val secret: String) {
    companion object {
        private val objectMapper = jacksonObjectMapper()
        private val signatureKey = AttributeKey<String>("signature")
        private val bodyKey = AttributeKey<String>("body")
    }

    private val listeners = mutableListOf<WebhookListener>()
    internal fun addListener(listener: WebhookListener) {
        listeners.add(listener)
    }

    internal fun Route.webhook() {
        route("/webhook") {
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
            header("X-GitHub-Event", "package") {
                post { handlePackage() }
            }
        }
        post {
            log.info("Mottatt ukjent hook")
            call.response.status(HttpStatusCode.Accepted)
        }
    }

    private fun PipelineContext<Unit, ApplicationCall>.handlePackage(): Unit {
        log.info("Mottatt package hook")
        val hook = objectMapper.readTree(call.attributes[bodyKey])

        val registry = hook.requiredAt("/package/registry/url").textValue()
        val app = hook.requiredAt("/package/name").textValue()
        val version = hook.requiredAt("/package/package_version/version").textValue()
        val partialTag = registry.replace("^https?://(.+)".toRegex()) { it.groupValues[1] }
        val tag = "$partialTag/$app:$version"

        val json = objectMapper.writeValueAsString(
            mapOf(
                "registry" to registry,
                "app" to app,
                "version" to version,
                "tag" to tag
            )
        )

        log.info("Ny versjon: $json")

        listeners.forEach { it(json) }

        call.response.status(HttpStatusCode.OK)
    }
}
