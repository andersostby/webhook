package com.andersostby.webhook

import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import org.slf4j.LoggerFactory
import java.io.Closeable

private val log = LoggerFactory.getLogger("RabbitMQProducer")

internal class RabbitMQProducer(connectionFactory: ConnectionFactory) : Closeable {
    constructor(rabbitmq: Environment.Rabbitmq) :
            this(ConnectionFactory().apply {
                this.username = rabbitmq.username
                this.password = rabbitmq.password
                this.host = rabbitmq.host
                this.port = ConnectionFactory.USE_DEFAULT_PORT
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
