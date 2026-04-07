package cn.sorryqin.msgcenter.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * 前端控制台页面路由
 */
@Controller
public class ConsoleController {

    @GetMapping("/")
    public String root() {
        return "redirect:/console/";
    }

    @GetMapping("/console")
    public String console() {
        return "redirect:/console/";
    }

    /**
     * 处理 /console/ 请求，直接返回 index.html 内容，
     * 避免 redirect 导致的无限循环
     */
    @GetMapping("/console/")
    @ResponseBody
    public String consoleIndex(HttpServletRequest request) throws IOException {
        ClassPathResource resource = new ClassPathResource("console/index.html");
        try {
            byte[] bytes = new byte[(int) resource.contentLength()];
            resource.getInputStream().read(bytes);
            return new String(bytes, "UTF-8");
        } catch (IOException e) {
            request.setAttribute("javax.servlet.error.status_code", 404);
            throw new RuntimeException("index.html not found", e);
        }
    }
}
