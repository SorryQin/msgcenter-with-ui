package cn.sorryqin.msgcenter.service.impl;

import cn.sorryqin.msgcenter.common.conf.SendMsgConf;
import cn.sorryqin.msgcenter.constant.Constants;
import cn.sorryqin.msgcenter.enums.TemplateStatus;
import cn.sorryqin.msgcenter.exception.BusinessException;
import cn.sorryqin.msgcenter.exception.ErrorCode;
import cn.sorryqin.msgcenter.mapper.TemplateMapper;
import cn.sorryqin.msgcenter.model.TemplateModel;
import cn.sorryqin.msgcenter.service.TemplateService;
import cn.sorryqin.msgcenter.utils.JSONUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Service
public class TemplateServiceImpl implements TemplateService {

    private static final Logger log = LoggerFactory.getLogger(TemplateServiceImpl.class);

    @Autowired
    private TemplateMapper templateMapper;

    @Autowired
    SendMsgConf sendMsgConf;

    @Resource
    RedisTemplate<String,String> redisTemplate;

    @Override
    public String CreateTemplate(TemplateModel templateModel) {
        // 校验参数
        if(templateModel.getChannel() == 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"校验 chanenl 参数出错");
        }
        //其他参数校验，略

        // 生成模板 ID
        templateModel.setTemplateId(UUID.randomUUID().toString());
        templateModel.setRelTemplateId(UUID.randomUUID().toString());
        templateModel.setStatus(TemplateStatus.TEMPLATE_STATUS_PENDING.getStatus());

        // 存入数据库
        templateMapper.save(templateModel);
        return templateModel.getTemplateId();
    }

    @Override
    public void DeleteTemplate(String templateID) {
         templateMapper.deleteById(templateID);
    }

    @Override
    public void UpdateTemplate(TemplateModel templateModel) {
        templateMapper.update(templateModel);
    }

    @Override
    public TemplateModel GetTemplate(String templateID) {
        return templateMapper.getTemplateById(templateID);
    }

    @Override
    public TemplateModel GetTemplateWithCache(String templateID) {
        String templateCacheKey = Constants.REDIS_KEY_TEMPLATE+templateID;
        String cacheTp = redisTemplate.opsForValue().get(templateCacheKey);
        TemplateModel tp = null;
        if(!StringUtils.isEmpty(cacheTp) && sendMsgConf.isOpenCache()){
            tp = JSONUtil.parseObject(cacheTp,TemplateModel.class);
            if(tp != null){
                return tp;
            }
        }

        // 从数据库获取
        tp = templateMapper.getTemplateById(templateID);

        // 存入缓存
        redisTemplate.opsForValue().set(templateCacheKey,JSONUtil.toJsonString(tp), Duration.ofSeconds(30));

        return tp;
    }

    @Override
    public List<TemplateModel> FindAll() {
        return templateMapper.findAll();
    }
}
