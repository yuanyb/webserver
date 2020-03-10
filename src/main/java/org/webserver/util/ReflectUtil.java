package org.webserver.util;

import java.beans.JavaBean;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;

public class ReflectUtil {
//===========是否标记了某个注解=============
    public static boolean annotatedWith(Class<?> clazz, Class<? extends Annotation> annotation) {
        return clazz.getDeclaredAnnotation(annotation) != null;
    }

    public static boolean annotatedWith(Method method, Class<? extends Annotation> annotation) {
        return method.getDeclaredAnnotation(annotation) != null;
    }

    public static boolean annotatedWith(Parameter parameter, Class<? extends Annotation> annotation) {
        return parameter.getAnnotation(annotation) != null;
    }


// ==========类型比较==========
    public static boolean typeEquals(Parameter parameter, Class<?> parameterType) {
        return parameter.getType().equals(parameterType);
    }

    public static boolean typeEquals(Class<?> type1, Class<?> type2) {
        return type1.equals(type2);
    }


    /**
     * 是否是简单类型
     */
    private static final Set<Class> simpleTypes = new HashSet<>();
    static {
        simpleTypes.addAll(Arrays.asList(String.class, int.class, Integer.class,
                Long.class, long.class, short.class, Short.class, Boolean.class,
                boolean.class, Double.class, double.class, Float.class, float.class,
                Byte.class, byte.class, Character.class, char.class));
    }
    public static boolean isNotSimpleType(Class<?> type) {
        return !simpleTypes.contains(type);
    }

    public static boolean isSimpleType(Class<?> type) {
        return simpleTypes.contains(type);
    }


    /**
     * 类型处理器，仅处理简单类型，buildBeanFromRequest方法会递归处理
     */
    private static final Map<Class, Function<String, Object>> typeHandler = new HashMap<>();
    private static final ThreadLocal<SimpleDateFormat> format = new ThreadLocal<>();
    static {
        format.set(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
        typeHandler.put(String.class, v -> v);
        typeHandler.put(int.class, Integer::valueOf);
        typeHandler.put(Integer.class, Integer::valueOf);
        typeHandler.put(long.class, Long::valueOf);
        typeHandler.put(Long.class, Long::valueOf);
        typeHandler.put(double.class, Double::valueOf);
        typeHandler.put(Double.class, Double::valueOf);
        typeHandler.put(float.class, Float::valueOf);
        typeHandler.put(Float.class, Float::valueOf);
        typeHandler.put(byte.class, Byte::valueOf);
        typeHandler.put(Byte.class, Byte::valueOf);
        typeHandler.put(char.class, str -> str.charAt(0));
        typeHandler.put(Character.class, str -> str.charAt(0));
        typeHandler.put(short.class, Short::valueOf);
        typeHandler.put(Short.class, Short::valueOf);
        typeHandler.put(boolean.class, Boolean::valueOf);
        typeHandler.put(Boolean.class, Boolean::valueOf);
        typeHandler.put(Date.class, str -> {
            try {
                return format.get().parse(str);
            } catch (ParseException e) {
                throw new RuntimeException();
            }
        });
    }

    public static Object cast(String val, Class type)  {
        try {
            return typeHandler.get(type).apply(val);
        } catch (RuntimeException e) { // 解析出错的话
            return defaultValue(type);
        }
    }

    /**
     * 空默认值
     */
    public static Object defaultValue(Class type) {
        if (typeEquals(type, String.class))
            return "";
        if(typeEquals(type, boolean.class) || typeEquals(type, Boolean.class))
            return false;
        else if(typeEquals(Date.class, type))
            return new Date();
        else
            return 0;
    }

    public static void main(String[] args) {
        System.out.println(cast("a123", Double.class));
        System.out.println(cast("ffflse", Boolean.class));
        System.out.println(cast("2017-01-02 11:11:11", Date.class));
    }
}
