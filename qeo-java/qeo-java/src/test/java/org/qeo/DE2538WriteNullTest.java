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

import java.util.concurrent.Semaphore;

import org.qeo.testframework.QeoTestCase;

/**
 * 
 */
public class DE2538WriteNullTest
    extends QeoTestCase
{
    private StateWriter<Outer> sw;
    private StateReader<Outer> sr;
    private Outer o1;
    private MyStateReaderListener ol;

    @Override
    public void setUp()
        throws Exception
    {
        super.setUp();
        println("setup");

        sw = mQeo.createStateWriter(Outer.class);
        ol = new MyStateReaderListener();
        sr = mQeo.createStateReader(Outer.class, ol);
        o1 = new Outer();

        assertNotNull(sw);
        assertNotNull(sr);
    }

    @Override
    public void tearDown()
        throws Exception
    {
        if (sw != null) {
            sw.close();
        }
        if (sr != null) {
            sr.close();
        }
        super.tearDown();
    }

    public static class Inner
    {
        public int id;
        public String name;
    }

    public static class Outer
    {
        public int id;
        @Key
        public Inner inner;

        public Outer()
        {
        }
    }

    private static class MyStateReaderListener
        implements StateReaderListener
    {
        final Semaphore sem = new Semaphore(0);

        @Override
        public void onUpdate()
        {
            sem.release();
        }
    };

    public void testFail()
    {
        try {
            sw.write(o1); // write empty outer object
            fail("Should not come here");
        }
        catch (IllegalArgumentException e) {
            // expected
        }

    }
}
