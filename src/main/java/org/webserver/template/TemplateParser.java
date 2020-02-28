package org.webserver.template;

import org.webserver.constant.TemplateConstant;
import org.webserver.exception.TemplateParseException;
import org.webserver.http.request.HttpRequest;
import org.webserver.http.response.HttpResponse;
import org.webserver.util.StringUtil;

import javax.swing.plaf.synth.SynthOptionPaneUI;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Method;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TemplateParser {
    public static final Logger logger = Logger.getLogger(TemplateParser.class.getPackageName());

    public static final String WEBAPP_ROOT_PATH =
            TemplateParser.class.getResource("/")
                    .toString().substring(6) + "webapp/"; //substring(6)去掉开头的file:/

    /**
     * Pattern 类是线程安全的不可变类。
     *
     * Pattern: ${xxx.yyy...}
     * 等价于：xxx.getYyy()...
     */
    private static final Pattern holderPattern = Pattern.compile("\\$\\{(.*)}");


    public static void parse(HttpRequest request, HttpResponse response, String path) throws TemplateParseException {
        StringBuilder sb = new StringBuilder();
        String content;
        try {
            content = Files.readString(Path.of(WEBAPP_ROOT_PATH + path));
        } catch (IOException e) {
            e.printStackTrace();
            logger.info("读取模板失败：" + e.getMessage());
            return;
        }

        parseHolder(request, content, sb);

        try {
            Writer writer = response.getWriter();
            if (sb.length() != 0) {
                writer.write(sb.toString());
            } else {
                writer.write(content);
            }
            writer.close();
        } catch (IOException ignore) {}
    }

    private static void parseHolder(HttpRequest request, String content, StringBuilder sb) throws TemplateParseException {
        Matcher matcher = holderPattern.matcher(content);
        while (matcher.find()) {
            String holder = matcher.group(1);
            int firstDotPos = holder.indexOf('.');
            if (firstDotPos == -1) { // 无效
                throw new TemplateParseException();
            }
            String scope = holder.substring(0, firstDotPos); // 作用域：request or session
            String[] keys = holder.substring(firstDotPos + 1).split("\\.");
            Object value = null;
            if (scope.equals(TemplateConstant.SCOPE_REQUEST)) {
                value = request.getAttribute(keys[0]);
            } else if(scope.equals(TemplateConstant.SCOPE_SESSION)) {
                value = request.getAttribute(keys[0]);
            }
            value = parseHolderHelper(value, keys, 1);
            // 替换
            if (value == null) {
                matcher.appendReplacement(sb, "");
            } else {
                matcher.appendReplacement(sb, value.toString());
            }
        }
        matcher.appendTail(sb); // 补上剩余内容
    }

    /**
     * 递归处理剩下级联内容
     */
    private static Object parseHolderHelper(Object value, String[] keys, int index) throws TemplateParseException {
        if (index == keys.length) { // 到头了
            return value;
        }
        Object ret;
        try {
            Method getter = value.getClass().getMethod(StringUtil.getterName(keys[index]));
            ret = getter.invoke(value);
        } catch (Exception e) {
            throw new TemplateParseException();
        }
        return parseHolderHelper(ret, keys, index + 1);
    }

    public static void main(String[] args) throws TemplateParseException, IOException {
        HttpRequest request = new HttpRequest();
        HttpResponse response = new HttpResponse();

        request.setAttribute("key", "^_^");
        String content = "this is the value: ${request.key.class}";
        StringBuilder sb = new StringBuilder();
        parse(request, response, "test.html");
    }
}
