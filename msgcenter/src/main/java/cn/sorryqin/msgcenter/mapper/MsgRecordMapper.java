package cn.sorryqin.msgcenter.mapper;


import cn.sorryqin.msgcenter.model.MsgRecordModel;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MsgRecordMapper {

    void save(@Param("msgRecordModel") MsgRecordModel msgRecordModel);

    void setStatus(@Param("msgId") String msgId,@Param("status") int status);

    // 增加消息记录的重试功能
    void incrementRetryCount(@Param("msgId") String msgId,@Param("newCount") int newCount);

    MsgRecordModel getMsgById(@Param("msgId") String msgId);
}
