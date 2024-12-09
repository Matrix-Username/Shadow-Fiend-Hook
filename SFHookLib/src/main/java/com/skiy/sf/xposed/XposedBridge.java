/*
 * This file is part of AliuHook, a library providing XposedAPI bindings to LSPlant
 * Copyright (c) 2021 Juby210 & Vendicated
 * Licensed under the Open Software License version 3.0
 *
 * Originally written by rovo89 as part of the original Xposed
 * Copyright 2013 rovo89, Tungstwenty
 * Licensed under the Apache License, Version 2.0, see http://www.apache.org/licenses/LICENSE-2.0
 */

package com.skiy.sf.xposed;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.lang.reflect.*;
import java.util.*;

public class XposedBridge {
    private static final String TAG = "AliuHook-XposedBridge";

    static {
        try {
            callbackMethod = XposedBridge.HookInfo.class.getMethod("callback", Object[].class);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to initialize", t);
        }

        String dirPath = new File(getContextReflectively().getFilesDir().getAbsolutePath() + "/neip").getAbsolutePath();

        System.load(dirPath + "/libc++_shared.so");
        System.load(dirPath + "/liblsplant.so");
        System.load(dirPath + "/libaliuhook.so");
    }

    private static final Object[] EMPTY_ARRAY = new Object[0];

    private static final Map<Member, HookInfo> hookRecords = new HashMap<>();
    private static final Method callbackMethod;

    private static native Method hook0(Object context, Member original, Method callback);

    private static native boolean unhook0(Member target);

    private static native boolean deoptimize0(Member target);

    private static native boolean makeClassInheritable0(Class<?> target);

    private static native Object allocateInstance0(Class<?> clazz);

    private static native boolean invokeConstructor0(Object instance, Constructor<?> constructor, Object[] args);

    private static native boolean isHooked0(Member target);

    public static native boolean disableProfileSaver();

    public static native boolean disableHiddenApiRestrictions();

    private static void checkMethod(Member method) {
        if (method == null)
            throw new NullPointerException("method must not be null");
        if (!(method instanceof Method || method instanceof Constructor<?>))
            throw new IllegalArgumentException("method must be a Method or Constructor");

        var modifiers = method.getModifiers();
        if (Modifier.isAbstract(modifiers))
            throw new IllegalArgumentException("method must not be abstract");
    }

    public static boolean isHooked(Member method) {
        return hookRecords.containsKey(method);
    }

    public static boolean makeClassInheritable(Class<?> clazz) {
        if (clazz == null) throw new NullPointerException("class must not be null");

        return makeClassInheritable0(clazz);
    }

    public static boolean deoptimizeMethod(Member method) {
        checkMethod(method);
        return deoptimize0(method);
    }

    public static XC_MethodHook.Unhook hookMethod(Member method, XC_MethodHook callback) {
        checkMethod(method);
        if (callback == null) throw new NullPointerException("callback must not be null");

        HookInfo hookRecord;
        synchronized (hookRecords) {
            hookRecord = hookRecords.get(method);
            if (hookRecord == null) {
                hookRecord = new HookInfo(method);
                var backup = hook0(hookRecord, method, callbackMethod);
                if (backup == null) throw new IllegalStateException("Failed to hook method");
                hookRecord.backup = backup;
                hookRecords.put(method, hookRecord);
            }
        }

        hookRecord.callbacks.add(callback);

        return callback.new Unhook(method);
    }

    public static Set<XC_MethodHook.Unhook> hookAllMethods(Class<?> hookClass, String methodName, XC_MethodHook callback) {
        Set<XC_MethodHook.Unhook> unhooks = new HashSet<>();
        for (Member method : hookClass.getDeclaredMethods())
            if (method.getName().equals(methodName))
                unhooks.add(hookMethod(method, callback));
        return unhooks;
    }

    public static Set<XC_MethodHook.Unhook> hookAllConstructors(Class<?> hookClass, XC_MethodHook callback) {
        Set<XC_MethodHook.Unhook> unhooks = new HashSet<>();
        for (Member constructor : hookClass.getDeclaredConstructors())
            unhooks.add(hookMethod(constructor, callback));
        return unhooks;
    }

    @Deprecated
    public static void unhookMethod(Member method, XC_MethodHook callback) {
        synchronized (hookRecords) {
            var record = hookRecords.get(method);
            if (record != null) {
                record.callbacks.remove(callback);
                if (record.callbacks.size() == 0) {
                    hookRecords.remove(method);
                    unhook0(method);
                }
            }
        }
    }

    public static Object invokeOriginalMethod(Member method, Object thisObject, Object[] args)
            throws NullPointerException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        if (args == null)
            args = EMPTY_ARRAY;

