package cn.sorryqin.msgcenter.msgpush.channel;

import cn.sorryqin.msgcenter.common.conf.SendMsgConf;
import cn.sorryqin.msgcenter.exception.BusinessException;
import cn.sorryqin.msgcenter.exception.ErrorCode;
import cn.sorryqin.msgcenter.msgpush.MsgPushService;
import cn.sorryqin.msgcenter.msgpush.base.ChannelMsgBase;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

@Service
public class EmailServiceImpl implements MsgPushService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);

    @Autowired
    SendMsgConf sendMsgConf;

    @Override
    public void pushMsg(ChannelMsgBase msgBase) {
        log.info("发送 Email !!!!! content:" + msgBase.getContent());

        String recipient = msgBase.getTo();
        String subject = msgBase.getSubject();
        String body = msgBase.getContent();

        try {
            sendViaQqSmtp(recipient, subject, body);
            log.info("邮件通过 QQ SMTP 发送成功");
        } catch (MessagingException e) {
            log.warn("QQ 邮箱 SMTP 发送失败，templateId={}，尝试阿里云: {}", msgBase.getTemplateId(), e.getMessage());
            if (isAliyunFallbackConfigured()) {
                try {
                    sendViaAliyunSmtp(recipient, subject, body);
                    log.info("邮件通过阿里云 SMTP 发送成功");
                    return;
                } catch (MessagingException ex) {
                    log.error("阿里云 SMTP 发送失败", ex);
                    throw new BusinessException(ErrorCode.PUSH_MSG_ERROR, " email push msg error (QQ + Aliyun failed)");
                }
            }
            throw new BusinessException(ErrorCode.PUSH_MSG_ERROR, " email push msg error");
        }
    }

    private boolean isAliyunFallbackConfigured() {
        return StringUtils.isNotBlank(sendMsgConf.getAliyunEmailHost())
                && StringUtils.isNotBlank(sendMsgConf.getAliyunEmailAccount())
                && StringUtils.isNotBlank(sendMsgConf.getAliyunEmailPassword());
    }

    /** QQ 邮箱：STARTTLS + 常见 587 端口 */
    private void sendViaQqSmtp(String recipient, String subject, String body) throws MessagingException {
        String host = sendMsgConf.getEmailHost();
        String port = sendMsgConf.getEmailPort();
        String username = sendMsgConf.getEmailAccount();
        String password = sendMsgConf.getEmailAuthCode();

        Properties properties = new Properties();
        properties.put("mail.smtp.host", host);
        properties.put("mail.smtp.port", port);
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");

        Session session = Session.getInstance(properties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });
        transportSend(session, username, recipient, subject, body);
    }

    /** 阿里云 Direct Mail：SMTP SSL，端口一般为 465 */
    private void sendViaAliyunSmtp(String recipient, String subject, String body) throws MessagingException {
        String host = sendMsgConf.getAliyunEmailHost();
        String port = sendMsgConf.getAliyunEmailPort();
        String username = sendMsgConf.getAliyunEmailAccount();
        String password = sendMsgConf.getAliyunEmailPassword();

        Properties properties = new Properties();
        properties.put("mail.smtp.host", host);
        properties.put("mail.smtp.port", port);
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.ssl.enable", "true");
        properties.put("mail.smtp.socketFactory.port", port);
        properties.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        properties.put("mail.smtp.socketFactory.fallback", "false");

        Session session = Session.getInstance(properties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });
        transportSend(session, username, recipient, subject, body);
    }

    private void transportSend(Session session, String fromAddress, String recipient, String subject, String body)
            throws MessagingException {
        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(fromAddress));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipient));
        message.setSubject(subject);
        message.setText(body);
        Transport.send(message);
    }
}
