package cn.sorryqin.msgcenter.consumer.poll;

import cn.sorryqin.msgcenter.common.conf.SendMsgConf;
import cn.sorryqin.msgcenter.enums.MsgStatus;
import cn.sorryqin.msgcenter.enums.TemplateStatus;
import cn.sorryqin.msgcenter.exception.BusinessException;
import cn.sorryqin.msgcenter.exception.ErrorCode;
import cn.sorryqin.msgcenter.manager.SendMsgManager;
import cn.sorryqin.msgcenter.mapper.MsgQueueTimerMapper;
import cn.sorryqin.msgcenter.model.TemplateModel;
import cn.sorryqin.msgcenter.model.dto.SendMsgReq;
import cn.sorryqin.msgcenter.service.TemplateService;
import cn.sorryqin.msgcenter.tools.MsgRecordService;
import cn.sorryqin.msgcenter.utils.JSONUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class TimerMsgResendPollTask {

    private static final Logger log = LoggerFactory.getLogger(TimerMsgResendPollTask.class);

    @Autowired
    MsgQueueTimerMapper msgQueueTimerMapper;


    @Autowired
    SendMsgManager sendMsgManager;

    @Autowired
    SendMsgConf sendMsgConf;

    @Autowired
    MsgRecordService msgRecordService;

    @Autowired
    TemplateService templateService;

    @Async("timerMsgPoll")
    public void asyncHandleMsg(String  reqStr) {
        SendMsgReq sendMsgReq = JSONUtil.parseObject(reqStr,SendMsgReq.class);
        if (sendMsgReq == null){
            return;
        }
        TemplateModel tp = templateService.GetTemplateWithCache(sendMsgReq.getTemplateId());
        if(tp.getStatus() != TemplateStatus.TEMPLATE_STATUS_NORMAL.getStatus()){
            throw new BusinessException(ErrorCode.TEMPLATE_STATUS_ERROR, "模板尚未准备好，检查模板状态");
        }
        boolean success = false;
        try {
            if(sendMsgConf.isMysqlAsMq()){
                // 发送到 Mysql
                sendMsgManager.SendToMysql(sendMsgReq);
            }else{
                // 发送到 MQ
                sendMsgManager.SendToMq(sendMsgReq);
            }
            success = true;
        }catch (Exception e){
            // 重试一次
            if(sendMsgConf.isMysqlAsMq()){
                // 发送到 Mysql
                sendMsgManager.SendToMysql(sendMsgReq);
            }else{
                // 发送到 MQ
                sendMsgManager.SendToMq(sendMsgReq);
            }
            success = true;
        }

        // 2.更新消息记录状态
        if (success) {
            msgRecordService.CreateOrUpdateMsgRecord(sendMsgReq.getMsgID(),sendMsgReq,tp,MsgStatus.Pending);
        }else{
            msgRecordService.CreateOrUpdateMsgRecord(sendMsgReq.getMsgID(),sendMsgReq,tp,MsgStatus.Failed);
        }

        // 将msgId消息变为处理中Succeed
        msgQueueTimerMapper.setStatus(sendMsgReq.getMsgID(), MsgStatus.Succeed.getStatus());
    }
}
