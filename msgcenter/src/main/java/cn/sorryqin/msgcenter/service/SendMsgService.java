package cn.sorryqin.msgcenter.service;

import cn.sorryqin.msgcenter.model.TemplateModel;
import cn.sorryqin.msgcenter.model.dto.SendMsgReq;

public interface SendMsgService {

    String SendMsg(SendMsgReq sendMsgReq);

}
