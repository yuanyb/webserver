package org.webserver.controller;

import org.webserver.container.annotation.Controller;
import org.webserver.container.annotation.RequestMapping;
import org.webserver.container.annotation.RequestParam;
import org.webserver.http.Cookie;
import org.webserver.http.request.HttpRequest;
import org.webserver.http.response.HttpResponse;
import org.webserver.http.session.HttpSession;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Controller
public class EchoController {

    @RequestMapping("/echo")
    public String a(HttpRequest request, @RequestParam(value = "msg", defaultValue = "输入为空") String msg) {
        LocalDateTime localDateTime = LocalDateTime.ofEpochSecond(request.getSession().getLastAccessedTime() / 1000, 0, ZoneOffset.ofHours(8));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        request.setAttribute("lastAccessedTime", localDateTime.format(formatter));
        request.setAttribute("msg", msg);
        return "test.html";
    }
}
