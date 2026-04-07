package cn.sorryqin.msgcenter.msgpush.channel;

import cn.sorryqin.msgcenter.msgpush.MsgPushService;
import cn.sorryqin.msgcenter.msgpush.base.ChannelMsgBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SMSServiceImpl implements MsgPushService {

    private static final Logger log = LoggerFactory.getLogger(SMSServiceImpl.class);

    @Override
    public void pushMsg(ChannelMsgBase msgBase) {

        log.info("发送 SMS 短信!!!!! content:"+msgBase.getContent());
    }
}
