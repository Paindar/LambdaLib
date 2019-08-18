/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of LambdaLib modding library.
* https://github.com/LambdaInnovation/LambdaLib
* Licensed under MIT, see project root for more information.
*/
package cn.lambdalib.util.generic;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import cn.lambdalib.core.LambdaLib;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModAPIManager;
import cpw.mods.fml.common.discovery.ASMDataTable;
import cpw.mods.fml.common.discovery.ASMDataTable.ASMData;
import cpw.mods.fml.common.discovery.asm.ModAnnotation.EnumHolder;
import org.objectweb.asm.Type;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
public class ReflectionUtils {
    private static final String SIDE_ONLY_PATH = "cpw.mods.fml.relauncher.SideOnly";
    private static final String OPTIONAL_METHOD_PATH = "cpw.mods.fml.common.Optional$Method";
    private static final String OPTIONAL_INTERFACE = "cpw.mods.fml.common.Optional$Interface";

    private static Set<String> removedClasses = new HashSet<>();
    private static Set<ASMData> removedMethods = new HashSet<ASMData>();

    private static ASMDataTable table;

    private static String GetEHField(EnumHolder eh, String fieldName)
    {
        try
        {
            Field field = EnumHolder.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            return (String)field.get(eh);
        } catch (NoSuchFieldException | IllegalAccessException e)
        {
            e.printStackTrace();
        }
        return null;
    }

    public static void _init(ASMDataTable _table) {
        table = _table;
        String startSide = FMLCommonHandler.instance().getSide().toString();
        Set<ASMData> sideData = table.getAll(SIDE_ONLY_PATH);
        Set<ASMData> optionalMethods = table.getAll(OPTIONAL_METHOD_PATH);
        Set<ASMData> optionalClasses = table.getAll(OPTIONAL_INTERFACE);

        for (ASMDataTable.ASMData asmData: sideData) {
            if (Objects.equals(asmData.getClassName(), asmData.getObjectName())) { // Is a class
                EnumHolder enumHolder = (EnumHolder) asmData.getAnnotationInfo().get("value");
                if (!Objects.equals(GetEHField(enumHolder,"value"), startSide)) {
                    removedClasses.add(asmData.getClassName());
                }
            }
            if (asmData.getObjectName().contains("(")) { // Is a method
                String assumedSide = GetEHField((EnumHolder) asmData.getAnnotationInfo().get("value"),"value");
                if (!assumedSide.equals(startSide))
                    removedMethods.add(asmData);
            }
        }

        for (ASMDataTable.ASMData optional : optionalClasses) {
            String modid = (String) optional.getAnnotationInfo().get("modid");
            // Ref: ModAPITransformer#72
            if (Loader.isModLoaded(modid) || ModAPIManager.INSTANCE.hasAPI(modid)) {
                continue;
            }
            removedClasses.add(optional.getClassName());
        }

        for (ASMDataTable.ASMData optional : optionalMethods) {
            String modid = (String) optional.getAnnotationInfo().get("modid");
            // Ref: ModAPITransformer#72
            if (Loader.isModLoaded(modid) || ModAPIManager.INSTANCE.hasAPI(modid)) {
                continue;
            }
            removedMethods.add(optional);
        }
    }

    /**
     * Get all the methods for a class, including those that are private or protected in parent class.
     * All the methods are made accessible.
     */
    public static List<Method> getAccessibleMethods(Class cls) {
        List<Method> ret = new ArrayList<>();

        while (cls != null) {
            for (Method m : cls.getDeclaredMethods()) {
                m.setAccessible(true);
                ret.add(m);
            }
            cls = cls.getSuperclass();
        }

        return ret;
    }

