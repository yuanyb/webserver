package org.webserver.container;

import org.webserver.container.annotations.CookieValue;
import org.webserver.container.annotations.RequestHeader;
import org.webserver.container.annotations.RequestParam;
import org.webserver.exception.InternalServerException;
import org.webserver.http.request.HttpRequest;
import org.webserver.http.response.HttpResponse;
import org.webserver.http.response.HttpStatus;
import org.webserver.http.session.HttpSession;
import org.webserver.util.ErrorResponseUtil;
import org.webserver.util.ReflectUtil;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.logging.Logger;

/**
 * 请求对应的目标处理方法
 */
public class TargetMethod {
    private static final Logger logger = Logger.getLogger(TargetMethod.class.getPackageName());
    private final String methodDescriptor; // 方法描述符，用于异常定位
    private final Method method; // 对应的处理方法
    private final Parameter[] parameters;

    TargetMethod(Method method) {
        this.method = method;
        this.parameters = method.getParameters();
        this.methodDescriptor = method.getDeclaringClass().getName()  + "#" + method.getName();
    }

    /**
     * 调用对应的响应方法
     * @param request
     * @return
     */
    public HttpResponse invoke(HttpRequest request) {
        HttpResponse response = new HttpResponse();
        try {
            // 返回值为 String，表示渲染的页面路径
            String path = (String)method.invoke(request, buildParameters(request, response));
            response.setResponsePath(path);
        } catch (InternalServerException e) {
            logger.severe(e.getMessage());
            ErrorResponseUtil.renderErrorResponse(response, HttpStatus.SC_500, e.getMessage());
        } catch (Exception ignore) {
        }
        return response;
    }

    /**
     * 构造要传给method的参数列表
     * @return
     */
    private Object[] buildParameters(HttpRequest request, HttpResponse response) throws InternalServerException {
        Object[] realParameters = new Object[method.getParameterCount()];
        // 填充参数
        for (int i = 0; i < realParameters.length; i++) {
            if (ReflectUtil.annotatedWith(parameters[i], RequestParam.class)) {
                if (!parameters[i].getType().equals(String.class)) {
                    throw new InternalServerException("控制器编写错误，@RequestParam注解没有标记到String类型的参数上；方法：" + methodDescriptor);
                }
                realParameters[i] = request.getParameter(
                        parameters[i].getAnnotation(RequestParam.class).value());
            }
            else if (ReflectUtil.annotatedWith(parameters[i], RequestHeader.class)) {
                if (!parameters[i].getType().equals(String.class)) {
                    throw new InternalServerException("控制器编写错误，@RequestHeader注解没有标记到String类型的参数上；方法：" + methodDescriptor);
                }
                realParameters[i] = request.getParameter(
                        parameters[i].getAnnotation(RequestHeader.class).value());
            }
            else if (ReflectUtil.annotatedWith(parameters[i], CookieValue.class)) {
                if (!parameters[i].getType().equals(String.class)) {
                    throw new InternalServerException("控制器编写错误，@CookieValue注解没有标记到String类型的参数上；方法：" + methodDescriptor);
                }
                realParameters[i] = request.getCookie
                        (parameters[i].getAnnotation(CookieValue.class).value());
            }
            else if (ReflectUtil.typeEquals(parameters[i], HttpRequest.class)) {
                realParameters[i] = request;
            }
            else if (ReflectUtil.typeEquals(parameters[i], HttpResponse.class)) {
                realParameters[i] = response;
            }
            else if (ReflectUtil.typeEquals(parameters[i], HttpSession.class)) {
                realParameters[i] = request.getSession();
            }
            else if (ReflectUtil.isNotBasicType(parameters[i].getType())) {
                realParameters[i] = buildBeanFromRequest(request, parameters[i].getType());
            }
        }
        return realParameters;
    }

    /**
     * 从Http请求中构造实体对象
     */
    private static <T> T buildBeanFromRequest(HttpRequest request, Class<T> type) throws InternalServerException {
        try {
            T bean = type.getConstructor().newInstance();
            Field[] fields = type.getDeclaredFields();
            for (Field field : fields) {
                if (request.getParameter(field.getName()) == null) {
                    continue;
                }
                String setterName = "set" + Character.toUpperCase(field.getName().charAt(0))
                        + field.getName().substring(1);
                Method method = type.getMethod(setterName, field.getType());
                // 简单实现，仅支持集中简单的类型
                method.invoke(bean, cast(request.getParameter(field.getName()), field.getType()));
            }
            return bean;
        } catch (Exception e) {
            throw new InternalServerException("实体类" + type.getName() + "不符合规范");
        }
    }

    /**
     * 类型处理器，简单实现，仅支持几种简单的类型
     */
    private static <T> T cast(String val, Class<T> type) throws Exception {
        if (ReflectUtil.typeEquals(String.class, type)) {
            return type.cast(val);
        } else if (ReflectUtil.typeEquals(Integer.class, type) || ReflectUtil.typeEquals(int.class, type)) {
            return type.cast(Integer.parseInt(val));
        } else if (ReflectUtil.typeEquals(Double.class, type) || ReflectUtil.typeEquals(double.class, type)) {
            return type.cast(Double.parseDouble(val));
        } else if (ReflectUtil.typeEquals(Float.class, type) || ReflectUtil.typeEquals(float.class, type)) {
            return type.cast(Float.parseFloat(val));
        } else if (ReflectUtil.typeEquals(Boolean.class, type) || ReflectUtil.typeEquals(boolean.class, type)) {
            return type.cast(Boolean.parseBoolean(val));
        } else if (ReflectUtil.typeEquals(Long.class, type) || ReflectUtil.typeEquals(long.class, type)) {
            return type.cast(Long.parseLong(val));
        }
        try {
            return type.getConstructor().newInstance();
        } catch (Exception e) {
            throw e;
        }
    }
}
