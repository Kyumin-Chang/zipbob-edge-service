package cloud.zipbob.edgeservice.config;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class RabbitMQConfig {
    private final RabbitMQProperties rabbitMQProperties;

    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, Jackson2JsonMessageConverter jsonMessageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter);
        return rabbitTemplate;
    }

    @Bean
    public Queue mainQueue() {
        return QueueBuilder.durable(rabbitMQProperties.getQueueName())
                .withArgument("x-message-ttl", 60000)
                .withArgument("x-dead-letter-exchange", rabbitMQProperties.getDlx())
                .withArgument("x-dead-letter-routing-key", rabbitMQProperties.getDlqRoutingKey())
                .build();
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(rabbitMQProperties.getDlq()).build();
    }

    @Bean
    public TopicExchange mainExchange() {
        return new TopicExchange(rabbitMQProperties.getExchangeName());
    }

    @Bean
    public TopicExchange deadLetterExchange() {
        return new TopicExchange(rabbitMQProperties.getDlx());
    }

    @Bean
    public Binding mainQueueBinding(@Qualifier("mainQueue") Queue mainQueue, @Qualifier("mainExchange") TopicExchange mainExchange) {
        return BindingBuilder.bind(mainQueue).to(mainExchange).with(rabbitMQProperties.getRoutingKey());
    }

    @Bean
    public Binding deadLetterQueueBinding(@Qualifier("deadLetterQueue") Queue deadLetterQueue, @Qualifier("deadLetterExchange") TopicExchange deadLetterExchange) {
        return BindingBuilder.bind(deadLetterQueue).to(deadLetterExchange).with(rabbitMQProperties.getDlqRoutingKey());
    }
}
