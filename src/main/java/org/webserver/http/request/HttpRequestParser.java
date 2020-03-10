package org.webserver.http.request;

import org.webserver.connector.SocketWrapper;
import org.webserver.constant.HttpConstant;
import org.webserver.container.Container;
import org.webserver.http.Cookie;
import org.webserver.http.HttpMethod;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class HttpRequestParser {
    public static HttpRequest parseRequest(SocketWrapper socketWrapper, Container container) {
        // 应该逐字节判断？长连接需要根据Content-Length，判断请求结束，为了方便就用Scanner了
        HttpRequest request = new HttpRequest();
        Scanner in = new Scanner(socketWrapper.getClient());
        parseReqLine(in, request);
        parseParams(in, request);
        parseHeaders(in, request);
        parseCookies(in, request);
        parseSession(container, request);
        parseBody(in, request);
        return request;
    }

    private static void parseReqLine(Scanner in, HttpRequest request) {
        String line = URLDecoder.decode(in.nextLine(), StandardCharsets.UTF_8);
        String[] sa = line.split(" ");
        request.setMethod(HttpMethod.get(sa[0]));
        request.setRequestURI(sa[1]);
    }

    private static void parseParams(Scanner in, HttpRequest request) {
        Map<String, List<String>> params = new HashMap<>();
        if (request.getRequestURI().indexOf('?') == -1) {
            request.setParams(params);
            return;
        }
        String[] sa, kv;
        sa = request.getRequestURI().substring(request.getRequestURI().indexOf('?') + 1).split("&");
        for (String param : sa) {
            kv = param.split("=");
            params.putIfAbsent(kv[0], new ArrayList<>(1));
            params.get(kv[0]).add(kv[1]);
        }
        request.setParams(params);
    }

    private static void parseHeaders(Scanner in, HttpRequest request) {
        Map<String, String> headers = new HashMap<>();
        String line;
        while (!(line = in.nextLine()).equals("")) {
            String[] h = line.split(":");
            headers.put(h[0], h[1].trim());
        }
        request.setHeaders(headers);
    }

    private static void parseCookies(Scanner in, HttpRequest request) {
        String cookieStr = request.getHeader(HttpConstant.COOKIE);
        if (cookieStr == null) {
            request.setCookies(new HashMap<>());
            return;
        }
        String[] sa = cookieStr.split(";"), kv;
        Map<String, Cookie> cookies = new HashMap<>();
        for (String s : sa) {
            kv = s.split("=");
            cookies.put(kv[0].trim(), new Cookie(kv[0].trim(), kv[1].trim()));
        }
        request.setCookies(cookies);
    }

    private static void parseSession(Container container, HttpRequest request) {
        Cookie jsessionid = request.getCookie(HttpConstant.JSESSIONID);
        if (jsessionid != null) {
            if (container.getSession(jsessionid.getValue()) != null) { // session没被销毁
                request.setSession(container.getSession(jsessionid.getValue()));
            } else { // 被销毁了，重新创建
                request.setSession(container.createNewSession());
            }
        } else { // 无则创建
            request.setSession(container.createNewSession());
        }
    }


    private static void parseBody(Scanner in, HttpRequest request) {
        if (!request.getMethod().equals(HttpMethod.POST))
            return;
        if (request.getHeader(HttpConstant.CONTENT_TYPE).equals(HttpConstant.POST_COMMIT_FORM)) {
            String[] sa, kv;
            sa = URLDecoder.decode(in.nextLine(), StandardCharsets.UTF_8).split("&");
            for (String param : sa) {
                kv = param.split("=");
                request.addParam(kv[0], kv[1]);
            }
        }
//        } else if (false && headers.get(HttpConstant.CONTENT_TYPE).equals(HttpConstant.POST_UPLOAD_FILE)) {
//            // 上传文件
//        }
    }

    public static void main(String[] args) {
//        String req = "POST /s?ie=utf-8&word=123%E5%93%88%E5%93%88 HTTP/1.1\r\n" +
//            "Host: www.baidu.com\r\n" +
//            "Connection: keep-alive\r\n" +
//            "Upgrade-Insecure-Requests: 1\r\n" +
//            "Cookie: a=b; c=3; fuck=123\r\n" +
//            "Content-Type: application/x-www-form-urlencoded\r\n" +
//            "User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/79.0.3945.130 Safari/537.36\r\n" +
//            "\r\n" +
//            "post=ppp&ok=ok";
//        HttpRequest res = parseRequest(null, req.getBytes());
//        System.out.println(res);
    }
}
