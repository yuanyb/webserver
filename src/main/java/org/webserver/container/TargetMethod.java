package org.webserver.container;

import org.webserver.container.annotation.CookieValue;
import org.webserver.container.annotation.RequestHeader;
import org.webserver.container.annotation.RequestParam;
import org.webserver.exception.HttpMethodNotSupportedException;
import org.webserver.exception.InternalServerException;
import org.webserver.exception.TemplateParseException;
import org.webserver.http.HttpMethod;
import org.webserver.http.request.HttpRequest;
import org.webserver.http.response.HttpResponse;
import org.webserver.http.response.HttpStatus;
import org.webserver.http.session.HttpSession;
import org.webserver.template.TemplateParser;
import org.webserver.util.ErrorResponseUtil;
import org.webserver.util.ReflectUtil;
import org.webserver.util.StringUtil;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.sql.Ref;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.RunnableFuture;
import java.util.function.Function;
import java.util.logging.Logger;

/**
 * HTTP请求映射的控制器里的处理方法
 */
public class TargetMethod {
    private static final Logger logger = Logger.getLogger(TargetMethod.class.getPackageName());
    /** 获得方法描述符，用于异常定位 pkg1.pkg2.Clazz#method(..) */
    private final String methodDescriptor;
    /** 所属的控制器 */
    private final Object controller;
    /** 被 @RequestMapping标记的处理方法 */
    private final Method method;
    /** 参数列表 */
    private final Parameter[] parameters;
    /** 方法类型 */
    private final HttpMethod httpMethodType;

    TargetMethod(Object controller, Method method, HttpMethod httpMethodType) {
        this.controller = controller;
        this.method = method;
        this.parameters = method.getParameters();
        this.methodDescriptor = method.getDeclaringClass().getName()  + "#" + method.getName();
        this.httpMethodType = httpMethodType;
    }


    /**
     * 调用对应的响应方法
     */
    public HttpResponse invoke(HttpRequest request) {
        HttpResponse response = new HttpResponse();
        try {
            // 请求方法类型不支持
            if (this.httpMethodType != HttpMethod.ANY && request.getMethod() != this.httpMethodType) {
                throw new HttpMethodNotSupportedException();
            }
            // 返回 String 或 void，表示渲染的页面路径或不使用模板
            String path = (String)method.invoke(controller, buildParameters(request, response));
            // 渲染
            if (path != null) {
                TemplateParser.parse(request, response, path);
            }
            response.setContentType("text/html; charset=utf-8");
        } catch (InternalServerException e) {
            logger.severe(e.getMessage());
            ErrorResponseUtil.renderErrorResponse(response, HttpStatus.SC_500, e.getMessage());
            e.printStackTrace();
        } catch (TemplateParseException e) {
            logger.severe("模板解析错误" + e.getMessage());
            ErrorResponseUtil.renderErrorResponse(response, HttpStatus.SC_500, "模板解析错误：" + e.getMessage());
            e.printStackTrace();
        } catch (HttpMethodNotSupportedException e) {
            ErrorResponseUtil.renderErrorResponse(response, HttpStatus.SC_405, "不支持该请求方法（"+ request.getMethod() +"）：" + e.getMessage());
        } catch (Exception ignore) {
        }
        return response;
    }

