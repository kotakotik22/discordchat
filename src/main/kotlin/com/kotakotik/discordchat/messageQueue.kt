package com.kotakotik.discordchat

import dev.kord.core.Kord
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.execute
import dev.kord.core.entity.Webhook
import dev.kord.core.entity.channel.TextChannel
import dev.kord.rest.builder.message.create.UserMessageCreateBuilder
import dev.kord.rest.builder.message.create.WebhookMessageCreateBuilder
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED

class QueueContext(val kord: Kord, val webhook: Webhook, val channel: TextChannel) {
    val webhookToken = webhook.token ?: error("webhook should have token")
}

fun interface QueueAction {
    suspend fun QueueContext.executeInQueue()
}

val queueChannel = Channel<QueueAction>(capacity = UNLIMITED)

suspend fun setUpQueueReceiver(kord: Kord, webhook: Webhook, channel: TextChannel) {
    val ctx = QueueContext(kord, webhook, channel)

    logger.info("Receiving from queue")
    for (msg in queueChannel) {
        with(msg) {
            ctx.executeInQueue()
        }
    }

    logger.info("Queue channel closed, cleaning up")
    channel.createMessage(Config.Messages.serverStopping)
    discordContext.close()
}

suspend fun enqueue(action: QueueAction) =
    queueChannel.send(action)

suspend inline fun enqueueBotMessage(crossinline builder: suspend UserMessageCreateBuilder.() -> Unit) =
    enqueue {
        channel.createMessage {
            builder()
        }
    }

suspend inline fun enqueueWebhookMessage(crossinline builder: suspend WebhookMessageCreateBuilder.() -> Unit) =
    enqueue {
        webhook.execute(webhookToken) {
            builder()
        }
    }