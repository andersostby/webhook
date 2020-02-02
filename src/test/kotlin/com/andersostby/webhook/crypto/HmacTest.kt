package com.andersostby.webhook.crypto

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class HmacTest {
    private val signature = "sha1=15fbdf5752e486f745c2dc5ee8c4774e86f8cc2d"
    private val secret = "Hemmelig"
    private val payload = "{\"package\":{\"name\":\"house-gh-webhook\",\"package_version\":{\"version\":\"453b786\"},\"registry\":{\"url\":\"https://docker.pkg.github.com/andersostby/house-gh-webhook\"}}}"

    @Test
    internal fun `verifiserer payload`() {
        val hmac = Hmac(
                secret = secret,
                signature = signature,
                payload = payload
        )
        assertTrue(hmac.verify())
    }

    @Test
    internal fun `feil secret kaster feil`() {
        val hmac = Hmac(
                secret = "FeilSecret",
                signature = signature,
                payload = payload
        )
        assertThrows<IllegalStateException> { hmac.verify() }
    }

    @Test
    internal fun `feil signature kaster feil`() {
        val hmac = Hmac(
                secret = secret,
                signature = signature.takeLast(40).reversed(),
                payload = payload
        )
        assertThrows<IllegalStateException> { hmac.verify() }
    }

    @Test
    internal fun `for kort signature kaster feil`() {
        assertThrows<IllegalArgumentException> {
            Hmac(
                    secret = secret,
                    signature = signature.takeLast(39).reversed(),
                    payload = payload
            )
        }
    }
}
