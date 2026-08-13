package com.bookzzang.api.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.Body;
import software.amazon.awssdk.services.sesv2.model.Content;
import software.amazon.awssdk.services.sesv2.model.Destination;
import software.amazon.awssdk.services.sesv2.model.EmailContent;
import software.amazon.awssdk.services.sesv2.model.Message;
import software.amazon.awssdk.services.sesv2.model.SendEmailRequest;

@Service
class SesEmailSender {
    private final SesV2Client ses;
    private final String sender;

    SesEmailSender(SesV2Client ses, @Value("${shelfie.auth.email-verification.ses.sender:}") String sender) {
        this.ses = ses;
        this.sender = sender;
    }

    void sendVerificationCode(String recipient, String code) {
        if (!StringUtils.hasText(sender)) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "email service is not configured");
        String text = "책짱 회원가입 인증코드는 " + code + "입니다.\n10분 안에 입력해 주세요.\n본인이 요청하지 않았다면 이 메일을 무시하세요.";
        Message message = Message.builder()
                .subject(Content.builder().data("[책짱] 이메일 인증코드").charset("UTF-8").build())
                .body(Body.builder().text(Content.builder().data(text).charset("UTF-8").build()).build())
                .build();
        ses.sendEmail(SendEmailRequest.builder().fromEmailAddress(sender)
                .destination(Destination.builder().toAddresses(recipient).build())
                .content(EmailContent.builder().simple(message).build()).build());
    }
}
