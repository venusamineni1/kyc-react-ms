package com.venus.kyc.risk.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory;
import org.springframework.integration.sftp.session.SftpRemoteFileTemplate;

@Configuration
public class SftpConfig {

    @Value("${sftp.host:localhost}")
    private String sftpHost;

    @Value("${sftp.port:22}")
    private int sftpPort;

    @Value("${sftp.user:user}")
    private String sftpUser;

    @Value("${sftp.password:#{null}}")
    private String sftpPassword;

    @Value("${sftp.privateKey:#{null}}")
    private String sftpPrivateKey; // Path to private key if used

    @Bean
    public DefaultSftpSessionFactory sftpSessionFactory() {
        DefaultSftpSessionFactory factory = new DefaultSftpSessionFactory();
        factory.setHost(sftpHost);
        factory.setPort(sftpPort);
        factory.setUser(sftpUser);
        if (sftpPassword != null && !sftpPassword.isEmpty()) {
            factory.setPassword(sftpPassword);
        }
        factory.setAllowUnknownKeys(true); // For dev/test
        return factory;
    }

    @Bean
    public SftpRemoteFileTemplate sftpRemoteFileTemplate(DefaultSftpSessionFactory sftpSessionFactory) {
        return new SftpRemoteFileTemplate(sftpSessionFactory);
    }
}
