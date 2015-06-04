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

package org.qeo.accesscontrollertests;

import java.security.AccessControlException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.qeo.DefaultStateChangeReaderListener;
import org.qeo.Key;
import org.qeo.QeoEnumeration;
import org.qeo.QeoFactory;
import org.qeo.StateChangeReader;
import org.qeo.StateWriter;
import org.qeo.exception.QeoException;
import org.qeo.java.QeoConnectionListener;
import org.qeo.java.QeoJava;
import org.qeo.jni.NativeQeo;

/**
 * Class to check if Qeo also works with a java security policy applied.<br/>
 * This runs as a separate application from gradle (gradle accessControlTest) and is unrelated to unit tests.
 */
public class AccessControlTest
    extends QeoConnectionListener
{
    private static final Semaphore SEM = new Semaphore(0);
    private static final Semaphore SEM_QEO_OK = new Semaphore(0);
    private QeoFactory mQeo = null;

    public static void main(String[] args)
        throws Exception
    {
        System.out.println("Starting SecurityManager AccessControlTest");
        SecurityManager sm = System.getSecurityManager();
        if (sm == null) {
            throw new IllegalStateException("SecurityManager is not active");
        }
        NativeQeo.setStoragePath("src/test/data/home.qeo/");
        NativeQeo.setQeoParameter("FWD_DISABLE_LOCATION_SERVICE", "1");

        AccessControlTest pt = new AccessControlTest();
        pt.testSelf();
        pt.run();
        System.out.println("SecurityManger AccessControlTest OK");
    }

    /**
     * Try to access the QEO_DEBUG env variable. This class should not have access to it!
     */
    public void testSelf()
    {
        try {
            System.getenv("QEO_DEBUG");
            throw new IllegalStateException("SecurityManager is not configured correctly");
        }
        catch (AccessControlException e) {
            // should get here
        }
    }

    /**
     * Try to create a reader and writer. It should just work fine.
     * 
     * @throws Exception If something goes wrong.
     */
    public void run()
        throws Exception
    {
        QeoJava.initQeo(QeoJava.DEFAULT_ID, this);
        if (!SEM_QEO_OK.tryAcquire(20, TimeUnit.SECONDS)) {
            throw new QeoException("Reader/writer not working");
        }

        MyClass data = new MyClass();
        data.id = 5;
        data.string = "string";
        data.enumerated = MyEnum.THREE;

        StateWriter<MyClass> sw = null;
        StateChangeReader<MyClass> sr = null;
        try {
            sw = mQeo.createStateWriter(MyClass.class);
            sr = mQeo.createStateChangeReader(MyClass.class, new MyStateChangeReader());
            sw.write(data);
            if (!SEM.tryAcquire(10, TimeUnit.SECONDS)) {
                throw new QeoException("Reader/writer not working");
            }
        }
        finally {
            if (sr != null) {
                sr.close();
            }
            if (sw != null) {
                sw.close();
            }
            QeoJava.closeQeo(this);
        }
    }

    private static class MyStateChangeReader
        extends DefaultStateChangeReaderListener<MyClass>
    {
        @Override
        public void onData(MyClass t)
        {
            SEM.release();
        }

    }

    public static enum MyEnum implements QeoEnumeration {
        ONE, TWO, THREE
    }

    public static class MyClass
    {
        public String string;
        public MyEnum enumerated;
        @Key
        public int id;
    }

    @Override
    public void onQeoReady(QeoFactory qeo)
    {
        mQeo = qeo;
        SEM_QEO_OK.release();
    }

    @Override
    public void onQeoClosed(QeoFactory qeo)
    {
    }

    @Override
    public void onQeoError(QeoException ex)
    {
    }
}
