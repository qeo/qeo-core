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
import org.qeo.system.RegistrationCredentials;
import org.qeo.system.RegistrationRequest;
import org.qeo.testframework.QeoConnectionTestListener;
import org.qeo.testframework.QeoTestCase;
import org.qeo.unittesttypes.TestState;

public class MultipleFactoryTest
    extends QeoTestCase
{
    private QeoFactory mQeoOpen;
    private QeoConnectionTestListener mQeoReadyListener;
    StateWriter<?> swOpen;

    @Override
    public void setUp()
        throws Exception
    {
        super.setUp();
        mQeoReadyListener = createFactory(getFactoryOpenId());
        mQeoOpen = mQeoReadyListener.getFactory();
        assertNotNull(mQeoOpen);
    }

    @Override
    public void tearDown()
        throws Exception
    {
        if (swOpen != null) {
            swOpen.close();
        }
        closeFactory(mQeoReadyListener);
        super.tearDown();
    }

    /**
     * Create 2 different topics, first on closed factory.
     * 
     * @throws QeoException
     */
    public void testDifferentTopics1()
        throws QeoException
    {
        StateWriter<TestState> sw1 = mQeo.createStateWriter(TestState.class);
        addJunitReaderWriter(sw1);
        swOpen = mQeoOpen.createStateWriter(RegistrationRequest.class);
    }

    /**
     * Create 2 different topics, first on open factory.
     * 
     * @throws QeoException
     */
    public void testDifferentTopics2()
        throws QeoException
    {
        swOpen = mQeoOpen.createStateWriter(RegistrationCredentials.class);
        StateWriter<TestState> sw2 = mQeo.createStateWriter(TestState.class);
        addJunitReaderWriter(sw2);
    }

    // The following two tests are commented out for the following reason:
    // DE2599 [QeoCCore] Registration of the same type on different factories is currently impossible
    // https://rally1.rallydev.com/#/9381731412ud/detail/defect/13702407941

    // /**
    // * Create 2 same topics, first on closed factory.
    // *
    // * @throws QeoException
    // */
    // public void testSameTopics1()
    // throws QeoException
    // {
    // EventWriter<Class1> ew2 = mQeo.createEventWriter(Class1.class);
    // addJunitWriter(ew2);
    // EventWriter<Class1> ew1 = mQeoOpen.createEventWriter(Class1.class);
    // addJunitWriter(ew1);
    // }
    //
    // /**
    // * Create 2 same topics, first on open factory.
    // *
    // * @throws QeoException
    // */
    // public void testSameTopics2()
    // throws QeoException
    // {
    // EventWriter<Class1> ew1 = mQeoOpen.createEventWriter(Class1.class);
    // addJunitWriter(ew1);
    // EventWriter<Class1> ew2 = mQeo.createEventWriter(Class1.class);
    // addJunitWriter(ew2);
    // }
}
