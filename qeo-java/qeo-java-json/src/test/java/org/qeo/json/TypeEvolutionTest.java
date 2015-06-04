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

package org.qeo.json;

import org.json.JSONException;
import org.json.JSONObject;
import org.qeo.DefaultEventReaderListener;
import org.qeo.EventReader;
import org.qeo.EventWriter;
import org.qeo.exception.QeoException;
import org.qeo.internal.common.ObjectType;
import org.qeo.json.internal.JSONIntrospection;
import org.qeo.testframework.QeoTestCase;

/**
 * 
 */
public class TypeEvolutionTest
    extends QeoTestCase
{
    private QeoFactoryJSON mQeoJson;
    private static final String TD_EVENT_MEMBERS = "'longtype': {type: 'INT64'}, 'inttype': {type: 'int32'}";
    private static final String TD_EVENT_MEMBERS2 = TD_EVENT_MEMBERS + ", 'inttype2': {type: 'int32'}";

    private static final String TD_EVENT = "{ topic: 'org.qeo.EvolutionType', behavior: 'event', properties: {"
        + TD_EVENT_MEMBERS + "}}";
    private static final String TD_EVENT2 = "{ topic: 'org.qeo.EvolutionType', behavior: 'event', properties: {"
        + TD_EVENT_MEMBERS2 + "}}";
    private static final String TD_EVENT_PARENT1 = "{ topic: 'org.qeo.Parent1', behavior: 'event', properties: {"
        + "'types1': {type: object, item: " + TD_EVENT + "}}}";
    private static final String TD_EVENT_PARENT2 = "{ topic: 'org.qeo.Parent2', behavior: 'event', properties: {"
        + "'types1': {type: object, item: " + TD_EVENT2 + "}}}";
    private JSONObject jsonTdEvent;
    private JSONObject jsonTdEvent2;
    private JSONIntrospection introspectionJson;

    @Override
    public void setUp()
        throws Exception
    {
        super.setUp();
        mQeoJson = QeoJSON.getFactory(mQeo);

        jsonTdEvent = new JSONObject(TD_EVENT);
        jsonTdEvent2 = new JSONObject(TD_EVENT2);
        introspectionJson = new JSONIntrospection();
    }

    public void testEquals()
    {
        ObjectType typeJson = introspectionJson.typeFromTypedesc(jsonTdEvent);
        assertTrue(typeJson.equals(typeJson));
        ObjectType typeJsonSame = introspectionJson.typeFromTypedesc(jsonTdEvent); // same
        assertTrue(typeJson.equals(typeJsonSame));
        ObjectType typeJsonOther = introspectionJson.typeFromTypedesc(jsonTdEvent2);
        assertFalse(typeJson.equals(typeJsonOther));
    }

    public void testEqualsAdvanced()
        throws JSONException
    {
        JSONObject typeJsonAdvanced = new JSONObject(JsonTestEvent.TD_ADVANCED);
        ObjectType typeJson1 = introspectionJson.typeFromTypedesc(typeJsonAdvanced);
        ObjectType typeJson2 = introspectionJson.typeFromTypedesc(typeJsonAdvanced);
        assertTrue(typeJson1.equals(typeJson2));

        JSONObject typeJsonArray = new JSONObject(JsonTestEvent.TD_EVENT_ARRAY);
        typeJson1 = introspectionJson.typeFromTypedesc(typeJsonArray);
        typeJson2 = introspectionJson.typeFromTypedesc(typeJsonArray);
        assertTrue(typeJson1.equals(typeJson2));

    }

    public void testGood()
        throws QeoException
    {
        EventWriter<JSONObject> w1 = mQeoJson.createEventWriter(jsonTdEvent);
        addJunitReaderWriter(w1);
        EventWriter<JSONObject> w2 = mQeoJson.createEventWriter(jsonTdEvent);
        addJunitReaderWriter(w2);
    }

    /**
     * 2 EventWriters with the same type but other fields.
     */
    public void testInvalid1()
        throws QeoException
    {
        EventWriter<JSONObject> w1 = mQeoJson.createEventWriter(jsonTdEvent);
        addJunitReaderWriter(w1);
        try {
            EventWriter<JSONObject> w2 = mQeoJson.createEventWriter(jsonTdEvent2);
            addJunitReaderWriter(w2);
            fail("Should not get here");
        }
        catch (IllegalStateException e) {
            // expected
        }
    }

    /**
     * 2 EventWriters with same type and fields but different subclass.
     */
    public void testInvalid2()
        throws Exception
    {
        JSONObject jsonTdParent1 = new JSONObject(TD_EVENT_PARENT1);
        JSONObject jsonTdParent2 = new JSONObject(TD_EVENT_PARENT2);
        EventWriter<JSONObject> w1 = mQeoJson.createEventWriter(jsonTdParent1);
        addJunitReaderWriter(w1);
        try {
            EventWriter<JSONObject> w2 = mQeoJson.createEventWriter(jsonTdParent2);
            addJunitReaderWriter(w2);
            fail("Should not get here");
        }
        catch (IllegalStateException e) {
            // expected
        }
    }

    /**
     * Combination of reader/writer.
     */
    public void testInvalid3()
        throws QeoException
    {
        EventWriter<JSONObject> w1 = mQeoJson.createEventWriter(jsonTdEvent);
        addJunitReaderWriter(w1);
        try {
            EventReader<JSONObject> r1 =
                mQeoJson.createEventReader(jsonTdEvent2, new DefaultEventReaderListener<JSONObject>());
            addJunitReaderWriter(r1);
            fail("Should not get here");
        }
        catch (IllegalStateException e) {
            // expected
        }
    }
}
