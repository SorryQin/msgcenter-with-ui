package cn.sorryqin.msgcenter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"cn.sorryqin.msgcenter", "cn.sorryqin.common"})
@EnableScheduling
public class MsgCennterApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsgCennterApplication.class, args);
    }

}
