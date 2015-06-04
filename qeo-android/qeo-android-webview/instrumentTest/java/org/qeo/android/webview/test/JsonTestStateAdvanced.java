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

package org.qeo.android.webview.test;

import java.util.concurrent.Semaphore;

import org.json.JSONObject;
import org.qeo.StateReader;
import org.qeo.StateReaderListener;
import org.qeo.StateWriter;
import org.qeo.internal.common.ObjectType;
import org.qeo.internal.reflection.ReflectionUtil;
import org.qeo.json.QeoFactoryJSON;
import org.qeo.json.QeoJSON;
import org.qeo.json.internal.JSONIntrospection;
import org.qeo.json.types.JsonStateAdvanced;

public class JsonTestStateAdvanced
    extends JsonAndroidTestCase
{
    private JSONObject jstAdvanced;

    private QeoFactoryJSON mQeoJson;

    private static final String TD_STATE_ADVANCED_SUBCLASS1 =
        "{ topic: 'org.qeo.json.types.JsonStateAdvanced_SubClass1', " + "properties: {"
            + "id: {type: 'int32', key:true}," + "name: {type: 'string'}" + "}}";
    private static final String TD_STATE_ADVANCED_SUBCLASS2 =
        "{ topic: 'org.qeo.json.types.JsonStateAdvanced_SubClass2', " + "properties: {" + "id: {type: 'int32'},"
            + "name: {type: 'string'}" + "}}";
    private static final String TD_STATE_ADVANCED =
        "{ topic: 'org.qeo.json.types.JsonStateAdvanced', behavior: 'STATE', "
            + "properties: {"
            + "id: {type: 'int32', key: true },"
            + "id2: {type: 'int32' }," // no key
            + "id3: {type: 'int32', key:false }," // no key
            + "keyedClass1: { type: 'object', key: true, item:  " + TD_STATE_ADVANCED_SUBCLASS1 + "},"
            + "unKeyedClass1: { type: 'object', item:  " + TD_STATE_ADVANCED_SUBCLASS1 + "},"
            + "keyedClass2: { type: 'object', key: true, item:  " + TD_STATE_ADVANCED_SUBCLASS2 + "},"
            + "unKeyedClass2: { type: 'object', item:  " + TD_STATE_ADVANCED_SUBCLASS2 + "}" + "}}";
    private JsonStateAdvanced jsonStateAdvanced;
    private JSONObject jsonStateAdvancedJson;
    private Semaphore sem;

    @Override
    public void setUp()
        throws Exception
    {
        super.setUp(); // call as first
        sem = new Semaphore(0);
        mQeoJson = QeoJSON.getFactory(mQeo);

        jstAdvanced = new JSONObject(TD_STATE_ADVANCED);

        jsonStateAdvanced = new JsonStateAdvanced();
        jsonStateAdvancedJson = new JSONObject();

        jsonStateAdvanced.id = 5;
        jsonStateAdvancedJson.put("id", 5);
        jsonStateAdvanced.id2 = 7;
        jsonStateAdvancedJson.put("id2", 7);
        jsonStateAdvanced.id3 = 9;
        jsonStateAdvancedJson.put("id3", 9);

        JsonStateAdvanced.SubClass1 keyedClass1 = new JsonStateAdvanced.SubClass1();
        JSONObject keyedJson1 = new JSONObject();
        keyedClass1.id = 10;
        keyedJson1.put("id", 10);
        keyedClass1.name = "keyedClass1";
        keyedJson1.put("name", "keyedClass1");
        jsonStateAdvanced.keyedClass1 = keyedClass1;
        jsonStateAdvancedJson.put("keyedClass1", keyedJson1);

        JsonStateAdvanced.SubClass2 keyedClass2 = new JsonStateAdvanced.SubClass2();
        JSONObject keyedJson2 = new JSONObject();
        keyedClass2.id = 11;
        keyedJson2.put("id", 11);
        keyedClass2.name = "keyedClass2";
        keyedJson2.put("name", "keyedClass2");
        jsonStateAdvanced.keyedClass2 = keyedClass2;
        jsonStateAdvancedJson.put("keyedClass2", keyedJson2);

        JsonStateAdvanced.SubClass1 unkeyedClass1 = new JsonStateAdvanced.SubClass1();
        JSONObject unkeyedJson1 = new JSONObject();
        unkeyedClass1.id = 12;
        unkeyedJson1.put("id", 12);
        unkeyedClass1.name = "unkeyedClass1";
        unkeyedJson1.put("name", "unkeyedClass1");
        jsonStateAdvanced.unKeyedClass1 = unkeyedClass1;
        jsonStateAdvancedJson.put("unKeyedClass1", unkeyedJson1);

        JsonStateAdvanced.SubClass2 unkeyedClass2 = new JsonStateAdvanced.SubClass2();
        JSONObject unkeyedJson2 = new JSONObject();
        unkeyedClass2.id = 13;
        unkeyedJson2.put("id", 13);
        unkeyedClass2.name = "unkeyedClass2";
        unkeyedJson2.put("name", "unkeyedClass2");
        jsonStateAdvanced.unKeyedClass2 = unkeyedClass2;
        jsonStateAdvancedJson.put("unKeyedClass2", unkeyedJson2);

    }

    @Override
    public void tearDown()
        throws Exception
    {
        super.tearDown(); // call as last
    }

    public void testAdvancedInternal()
        throws Exception
    {
        JSONIntrospection introspectionJson = new JSONIntrospection();
        ObjectType typeJson = introspectionJson.typeFromTypedesc(jstAdvanced);

        ReflectionUtil<JsonStateAdvanced> introspectionJava =
            new ReflectionUtil<JsonStateAdvanced>(JsonStateAdvanced.class);
        ObjectType typeJava = introspectionJava.typeFromTypedesc(JsonStateAdvanced.class);

        assertEquals(typeJava.toString(2), typeJson.toString(2));
    }

    public void testWriteJson()
        throws Exception
    {
        StateWriter<JSONObject> sw = mQeoJson.createStateWriter(jstAdvanced);
        StateReader<JsonStateAdvanced> sr = mQeo.createStateReader(JsonStateAdvanced.class, new StateReaderListener() {

            @Override
            public void onUpdate()
            {
                sem.release();
            }
        });
        sw.write(jsonStateAdvancedJson);

        waitForData(sem);
        boolean first = true;
        for (JsonStateAdvanced s : sr) {
            assertTrue(first);
            first = false;

            assertEquals(jsonStateAdvanced.id, s.id);
            assertEquals(jsonStateAdvanced.id2, s.id2);
            assertEquals(jsonStateAdvanced.id3, s.id3);
            assertEquals(jsonStateAdvanced.keyedClass1.id, s.keyedClass1.id);
            assertEquals(jsonStateAdvanced.keyedClass1.name, s.keyedClass1.name);
            assertEquals(jsonStateAdvanced.keyedClass2.id, s.keyedClass2.id);
            assertEquals(jsonStateAdvanced.keyedClass2.name, s.keyedClass2.name);
            assertEquals(jsonStateAdvanced.unKeyedClass1.id, s.unKeyedClass1.id);
            assertEquals(jsonStateAdvanced.unKeyedClass1.name, s.unKeyedClass1.name);
            assertEquals(jsonStateAdvanced.unKeyedClass2.id, s.unKeyedClass2.id);
            assertEquals(jsonStateAdvanced.unKeyedClass2.name, s.unKeyedClass2.name);

        }

        sw.close();
        sr.close();
    }

    public void testWriteJava()
        throws Exception
    {

        StateReader<JSONObject> sr = mQeoJson.createStateReader(jstAdvanced, new StateReaderListener() {

            @Override
            public void onUpdate()
            {
                sem.release();
            }
        });
        StateWriter<JsonStateAdvanced> sw = mQeo.createStateWriter(JsonStateAdvanced.class);
        sw.write(jsonStateAdvanced);

        waitForData(sem);
        boolean first = true;
        for (JSONObject s : sr) {
            assertTrue(first);
            first = false;

            assertEquals(jsonStateAdvancedJson.toString(2), s.toString(2));
        }

        sw.close();
        sr.close();
    }
}
