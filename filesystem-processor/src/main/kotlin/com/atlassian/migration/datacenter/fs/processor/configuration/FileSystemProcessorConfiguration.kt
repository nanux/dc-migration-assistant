package com.atlassian.migration.datacenter.fs.processor.configuration

import com.amazonaws.services.s3.event.S3EventNotification
import com.amazonaws.services.sqs.AmazonSQSAsync
import com.atlassian.migration.datacenter.fs.processor.configuration.AWSServicesConfiguration.Companion.STACK_NAME
import com.atlassian.migration.datacenter.fs.processor.services.SQSMessageProcessor
import org.springframework.cloud.aws.context.config.annotation.EnableStackConfiguration
import org.springframework.cloud.aws.core.env.ResourceIdResolver
import org.springframework.cloud.aws.messaging.support.destination.DynamicQueueUrlDestinationResolver
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.integration.annotation.ServiceActivator
import org.springframework.integration.annotation.Transformer
import org.springframework.integration.aws.inbound.SqsMessageDrivenChannelAdapter
import org.springframework.integration.channel.ExecutorChannel
import org.springframework.integration.channel.PublishSubscribeChannel
import org.springframework.integration.core.MessageProducer
import org.springframework.integration.dispatcher.RoundRobinLoadBalancingStrategy
import org.springframework.integration.endpoint.EventDrivenConsumer
import org.springframework.integration.handler.LoggingHandler
import org.springframework.messaging.SubscribableChannel
import org.springframework.scheduling.TaskScheduler
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler


@Configuration
@ComponentScan
@EnableStackConfiguration(stackName = STACK_NAME)
open class FileSystemProcessorConfiguration {

    @Bean
    @ServiceActivator(inputChannel = "errorChannel")
    open fun logging(): LoggingHandler? {
        val adapter = LoggingHandler(LoggingHandler.Level.INFO)
        adapter.setLoggerName("ERROR_LOGGER")
        adapter.setLogExpressionString("headers.id + ': ' + payload")
        return adapter
    }

    @Bean
    open fun inboundChannel(): SubscribableChannel? {
        return PublishSubscribeChannel()
    }

    @Bean
    open fun routingChannel(): SubscribableChannel? {
        return PublishSubscribeChannel()
    }

    @Transformer(inputChannel = "routingChannel", outputChannel = "inboundTransformChannel")
    open fun transformPayload(raw: String?): S3EventNotification? {
        return S3EventNotification.parseJson(raw)
    }

    @Bean
    open fun inboundTransformChannel(threadPoolTaskExecutor: ThreadPoolTaskExecutor): SubscribableChannel? {
        return ExecutorChannel(threadPoolTaskExecutor, RoundRobinLoadBalancingStrategy())
    }

    @Bean
    open fun sqsMessageDrivenChannelAdapter(destinationResolver: DynamicQueueUrlDestinationResolver?, errorChannel: PublishSubscribeChannel?, inboundChannel: SubscribableChannel?, amazonSqs: AmazonSQSAsync): MessageProducer? {
        val adapter = SqsMessageDrivenChannelAdapter(amazonSqs, QUEUE_LOGICAL_NAME)
        adapter.setDestinationResolver(destinationResolver)
        adapter.setQueueStopTimeout(60000)
        adapter.outputChannel = inboundChannel
        adapter.errorChannel = errorChannel
        return adapter
    }

    @Bean
    open fun consumer(inboundTransformChannel: SubscribableChannel?, sqsMessageProcessor: SQSMessageProcessor?): EventDrivenConsumer {
        return EventDrivenConsumer(inboundTransformChannel, sqsMessageProcessor)
    }

    @Bean
    open fun dynamicQueueUrlDestinationResolver(idResolver: ResourceIdResolver, amazonSqs: AmazonSQSAsync): DynamicQueueUrlDestinationResolver? {
        return DynamicQueueUrlDestinationResolver(amazonSqs, idResolver)
    }

    @Bean
    open fun taskScheduler(): TaskScheduler? {
        val scheduler = ThreadPoolTaskScheduler()
        val cores = Runtime.getRuntime().availableProcessors()
        scheduler.poolSize = cores
        return scheduler
    }

    companion object {
        private const val QUEUE_LOGICAL_NAME: String = "MigrationQueue"
        private const val STACK_NAME: String = "migration-helper"
    }
}