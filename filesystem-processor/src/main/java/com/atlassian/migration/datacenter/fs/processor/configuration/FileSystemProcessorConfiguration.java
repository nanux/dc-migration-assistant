package com.atlassian.migration.datacenter.fs.processor.configuration;

import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cloud.aws.core.env.ResourceIdResolver;
import org.springframework.cloud.aws.messaging.config.QueueMessageHandlerFactory;
import org.springframework.cloud.aws.messaging.listener.QueueMessageHandler;
import org.springframework.cloud.aws.messaging.listener.SimpleMessageListenerContainer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.ArrayList;
import java.util.List;

@Configuration
@ComponentScan
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
    public QueueMessageHandler queueMessageHandler(AmazonSQSAsync amazonSQSAsync, List<MessageConverter> customMessageConverters) {
        QueueMessageHandlerFactory queueMessageHandlerFactory = new QueueMessageHandlerFactory();
        queueMessageHandlerFactory.setAmazonSqs(amazonSQSAsync);
        queueMessageHandlerFactory.setMessageConverters(customMessageConverters);
        return queueMessageHandlerFactory.createQueueMessageHandler();
    }

    @Bean
    @Primary
    public List<MessageConverter> customMessageConverters() {
        ObjectMapper objectMapper = new ObjectMapper();
        MappingJackson2MessageConverter messageConverter = new MappingJackson2MessageConverter();
        messageConverter.setObjectMapper(objectMapper);
        List<MessageConverter> converters = new ArrayList<>(1);
        converters.add(messageConverter);
        return converters;
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
