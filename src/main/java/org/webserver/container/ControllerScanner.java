package org.webserver.container;

import org.webserver.container.annotation.Controller;
import org.webserver.container.annotation.RequestMapping;
import org.webserver.exception.InternalServerException;
import org.webserver.http.HttpMethod;
import org.webserver.util.IOUtil;
import org.webserver.util.ReflectUtil;

import java.io.*;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class ControllerScanner {
    private static final Logger logger = Logger.getLogger(ControllerScanner.class.getPackageName());
    private static final String CONTROLLER_ROOT_PATH =
            ControllerScanner.class.getResource("/org/webserver/controller/")
            .toString().substring(6); //substring(6)去掉开头的file://

    /**
     * 扫描 webapp 包下的控制器
     * @return
     * @throws InternalServerException
     */
    static Map<String, TargetMethod> scan() throws InternalServerException {
        logger.info("开始扫描控制器");
        Map<String, TargetMethod> map = new HashMap<>();
        try {
            IOUtil.traverseDirectory(Path.of(CONTROLLER_ROOT_PATH), path -> {
                String className = getClassName(path);
                try {
                    Class clazz = Class.forName(className);
                    // 如果标注了 @Controller注解的话
                    if (ReflectUtil.annotatedWith(clazz, Controller.class)) {
                        logger.info("扫描到控制器 " + clazz.getName());
                        // 解析 requestMapping 映射
                        parseRequestMapping(map, clazz);
                    }
                } catch (ClassNotFoundException e) {
                    logger.info("加载类时异常：" + e.getMessage());
                    e.printStackTrace();
                }
            });
        } catch (IOException e) {
            logger.info("加载类失败（IO错误）：" + e.getMessage());
            throw new InternalServerException("加载类失败（IO错误）：" + e.getMessage());
        }
        return map;
    }

    private static String getClassName(Path path) {
        return "org.webserver.controller." + path.toString()
                .substring(CONTROLLER_ROOT_PATH.length(), path.toString().lastIndexOf(".class"))
                .replaceAll("[/\\\\]", "\\\\.");
    }


    /**
     * 解析控制器类的映射方法
     * @param map
     * @param clazz
     */
    private static void parseRequestMapping(Map<String, TargetMethod> map, Class<?> clazz) {
        Method[] methods = clazz.getMethods();
        // 如果控制器类使用了@RequestMapping注解的话
        String baseRequestMapping = "";
        if (ReflectUtil.annotatedWith(clazz, RequestMapping.class)) {
            baseRequestMapping = clazz.getAnnotation(RequestMapping.class).value();
            // 注解在类上的注解，去掉尾部的 "/"
            if (baseRequestMapping.endsWith("/")) {
                baseRequestMapping = baseRequestMapping.substring(0, baseRequestMapping.length() - 1);
            }
        }
        for (Method method : methods) {
            if (ReflectUtil.annotatedWith(method, RequestMapping.class)) {
                String methodRequestMapping = method.getAnnotation(RequestMapping.class).value();
                if (!methodRequestMapping.startsWith("/")) {
                    methodRequestMapping = "/" + methodRequestMapping;
                }
                HttpMethod methodType = method.getAnnotation(RequestMapping.class).method();
                TargetMethod targetMethod = null;
                try {
                    targetMethod = new TargetMethod(clazz.newInstance(), method, methodType);
                } catch (Exception ignore) {}
                map.put(baseRequestMapping + methodRequestMapping, targetMethod);
                logger.info(String.format("发现请求映射：[ %s => %s]", baseRequestMapping + methodRequestMapping, targetMethod.getMethodDescriptor()));
            }
        }
    }


    public static void main(String[] args) throws Exception {
        Map<String, TargetMethod> map = scan();
        System.out.println(map);

    }
}
