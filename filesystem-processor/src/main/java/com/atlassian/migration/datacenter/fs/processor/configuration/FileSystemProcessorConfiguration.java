package com.atlassian.migration.datacenter.fs.processor.configuration;

import com.amazonaws.services.sqs.AmazonSQSAsync;
import org.springframework.cloud.aws.core.env.ResourceIdResolver;
import org.springframework.cloud.aws.messaging.config.QueueMessageHandlerFactory;
import org.springframework.cloud.aws.messaging.listener.QueueMessageHandler;
import org.springframework.cloud.aws.messaging.listener.SimpleMessageListenerContainer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Collections;

@Configuration
@ComponentScan
@Profile({"production", "localStack"})
public class FileSystemProcessorConfiguration {

    @Bean
    public SimpleMessageListenerContainer simpleMessageListenerContainer(ThreadPoolTaskExecutor threadPoolTaskExecutor, ResourceIdResolver resolver, AmazonSQSAsync amazonSQSAsync, QueueMessageHandler queueMessageHandler) {
        SimpleMessageListenerContainer simpleMessageListenerContainer = new SimpleMessageListenerContainer();
        simpleMessageListenerContainer.setAmazonSqs(amazonSQSAsync);
        simpleMessageListenerContainer.setMessageHandler(queueMessageHandler);
        simpleMessageListenerContainer.setMaxNumberOfMessages(10);
        simpleMessageListenerContainer.setResourceIdResolver(resolver);
        simpleMessageListenerContainer.setTaskExecutor(threadPoolTaskExecutor);
        return simpleMessageListenerContainer;
    }

    @Bean
    public QueueMessageHandler queueMessageHandler(AmazonSQSAsync amazonSQSAsync) {
        QueueMessageHandlerFactory factory = new QueueMessageHandlerFactory();
        MappingJackson2MessageConverter messageConverter = new MappingJackson2MessageConverter();
        messageConverter.setStrictContentTypeMatch(false);
        factory.setAmazonSqs(amazonSQSAsync);
        factory.setMessageConverters(Collections.singletonList(messageConverter));
        return factory.createQueueMessageHandler();
    }

    @Bean
    public ThreadPoolTaskExecutor threadPoolTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        final int cores = Runtime.getRuntime().availableProcessors();
        executor.setCorePoolSize(cores);
        executor.setMaxPoolSize(cores);
        executor.initialize();
        return executor;
    }


}
