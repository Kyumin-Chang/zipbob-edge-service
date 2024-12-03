package cloud.zipbob.edgeservice.global.email;

import cloud.zipbob.edgeservice.config.RabbitMQProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailConsumer {
    private final EmailService emailService;
    private final RabbitMQProperties rabbitMQProperties;

    @RabbitListener(queues = "#{rabbitMQProperties.queueName}")
    public void consumeEmailRequest(EmailRequest emailRequest) {
        log.info("Consuming email request from RabbitMQ: {}", emailRequest);
        emailService.sendEmail(
                emailRequest.getReceiver(),
                emailRequest.getNickname(),
                emailRequest.getType()
        );
    }
}
