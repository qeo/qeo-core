/*
 * Copyright (c) 2016 - Qeo LLC
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

package org.qeo;

import org.qeo.exception.QeoException;
import org.qeo.testframework.QeoTestCase;

/**
 * Type specific tests.
 */
public class TypeTest
    extends QeoTestCase
{
    public static class DataNoDefaultConstructor
    {
        public int id;

        public DataNoDefaultConstructor(int pId)
        {
            id = pId;
        }

    }

    public void testNoDefaultConstructor()
        throws Exception
    {
        boolean excepted = false;

        try {
            mQeo.createEventWriter(DataNoDefaultConstructor.class);
        }
        catch (IllegalArgumentException e) {
            if (e.getMessage()
                .equals("Class org.qeo.TypeTest$DataNoDefaultConstructor must have a default constructor")) {
                excepted = true;
            }
            else {
                throw e;
            }
        }
        assertTrue(excepted);
    }

    public static class NoPublicFields
    {
        private int mId;
        private String mName;

        public int getId()
        {
            return mId;
        }

        public void setId(int id)
        {
            mId = id;
        }

        public String getName()
        {
            return mName;
        }

        public void setName(String name)
        {
            mName = name;
        }
    }

    public void testNoPublicFields()
        throws QeoException
    {
        EventWriter<NoPublicFields> ew = null;
        try {
            ew = mQeo.createEventWriter(NoPublicFields.class);
            fail("It should not be possible to create a writer for this type");
        }
        catch (IllegalArgumentException ex) {
            // good, we should come here
        }
        finally {
            if (ew != null) {
                ew.close();
            }
        }
    }

    public static enum NonQeoEnum {
        ONE, TWO, THREE
    }

    public static class TypeWithNonQeoEnum
    {
        public NonQeoEnum myEnum;
    }

    public void testTypeWithNonQeoEnum()
        throws QeoException
    {
        EventWriter<TypeWithNonQeoEnum> ew = null;
        try {
            ew = mQeo.createEventWriter(TypeWithNonQeoEnum.class);
            fail("It should not be possible to create a writer for this type");
        }
        catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("does not implement"));
        }
        finally {
            if (ew != null) {
                ew.close();
            }
        }
    }

    public static enum EmptyQeoEnum implements QeoEnumeration {
    }

    public static class TypeWithEmptyQeoEnum
    {
        public EmptyQeoEnum myEnum;
    }

    public void testTypeWithEmptyQeoEnum()
        throws QeoException
    {
        EventWriter<TypeWithEmptyQeoEnum> ew = null;
        try {
            ew = mQeo.createEventWriter(TypeWithEmptyQeoEnum.class);
            fail("It should not be possible to create a writer for this type");
        }
        catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("is empty"));
        }
        finally {
            if (ew != null) {
                ew.close();
            }
        }
    }
}
