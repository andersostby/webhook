package com.andersostby.webhook

import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.routing.routing
import io.ktor.server.testing.setBody
import io.ktor.server.testing.withTestApplication
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class WebhookListenerTest {
    private val body = "{\"package\":{\"name\":\"house-gh-webhook\",\"package_version\":{\"version\":\"453b786\"},\"registry\":{\"url\":\"https://docker.pkg.github.com/andersostby/house-gh-webhook\"}}}"

    private var resultPayload: String? = null
    private var responseStatus: HttpStatusCode? = null

    @BeforeEach
    internal fun before() {
        resultPayload = null
        responseStatus = null
    }

    @Test
    internal fun `Listener blir kalt med nytt event når webhook trigges med gydlig signatur`() {
        val webhook = Webhook()
        withTestApplication({
            routing {
                webhook.apply { webhook() }
            }
        }) {
            webhook.addListener { resultPayload = it }
            with(handleRequest {
                method = HttpMethod.Post
                uri = "/webhook"
                addHeader("X-Hub-Signature", "15fbdf5752e486f745c2dc5ee8c4774e86f8cc2d")
                setBody(body)
            }) {
                responseStatus = response.status()
            }

            assertEquals(HttpStatusCode.OK, responseStatus)
            assertEquals("docker.pkg.github.com/andersostby/house-gh-webhook/house-gh-webhook:453b786", resultPayload)
        }
    }

    @Test
    internal fun `Listener blir ikke kalt med nytt event når webhook trigges med ugydlig signatur - gir 401`() {
        val webhook = Webhook()
        withTestApplication({
            routing {
                webhook.apply { webhook() }
            }
        }) {
            webhook.addListener { resultPayload = it }
            with(handleRequest {
                method = HttpMethod.Post
                uri = "/webhook"
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
    internal fun `Listener blir ikke kalt med nytt event når webhook trigges med for kort signatur - gir 401`() {
        val webhook = Webhook()
        withTestApplication({
            routing {
                webhook.apply { webhook() }
            }
        }) {
            webhook.addListener { resultPayload = it }
            with(handleRequest {
                method = HttpMethod.Post
                uri = "/webhook"
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
    internal fun `Listener blir ikke kalt med nytt event når webhook trigges uten signatur - gir 400`() {
        val webhook = Webhook()
        withTestApplication({
            routing {
                webhook.apply { webhook() }
            }
        }) {
            webhook.addListener { resultPayload = it }
            with(handleRequest {
                method = HttpMethod.Post
                uri = "/webhook"
                setBody(body)
            }) {
                responseStatus = response.status()
            }

            assertEquals(HttpStatusCode.BadRequest, responseStatus)
            assertNull(resultPayload)
        }
    }
}
