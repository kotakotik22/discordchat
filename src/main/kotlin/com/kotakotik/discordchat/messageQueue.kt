package com.kotakotik.discordchat

import dev.kord.core.Kord
import dev.kord.core.behavior.channel.MessageChannelBehavior
import dev.kord.core.behavior.execute
import dev.kord.core.cache.data.MessageData
import dev.kord.core.entity.Message
import dev.kord.core.entity.Webhook
import dev.kord.core.entity.channel.TextChannel
import dev.kord.rest.builder.message.AllowedMentionsBuilder
import dev.kord.rest.builder.message.create.UserMessageCreateBuilder
import dev.kord.rest.builder.message.create.WebhookMessageCreateBuilder
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.onSubscription

sealed interface Instruction {
    class BotMessagePlan(val value: UserMessageCreateBuilder) : Instruction
    class WebhookMessagePlan(
        // todo: we can probably make these into just properties
        var content: String? = null,
        var username: String? = null,
        var avatarUrl: String? = null,
        var allowedMentions: AllowedMentionsBuilder? = null
    ) : Instruction {
        fun applyTo(builder: WebhookMessageCreateBuilder) {
            builder.content = content
            builder.username = username
            builder.avatarUrl = avatarUrl
            builder.allowedMentions = allowedMentions
        }

        inline fun allowedMentions(b: AllowedMentionsBuilder.() -> Unit) {
            allowedMentions = AllowedMentionsBuilder().apply(b)
        }
    }

    object Stop : Instruction
}

val messageQueue = MutableSharedFlow<Instruction>(extraBufferCapacity = Config.queueSize)


suspend fun MessageChannelBehavior.sendMessage(message: UserMessageCreateBuilder): Message {
    val response = kord.rest.channel.createMessage(id, message.toRequest())
    val data = MessageData.from(response)

    return Message(data, kord)
}

suspend fun setUpMessageSender(kord: Kord, webhook: Webhook, channel: TextChannel): Nothing =
    coroutineScope {
        logger.info("Setting up queue")
        messageQueue.onSubscription {
            emit(Instruction.BotMessagePlan(UserMessageCreateBuilder().apply {
                content = Config.Messages.serverStarted
            }))
        }.collect {
            when (it) {
                is Instruction.BotMessagePlan -> {
                    channel.sendMessage(it.value)
                }
                is Instruction.WebhookMessagePlan -> {
                    webhook.execute(webhook.token ?: error("webhook should have token attached")) {
                        it.applyTo(this)
                    }
                }
                is Instruction.Stop -> {
                    logger.info("Received stop instruction, cleaning up")
                    channel.createMessage(Config.Messages.serverStopping)
                    try {
                        kord.logout()
                    } catch (t: Throwable) {
                        logger.info("Got error while trying to log out of kord, this happens sometimes")
                        throw t
                    } finally {
                        discordContext.close()
                    }
                }
            }
        }
    }

suspend fun enqueueInstruction(message: Instruction) =
    messageQueue.emit(message)

suspend fun enqueueBotMessage(v: UserMessageCreateBuilder) =
    enqueueInstruction(Instruction.BotMessagePlan(v))

suspend inline fun enqueueBotMessage(builder: UserMessageCreateBuilder.() -> Unit) =
    enqueueBotMessage(UserMessageCreateBuilder().apply(builder))

suspend inline fun enqueueWebhookMessage(builder: Instruction.WebhookMessagePlan.() -> Unit) =
    enqueueInstruction(Instruction.WebhookMessagePlan().apply(builder))