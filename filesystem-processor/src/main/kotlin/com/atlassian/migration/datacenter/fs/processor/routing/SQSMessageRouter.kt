package com.atlassian.migration.datacenter.fs.processor.routing

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.integration.annotation.Router
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.stereotype.Component

@Component
class SQSMessageRouter {

    @Autowired
    lateinit var routingChannel: MessageChannel

    @Autowired
    lateinit var errorChannel: MessageChannel

    @Router(inputChannel = "inboundChannel")
    fun route(message: Message<String>): MessageChannel {
        val body = message.payload
        val testEvent = body.contains("TestEvent", true)
        return if (testEvent) {
            this.errorChannel
        } else {
            this.routingChannel
        }
    }

}

