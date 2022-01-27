package com.andersostby.webhook

import io.ktor.http.*
import io.ktor.routing.*
import io.ktor.server.testing.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class WebhookListenerTest {
    private val body =
        """{"package":{"package_version":{"package_url":"ghcr.io/andersostby/webhook/webhook:9921bc9"}}}"""
    private val tagBody =
        """{"ref": "deployer-525eda2","ref_type": "tag"}"""

    private var resultPayload: String? = null
    private var responseStatus: HttpStatusCode? = null

    @BeforeEach
    fun before() {
        resultPayload = null
        responseStatus = null
    }

    @Test
    fun `Listener blir kalt med nytt event når webhook trigges med gydlig signatur`() {
        val webhook = Webhook("Hemmelig")
        withTestApplication({
            routing {
                webhook.apply { webhook() }
            }
        }) {
            webhook.addListener { resultPayload = it }
            with(handleRequest {
                method = HttpMethod.Post
                uri = "/webhook"
                addHeader("X-GitHub-Event", "package")
                addHeader("X-Hub-Signature", "8c4a5cfccbde734da42d755fe46b8acdcc8e447c")
                setBody(body)
            }) {
                responseStatus = response.status()
            }

            assertEquals(HttpStatusCode.OK, responseStatus)
            assertEquals(
                """{"app":"webhook","version":"9921bc9","tag":"ghcr.io/andersostby/webhook/webhook:9921bc9"}""",
                resultPayload
            )
        }
    }

    @Test
    fun `Listener blir ikke kalt med nytt event når webhook trigges med ugydlig signatur - gir 401`() {
        val webhook = Webhook("Hemmelig")
        withTestApplication({
            routing {
                webhook.apply { webhook() }
            }
        }) {
            webhook.addListener { resultPayload = it }
            with(handleRequest {
                method = HttpMethod.Post
                uri = "/webhook"
                addHeader("X-GitHub-Event", "package")
                addHeader("X-Hub-Signature", "15fbdf5752e486f745c2dc5ee8c4774e86f8cc2e")
                setBody(body)
            }) {
                responseStatus = response.status()
            }

            assertEquals(HttpStatusCode.Unauthorized, responseStatus)
            assertNull(resultPayload)
        }
    }

    @Test
    fun `Listener blir ikke kalt med nytt event når webhook trigges med for kort signatur - gir 401`() {
        val webhook = Webhook("Hemmelig")
        withTestApplication({
            routing {
                webhook.apply { webhook() }
            }
        }) {
            webhook.addListener { resultPayload = it }
            with(handleRequest {
                method = HttpMethod.Post
                uri = "/webhook"
                addHeader("X-GitHub-Event", "package")
                addHeader("X-Hub-Signature", "15fbdf5752e486f745c2dc5ee8c4774e86f8cc2")
                setBody(body)
            }) {
                responseStatus = response.status()
            }

            assertEquals(HttpStatusCode.Unauthorized, responseStatus)
            assertNull(resultPayload)
        }
    }

    @Test
    fun `Listener blir ikke kalt med nytt event når webhook trigges uten signatur - gir 400`() {
        val webhook = Webhook("Hemmelig")
        withTestApplication({
            routing {
                webhook.apply { webhook() }
            }
        }) {
            webhook.addListener { resultPayload = it }
            with(handleRequest {
                method = HttpMethod.Post
                uri = "/webhook"
                addHeader("X-GitHub-Event", "package")
                setBody(body)
            }) {
                responseStatus = response.status()
            }

            assertEquals(HttpStatusCode.BadRequest, responseStatus)
            assertNull(resultPayload)
        }
    }

    @Test
    fun `Returnerer Accepted når webhook trigges med gydlig signatur og ukjent X-GitHub-Event`() {
        val webhook = Webhook("Hemmelig")
        withTestApplication({
            routing {
                webhook.apply { webhook() }
            }
        }) {
            webhook.addListener { resultPayload = it }
            with(handleRequest {
                method = HttpMethod.Post
                uri = "/webhook"
                addHeader("X-GitHub-Event", "registry_package")
                addHeader("X-Hub-Signature", "8c4a5cfccbde734da42d755fe46b8acdcc8e447c")
                setBody(body)
            }) {
                responseStatus = response.status()
            }

            assertEquals(HttpStatusCode.Accepted, responseStatus)
            assertNull(resultPayload)
        }
    }

    @Test
    fun `Listener blir kalt med nytt event når webhook trigges med tag og kjent app`() {
        val webhook = Webhook("Hemmelig")
        withTestApplication({
            routing {
                webhook.apply { webhook() }
            }
        }) {
            webhook.addListener { resultPayload = it }
            with(handleRequest {
                method = HttpMethod.Post
                uri = "/webhook"
                addHeader("X-GitHub-Event", "create")
                addHeader("X-Hub-Signature", "160180c665e438f9ef705de4abdf7ece2a8352fc")
                setBody(tagBody)
            }) {
                responseStatus = response.status()
            }

            assertEquals(HttpStatusCode.OK, responseStatus)
            assertEquals(
                """{"app":"deployer","version":"525eda2","tag":"ghcr.io/andersostby/deployer:525eda2"}""",
                resultPayload
            )
        }
    }
}
