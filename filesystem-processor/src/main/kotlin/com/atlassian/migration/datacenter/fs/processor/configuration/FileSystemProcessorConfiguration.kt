package com.atlassian.migration.datacenter.fs.processor.configuration

import com.amazonaws.services.sqs.AmazonSQSAsync
import com.atlassian.migration.datacenter.fs.processor.services.SQSMessageProcessor
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.integration.aws.inbound.SqsMessageDrivenChannelAdapter
import org.springframework.integration.channel.ExecutorChannel
import org.springframework.integration.core.MessageProducer
import org.springframework.integration.dispatcher.RoundRobinLoadBalancingStrategy
import org.springframework.integration.endpoint.EventDrivenConsumer
import org.springframework.messaging.SubscribableChannel
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor


@Configuration
@ComponentScan
@Profile("production", "localStack")
open class FileSystemProcessorConfiguration {

    @Bean
    open fun subscribableChannel(threadPoolTaskExecutor: ThreadPoolTaskExecutor): SubscribableChannel? {
        return ExecutorChannel(threadPoolTaskExecutor, RoundRobinLoadBalancingStrategy())
    }

    @Bean
    open fun sqsMessageDrivenChannelAdapter(subscribableChannel: SubscribableChannel?, amazonSqs: AmazonSQSAsync?): MessageProducer? {
        val adapter = SqsMessageDrivenChannelAdapter(amazonSqs, QUEUE_LOGICAL_NAME)
        adapter.outputChannel = subscribableChannel
        return adapter
    }

    @Bean
    open fun consumer(subscribableChannel: SubscribableChannel?, sqsMessageProcessor: SQSMessageProcessor?): EventDrivenConsumer {
        return EventDrivenConsumer(subscribableChannel, sqsMessageProcessor)
    }


    @Bean
    open fun taskScheduler(): ThreadPoolTaskExecutor? {
        val executor = ThreadPoolTaskExecutor()
        val cores = Runtime.getRuntime().availableProcessors()
        executor.corePoolSize = cores
        executor.maxPoolSize = cores
        executor.initialize()
        return executor
    }

    companion object {
        private const val QUEUE_LOGICAL_NAME: String = "MigrationQueue"
    }

}