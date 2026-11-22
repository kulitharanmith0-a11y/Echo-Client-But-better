package dev.dov.hack.echo.client.modules.impl.internals;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;

public record StringMemoryAccess(Object unsafe, Method objectFieldOffset, Method staticFieldOffset, Method staticFieldBase, Method getObject, Method putObject) {
    private static volatile Method putIntMethod;
    private static volatile Method putBooleanMethod;
    private static volatile long stringValueOffset = Long.MIN_VALUE;
    private static volatile long stringHashOffset = Long.MIN_VALUE;
    private static volatile long stringHashZeroOffset = Long.MIN_VALUE;
    private static volatile long builderValueOffset = Long.MIN_VALUE;
    private static volatile long classLoaderClassesOffset = Long.MIN_VALUE;

    public static StringMemoryAccess create() {
        try {
            Class<?> type = Class.forName("sun.misc.Unsafe");
            Field singleton = type.getDeclaredField("theUnsafe");
            if (!singleton.trySetAccessible()) {
                return null;
            }
            Object instance = singleton.get(null);
            StringMemoryAccess access = new StringMemoryAccess(
                    instance,
                    type.getMethod("objectFieldOffset", Field.class),
                    type.getMethod("staticFieldOffset", Field.class),
                    type.getMethod("staticFieldBase", Field.class),
                    type.getMethod("getObject", Object.class, long.class),
                    type.getMethod("putObject", Object.class, long.class, Object.class));
            initializeCachedOffsets(type, access);
            return access;
        } catch (ReflectiveOperationException | RuntimeException unavailable) {
            return null;
        }
    }

    private static void initializeCachedOffsets(Class<?> unsafeType, StringMemoryAccess access) {
        if (stringValueOffset != Long.MIN_VALUE) {
            return;
        }
        try {
            putIntMethod = unsafeType.getMethod("putInt", Object.class, long.class, int.class);
        } catch (ReflectiveOperationException unavailable) {
            putIntMethod = null;
        }
        try {
            putBooleanMethod = unsafeType.getMethod("putBoolean", Object.class, long.class, boolean.class);
        } catch (ReflectiveOperationException unavailable) {
            putBooleanMethod = null;
        }
        stringValueOffset = resolveFieldOffset(access, String.class, "value");
        stringHashOffset = resolveFieldOffset(access, String.class, "hash");
        stringHashZeroOffset = resolveFieldOffset(access, String.class, "hashIsZero");
        Class<?> builderSuper = StringBuilder.class.getSuperclass();
        builderValueOffset = builderSuper == null ? -1L : resolveFieldOffset(access, builderSuper, "value");
        classLoaderClassesOffset = resolveFieldOffset(access, ClassLoader.class, "classes");
    }

    private static long resolveFieldOffset(StringMemoryAccess access, Class<?> owner, String fieldName) {
        try {
            Field field = owner.getDeclaredField(fieldName);
            return (Long) access.objectFieldOffset.invoke(access.unsafe, field);
        } catch (ReflectiveOperationException | RuntimeException unavailable) {
            return -1L;
        }
    }

    public Object readObject(Field field, Object owner) {
        if (field == null || field.getType().isPrimitive()) {
            return null;
        }
        try {
            boolean isStatic = Modifier.isStatic(field.getModifiers());
            Object base = isStatic ? staticFieldBase.invoke(unsafe, field) : owner;
            if (base == null) {
                return null;
            }
            Method offsetQuery = isStatic ? staticFieldOffset : objectFieldOffset;
            long offset = (Long) offsetQuery.invoke(unsafe, field);
            return getObject.invoke(unsafe, base, offset);
        } catch (ReflectiveOperationException | RuntimeException unavailable) {
            return null;
        }
    }

    public void clearReference(Field field, Object owner) {
        if (field == null || field.getType().isPrimitive()) {
            return;
        }
        try {
            boolean isStatic = Modifier.isStatic(field.getModifiers());
            Object base = isStatic ? staticFieldBase.invoke(unsafe, field) : owner;
            if (base == null) {
                return;
            }
            Method offsetQuery = isStatic ? staticFieldOffset : objectFieldOffset;
            long offset = (Long) offsetQuery.invoke(unsafe, field);
            putObject.invoke(unsafe, base, offset, null);
        } catch (ReflectiveOperationException | RuntimeException unavailable) {
        }
    }

    public void clearString(String string) {
        if (string == null || string.isEmpty() || stringValueOffset < 0L) {
            return;
        }
        try {
            Object storage = getObject.invoke(unsafe, string, stringValueOffset);
            zeroBackingArray(storage);
            if (stringHashOffset >= 0L && putIntMethod != null) {
                putIntMethod.invoke(unsafe, string, stringHashOffset, 0);
            }
            if (stringHashZeroOffset >= 0L && putBooleanMethod != null) {
                putBooleanMethod.invoke(unsafe, string, stringHashZeroOffset, false);
            }
        } catch (ReflectiveOperationException | RuntimeException unavailable) {
        }
    }

    public void clearBuilder(CharSequence sequence) {
        if (sequence == null) {
            return;
        }
        Object storage = null;
        if (builderValueOffset >= 0L && (sequence instanceof StringBuilder || sequence instanceof StringBuffer)) {
            try {
                storage = getObject.invoke(unsafe, sequence, builderValueOffset);
            } catch (ReflectiveOperationException | RuntimeException unavailable) {
                storage = null;
            }
        }
        if (sequence instanceof StringBuilder builder) {
            builder.setLength(0);
        } else if (sequence instanceof StringBuffer buffer) {
            buffer.setLength(0);
        }
        zeroBackingArray(storage);
    }

    public void readLoadedClasses(ClassLoader loader, Collection<Class<?>> destination) {
        if (loader == null || destination == null || classLoaderClassesOffset < 0L) {
            return;
        }
        try {
            Object rawClasses = getObject.invoke(unsafe, loader, classLoaderClassesOffset);
            if (rawClasses instanceof Collection<?> collection) {
                Object[] snapshot;
                synchronized (collection) {
                    snapshot = collection.toArray();
                }
                for (Object item : snapshot) {
                    if (item instanceof Class<?> type) {
                        destination.add(type);
                    }
                }
            }
        } catch (ReflectiveOperationException | RuntimeException unavailable) {
        }
    }

    private static void zeroBackingArray(Object storage) {
        if (storage instanceof byte[] bytes) {
            Arrays.fill(bytes, (byte) 0);
        } else if (storage instanceof char[] characters) {
            Arrays.fill(characters, (char) 0);
        }
    }
}