    /**
     * 构造要传给method的参数列表
     */
    private Object[] buildParameters(HttpRequest request, HttpResponse response) throws InternalServerException {
        Object[] realParameters = new Object[method.getParameterCount()];
        // 填充参数
        for (int i = 0; i < realParameters.length; i++) {
            // @RequestParam
            if (ReflectUtil.annotatedWith(parameters[i], RequestParam.class)) {
                if (ReflectUtil.isSimpleType(parameters[i].getType())) { // 简单类型的值
                    if (request.getParameter(parameters[i].getAnnotation(RequestParam.class).value()) != null) {
                        // 参数的值
                        realParameters[i] = ReflectUtil.cast(request.getParameter(
                                parameters[i].getAnnotation(RequestParam.class).value()), parameters[i].getType());
                    } else if (!parameters[i].getAnnotation(RequestParam.class).defaultValue().equals("")) {
                        // 注解提供的默认值
                        realParameters[i] = ReflectUtil.cast(parameters[i].getAnnotation(RequestParam.class).defaultValue(), parameters[i].getType());
                    } else {
                        // 空默认值
                        realParameters[i] = ReflectUtil.defaultValue(parameters[i].getType());
                    }
                } else { // Java Bean
                    realParameters[i] = buildBeanFromRequest(request, parameters[i].getType(),
                            parameters[i].getAnnotation(RequestParam.class).value() + ".");
                }
            }
            // @RequestHeader
            else if (ReflectUtil.annotatedWith(parameters[i], RequestHeader.class) &&
                    ReflectUtil.typeEquals(parameters[i].getType(), String.class)) {
                realParameters[i] = request.getParameter(
                        parameters[i].getAnnotation(RequestHeader.class).value());
            }
            // @CookieValue
            else if (ReflectUtil.annotatedWith(parameters[i], CookieValue.class) &&
                    ReflectUtil.typeEquals(parameters[i].getType(), String.class)) {
                /*realParameters[i] = request.getCookie(
                        parameters[i].getAnnotation(CookieValue.class).value());*/
                Cookie cookie = request.getCookie(parameters[i].getAnnotation(CookieValue.class).value());
                if (cookie == null) {
                    realParameters[i] = "";
                } else {
                    realParameters[i] = cookie.getValue();
                }
            }
            // HttpRequest
            else if (ReflectUtil.typeEquals(parameters[i], HttpRequest.class)) {
                realParameters[i] = request;
            }
            // HttpResponse
            else if (ReflectUtil.typeEquals(parameters[i], HttpResponse.class)) {
                realParameters[i] = response;
            }
            // HttpSession
            else if (ReflectUtil.typeEquals(parameters[i], HttpSession.class)) {
                realParameters[i] = request.getSession();
            }
            // 简单类型，且参数名在HTTP请求中有对应值
            else if (ReflectUtil.isSimpleType(parameters[i].getType())) {
                realParameters[i] = ReflectUtil.cast(
                        request.getParameter(parameters[i].getName()), parameters[i].getType());
            }
            // JavaBean，支持级联赋值
            else if (ReflectUtil.isNotSimpleType(parameters[i].getType())) {
                realParameters[i] = buildBeanFromRequest(request, parameters[i].getType(),
                        parameters[i].getName() + ".");
            }
        }
        return realParameters;
    }


    /**
     * 从Http请求中构造实体对象
     * @param prefix 级联赋值的前缀，如：user.info.val
     */
    private static Object buildBeanFromRequest(HttpRequest request, Class<?> type, String prefix) throws InternalServerException {
        try {
            Object bean = type.getConstructor().newInstance();
            Field[] fields = type.getDeclaredFields();
            for (Field field : fields) {
                field.setAccessible(true);
                String cascade = prefix + field.getName(); // 级联名，如：user.info.val.val2
                String setterName = StringUtil.setterName(field.getName());
                Method setterMethod = type.getMethod(setterName, field.getType());
                if (ReflectUtil.isNotSimpleType(field.getType())) {
                    // 递归处理
                    Object ret = buildBeanFromRequest(request, field.getType(), cascade + ".");
                    setterMethod.invoke(bean, ret);
                } else {
                    if (request.getParameter(cascade) == null) {
                        continue;
                    }
                    setterMethod.invoke(bean, ReflectUtil.cast(request.getParameter(cascade), field.getType()));
                }
            }
            return bean;
        } catch (Exception e) {
            throw new InternalServerException("实体类" + type.getName() + "不符合规范");
        }
    }


    /**
     * 获得方法描述符，用于异常定位 pkg1.pkg2.Clazz#method
     */
    String getMethodDescriptor() {
        return this.methodDescriptor;
    }

    HttpMethod getHttpMethodType() {
        return httpMethodType;
    }
}
