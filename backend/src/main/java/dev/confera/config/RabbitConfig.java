package dev.confera.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String EXCHANGE             = "confera.events";
    public static final String DLX_EXCHANGE         = "confera.events.dlx";
    public static final String QUEUE                = "confera.file-import";
    public static final String DLQ                  = "confera.file-import.dlq";
    public static final String ROUTING_KEY          = "file.import.created";
    public static final String RECONCILIATION_QUEUE = "confera.reconciliation";
    public static final String FILE_IMPORT_COMPLETED = "file.import.completed";

    @Bean
    public TopicExchange eventExchange() {
        return ExchangeBuilder.topicExchange(EXCHANGE).durable(true).build();
    }

    @Bean
    public TopicExchange dlxExchange() {
        return ExchangeBuilder.topicExchange(DLX_EXCHANGE).durable(true).build();
    }

    @Bean
    public Queue fileImportQueue() {
        return QueueBuilder.durable(QUEUE)
            .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", ROUTING_KEY)
            .build();
    }

    @Bean
    public Queue fileImportDlq() {
        return QueueBuilder.durable(DLQ).build();
    }

    @Bean
    public Binding fileImportBinding(Queue fileImportQueue, TopicExchange eventExchange) {
        return BindingBuilder.bind(fileImportQueue).to(eventExchange).with(ROUTING_KEY);
    }

    @Bean
    public Binding dlqBinding(Queue fileImportDlq, TopicExchange dlxExchange) {
        return BindingBuilder.bind(fileImportDlq).to(dlxExchange).with(ROUTING_KEY);
    }

    @Bean
    public Queue reconciliationQueue() {
        return QueueBuilder.durable(RECONCILIATION_QUEUE).build();
    }

    @Bean
    public Binding reconciliationBinding(Queue reconciliationQueue, TopicExchange eventExchange) {
        return BindingBuilder.bind(reconciliationQueue).to(eventExchange).with(FILE_IMPORT_COMPLETED);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        DefaultJackson2JavaTypeMapper typeMapper = new DefaultJackson2JavaTypeMapper();
        typeMapper.setTypePrecedence(Jackson2JavaTypeMapper.TypePrecedence.INFERRED);
        converter.setJavaTypeMapper(typeMapper);
        return converter;
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         Jackson2JsonMessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
        ConnectionFactory connectionFactory,
        Jackson2JsonMessageConverter messageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        return factory;
    }
}