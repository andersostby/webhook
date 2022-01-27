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
            header("X-GitHub-Event", "create") {
                post { handleTag() }
            }
            post {
                log.info("Mottatt ukjent hook")
                call.response.status(HttpStatusCode.Accepted)
            }
        }
    }

    private fun PipelineContext<Unit, ApplicationCall>.handlePackage() {
        log.info("Mottatt package hook")
        val hook = objectMapper.readTree(call.attributes[bodyKey])

        val tag = hook.requiredAt("/package/package_version/package_url").textValue()
        val result = tag.takeLastWhile { it != '/' }.split(':')

        val app = result[0]
        val version = result[1]

        val json = objectMapper.writeValueAsString(
            mapOf(
                "app" to app,
                "version" to version,
                "tag" to tag
            )
        )

        log.info("Ny versjon: $json")

        listeners.forEach { it(json) }

        call.response.status(HttpStatusCode.OK)
    }

    private fun PipelineContext<Unit, ApplicationCall>.handleTag() {
        log.info("Mottatt create hook")
        val hook = objectMapper.readTree(call.attributes[bodyKey])
        if (hook.requiredAt("/ref_type").textValue() != "tag") {
            log.info("ref_type er ikke \"tag\"")
            return call.response.status(HttpStatusCode.Accepted)
        }

        val ref = hook.requiredAt("/ref").textValue()
        val app = ref.takeWhile { it != '-' }
        if(app !in arrayOf("deployer")){
            log.info("Ukjent app. Deployer ikke")
            return call.response.status(HttpStatusCode.Accepted)
        }
        val version = ref.takeLastWhile { it != '-' }
        val partialTag = "ghcr.io/andersostby"
        val tag = "$partialTag/$app:$version"

        val json = objectMapper.writeValueAsString(
            mapOf(
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
