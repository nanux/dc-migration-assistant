package com.atlassian.migration.datacenter.fs.processor.configuration

import com.amazonaws.services.sqs.AmazonSQSAsync
import org.springframework.cloud.aws.core.env.ResourceIdResolver
import org.springframework.cloud.aws.messaging.config.QueueMessageHandlerFactory
import org.springframework.cloud.aws.messaging.listener.QueueMessageHandler
import org.springframework.cloud.aws.messaging.listener.SimpleMessageListenerContainer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.messaging.converter.MappingJackson2MessageConverter
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor

@Configuration
@ComponentScan
@Profile("production", "localStack")
open class FileSystemProcessorConfiguration {

    @Bean
    open fun simpleMessageListenerContainer(threadPoolTaskExecutor: ThreadPoolTaskExecutor?, resolver: ResourceIdResolver?, amazonSQSAsync: AmazonSQSAsync?, queueMessageHandler: QueueMessageHandler?): SimpleMessageListenerContainer? {
        val simpleMessageListenerContainer = SimpleMessageListenerContainer()
        simpleMessageListenerContainer.setAmazonSqs(amazonSQSAsync)
        simpleMessageListenerContainer.setMessageHandler(queueMessageHandler)
        simpleMessageListenerContainer.setMaxNumberOfMessages(10)
        simpleMessageListenerContainer.setResourceIdResolver(resolver)
        simpleMessageListenerContainer.setTaskExecutor(threadPoolTaskExecutor)
        return simpleMessageListenerContainer
    }

    @Bean
    open fun queueMessageHandler(amazonSQSAsync: AmazonSQSAsync?): QueueMessageHandler? {
        val factory = QueueMessageHandlerFactory()
        val messageConverter = MappingJackson2MessageConverter()
        messageConverter.isStrictContentTypeMatch = false
        factory.setAmazonSqs(amazonSQSAsync)
        factory.messageConverters = listOf(messageConverter)
        return factory.createQueueMessageHandler()
    }

    @Bean
    open fun threadPoolTaskExecutor(): ThreadPoolTaskExecutor? {
        val executor = ThreadPoolTaskExecutor()
        val cores = Runtime.getRuntime().availableProcessors()
        executor.corePoolSize = cores
        executor.maxPoolSize = cores
        executor.initialize()
        return executor
    }

}