    public static Method getObfMethod(Class<?> cl, String methodName, String obfName, Class... parameterTypes) {
        Method m = null;
        try {
            try {
                m = cl.getDeclaredMethod(methodName, parameterTypes);
            } catch (Exception ignored) {
            }

            if (m == null)
                m = cl.getDeclaredMethod(obfName, parameterTypes);

            m.setAccessible(true);
            return m;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get a class field (both in workspace and in builds) by its deobf name and obf name.
     */
    public static Field getObfField(Class cl, String normName, String obfName) {
        Field f = null;
        try {
            try {
                f = cl.getDeclaredField(normName);
            } catch (Exception ignored) {}

            if (f == null) {
                f = cl.getDeclaredField(obfName);
            }
            f.setAccessible(true);
            return f;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get all methods in all classes with given annotation.
     */
    public static List<Method> getMethods(Class<? extends Annotation> annoClass) {
        List<ASMData> objects = getRawObjects(annoClass.getCanonicalName());
        return objects.stream()
                .map(data -> {
                    try {
                        Class<?> type = Class.forName(data.getClassName());

                        String fullDesc = data.getObjectName();
                        int idx = fullDesc.indexOf('(');
                        String methodName = fullDesc.substring(0, idx);
                        String desc = fullDesc.substring(idx);

                        Type[] rawArgs = Type.getArgumentTypes(desc);
                        Class[] args = new Class[rawArgs.length];
                        for (int i = 0; i < rawArgs.length; ++i) {
                            args[i] = Class.forName(rawArgs[i].getClassName());
                        }

                        Method method = type.getDeclaredMethod(methodName, args);
                        return method;
                    } catch (RuntimeException e) {
                        throw e;
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toList());
    }

    /**
     * Get all classes with given annotation.
     */
    public static List<Class<?>> getClasses(Class<? extends Annotation> annoClass) {
        List<ASMData> objects = getRawObjects(annoClass.getCanonicalName());
        return objects.stream()
                .map(ASMData::getClassName)
                .distinct()
                .map(it -> {
                    try {
                        return Class.forName(it);
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toList());
    }

    /**
     * Get all fields with given annotation.
     */
    public static List<Field> getFields(Class<? extends Annotation> annoClass) {
        List<ASMData> objects = getRawObjects(annoClass.getName());
        List<Field> ret = objects.stream()
                .filter(obj -> !obj.getObjectName().equals(obj.getClassName()))
                .map(it -> {
                    try {
                        return Class.forName(it.getClassName()).getDeclaredField(it.getObjectName());
                    } catch (ClassNotFoundException|NoSuchFieldException ex) {
                        throw new RuntimeException(ex);
                    } catch (NoClassDefFoundError ex)
                    {
                        LambdaLib.log.warn(String.format("Error when get field %s.%s ", it.getClassName(), it.getObjectName()));
                        throw new RuntimeException(ex);
                    }
                })
                .collect(Collectors.toList());
        for (Field f : ret)
            f.setAccessible(true);
        return ret;
    }

    public static List<ASMData> getRawObjects(String annoName) {
        return getRawObjects(annoName, true);
    }

    public static List<ASMData> getRawObjects(String annoName, boolean removeSideOnly) {
        Set<ASMData> sets = table.getAll(annoName);
        for(ASMData asm: sets)
        {
            LambdaLib.log.info(asm.getClassName());
        }
        Stream<ASMData> stream = table.getAll(annoName).stream();
        if (removeSideOnly) {
            stream = stream.filter(it -> !removedClasses.contains(it.getClassName()))
                    .filter(it -> removedMethods.stream().noneMatch(m -> isClassObjectEqual(it, m)));
        }
        return stream.collect(Collectors.toList());
    }

    private static boolean isClassObjectEqual(ASMData lhs, ASMData rhs) {
        return (lhs.getObjectName().equals(rhs.getObjectName())) &&
                (lhs.getClassName().equals(rhs.getClassName()));
    }


    /**
     * Get all the methods for a class, including those that are private or protedted in parent class.
     * All the methods are made accessible.
     */
    public static List<Method> getAllAccessibleMethods(Class cls) {
        List<Method> ret = new ArrayList<>();

        while (cls != null) {
            for (Method m : cls.getDeclaredMethods()) {
                m.setAccessible(true);
                ret.add(m);
            }
            cls = cls.getSuperclass();
        }

        return ret;
    }

}
