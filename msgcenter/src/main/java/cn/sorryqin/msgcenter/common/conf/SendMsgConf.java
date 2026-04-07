package cn.sorryqin.msgcenter.common.conf;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SendMsgConf {

    @Value("${send-msg-conf.mysql-as-mq}")
    private boolean mysqlAsMq;

    @Value("${send-msg-conf.open-cache}")
    private boolean openCache;

    @Value("${send-msg-conf.max-retry-count}")
    private int maxRetryCount;

    @Value("${send-msg-conf.email-account}")
    private String emailAccount;

    @Value("${send-msg-conf.email-auth-code}")
    private String emailAuthCode;

    @Value("${send-msg-conf.email-host}")
    private String emailHost;

    @Value("${send-msg-conf.email-port}")
    private String emailPort;

    /** 阿里云邮件推送 SMTP（如 smtpdm.aliyun.com），为空表示不在 QQ 失败后回退 */
    @Value("${send-msg-conf.aliyun-email-host:}")
    private String aliyunEmailHost;

    @Value("${send-msg-conf.aliyun-email-port:465}")
    private String aliyunEmailPort;

    @Value("${send-msg-conf.aliyun-email-account:}")
    private String aliyunEmailAccount;

    @Value("${send-msg-conf.aliyun-email-password:}")
    private String aliyunEmailPassword;

    public boolean isMysqlAsMq() {
        return mysqlAsMq;
    }

    public void setMysqlAsMq(boolean mysqlAsMq) {
        this.mysqlAsMq = mysqlAsMq;
    }

    public String getEmailAccount() {
        return emailAccount;
    }

    public void setEmailAccount(String emailAccount) {
        this.emailAccount = emailAccount;
    }

    public String getEmailAuthCode() {
        return emailAuthCode;
    }

    public void setEmailAuthCode(String emailAuthCode) {
        this.emailAuthCode = emailAuthCode;
    }

    public String getEmailHost() {
        return emailHost;
    }

    public void setEmailHost(String emailHost) {
        this.emailHost = emailHost;
    }

    public String getEmailPort() {
        return emailPort;
    }

    public void setEmailPort(String emailPort) {
        this.emailPort = emailPort;
    }

    public boolean isOpenCache() {
        return openCache;
    }

    public void setOpenCache(boolean openCache) {
        this.openCache = openCache;
    }

    public int getMaxRetryCount() {
        return maxRetryCount;
    }

    public void setMaxRetryCount(int maxRetryCount) {
        this.maxRetryCount = maxRetryCount;
    }

    public String getAliyunEmailHost() {
        return aliyunEmailHost;
    }

    public String getAliyunEmailPort() {
        return aliyunEmailPort;
    }

    public String getAliyunEmailAccount() {
        return aliyunEmailAccount;
    }

    public String getAliyunEmailPassword() {
        return aliyunEmailPassword;
    }
}