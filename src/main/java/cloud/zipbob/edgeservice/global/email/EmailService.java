package cloud.zipbob.edgeservice.global.email;

import cloud.zipbob.edgeservice.config.RabbitMQProperties;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.File;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final RabbitTemplate rabbitTemplate;
    private final RabbitMQProperties rabbitMQProperties;

    public void sendEmailRequest(String receiver, String nickname, String type) {
        EmailRequest emailRequest = new EmailRequest(receiver, nickname, type);
        rabbitTemplate.convertAndSend(
                rabbitMQProperties.getExchangeName(),
                rabbitMQProperties.getRoutingKey(),
                emailRequest
        );
        log.info("Email request sent to RabbitMQ: {}", emailRequest);
    }

    public void sendEmail(String receiver, String nickname, String type) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(receiver);

            if (type.equals("welcome")) {
                helper.setSubject("집밥요리사 회원가입을 축하합니다.");
            } else if (type.equals("goodbye")) {
                helper.setSubject("집밥요리사 회원탈퇴가 완료되었습니다.");
            } else {
                throw new MessagingException("잘못된 타입의 이메일 전송입니다.");
            }

            Context context = new Context();
            context.setVariable("nickname", nickname);
            context.setVariable("email", receiver);
            context.setVariable("logoImage", "cid:logoImage");

            String htmlContent = templateEngine.process(type, context);
            helper.setText(htmlContent, true);
            File logoImage = new File("src/main/resources/static/images/zipboblogo.png");
            helper.addInline("logoImage", logoImage);

            mailSender.send(message);
        } catch (MessagingException e) {
            log.error("이메일 전송 중 오류가 발생하였습니다. Error: {}", e.getMessage(), e);
        }
    }
}
