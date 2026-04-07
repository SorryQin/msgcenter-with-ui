package cn.sorryqin.msgcenter.msgpush.channel;

import cn.sorryqin.msgcenter.msgpush.MsgPushService;
import cn.sorryqin.msgcenter.msgpush.base.ChannelMsgBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class LarkServiceImpl implements MsgPushService {

    private static final Logger log = LoggerFactory.getLogger(LarkServiceImpl.class);

    @Override
    public void pushMsg(ChannelMsgBase msgBase) {

        log.info("发送 Lark!!!!! content:"+msgBase.getContent());
    }
}
