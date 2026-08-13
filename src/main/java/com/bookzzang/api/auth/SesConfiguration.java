package com.bookzzang.api.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sesv2.SesV2Client;

@Configuration
class SesConfiguration {
    @Bean
    SesV2Client sesV2Client(@Value("${shelfie.auth.email-verification.ses.region}") String region,
                            @Value("${shelfie.auth.email-verification.ses.access-key:}") String accessKey,
                            @Value("${shelfie.auth.email-verification.ses.secret-key:}") String secretKey) {
        var builder = SesV2Client.builder().region(Region.of(region));
        if (StringUtils.hasText(accessKey) && StringUtils.hasText(secretKey)) {
            builder.credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)));
        } else {
            builder.credentialsProvider(DefaultCredentialsProvider.create());
        }
        return builder.build();
    }
}
