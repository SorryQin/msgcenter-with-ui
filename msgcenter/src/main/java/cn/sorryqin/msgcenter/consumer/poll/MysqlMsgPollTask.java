package cn.sorryqin.msgcenter.consumer.poll;

import cn.sorryqin.msgcenter.common.conf.SendMsgConf;
import cn.sorryqin.msgcenter.constant.Constants;
import cn.sorryqin.msgcenter.enums.MsgStatus;
import cn.sorryqin.msgcenter.enums.PriorityEnum;
import cn.sorryqin.msgcenter.manager.DealMsgManager;
import cn.sorryqin.msgcenter.manager.SendMsgManager;
import cn.sorryqin.msgcenter.mapper.MsgQueueMapper;
import cn.sorryqin.msgcenter.mapper.MsgRecordMapper;
import cn.sorryqin.msgcenter.model.MsgQueueModel;
import cn.sorryqin.msgcenter.model.MsgRecordModel;
import cn.sorryqin.msgcenter.model.dto.SendMsgReq;
import cn.sorryqin.msgcenter.utils.JSONUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class MysqlMsgPollTask {

    private static final Logger log = LoggerFactory.getLogger(MysqlMsgPollTask.class);

    @Autowired
    DealMsgManager dealMsgManager;

    @Autowired
    MsgQueueMapper msgQueueMapper;

    @Autowired
    MsgRecordMapper msgRecordMapper;

    @Autowired
    SendMsgConf sendMsgConf;

    @Autowired
    SendMsgManager sendMsgManager;

    @Async("mysqlMsgDealPoll")
    public void asyncHandleMsg(SendMsgReq req) {

        String tableName = Constants.TableNamePre_MsgQueue+ PriorityEnum.GetPriorityStr(req.getPriority());

        // 走消息发送逻辑
        try{
            dealMsgManager.DealOneMsg(req);
            // 发送成功
            msgQueueMapper.setStatus(tableName,req.getMsgID(),MsgStatus.Succeed.getStatus());
        }catch (Exception e){
            if(req.getPriority() != PriorityEnum.PRIORITY_RETRY.getPriorty()){
                msgQueueMapper.setStatus(tableName,req.getMsgID(),MsgStatus.Failed.getStatus());
            }
            // 走重试队列
            dealRetryMysqlQueue(req);
        }
    }

    private void dealRetryMysqlQueue(SendMsgReq req){
        // 增加重试次数并检查是否达到上限
        MsgRecordModel mrd = msgRecordMapper.getMsgById(req.getMsgID());
        String retryTableName = Constants.TableNamePre_MsgQueue+ PriorityEnum.GetPriorityStr(PriorityEnum.PRIORITY_RETRY.getPriorty());

        //检查重试次数是否到达上线

        if(mrd.getRetryCount() > 0 && mrd.getRetryCount() >= sendMsgConf.getMaxRetryCount()){
            log.info("消息"+req.getMsgID()+"已达到最大重试次数，不再重试:"+ sendMsgConf.getMaxRetryCount());
            // 更新【消息记录】状态为最终失败
            msgRecordMapper.setStatus(req.getMsgID(), MsgStatus.Failed.getStatus());
            // 更新重试队列状态为最终失败
            msgQueueMapper.setStatus( retryTableName, req.getMsgID(), MsgStatus.Failed.getStatus());
            return;
        }
        // 重试次数+1
        int newCount = mrd.getRetryCount()+1;
        msgRecordMapper.incrementRetryCount(req.getMsgID(),newCount);

        // 判断重试队列表中是否已经存在
        MsgQueueModel msgQueueModel = msgQueueMapper.getMsgById(retryTableName,req.getMsgID());
        if(msgQueueModel == null){
            // 重新发送消息到重试队列
            req.setPriority(PriorityEnum.PRIORITY_RETRY.getPriorty());
            sendMsgManager.SendToMysql(req);
        }else{
            msgQueueMapper.setStatus( retryTableName, req.getMsgID(), MsgStatus.Pending.getStatus());
        }

        log.info("消息"+req.getMsgID()+"已加入MySQL重试队列，当前重试次数:", newCount);
    }
}
