package com.heimdall.redis.cache.spring.boot.starter;

import com.google.common.base.CaseFormat;
import com.heimdall.redis.cache.core.KeyFormat;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author crh
 * @date 2019-06-11
 * @description
 */
public class BeanUtils {

    /**
     * 是否是数字
     *
     * @param clazz
     * @param <T>
     * @return
     */
    public static <T> boolean isNumber(Class<T> clazz) {
        if (clazz == null) {
            return false;
        }
        if (byte.class == clazz || short.class == clazz || int.class == clazz || long.class == clazz || float.class == clazz || double.class == clazz) {
            return true;
        }
        return Number.class.equals(clazz.getSuperclass());
    }

    /**
     * 判断一个对象是否是基本类型或基本类型的包装类型
     *
     * @param obj
     * @return
     */
    public static boolean isPrimitive(Object obj) {
        try {
            return ((Class<?>) obj
                    .getClass()
                    .getField("TYPE")
                    .get(null)).isPrimitive();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取类所有字段（包括父类，不包括子类）
     *
     * @param clazz
     * @return
     */
    public static List<Field> getAllFields(Class<?> clazz) {
        if (null == clazz) {
            return Collections.emptyList();
        }
        List<Field> list = new ArrayList<>();
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            // 过滤静态属性
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            // 过滤 transient 关键字修饰的属性
            if (Modifier.isTransient(field.getModifiers())) {
                continue;
            }
            list.add(field);
        }
        // 获取父类字段
        Class<?> superClass = clazz.getSuperclass();
        if (Object.class.equals(superClass)) {
            return list;
        }
        list.addAll(getAllFields(superClass));
        return list;
    }

    /**
     * 类名格式转换
     *
     * @param str
     * @param keyFormat
     * @return
     */
    public static String formatKey(String str, KeyFormat keyFormat) {
        switch (keyFormat) {
            case HYPHEN:
                return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_HYPHEN, str);
            case CAMEL:
                return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, str);
            case UNDERLINE:
            default:
                return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, str);
        }
    }

    /**
     * 获取集合泛型类
     *
     * @param methodSignature
     * @return
     */
    public static Type[] getMethodGenericClass(MethodSignature methodSignature) {
        Type genericReturnType = methodSignature.getMethod().getGenericReturnType();
        if (genericReturnType instanceof ParameterizedType) {
            return ((ParameterizedType) genericReturnType).getActualTypeArguments();
        }
        return new Type[0];
    }

    @SuppressWarnings("unchecked")
    public static <E> E[] toArray(Object[] elementData, Class<E> type) {
        Object copy = Array.newInstance(type, elementData.length);
        System.arraycopy(elementData, 0, copy, 0, elementData.length);
        return (E[]) copy;
    }

}
