package com.andersostby.webhook

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.io.File

internal class Environment(env: JsonNode) {
    internal constructor(path: String = "/var/run/environment/env.json") :
            this(jacksonObjectMapper().readTree(File(path)))

    internal val secret: String = env["secret"].asText()

    internal val rabbitmq = env["rabbitmq"].let {
        Rabbitmq(
                host = it["host"].asText(),
                username = it["username"].asText(),
                password = it["password"].asText()
        )
    }

    internal class Rabbitmq(internal val host: String, internal val username: String, internal val password: String)
}
