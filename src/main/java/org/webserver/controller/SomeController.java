package org.webserver.controller;

import org.webserver.container.annotation.Controller;
import org.webserver.container.annotation.RequestMapping;
import org.webserver.container.annotation.RequestParam;
import org.webserver.http.Cookie;
import org.webserver.http.request.HttpRequest;
import org.webserver.http.response.HttpResponse;

@Controller
@RequestMapping("/user/")
public class SomeController {

    @RequestMapping("/login")
    public String a(HttpRequest request, HttpResponse response, @RequestParam("msg") String msg) {
        request.setAttribute("msg", msg);
        response.addHeader("Server", "MyWebServer");
        return "test.html";
    }

    @RequestMapping("logout")
    public void b() {

    }
}
