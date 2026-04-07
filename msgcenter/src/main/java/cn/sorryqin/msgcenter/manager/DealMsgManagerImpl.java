package cn.sorryqin.msgcenter.manager;

import cn.sorryqin.msgcenter.enums.ChannelEnum;
import cn.sorryqin.msgcenter.enums.MsgStatus;
import cn.sorryqin.msgcenter.model.TemplateModel;
import cn.sorryqin.msgcenter.model.dto.SendMsgReq;
import cn.sorryqin.msgcenter.msgpush.MsgPushService;
import cn.sorryqin.msgcenter.msgpush.base.ChannelMsgBase;
import cn.sorryqin.msgcenter.service.TemplateService;
import cn.sorryqin.msgcenter.tools.ChannelCircuitBreakerService;
import cn.sorryqin.msgcenter.tools.MsgRecordService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@Service
public class DealMsgManagerImpl implements DealMsgManager{

    private static final Logger log = LoggerFactory.getLogger(DealMsgManagerImpl.class);

    public static Map<Integer, MsgPushService> channelStrategyMap = new HashMap<>();

    @Autowired
    MsgPushService emailServiceImpl;
    @Autowired
    MsgPushService larkServiceImpl;
    @Autowired
    MsgPushService SMSServiceImpl;

    // 初始化各种推送策略服务 Email|Lark|SMS
    @PostConstruct
    public void initChannelStrategyMap() {
        channelStrategyMap.put(ChannelEnum.Channel_EMAIL.getChannel(), emailServiceImpl);
        channelStrategyMap.put(ChannelEnum.Channel_LARK.getChannel(), larkServiceImpl);
        channelStrategyMap.put(ChannelEnum.Channel_SMS.getChannel(), SMSServiceImpl);
    }
    @Autowired
    TemplateService templateService;

    @Autowired
    MsgRecordService msgRecordService;

    @Autowired
    ChannelCircuitBreakerService channelCircuitBreakerService;

    @Override
    public void DealOneMsg(SendMsgReq sendMsgReq) {

        // 1. 查找模板
        TemplateModel tp = templateService.GetTemplateWithCache(sendMsgReq.getTemplateId());

        // 2.替换模板中的变量
        String msgContent = replaceStr(tp.getContent(),sendMsgReq.getTemplateData());

        // 3. 构建推送消息的基本参数
        ChannelMsgBase base = new ChannelMsgBase();
        base.setTo(sendMsgReq.getTo());
        base.setSubject(sendMsgReq.getSubject());
        base.setContent(msgContent);
        base.setPriority(sendMsgReq.getPriority());
        base.setTemplateId(sendMsgReq.getTemplateId());
        base.setTemplateData(sendMsgReq.getTemplateData());

        // 4. 解析实际发送渠道（熔断时降级到备用渠道）
        int effectiveChannel = channelCircuitBreakerService.resolveEffectiveChannel(tp.getChannel());
        MsgPushService msgService = channelStrategyMap.get(effectiveChannel);
        if (msgService == null) {
            log.error("no MsgPushService for channel {}", effectiveChannel);
            throw new RuntimeException("unsupported channel: " + effectiveChannel);
        }

        // 5. 调用具体策略服务去推送消息（成功/失败更新 Redis 连续计数与熔断）
        try {
            msgService.pushMsg(base);
            channelCircuitBreakerService.recordSuccess(effectiveChannel);
        } catch (Exception e) {
            channelCircuitBreakerService.recordFailure(effectiveChannel);
            throw e;
        }

        // 6. 存储消息发送记录
        try{
            msgRecordService.CreateOrUpdateMsgRecord(sendMsgReq.getMsgID(),sendMsgReq,tp, MsgStatus.Succeed);
        }catch (Exception e){
            log.error("存储消息发送记录失败， msgId",sendMsgReq.getMsgID());
        }

    }

    private String replaceStr(String template,Map<String,String> paramsMap) {
        String remark = template;
        for (Map.Entry<String, String> entry : paramsMap.entrySet()) {
            remark = StringUtils.replace(remark, "${" + entry.getKey() + "}", entry.getValue());
        }
        return remark;
    }
}
