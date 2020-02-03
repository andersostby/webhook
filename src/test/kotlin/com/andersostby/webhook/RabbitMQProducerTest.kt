package com.andersostby.webhook

import com.rabbitmq.client.Channel
import com.rabbitmq.client.ConnectionFactory
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test

internal class RabbitMQProducerTest {
    @Test
    internal fun `Sender melding til rapid`() {
        val channel = mockk<Channel> {
            every { basicPublish(any(), any(), isNull(), any()) } returns Unit
        }
        val connectionFactory = mockk<ConnectionFactory> {
            every { newConnection() } returns mockk {
                every { createChannel() } returns channel
            }
        }
        val producer = RabbitMQProducer(connectionFactory)
        verify(exactly = 0) { channel.basicPublish(any(), any(), isNull(), any()) }
        producer.send("Test")
        verify(exactly = 1) { channel.basicPublish(any(), any(), isNull(), any()) }
    }
}