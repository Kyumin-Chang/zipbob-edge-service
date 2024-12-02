package cloud.zipbob.edgeservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Setter
@Getter
@Configuration
@ConfigurationProperties(prefix = "rabbit")
public class RabbitMQProperties {
    private String queueName;

    private String exchangeName;

    private String routingKey;

    private String dlq;

    private String dlx;

    private String dlqRoutingKey;
}
