package br.com.uol.runas.service.helper;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

@SuppressWarnings("unchecked")
public class Reflections {

    public static <E> E getStaticFieldValue(Class<?> clazz, String fieldName) {
        final Field staticField = findField(clazz, fieldName);
        return (staticField != null) ? (E) getStaticFieldValue(staticField) : null;
    }

    public static <E> E getStaticFieldValue(String className, String fieldName) {
        return (E) getStaticFieldValue(className, fieldName, false);
    }

    public static <E> E getStaticFieldValue(String className, String fieldName, boolean trySystemCL) {
        final Field staticField = findFieldOfClass(className, fieldName, trySystemCL);
        return (staticField != null) ? (E) getStaticFieldValue(staticField) : null;
    }

    public static Field findFieldOfClass(String className, String fieldName) {
        return findFieldOfClass(className, fieldName, false);
    }

    public static Field findFieldOfClass(String className, String fieldName, boolean trySystemCL) {
        final Class<?> clazz = findClass(className, trySystemCL);
        if (clazz != null) {
            return findField(clazz, fieldName);
        } else {
            return null;
        }
    }

    public static Class<?> findClass(String className) {
        return findClass(className, false);
    }

    public static Class<?> findClass(String className, boolean trySystemCL) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            if (trySystemCL) {
                try {
                    return Class.forName(className, true, ClassLoader.getSystemClassLoader());
                } catch (ClassNotFoundException e1) {
                    return null;
                }
            }

            return null;
        } catch (Exception ex) {
            return null;
        }
    }

    public static Field findField(Class<?> clazz, String fieldName) {
        if (clazz == null)
            return null;

        try {
            final Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException ex) {
            return null;
        } catch (Exception ex) {
            return null;
        }
    }

    public static <T> T getStaticFieldValue(Field field) {
        try {
            if (!Modifier.isStatic(field.getModifiers())) {
                return null;
            }

            return (T) field.get(null);
        } catch (Exception ex) {
            return null;
        }
    }

    public static <T> T getFieldValue(Object obj, String fieldName) {
        final Field field = findField(obj.getClass(), fieldName);
        return (T) getFieldValue(field, obj);
    }

    public static <T> T getFieldValue(Field field, Object obj) {
        try {
            return (T) field.get(obj);
        } catch (Exception ex) {
            return null;
        }
    }
}
