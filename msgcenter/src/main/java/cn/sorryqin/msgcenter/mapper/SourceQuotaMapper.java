package cn.sorryqin.msgcenter.mapper;


import cn.sorryqin.msgcenter.model.GlobalQuotaModel;
import cn.sorryqin.msgcenter.model.SourceQuotaModel;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SourceQuotaMapper {

    void save(@Param("sourceQuotaModel") SourceQuotaModel sourceQuotaModel);

    SourceQuotaModel getSourceQuota(@Param("channel") int channel,@Param("sourceId") String sourceId);
}
