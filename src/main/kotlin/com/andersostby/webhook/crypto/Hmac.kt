package com.andersostby.webhook.crypto

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

internal class Hmac(
    secret: String,
    signature: String,
    payload: String
) {
    private val trimmedSignature: String =
        signature.replace("^[a-z0-9]*=([0-9a-f]{40})$".toRegex()) { it.groupValues[1] }
    private val calculatedSignature: String

    init {
        require(trimmedSignature.matches("^[0-9a-f]{40}$".toRegex()))

        calculatedSignature = Mac.getInstance("HmacSHA1").run {
            init(SecretKeySpec(secret.toByteArray(), algorithm))
            hashToString(doFinal(payload.toByteArray()))
        }
    }

    private fun hashToString(hash: ByteArray): String {
        return hash.fold("", { str, it -> str + "%02x".format(it) })
    }

    internal fun verify() {
        if (trimmedSignature != calculatedSignature) throw IllegalStateException("Signatures does not match")
    }
}
