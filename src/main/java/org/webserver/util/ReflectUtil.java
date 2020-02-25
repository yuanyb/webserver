package org.webserver.util;

import java.beans.JavaBean;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

public class ReflectUtil {
    public static boolean annotatedWith(Class<?> clazz, Class<? extends Annotation> annotation) {
        return clazz.getDeclaredAnnotation(annotation) != null;
    }

    public static boolean annotatedWith(Method method, Class<? extends Annotation> annotation) {
        return method.getDeclaredAnnotation(annotation) != null;
    }

    public static boolean annotatedWith(Parameter parameter, Class<? extends Annotation> annotation) {
        return parameter.getAnnotation(annotation) != null;
    }


    public static boolean typeEquals(Parameter parameter, Class<?> parameterType) {
        return parameter.getType().equals(parameterType);
    }

    public static boolean typeEquals(Class<?> type1, Class<?> type2) {
        return type1.equals(type2);
    }

    public static boolean isNotBasicType(Class<?> type) {
        return !(type.equals(int.class) ||
                type.equals(long.class) ||
                type.equals(byte.class) ||
                type.equals(short.class) ||
                type.equals(double.class) ||
                type.equals(float.class) ||
                type.equals(boolean.class)
                );
    }
}
