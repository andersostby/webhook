package com.andersostby.webhook

import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import org.slf4j.LoggerFactory
import java.io.Closeable

private val log = LoggerFactory.getLogger("RabbitMQProducer")

internal class RabbitMQProducer(connectionFactory: ConnectionFactory) : Closeable {
    constructor(username: String, password: String, host: String, port: Int = ConnectionFactory.USE_DEFAULT_PORT) :
            this(ConnectionFactory().apply {
                this.username = username
                this.password = password
                this.host = host
                this.port = port
            })

    private val connection: Connection =
            try {
                connectionFactory.newConnection()
            } catch (e: Exception) {
                log.error("Kunne ikke koble til rabbitmq", e)
                throw e
            }

    private val channel: Channel =
            try {
                connection.createChannel()
            } catch (e: Exception) {
                log.error("Kunne ikke opprette channel til rabbitmq", e)
                throw e
            }

    internal fun send(message: String) {
        send(message.toByteArray())
    }

    internal fun send(message: ByteArray) {
        log.info("Sender melding til rapid")
        try {
            channel.basicPublish("rapid", "", null, message)
        } catch (e: Exception) {
            log.error("Kunne ikke sende melding til rapid", e)
            throw e
        }
    }

    override fun close() {
        channel.close()
        connection.close()
    }
}
