package cn.sorryqin.msgcenter.manager;

import cn.sorryqin.msgcenter.constant.Constants;
import cn.sorryqin.msgcenter.enums.MsgStatus;
import cn.sorryqin.msgcenter.enums.PriorityEnum;
import cn.sorryqin.msgcenter.mapper.MsgQueueMapper;
import cn.sorryqin.msgcenter.mapper.MsgQueueTimerMapper;
import cn.sorryqin.msgcenter.model.MsgQueueModel;
import cn.sorryqin.msgcenter.model.MsgQueueTimerModel;
import cn.sorryqin.msgcenter.model.dto.SendMsgReq;
import cn.sorryqin.msgcenter.redis.TimerMsgCache;
import cn.sorryqin.msgcenter.utils.JSONUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class SendMsgManagerImpl implements SendMsgManager{

    private static final Logger log = LoggerFactory.getLogger(SendMsgManagerImpl.class);

    @Autowired
    MsgQueueMapper msgQueueMapper;

    @Autowired
    MsgQueueTimerMapper msgQueueTimerMapper;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    TimerMsgCache timerMsgCache;

    @Override
    public String SendToMysql(SendMsgReq sendMsgReq) {
        // 1. 生成消息 Id; msgId
        MsgQueueModel newMsgModel = new MsgQueueModel();
        if(StringUtils.isEmpty(sendMsgReq.getMsgID())) {
            sendMsgReq.setMsgID(UUID.randomUUID().toString());
        }

        // 2.构建 Mysql 中转消息模型 MsgQueueModel
        newMsgModel.setMsgId(sendMsgReq.getMsgID());
        newMsgModel.setSubject(sendMsgReq.getSubject());
        newMsgModel.setTo(sendMsgReq.getTo());
        newMsgModel.setPriority(sendMsgReq.getPriority());
        newMsgModel.setTemplateId(sendMsgReq.getTemplateId());
        newMsgModel.setTemplateData(JSONUtil.toJsonString(sendMsgReq.getTemplateData()));
        newMsgModel.setStatus(MsgStatus.Pending.getStatus());

        // 3.根据优先级确定要存入的表的表名    low|middle|high|retry
        String tableName = Constants.TableNamePre_MsgQueue+ PriorityEnum.GetPriorityStr(sendMsgReq.getPriority());

        // 4.存入数据库
        try{
            msgQueueMapper.save(tableName,newMsgModel);
        }catch (Exception e){
            log.error("存储优先级消息失败 msgid:"+newMsgModel.getMsgId());
        }

        // 返回消息 id
        return  sendMsgReq.getMsgID();
    }

    @Override
    public String SendToMq(SendMsgReq sendMsgReq) {
        // 1. 生成 MsgID
        if(StringUtils.isEmpty(sendMsgReq.getMsgID())) {
            sendMsgReq.setMsgID(UUID.randomUUID().toString());
        }

        // 2. 序列化请求为 一条 String 消息
        String mqData = JSONUtil.toJsonString(sendMsgReq);

        // 3.根据消息优先级，确定要投递的 Topic    low-topic|middel-topic|high-topic
        String topic =PriorityEnum.GetPriorityStr(sendMsgReq.getPriority())+Constants.Topic_Tail_MsgQueue;

        //4. 发送消息到消息队列中转
        kafkaTemplate.send(topic,mqData);

        // 5. 返回消息Id
        return  sendMsgReq.getMsgID();
    }

    @Override
    public String SendToTimer(SendMsgReq sendMsgReq) {
        // 生成消息 ID
        String msgId = UUID.randomUUID().toString();
        sendMsgReq.setMsgID(msgId);

        //序列化整个请求为 String
        String mqData = JSONUtil.toJsonString(sendMsgReq);

        // 构建MsgQueueTimerModel，数据库存入的参数模型
        MsgQueueTimerModel newMsgModel = new MsgQueueTimerModel();
        newMsgModel.setMsgId(msgId);
        newMsgModel.setReq(mqData);
        newMsgModel.setSendTimestamp(sendMsgReq.getSendTimestamp());
        newMsgModel.setStatus(MsgStatus.Pending.getStatus());

        // 存入数据库
        msgQueueTimerMapper.save(newMsgModel);

        // 时间点，存入 ZSET；
        timerMsgCache.cacheSaveMsgTimePoint(newMsgModel.getSendTimestamp());

        return msgId;
    }

}