        var hookRecord = hookRecords.get(method);
        try {
            // Checking method is not needed if we found hookRecord
            if (hookRecord != null)
                return invokeMethod(hookRecord.backup, thisObject, args);

            checkMethod(method);
            return invokeMethod(method, thisObject, args);
        } catch (InstantiationException ex) {
            // This should never be reached
            throw new IllegalArgumentException("The class this Constructor belongs to is abstract and cannot be instantiated");
        }
    }

    private static Object invokeMethod(Member member, Object thisObject, Object[] args) throws IllegalAccessException, InvocationTargetException, InstantiationException {
        if (member instanceof Method) {
            var method = (Method) member;
            method.setAccessible(true);
            return method.invoke(thisObject, args);
        } else {
            var ctor = (Constructor<?>) member;
            ctor.setAccessible(true);
            return ctor.newInstance(args);
        }
    }

    public static <T> T allocateInstance(Class<T> clazz) {
        Objects.requireNonNull(clazz);
        return (T) allocateInstance0(clazz);
    }

    public static <S, T extends S> boolean invokeConstructor(T instance, Constructor<S> constructor, Object... args) {
        Objects.requireNonNull(instance);
        Objects.requireNonNull(constructor);
        if (constructor.isVarArgs()) throw new IllegalArgumentException("varargs parameters are not supported");
        if (args.length == 0) args = null;
        return invokeConstructor0(instance, constructor, args);
    }

    public static final class CopyOnWriteSortedSet<E> {
        private transient volatile Object[] elements = EMPTY_ARRAY;

        // Aliucord added
        public int size() {
            return elements.length;
        }

        @SuppressWarnings("UnusedReturnValue")
        public synchronized boolean add(E e) {
            int index = indexOf(e);
            if (index >= 0)
                return false;

            Object[] newElements = new Object[elements.length + 1];
            System.arraycopy(elements, 0, newElements, 0, elements.length);
            newElements[elements.length] = e;
            Arrays.sort(newElements);
            elements = newElements;
            return true;
        }

        @SuppressWarnings("UnusedReturnValue")
        public synchronized boolean remove(E e) {
            int index = indexOf(e);
            if (index == -1)
                return false;

            Object[] newElements = new Object[elements.length - 1];
            System.arraycopy(elements, 0, newElements, 0, index);
            System.arraycopy(elements, index + 1, newElements, index, elements.length - index - 1);
            elements = newElements;
            return true;
        }

        private int indexOf(Object o) {
            for (int i = 0; i < elements.length; i++) {
                if (o.equals(elements[i]))
                    return i;
            }
            return -1;
        }

        public Object[] getSnapshot() {
            return elements;
        }
    }

    // Aliucord changed: public, so that it can be passed as lsplant context object
    public static class HookInfo {
        Member backup;
        private final Member method;
        final CopyOnWriteSortedSet<XC_MethodHook> callbacks = new CopyOnWriteSortedSet<>();
        private final boolean isStatic;
        private final Class<?> returnType;

        public HookInfo(Member method) {
            this.method = method;
            isStatic = Modifier.isStatic(method.getModifiers());
            if (method instanceof Method) {
                var rt = ((Method) method).getReturnType();
                if (!rt.isPrimitive()) {
                    returnType = rt;
                    return;
                }
            }
            returnType = null;
        }

        public Object callback(Object[] args) throws Throwable {
            var param = new XC_MethodHook.MethodHookParam();
            param.method = method;

            if (isStatic) {
                param.thisObject = null;
                param.args = args;
            } else {
                param.thisObject = args[0];
                param.args = new Object[args.length - 1];
                System.arraycopy(args, 1, param.args, 0, args.length - 1);
            }

            var hooks = callbacks.getSnapshot();
            var hookCount = hooks.length;

            // shouldn't happen since 0 remaining callbacks leads to unhook
            if (hookCount == 0) {
                try {
                    return invokeMethod(backup, param.thisObject, param.args);
                } catch (InvocationTargetException e) {
                    //noinspection ConstantConditions
                    throw e.getCause();
                }
            }

            int beforeIdx = 0;
            do {
                try {
                    ((XC_MethodHook) hooks[beforeIdx]).beforeHookedMethod(param);
                } catch (Throwable t) {
                    XposedBridge.log(t);

                    param.setResult(null);
                    param.returnEarly = false;
                    continue;
                }

                if (param.returnEarly) {
                    beforeIdx++;
                    break;
                }
            } while (++beforeIdx < hookCount);

            if (!param.returnEarly) {
                try {
                    param.setResult(invokeMethod(backup, param.thisObject, param.args));
                } catch (InvocationTargetException e) {
                    param.setThrowable(e.getCause());
                }
            }

            int afterIdx = beforeIdx - 1;
            do {
                Object lastResult = param.getResult();
                Throwable lastThrowable = param.getThrowable();

                try {
                    ((XC_MethodHook) hooks[afterIdx]).afterHookedMethod(param);
                } catch (Throwable t) {
                    XposedBridge.log(t);

                    if (lastThrowable == null)
                        param.setResult(lastResult);
                    else
                        param.setThrowable(lastThrowable);
                }
            } while (--afterIdx >= 0);

            var result = param.getResultOrThrowable();
            if (returnType != null)
                result = returnType.cast(result);
            return result;
        }
    }

    // Aliucord changed: private, so people don't abuse this
    private static void log(Throwable t) {
        Log.e(TAG, "Uncaught Exception", t);
    }

    public static Context getContextReflectively() {
        try {
            Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
            java.lang.reflect.Method currentApplicationMethod = activityThreadClass.getMethod("currentApplication");
            Object application = currentApplicationMethod.invoke(null);
            return (Context) application;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}
