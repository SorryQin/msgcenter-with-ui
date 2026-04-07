package cn.sorryqin.msgcenter.common.conf;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /**
     * 静态资源映射：
     *   /console/** → classpath:/console/ (即 resources/console/ 目录)
     *
     * 启动后访问 http://localhost:8082/console/ 即可使用前端控制台。
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 映射 /console/** 到 classpath:/console/ 下的静态资源
        registry.addResourceHandler("/console/**")
                .addResourceLocations("classpath:/console/");
    }
}
