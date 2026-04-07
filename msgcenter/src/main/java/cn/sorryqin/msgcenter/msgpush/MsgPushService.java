package cn.sorryqin.msgcenter.msgpush;

import cn.sorryqin.msgcenter.model.dto.SendMsgReq;
import cn.sorryqin.msgcenter.msgpush.base.ChannelMsgBase;

public interface MsgPushService {
    void pushMsg(ChannelMsgBase msgBase);
}
