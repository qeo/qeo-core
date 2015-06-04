/*
 * Copyright (c) 2015 - Qeo LLC
 *
 * The source code form of this Qeo Open Source Project component is subject
 * to the terms of the Clear BSD license.
 *
 * You can redistribute it and/or modify it under the terms of the Clear BSD
 * License (http://directory.fsf.org/wiki/License:ClearBSD). See LICENSE file
 * for more details.
 *
 * The Qeo Open Source Project also includes third party Open Source Software.
 * See LICENSE file for more details.
 */

package org.qeo.internal.reflection;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.qeo.Key;
import org.qeo.QeoEnumeration;
import org.qeo.internal.common.ArrayType;
import org.qeo.internal.common.EnumerationType;
import org.qeo.internal.common.ObjectData;
import org.qeo.internal.common.ObjectType;
import org.qeo.internal.common.PrimitiveArrayType;
import org.qeo.internal.common.PrimitiveType;
import org.qeo.internal.common.Type;
import org.qeo.internal.common.Type.MemberType;
import org.qeo.internal.common.Util;

/**
 * Utility class for converting classes and instances to types and data, and vice versa.
 * 
 * @param <T> The class to do reflection on.
 */
public final class ReflectionUtil<T>
    implements IntrospectionUtil<Class<T>, T>
{
    private static final Logger LOG = Logger.getLogger(ReflectionUtil.class.getName());
    private final Class<T> mRootClass;

    /**
     * Create an instance of reflectionUtil to inspect a given class.
     * 
     * @param rootClass The class to inspect.
     */
    public ReflectionUtil(Class<T> rootClass)
    {
        mRootClass = rootClass;
    }

    /**
     * Class for converting a Java class to a MemberType.
     */
    private static final class FieldTypeConvertor
    {
        private final Class<?> mClazz;
        private final MemberType mType;

        private static FieldTypeConvertor[] sTable = {new FieldTypeConvertor(byte.class, MemberType.TYPE_BYTE),
            new FieldTypeConvertor(byte[].class, MemberType.TYPE_BYTEARRAY),
            new FieldTypeConvertor(short.class, MemberType.TYPE_SHORT),
            new FieldTypeConvertor(short[].class, MemberType.TYPE_SHORTARRAY),
            new FieldTypeConvertor(int.class, MemberType.TYPE_INT),
            new FieldTypeConvertor(int[].class, MemberType.TYPE_INTARRAY),
            new FieldTypeConvertor(long.class, MemberType.TYPE_LONG),
            new FieldTypeConvertor(long[].class, MemberType.TYPE_LONGARRAY),
            new FieldTypeConvertor(String.class, MemberType.TYPE_STRING),
            new FieldTypeConvertor(String[].class, MemberType.TYPE_STRINGARRAY),
            new FieldTypeConvertor(boolean.class, MemberType.TYPE_BOOLEAN),
            new FieldTypeConvertor(boolean[].class, MemberType.TYPE_BOOLEANARRAY),
            new FieldTypeConvertor(float.class, MemberType.TYPE_FLOAT),
            new FieldTypeConvertor(float[].class, MemberType.TYPE_FLOATARRAY)};

        public static MemberType classToType(Class<?> clazz)
        {
            /*
             * First handle all primitive types for which we already have an implementation
             */
            for (int i = 0; i < sTable.length; i++) {
                if (sTable[i].mClazz == clazz) {
                    return sTable[i].mType;
                }
            }
            /*
             * Then check if we are dealing with an array.
             */
            if (clazz.isArray()) {
                return MemberType.TYPE_ARRAY;
            }
            /*
             * Then check whether it is an enumeration.
             */
            if (clazz.isEnum()) {
                if (!QeoEnumeration.class.isAssignableFrom(clazz)) {
                    throw new IllegalArgumentException("Enumeration " + clazz.getName() + " does not implement "
                        + QeoEnumeration.class.getName());
                }
                if (0 == clazz.getEnumConstants().length) {
                    throw new IllegalArgumentException("Enumeration " + clazz.getName() + " is empty");
                }
                return MemberType.TYPE_ENUM;
            }
            /*
             * then check if it is not a primitive type that we didn't cover yet. If so we are dealing with a class
             * type.
             */
            if (!clazz.isPrimitive()) {
                return MemberType.TYPE_CLASS;
            }
            /*
             * The type is not yet supported.
             */
            return null;
        }

        private FieldTypeConvertor(Class<?> c, MemberType type)
        {
            super();
            this.mClazz = c;
            this.mType = type;
        }

    }

    /**
     * Validates whether the class can be used to construct a Qeo type.
     * 
     * @param clazz The class to be validated.
     * 
     * @throws IllegalArgumentException if the class is invalid
     */
    private static void validateClass(final Class<?> clazz)
    {
        if (!Modifier.isPublic(clazz.getModifiers())) {
            throw new IllegalArgumentException("Class " + clazz.getName() + " must be declared public");
        }
        AccessController.doPrivileged(new PrivilegedAction<Object>() {
            @Override
            public Object run()
            {
                if (clazz.isMemberClass()) {
                    // inner class
                    if (!Modifier.isStatic(clazz.getModifiers())) {
                        throw new IllegalArgumentException("Inner class " + clazz.getName()
                            + " must be declared static");
                    }
                }
                try {
                    /* default constructor present? */
                    clazz.getConstructor();
                }
                catch (NoSuchMethodException e) {
                    throw new IllegalArgumentException("Class " + clazz.getName() + " must have a default constructor");
                }
                return null;
            }
        });
    }

    private static Type typeFromField(Class<?> clazz, String name, int memberId, boolean key)
    {
        Type member = null;
        final MemberType memberType = FieldTypeConvertor.classToType(clazz);

        if (memberType.getTypeImplementation() == MemberType.TypeImplemtation.OBJECT) {
            member = typeFromClass(clazz, clazz.getName(), memberId, key, name);
        }
        else if (memberType.getTypeImplementation() == MemberType.TypeImplemtation.ENUM) {
            String typeName = clazz.getName().replace('$', '_'); /* inner clas support */
            member = new EnumerationType(typeName, name, memberId, key, clazz.getEnumConstants());
        }
        else if (memberType.getTypeImplementation() == MemberType.TypeImplemtation.ARRAY) {
            Type arrayElement = typeFromField(clazz.getComponentType(), null, 0, key);
            member = new ArrayType(name, memberId, key, arrayElement);
        }
        else if (memberType.getTypeImplementation() == MemberType.TypeImplemtation.PRIMITIVEARRAY) {
            // key and name are not relevant here
            MemberType elementType = FieldTypeConvertor.classToType(clazz.getComponentType());
            member = new PrimitiveArrayType(name, memberId, key, elementType);
        }
        else {
            member = new PrimitiveType(name, memberId, key, memberType);
        }

        return member;
    }

    private static ObjectType typeFromClass(Class<?> clazz, String name, int id, boolean key, String memberName)
    {
        /* valid input arguments */
        validateClass(clazz);
        /* convert inner-class separator into underscore */
        String typeName = (null == name) ? clazz.getName() : name;
        final ObjectType type = new ObjectType(typeName.replace('$', '_'), id, key, memberName);
        for (final Field field : clazz.getFields()) {
            if (!isQeoField(field)) {
                continue;
            }

            AccessController.doPrivileged(new PrivilegedAction<Object>() {
                @Override
                public Object run()
                {
                    field.setAccessible(true);
                    return null;
                }
            });
            int memberId = Util.calculateID(field.getName());
            Type member = typeFromField(field.getType(), field.getName(), memberId, fieldIsKey(field));

            type.addMember(member, field.getName());
        }
        LOG.log(Level.FINER, "Created type from java class: ${0}", type);
        return type;
    }

    /**
     * Construct a type from a class.
     * 
     * @param typedesc The class from which to construct the type.
     * 
     * @return The constructed type.
     */
    @Override
    public ObjectType typeFromTypedesc(Class<T> typedesc)
    {
        return typeFromClass(typedesc, null, 0, false, null);
    }

    /**
     * Construct a data sample from an object.
     * 
     * @param t The object from which to construct the data sample.
     * @param type The type of the data sample.
     * 
     * @return The constructed data sample.
     */
    @Override
    public ObjectData dataFromObject(T t, ObjectType type)
    {
        return type.toData(t);
    }

    /**
     * Construct and object from a data sample.
     * 
     * @param data The data sample from which to contruct the object.
     * @param type The type of the data sample.
     * 
     * @return The constructed object or null if data is null.
     */
    @Override
    public T objectFromData(ObjectData data, ObjectType type)
    {
        T obj = null;

        if (null != data) {
            obj = data.toObject(mRootClass, type);
        }
        return obj;
    }

    /**
     * Method to determine if Qeo is going to use this field type.
     * 
     * @param field The field to be checked
     * @return true if it is a valid Qeo field
     */
    public static boolean isQeoField(Field field)
    {
        if (Modifier.isFinal(field.getModifiers())) {
            // don't use final field. The receiving site can't set them anyway.
            return false;
        }
        return true;
    }

    /**
     * Determine whether a field is marked with a Key annotation.
     * 
     * @param field The field.
     * 
     * @return True if the field is a key field, false if not.
     */
    public static boolean fieldIsKey(Field field)
    {
        final Key key = field.getAnnotation(Key.class);

        return (null != key);
    }

}
