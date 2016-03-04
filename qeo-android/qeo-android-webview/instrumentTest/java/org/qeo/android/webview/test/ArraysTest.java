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

package org.qeo.android.webview.test;

import java.util.Arrays;

import org.json.JSONObject;
import org.qeo.EventReader;
import org.qeo.EventWriter;
import org.qeo.internal.common.ObjectType;
import org.qeo.internal.reflection.ReflectionUtil;
import org.qeo.json.QeoFactoryJSON;
import org.qeo.json.QeoJSON;
import org.qeo.json.internal.JSONIntrospection;
import org.qeo.json.types.JsonArrayTypes2;
import org.qeo.testframework.TestListener;

/**
 * 
 */
public class ArraysTest
    extends JsonAndroidTestCase
{
    private static final String JSON_T2 = "{topic: org.qeo.json.types.JsonArrayTypes2_SubClass, properties: {"
        + "id: {type: int32}, name: {type: string}}}";
    private static final String JSON = "{topic: org.qeo.json.types.JsonArrayTypes2, type: object, behavior: EVENT,"
        + "properties: {id: {type: int32}" + ",myArrayOfInt: {type: array, items: {type: int32}}"
        // + ",myArrayOfArrayOfInt: {type: array, items: {type: array, items: {type: int32}}}"
        // + "myArrayOfArrayOfArrayOfInt: "
        // + "{type: array, items: {type: array, items: {type: array, items: {type: int32}}}}}"
        + ",myArrayOfStruct: {type: array, items: {type: object, item: " + JSON_T2 + "}}" + "}}";

    private static final String JSON_DATA = "{id: 5" + ",myArrayOfInt: [3,4,5]"
    // + ",myArrayOfArrayOfInt: [[1,2],[3,4,5]]"
    // + "myArrayOfArrayOfArrayOfInt: [[[1,2],[3,4,5]],[[7,9]]]"
        + ",myArrayOfStruct: [{id: 123, name: \"v1\"}, {id: 789, name: \"v2\"}]" + "}";
    private QeoFactoryJSON mQeoJson = null;
    private JSONObject jst;
    private JSONObject jsd;
    private JsonArrayTypes2 testClass;
    private EventReader<JsonArrayTypes2> erJava;
    private EventReader<JSONObject> erJson;
    private EventWriter<JsonArrayTypes2> ewJava;
    private EventWriter<JSONObject> ewJson;

    @Override
    public void setUp()
        throws Exception
    {
        super.setUp();
        mQeoJson = QeoJSON.getFactory(mQeo);
        int[] myArrayOfInt = new int[] {3, 4, 5};
        // int[][] myArrayOfArrayOfInt = new int[][] { {1, 2}, {3, 4, 5}};
        JsonArrayTypes2.SubClass t1 = new JsonArrayTypes2.SubClass();
        t1.id = 123;
        t1.name = "v1";
        JsonArrayTypes2.SubClass t2 = new JsonArrayTypes2.SubClass();
        t2.id = 789;
        t2.name = "v2";
        JsonArrayTypes2.SubClass[] myArrayOfStruct = new JsonArrayTypes2.SubClass[2];
        myArrayOfStruct[0] = t1;
        myArrayOfStruct[1] = t2;
        jst = new JSONObject(JSON);
        jsd = new JSONObject(JSON_DATA);

        testClass = new JsonArrayTypes2();
        testClass.id = 5;
        testClass.myArrayOfInt = myArrayOfInt;
        // testClass.myArrayOfArrayOfInt = myArrayOfArrayOfInt;
        testClass.myArrayOfStruct = myArrayOfStruct;

        erJava = null;
        erJson = null;
        ewJava = null;
        ewJson = null;
    }

    @Override
    public void tearDown()
        throws Exception
    {
        if (erJava != null) {
            erJava.close();
            erJava = null;
        }
        if (ewJava != null) {
            ewJava.close();
            ewJava = null;
        }
        if (erJson != null) {
            erJson.close();
            erJson = null;
        }
        if (ewJson != null) {
            ewJson.close();
            ewJson = null;
        }
        super.tearDown();
    }

    public void testNull()
    {
    }

    public void testInternals()
        throws Exception
    {
        JSONIntrospection i = new JSONIntrospection();
        ObjectType typeJson = i.typeFromTypedesc(jst);

        ReflectionUtil<JsonArrayTypes2> introspectionJava = new ReflectionUtil<JsonArrayTypes2>(JsonArrayTypes2.class);
        ObjectType typeJava = introspectionJava.typeFromTypedesc(JsonArrayTypes2.class);

        assertEquals(typeJava.toString(2), typeJson.toString(2));

    }

    public void testArrayOfArrayJsonWriter()
        throws Exception
    {

        TestListener<JsonArrayTypes2> l = new TestListener<JsonArrayTypes2>();

        erJava = mQeo.createEventReader(JsonArrayTypes2.class, l);
        ewJson = mQeoJson.createEventWriter(jst);
        ewJson.write(jsd); // write json data
        waitForData(l.onDataSem);
        JsonArrayTypes2 t = l.getLastReceived();
        assertNotNull(t);
        assertEquals(testClass.id, t.id);
        assertTrue(Arrays.equals(testClass.myArrayOfInt, t.myArrayOfInt));
        // assertEquals(Arrays.deepToString(myArrayOfArrayOfInt), Arrays.deepToString(t.myArrayOfArrayOfInt));
        assertEquals(Arrays.deepToString(testClass.myArrayOfStruct), Arrays.deepToString(t.myArrayOfStruct));

    }

    public void testArrayOfArrayJavaWriter()
        throws Exception
    {

        TestListener<JSONObject> l = new TestListener<JSONObject>();

        erJson = mQeoJson.createEventReader(jst, l);
        ewJava = mQeo.createEventWriter(JsonArrayTypes2.class);

        ewJava.write(testClass);

        waitForData(l.onDataSem);
        JSONObject t = l.getLastReceived();
        assertNotNull(t);
        assertEquals(jsd.toString(2), t.toString(2));

    }

}
