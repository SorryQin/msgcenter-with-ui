package cn.sorryqin.msgcenter.consumer;

import cn.sorryqin.msgcenter.common.conf.SendMsgConf;
import cn.sorryqin.msgcenter.constant.Constants;
import cn.sorryqin.msgcenter.enums.MsgStatus;
import cn.sorryqin.msgcenter.enums.PriorityEnum;
import cn.sorryqin.msgcenter.manager.DealMsgManager;
import cn.sorryqin.msgcenter.manager.SendMsgManager;
import cn.sorryqin.msgcenter.mapper.MsgQueueMapper;
import cn.sorryqin.msgcenter.mapper.MsgRecordMapper;
import cn.sorryqin.msgcenter.model.MsgRecordModel;
import cn.sorryqin.msgcenter.model.dto.SendMsgReq;
import cn.sorryqin.msgcenter.utils.JSONUtil;
import com.alibaba.fastjson.JSON;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * kafka 模版：消费者
 **/
@Component
public class KafkaMsgConsumer {

    private static final Logger log = LoggerFactory.getLogger(KafkaMsgConsumer.class);

    @Autowired
    DealMsgManager dealMsgManager;

    @Autowired
    MsgRecordMapper msgRecordMapper;

    @Autowired
    MsgQueueMapper msgQueueMapper;

    @Autowired
    SendMsgConf sendMsgConf;

    @Autowired
    SendMsgManager  sendMsgManager;

    @KafkaListener(topics = "low-topic", groupId = "TEST_GROUP",concurrency = "1", containerFactory = "kafkaManualAckListenerContainerFactory")
    public void consumeLow(ConsumerRecord<?, ?> record, Acknowledgment ack, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        handleMQMsg(record,ack,topic);
    }

    @KafkaListener(topics = "middle-topic", groupId = "TEST_GROUP",concurrency = "3", containerFactory = "kafkaManualAckListenerContainerFactory")
    public void consumeMiddle(ConsumerRecord<?, ?> record, Acknowledgment ack, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        handleMQMsg(record,ack,topic);
    }

    @KafkaListener(topics = "high-topic", groupId = "TEST_GROUP",concurrency = "6", containerFactory = "kafkaManualAckListenerContainerFactory")
    public void consumeHigh(ConsumerRecord<?, ?> record, Acknowledgment ack, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        handleMQMsg(record,ack,topic);
    }

    @KafkaListener(topics = "retry-topic", groupId = "TEST_GROUP",concurrency = "1", containerFactory = "kafkaManualAckListenerContainerFactory")
    public void consumeRetry(ConsumerRecord<?, ?> record, Acknowledgment ack, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        handleMQMsg(record,ack,topic);
    }

    private void handleMQMsg(ConsumerRecord<?, ?> record, Acknowledgment ack, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic){
        Optional message = Optional.ofNullable(record.value());
        if (message.isPresent()) {
            Object msg = message.get();
            SendMsgReq req = null;
            try {
                // 这里写你对接收到的消息的处理逻辑
                // 走消息发送逻辑
                req = JSONUtil.parseObject(msg.toString(),SendMsgReq.class);
                dealMsgManager.DealOneMsg(req);
                log.info("Kafka消费成功! Topic:" + topic + ",Message:" + msg);
            } catch (Exception e) {
                // 判断重试
                if(req != null){
                    handleMqRetryAfterFailure(req);
                }
                e.printStackTrace();
                log.error("Kafka消费失败！Topic:" + topic + ",Message:" + msg, e);
            }finally {
                // 手动ACK
                ack.acknowledge();
            }
        }
    }



    private void handleMqRetryAfterFailure(SendMsgReq req){
        // 增加重试次数并检查是否达到上限
        MsgRecordModel mrd = msgRecordMapper.getMsgById(req.getMsgID());

        if(mrd.getRetryCount() >= sendMsgConf.getMaxRetryCount()){
            log.info("消息"+req.getMsgID()+"已达到最大重试次数，不再重试:"+ sendMsgConf.getMaxRetryCount());
            // 更新消息状态为最终失败
            msgRecordMapper.setStatus(req.getMsgID(), MsgStatus.Failed.getStatus());
            return;
        }
        // 增加重试次数
        int newCount = mrd.getRetryCount()+1;
        msgRecordMapper.incrementRetryCount(req.getMsgID(),newCount);

        // 重新发送消息到重试队列
        req.setPriority(PriorityEnum.PRIORITY_RETRY.getPriorty());
        sendMsgManager.SendToMq(req);
    }

}




