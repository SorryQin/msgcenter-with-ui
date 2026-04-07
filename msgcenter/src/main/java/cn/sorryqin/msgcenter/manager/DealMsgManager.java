package cn.sorryqin.msgcenter.manager;

import cn.sorryqin.msgcenter.model.dto.SendMsgReq;

public interface DealMsgManager {

    public void DealOneMsg(SendMsgReq sendMsgReq);
}
