package cn.sorryqin.msgcenter.tools;

import cn.sorryqin.msgcenter.enums.MsgStatus;
import cn.sorryqin.msgcenter.model.MsgRecordModel;
import cn.sorryqin.msgcenter.model.TemplateModel;
import cn.sorryqin.msgcenter.model.dto.SendMsgReq;

public interface MsgRecordService {

    MsgRecordModel GetMsgRecordWithCache(String msgId);

    void CreateMsgRecord(String msgId,SendMsgReq sendMsgReq, TemplateModel tp, MsgStatus status);

    void CreateOrUpdateMsgRecord(String msgId,SendMsgReq sendMsgReq, TemplateModel tp, MsgStatus status);
}
