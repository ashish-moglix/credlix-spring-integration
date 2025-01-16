package com.credlix.abfl.config;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Date;
import java.util.Properties;

import javax.mail.BodyPart;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.search.AndTerm;
import javax.mail.search.ComparisonTerm;
import javax.mail.search.FlagTerm;
import javax.mail.search.FromTerm;
import javax.mail.search.ReceivedDateTerm;
import javax.mail.search.SearchTerm;
import javax.mail.search.SubjectTerm;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.Pollers;
import org.springframework.integration.mail.ImapMailReceiver;
import org.springframework.integration.mail.MailReceivingMessageSource;
import org.springframework.integration.mail.Pop3MailReceiver;
import org.springframework.integration.mail.SearchTermStrategy;
import org.springframework.integration.mail.dsl.Mail;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class AbflEmailInterceptorConfig {

    @Bean
    public ImapMailReceiver mailReceiver() throws UnsupportedEncodingException {
        Properties properties = new Properties();
        properties.setProperty("mail.imap.host", "outlook.office365.com");
        properties.setProperty("mail.imap.port", "993");
        properties.setProperty("mail.imap.starttls.enable", "true");
        properties.setProperty("mail.imap.ssl.enable", "true");
        properties.setProperty("mail.imap.ssl.protocols", "TLSv1.2");
        properties.setProperty("mail.debug", "true");
        // String username = URLEncoder.encode("credlix-test@outlook.com", "UTF-8");
        // String password = URLEncoder.encode("Test@1234@1234", "UTF-8");

        String username = URLEncoder.encode("ashish.awasthi@moglix.com", "UTF-8");
        String password = URLEncoder.encode("OptimusPrime@123", "UTF-8");
        ImapMailReceiver mailReceiver = new ImapMailReceiver(
                "imap://" + username + ":" + password + "@outlook.office365.com:993/INBOX");
        mailReceiver.setJavaMailProperties(properties);
        mailReceiver.setShouldDeleteMessages(false);
        mailReceiver.setShouldMarkMessagesAsRead(true);
        mailReceiver.setSimpleContent(true);

        mailReceiver.setSearchTermStrategy(
                new SearchTermStrategy() {
                    @Override
                    public SearchTerm generateSearchTerm(Flags supportedFlags, Folder folder) {
                        try {
                            // Construct a search term for filtering emails
                            SearchTerm senderTerm = new FromTerm(new InternetAddress("ashish.awasthi@moglix.com"));
                            SearchTerm subjectTerm = new SubjectTerm("ICICI Bank Limited");
                            SearchTerm unreadEmailsOnly = new FlagTerm(new Flags(Flags.Flag.SEEN), false);
                            SearchTerm receivedTodayTerm = new ReceivedDateTerm(ComparisonTerm.LE, new Date());

                            // Combine the terms using AndTerm
                            SearchTerm searchTerm = new AndTerm(new AndTerm(new AndTerm(senderTerm, subjectTerm),
                                    receivedTodayTerm), unreadEmailsOnly);

                            return searchTerm;
                        } catch (MessagingException e) {
                            log.info("Exception while creating search strategy ", e);
                            return null; // Return null if there's an exception
                        }
                    }
                });

        mailReceiver.afterPropertiesSet();
        return mailReceiver;
    }

    @Bean
    public IntegrationFlow emailFlow() throws UnsupportedEncodingException {
        return IntegrationFlows.from(
                Mail.imapInboundAdapter(mailReceiver()),
                e -> e.poller(Pollers.fixedDelay(5000)).autoStartup(true))
                .handle(message -> {
                    try {
                        MimeMessage mimeMessage = (MimeMessage) message.getPayload();
                        String from = mimeMessage.getFrom()[0].toString();
                        String subject = mimeMessage.getSubject();

                        log.info("Received email: from {} with subject {}", from, subject);
                        Object content = mimeMessage.getContent();
                        if (content instanceof Multipart) {
                            Multipart multipart = (Multipart) content;
                            for (int i = 0; i < multipart.getCount(); i++) {
                                BodyPart bodyPart = multipart.getBodyPart(i);
                                if (Part.ATTACHMENT.equalsIgnoreCase(bodyPart.getDisposition())) {
                                    String attachmentFileName = bodyPart.getFileName();
                                    if (attachmentFileName == null || attachmentFileName.isEmpty()) {
                                        continue;
                                    }
                                    String localFilePath = "/home/moglix/credlix/pocs/abfl/files/" + attachmentFileName;
                                    try (InputStream inputStream = bodyPart.getInputStream();
                                            FileOutputStream outputStream = new FileOutputStream(localFilePath)) {
                                        byte[] buffer = new byte[1024];
                                        int bytesRead;
                                        while ((bytesRead = inputStream.read(buffer)) != -1) {
                                            outputStream.write(buffer, 0, bytesRead);
                                        }
                                    }
                                    log.info("Attachment saved locally: files/{}",  attachmentFileName);
                                }
                            }
                        } else {
                            log.info("content is not of type multipart file it is {}", content.getClass());
                        }
                    } catch (Exception e) {
                        log.info("Exception occured during email message parsing ", e);
                    }
                })
                .get();
    }
}
