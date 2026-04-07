package cn.sorryqin.msgcenter.service;

import cn.sorryqin.msgcenter.model.TemplateModel;

import java.util.List;

public interface TemplateService {

    String CreateTemplate(TemplateModel templateModel);

    void DeleteTemplate(String templateID);

    void UpdateTemplate(TemplateModel templateModel);

    TemplateModel GetTemplate(String templateID);

    TemplateModel GetTemplateWithCache(String templateID);

    List<TemplateModel> FindAll();
}
