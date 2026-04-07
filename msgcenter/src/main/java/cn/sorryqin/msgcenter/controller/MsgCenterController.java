package cn.sorryqin.msgcenter.controller;

import cn.sorryqin.common.model.BaseModel;
import cn.sorryqin.common.model.ResponseEntity;
import cn.sorryqin.msgcenter.model.TemplateModel;
import cn.sorryqin.msgcenter.model.dto.SendMsgReq;
import cn.sorryqin.msgcenter.tools.MsgRecordService;
import cn.sorryqin.msgcenter.service.SendMsgService;
import cn.sorryqin.msgcenter.service.TemplateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * web服务接口：http 接口
 **/

@RestController
@RequestMapping("/msg")
public class MsgCenterController {

    private static final Logger log = LoggerFactory.getLogger(MsgCenterController.class);

    @Resource
    private TemplateService templateService;

    @Resource
    private SendMsgService sendMsgService;

    @Resource
    private MsgRecordService msgRecordService;

    @PostMapping(value = "/create_template")
    public ResponseEntity<String> createTemplate(@RequestBody TemplateModel templateModel){
        System.out.println("【接收到的模板数据】: " + templateModel.toString());

        try {
            String templateId = templateService.CreateTemplate(templateModel);
            return ResponseEntity.ok(templateId);
        } catch (Exception e) {
            // 👇 打印具体错误
            System.err.println("【创建模板失败】");
            e.printStackTrace(); // 这会输出红色异常栈
            throw e; // 交给全局异常处理器
        }
//        String templateId = templateService.CreateTemplate(templateModel);
//        return ResponseEntity.ok(templateId);
    }

    @GetMapping(value = "/get_template")
    public ResponseEntity<TemplateModel> getTemplate(@RequestParam(value = "templateId") String templateId){
        TemplateModel templateModel= templateService.GetTemplateWithCache(templateId);
        return ResponseEntity.ok(templateModel);
    }

    @PostMapping(value = "/update_template")
    public ResponseEntity<Void> updateTemplate(@RequestBody TemplateModel templateModel){
        templateService.UpdateTemplate(templateModel);
        return ResponseEntity.ok();
    }

    @PostMapping(value = "/del_template")
    public ResponseEntity<Void> delTemplate(@RequestParam(value = "templateId") String templateId){
        templateService.DeleteTemplate(templateId);
        return ResponseEntity.ok();
    }

    @PostMapping(value = "/send_msg")
    public ResponseEntity<String> send_msg(@RequestBody SendMsgReq sendMsgReq){
        String msgId = sendMsgService.SendMsg(sendMsgReq);
        return ResponseEntity.ok(msgId);
    }

    @GetMapping(value = "/get_msg_record")
    public ResponseEntity<BaseModel> getMsgRecord(@RequestParam(value = "msgId") String msgId){
        BaseModel msgRecordModel= msgRecordService.GetMsgRecordWithCache(msgId);
        return ResponseEntity.ok(msgRecordModel);
    }

    @GetMapping(value = "/find_all_template")
    public ResponseEntity<List<TemplateModel>> findAllTemplate(){
        List<TemplateModel> list = templateService.FindAll();
        return ResponseEntity.ok(list);
    }
}
