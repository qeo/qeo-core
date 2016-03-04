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
import org.qeo.unittesttypes.StaticFields;

/**
 * 
 */
public class StaticFieldTest
        extends QeoTestCase
{

    private EventWriter<StaticFields> ew1 = null;

    @Override
    public void setUp()
        throws Exception
    {
        super.setUp(); // call as first
        // verbose = true; //uncomment this for verbose printing
        println("setup");
        ew1 = mQeo.createEventWriter(StaticFields.class);
        assertNotNull(ew1);
    }

    @Override
    public void tearDown()
        throws Exception
    {
        println("tearDown");
        if (ew1 != null) {
            ew1.close();
        }
        super.tearDown(); // call as last
    }

    private static class MyListener
            implements EventReaderListener<StaticFields>
    {
        private final StaticFields expected;
        private final Semaphore sem;

        public MyListener(StaticFields sf, Semaphore s)
        {
            expected = sf;
            sem = s;
        }

        @Override
        public void onData(StaticFields t)
        {
            assertNotNull(t);
            assertEquals(expected.normalInt1, t.normalInt1);
            assertEquals(expected.normalInt2, t.normalInt2);
            assertEquals(expected.finalInt, t.finalInt);
            sem.release();
        }

        @Override
        public void onNoMoreData()
        {
            // nothing todo
        }
    }

    public void testFields()
        throws Exception
    {
        final StaticFields sf = new StaticFields();
        sf.normalInt1 = 789;
        sf.normalInt2 = 445;
        StaticFields.setStaticInt(777);
        final Semaphore s = new Semaphore(0);
        final EventReader<StaticFields> r = mQeo.createEventReader(StaticFields.class, new MyListener(sf, s));
        ew1.write(sf);
        waitForData(s);
        r.close();

    }
}
