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

package org.qeo.json;

import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.qeo.EventReader;
import org.qeo.EventWriter;
import org.qeo.internal.common.ObjectData;
import org.qeo.internal.common.ObjectType;
import org.qeo.internal.reflection.ReflectionUtil;
import org.qeo.json.internal.JSONIntrospection;
import org.qeo.json.types.JsonAdvancedTypes;
import org.qeo.json.types.JsonTestArrayTypes;
import org.qeo.json.types.JsonTestTypes;
import org.qeo.testframework.QeoTestCase;
import org.qeo.testframework.TestListener;
import org.qeo.unittesttypes.TestEnum;

/**
 * 
 */
public class JsonTestEvent
    extends QeoTestCase
{
    private QeoFactoryJSON mQeoJson;

    private static final String TD_ENUM = "{ enum: 'org.qeo.unittesttypes.TestEnum', " + "values: {"
        + " ENUM_ZERO: 0, ENUM_FIRST: 1, ENUM_SECOND: 2 }}";

    private static final String TD_EVENT_MEMBERS = "{ 'longtype': {type: 'INT64'}," + "'inttype': {type: 'int32'},"
        + "'shorttype': {type: 'int16'}," + "'stringtype': { type: 'STRING' }," + "'bytetype': {type: 'BYTE'},"
        + "'floattype': {type: 'float32'}," + "'booleantype': {type: 'BOOLEAN' }, "
        + "'enumtype': {type: 'enum', item: " + TD_ENUM + " }}";

    private static final String TD_EVENT =
        "{ topic: 'org.qeo.json.types.JsonTestTypes', behavior: 'event', properties: " + TD_EVENT_MEMBERS + "}";

    public static final String TD_EVENT_ARRAY =
        "{ topic: 'org.qeo.json.types.JsonTestArrayTypes', behavior: 'event', properties: {"
            + "longarraytype:    {type: 'array', items: {type: 'int64'}},"
            + "intarraytype:     {type: 'array', items: {type: 'int32'}},"
            + "shortarraytype:   {type: 'array', items: {type: 'int16'}},"
            + "stringarraytype:  {type: 'array', items: {type: 'string'}},"
            + "floatarraytype:   {type: 'array', items: {type: 'float32'}},"
            + "bytearraytype:    {type: 'array', items: {type: 'byte'}},"
            + "booleanarraytype: {type: 'array', items: {type: 'boolean'}},"
            + "enumarraytype: {type: 'array', items: {type: 'enum', item: " + TD_ENUM + "}}" + "}}";

    public static final String TD_ADVANCED =
        "{ topic: 'org.qeo.json.types.JsonAdvancedTypes', behavior: 'event', properties: {"
            + "'types1': {type: object, item: " + TD_EVENT + "}, 'types2': {type:object, item: " + TD_EVENT + "}}}";

    private JSONObject jsonTdEvent;
    private JSONObject jsonTdArrayEvent;
    private JSONObject jsonTdAdvanced;

    @Override
    public void setUp()
        throws Exception
    {
        super.setUp();
        mQeoJson = QeoJSON.getFactory(mQeo);

        jsonTdEvent = new JSONObject(TD_EVENT);
        jsonTdArrayEvent = new JSONObject(TD_EVENT_ARRAY);
        jsonTdAdvanced = new JSONObject(TD_ADVANCED);
    }

    public void testInternalEvent()
    {
        JSONIntrospection introspectionJson = new JSONIntrospection();
        ObjectType typeJson = introspectionJson.typeFromTypedesc(jsonTdEvent);

        ReflectionUtil<JsonTestTypes> introspectionJava = new ReflectionUtil<JsonTestTypes>(JsonTestTypes.class);
        ObjectType typeJava = introspectionJava.typeFromTypedesc(JsonTestTypes.class);

        assertEquals(typeJava.toString(2), typeJson.toString(2));
    }

    public void testInternalAdvanced()
    {
        JSONIntrospection introspectionJson = new JSONIntrospection();
        ObjectType typeJson = introspectionJson.typeFromTypedesc(jsonTdAdvanced);

        ReflectionUtil<JsonAdvancedTypes> introspectionJava =
            new ReflectionUtil<JsonAdvancedTypes>(JsonAdvancedTypes.class);
        ObjectType typeJava = introspectionJava.typeFromTypedesc(JsonAdvancedTypes.class);

        assertEquals(typeJava.toString(2), typeJson.toString(2));
    }

    public void testInternalArray()
    {
        JSONIntrospection introspectionJson = new JSONIntrospection();
        ObjectType typeJson = introspectionJson.typeFromTypedesc(jsonTdArrayEvent);

        ReflectionUtil<JsonTestArrayTypes> introspectionJava =
            new ReflectionUtil<JsonTestArrayTypes>(JsonTestArrayTypes.class);
        ObjectType typeJava = introspectionJava.typeFromTypedesc(JsonTestArrayTypes.class);

        assertEquals(typeJava.toString(2), typeJson.toString(2));
    }

    private static class TestTypesJson
        extends JSONObject
    {
        // CHECKSTYLE.OFF: ParameterNumber - need more than 7 to test all types
        public TestTypesJson(long longtypeVal, int inttypeVal, short shorttypeVal, String stringtypeVal,
            byte bytetypeVal, float floattypeVal, boolean booleantypeVal, TestEnum enumtypeVal)
        // CHECKSTYLE.ON: ParameterNumber
        {
            super(toMap(longtypeVal, inttypeVal, shorttypeVal, stringtypeVal, bytetypeVal, floattypeVal,
                booleantypeVal, enumtypeVal));
        }

        // CHECKSTYLE.OFF: ParameterNumber - need more than 7 to test all types
        public TestTypesJson(long[] longArraytypeVal, int[] intArraytypeVal, short[] shortArraytypeVal,
            String[] stringArraytypeVal, byte[] byteArraytypeVal, float[] floatArraytypeVal,
            boolean[] booleanArraytypeVal, TestEnum[] enumArraytypeVal)
        // CHECKSTYLE.ON: ParameterNumber
        {
            super(toMap(longArraytypeVal, intArraytypeVal, shortArraytypeVal, stringArraytypeVal, byteArraytypeVal,
                floatArraytypeVal, booleanArraytypeVal, enumArraytypeVal));
        }

        // CHECKSTYLE.OFF: ParameterNumber - need more than 7 to test all types
        private static Map<String, Object> toMap(long longtypeVal, int inttypeVal, short shorttypeVal,
            String stringtypeVal, byte bytetypeVal, float floattypeVal, boolean booleantypeVal, TestEnum enumtypeVal)
        // CHECKSTYLE.ON: ParameterNumber
        {
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("longtype", Long.toString(longtypeVal));
            map.put("inttype", inttypeVal);
            map.put("stringtype", stringtypeVal);
            map.put("bytetype", bytetypeVal);
            map.put("floattype", floattypeVal);
            map.put("booleantype", booleantypeVal);
            map.put("shorttype", shorttypeVal);
            map.put("enumtype", enumtypeVal.ordinal());
            return map;
        }

        private static JSONArray fromArray(Object array)
        {
            JSONArray json = new JSONArray();
            int size = Array.getLength(array);
            for (int i = 0; i < size; i++) {
                json.put(Array.get(array, i));
            }
            return json;
        }

        private static JSONArray fromEnumArray(TestEnum[] array)
        {
            JSONArray json = new JSONArray();
            for (int i = 0; i < array.length; i++) {
                json.put(array[i].ordinal());
            }
            return json;
        }

        // CHECKSTYLE.OFF: ParameterNumber - need more than 7 to test all types
        private static Map<String, Object> toMap(long[] longArraytypeVal, int[] intArraytypeVal,
            short[] shortArraytypeVal, String[] stringArraytypeVal, byte[] byteArraytypeVal, float[] floatArraytypeVal,
            boolean[] booleanArraytypeVal, TestEnum[] enumArraytypeVal)
        // CHECKSTYLE.ON: ParameterNumber
        {
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("longarraytype", fromArray(longArraytypeVal));
            map.put("intarraytype", fromArray(intArraytypeVal));
            map.put("stringarraytype", fromArray(stringArraytypeVal));
            map.put("bytearraytype", fromArray(byteArraytypeVal));
            map.put("floatarraytype", fromArray(floatArraytypeVal));
            map.put("booleanarraytype", fromArray(booleanArraytypeVal));
            map.put("shortarraytype", fromArray(shortArraytypeVal));
            map.put("enumarraytype", fromEnumArray(enumArraytypeVal));
            return map;

        }

    }

    private void compare(JsonTestTypes type, JSONObject obj)
        throws JSONException
    {
        assertNotNull(type);
        assertNotNull(obj);
        assertEquals(type.longtype, obj.getLong("longtype"));
        assertEquals(type.inttype, obj.getInt("inttype"));
        assertEquals(type.stringtype, obj.getString("stringtype"));
        assertEquals(type.bytetype, obj.getInt("bytetype"));
        assertEquals(type.floattype, obj.getDouble("floattype"), 0.0001);
        assertEquals(type.booleantype, obj.getBoolean("booleantype"));
        assertEquals(type.enumtype.ordinal(), obj.getInt("enumtype"));
    }

    private void compare(JsonTestArrayTypes type, JSONObject obj)
        throws JSONException
    {
        assertNotNull(type);
        assertNotNull(obj);

        /* Test for an int array */
        if (!(type.intarraytype == null || type.intarraytype.length == 0)) {
            JSONArray intArray = obj.getJSONArray("intarraytype");
            assertEquals(type.intarraytype.length, intArray.length());
            for (int i = 0; i < intArray.length(); i++) {
                assertEquals(type.intarraytype[i], intArray.getInt(i));
            }
        }

        /* Test for a long array */
        if (!(type.longarraytype == null || type.longarraytype.length == 0)) {
            JSONArray longArray = obj.getJSONArray("longarraytype");
            assertEquals(type.longarraytype.length, longArray.length());
            for (int i = 0; i < longArray.length(); i++) {
                assertEquals(type.longarraytype[i], longArray.getLong(i));
            }
        }

        /* Test for an int array */
        if (!(type.shortarraytype == null || type.shortarraytype.length == 0)) {
            JSONArray shortArray = obj.getJSONArray("shortarraytype");
            assertEquals(type.shortarraytype.length, shortArray.length());
            for (int i = 0; i < shortArray.length(); i++) {
                assertEquals(type.shortarraytype[i], shortArray.getInt(i));
            }
        }

        /* Test for a string array */
        if (!(type.stringarraytype == null || type.stringarraytype.length == 0)) {
            JSONArray stringArray = obj.getJSONArray("stringarraytype");
            assertEquals(type.stringarraytype.length, stringArray.length());
            for (int i = 0; i < stringArray.length(); i++) {
                assertEquals(type.stringarraytype[i], stringArray.getString(i));
            }
        }

        /* Test for a float array */
        if (!(type.floatarraytype == null || type.floatarraytype.length == 0)) {
            JSONArray floatArray = obj.getJSONArray("floatarraytype");
            assertEquals(type.floatarraytype.length, floatArray.length());
            for (int i = 0; i < floatArray.length(); i++) {
                assertEquals(type.floatarraytype[i], floatArray.getDouble(i), 0.0001);
            }
        }

        /* Test for a boolean array */
        if (!(type.booleanarraytype == null || type.booleanarraytype.length == 0)) {
            JSONArray booleanArray = obj.getJSONArray("booleanarraytype");
            assertEquals(type.booleanarraytype.length, booleanArray.length());
            for (int i = 0; i < booleanArray.length(); i++) {
                assertEquals(type.booleanarraytype[i], booleanArray.getBoolean(i));
            }
        }

        /* Test for a byte array */
        if (!(type.bytearraytype == null || type.bytearraytype.length == 0)) {
            JSONArray byteArray = obj.getJSONArray("bytearraytype");
            assertEquals(type.bytearraytype.length, byteArray.length());
            for (int i = 0; i < byteArray.length(); i++) {
                assertEquals(type.bytearraytype[i], byteArray.getInt(i));
            }
        }

        /* Test for an enum array */
        if (!(type.enumarraytype == null || type.enumarraytype.length == 0)) {
            JSONArray enumArray = obj.getJSONArray("enumarraytype");
            assertEquals(type.enumarraytype.length, enumArray.length());
            for (int i = 0; i < enumArray.length(); i++) {
                assertEquals(type.enumarraytype[i].ordinal(), enumArray.getInt(i));
            }
        }

        /** Test for a struct array once it is supported */
        /**
         * if (!(type.structarraytype == null || type.structarraytype.length == 0)) { JSONArray structArray =
         * obj.getJSONArray("structarraytype"); assertEquals(type.structarraytype.length, structArray.length());
         * 
         * for (int i = 0; i < structArray.length(); i++) { compare(type.structarraytype[i],
         * structArray.getJSONObject(i)); }
         * 
         * }
         */
    }

    private void compare(JsonAdvancedTypes type, JSONObject obj)
        throws JSONException
    {
        assertNotNull(type);
        assertNotNull(obj);
        compare(type.types1, obj.getJSONObject("types1"));
        compare(type.types2, obj.getJSONObject("types2"));
    }

    public void testEventArrayJsonToJava()
        throws Exception
    {

        /** Debug */
        /**
         * System.out
         * .println("################################################################## testEvenArrayJsonToJava");
         * 
         * JSONUtil jsutil = new JSONUtil(); System.out.println(jsutil.typeFromTypedesc(jsonTdArrayEvent)); System.out
         * .println("============================================================================================");
         * ReflectionUtil<TestArrayTypes> rfutil = new ReflectionUtil<TestArrayTypes>(TestArrayTypes.class);
         * System.out.println(rfutil.typeFromTypedesc(TestArrayTypes.class));
         */

        int numSamples = 10;
        int arraySize = 10;

        long[] longarraytype = new long[arraySize];
        int[] inttarrayype = new int[arraySize];
        String[] stringarraytype = new String[arraySize];
        float[] floatarraytype = new float[arraySize];
        boolean[] booleanarraytype = new boolean[arraySize];
        byte[] bytearraytype = new byte[arraySize];
        short[] shortarraytype = new short[arraySize];
        TestEnum[] enumarraytype = new TestEnum[arraySize];

        /** TestTypes[] structarraytype = new TestTypes[arraySize]; **/

        EventReader<JsonTestArrayTypes> readerJava = null;
        EventWriter<JSONObject> writerJson = null;
        try {
            TestListener<JsonTestArrayTypes> listenerJava = new TestListener<JsonTestArrayTypes>("listener-java");
            writerJson = mQeoJson.createEventWriter(jsonTdArrayEvent);
            readerJava = mQeo.createEventReader(JsonTestArrayTypes.class, listenerJava);

            Thread.sleep(100); // connection time
            for (int i = 0; i < numSamples; ++i) {

                for (int j = 0; j < arraySize; ++j) {

                    longarraytype[j] = j * 100L;
                    inttarrayype[j] = j * 10;
                    stringarraytype[j] = "String " + j;
                    floatarraytype[j] = (float) j / 10;
                    booleanarraytype[j] = ((j % 2 == 0) ? true : false);
                    bytearraytype[j] = (byte) j;
                    shortarraytype[j] = (short) (i * 2);
                    enumarraytype[j] = TestEnum.values()[j % TestEnum.values().length];
                }

                TestTypesJson type =
                    new TestTypesJson(longarraytype, inttarrayype, shortarraytype, stringarraytype, bytearraytype,
                        floatarraytype, booleanarraytype, enumarraytype);

                // write from json to java
                writerJson.write(type);
                waitForData(listenerJava.onDataSem);
                compare(listenerJava.getLastReceived(), type);
            }

        }
        finally {

            if (readerJava != null) {
                readerJava.close();
            }
            if (writerJson != null) {
                writerJson.close();
            }
        }

    }

    public void testEventArrayJavaToJson()
        throws Exception
    {

        int numSamples = 10;
        int arraySize = 10;

        long[] longarraytype = new long[arraySize];
        int[] inttarrayype = new int[arraySize];
        String[] stringarraytype = new String[arraySize];
        float[] floatarraytype = new float[arraySize];
        boolean[] booleanarraytype = new boolean[arraySize];
        byte[] bytearraytype = new byte[arraySize];
        TestEnum[] enumarraytype = new TestEnum[arraySize];

        /** TestTypes[] structarraytype = new TestTypes[arraySize]; **/

        EventWriter<JsonTestArrayTypes> writerJava = null;
        EventReader<JSONObject> readerJson = null;
        try {
            TestListener<JSONObject> listenerJson = new TestListener<JSONObject>("listener-json");
            writerJava = mQeo.createEventWriter(JsonTestArrayTypes.class);
            readerJson = mQeoJson.createEventReader(jsonTdArrayEvent, listenerJson);

            Thread.sleep(100); // connection time
            for (int i = 0; i < numSamples; ++i) {

                for (int j = 0; j < arraySize; ++j) {

                    longarraytype[j] = j * 100L;
                    inttarrayype[j] = j * 10;
                    stringarraytype[j] = "String " + j;
                    floatarraytype[j] = (float) j / 10;
                    booleanarraytype[j] = ((j % 2 == 0) ? true : false);
                    bytearraytype[j] = (byte) j;
                    enumarraytype[j] = TestEnum.values()[j % TestEnum.values().length];
                }

                /**
                 * for (int x = 0; x < arraySize; ++x) { structarraytype[x] = new TestTypes(longarraytype[x],
                 * inttarrayype[x], stringarraytype[x], bytearraytype[x], bytearraytype, floatarraytype[x],
                 * booleanarraytype[x]); }
                 */

                JsonTestArrayTypes type =
                    new JsonTestArrayTypes(longarraytype, inttarrayype, stringarraytype, bytearraytype, floatarraytype,
                        booleanarraytype, enumarraytype);

                // write from java to json
                writerJava.write(type);
                waitForData(listenerJson.onDataSem);
                compare(type, listenerJson.getLastReceived());
            }

        }
        finally {

            if (readerJson != null) {
                readerJson.close();
            }
            if (writerJava != null) {
                writerJava.close();
            }
        }
    }

    public void testEventJsonToJava()
        throws Exception
    {

        int numSamples = 10;
        EventReader<JsonTestTypes> readerJava = null;
        EventWriter<JSONObject> writerJson = null;
        try {
            TestListener<JsonTestTypes> listenerJava = new TestListener<JsonTestTypes>("listener-java");
            writerJson = mQeoJson.createEventWriter(jsonTdEvent);
            readerJava = mQeo.createEventReader(JsonTestTypes.class, listenerJava);

            Thread.sleep(100); // connection time
            for (int i = 0; i < numSamples; ++i) {
                TestTypesJson type1 =
                    new TestTypesJson(i * 100L, i * 10, (short) i, "String " + i, (byte) i, (float) i / 10, true,
                        TestEnum.ENUM_FIRST);

                // write from json to java
                writerJson.write(type1);
                waitForData(listenerJava.onDataSem);
                compare(listenerJava.getLastReceived(), type1);
            }

        }
        finally {

            if (readerJava != null) {
                readerJava.close();
            }
            if (writerJson != null) {
                writerJson.close();
            }
        }
    }

    public void testEventJavaToJson()
        throws Exception
    {

        int numSamples = 10;
        EventWriter<JsonTestTypes> writerJava = null;
        EventReader<JSONObject> readerJson = null;
        try {
            TestListener<JSONObject> listenerJson = new TestListener<JSONObject>("listener-json");
            writerJava = mQeo.createEventWriter(JsonTestTypes.class);
            readerJson = mQeoJson.createEventReader(jsonTdEvent, listenerJson);

            Thread.sleep(100); // connection time
            for (int i = 0; i < numSamples; ++i) {
                JsonTestTypes type2 =
                    new JsonTestTypes(i * 100L, i * 10, "String " + i, (byte) i, new byte[] {1, 2, 3, 4, 5, 6, 7},
                        (float) i / 10, true, TestEnum.ENUM_FIRST);

                // write from java to json
                writerJava.write(type2);
                waitForData(listenerJson.onDataSem);
                compare(type2, listenerJson.getLastReceived());
            }

        }
        finally {

            if (readerJson != null) {
                readerJson.close();
            }
            if (writerJava != null) {
                writerJava.close();
            }
        }
    }

    /**
     * Test jsonintrospection by calling it directly and not going over qeo
     */
    public void testTransformation()
        throws Exception
    {
        JSONIntrospection jsi = new JSONIntrospection();
        ObjectType type = jsi.typeFromTypedesc(jsonTdAdvanced);
        JSONObject type1 =
            new TestTypesJson(1, 2, (short) 10, "type1", (byte) 3, (float) 4.5, true, TestEnum.ENUM_FIRST);
        JSONObject type2 =
            new TestTypesJson(5, 6, (short) 11, "type2", (byte) 7, (float) 11.1, false, TestEnum.ENUM_SECOND);
        JSONObject at = new JSONObject();
        at.put("types1", type1);
        at.put("types2", type2);
        ObjectData od = jsi.dataFromObject(at, type); // to internal data
        JSONObject jso2 = jsi.objectFromData(od, type); // construct json data again
        assertNotNull(jso2);
        JSONObject type1a = jso2.getJSONObject("types1");
        assertEquals(type1.getLong("longtype"), type1a.getLong("longtype"));
        assertEquals(type1.getInt("inttype"), type1a.getInt("inttype"));
        assertEquals(type1.getInt("shorttype"), type1a.getInt("shorttype"));
        assertEquals(type1.getString("stringtype"), type1a.getString("stringtype"));
        assertEquals(type1.getDouble("floattype"), type1a.getDouble("floattype"), 0.0001);
        assertEquals(type1.getInt("bytetype"), type1a.getInt("bytetype"));
        assertEquals(type1.getBoolean("booleantype"), type1a.getBoolean("booleantype"));
        assertEquals(type1.getInt("enumtype"), type1a.getInt("enumtype"));
        JSONObject type2a = jso2.getJSONObject("types2");
        assertEquals(type2.getLong("longtype"), type2a.getLong("longtype"));
        assertEquals(type2.getInt("inttype"), type2a.getInt("inttype"));
        assertEquals(type2.getInt("shorttype"), type2a.getInt("shorttype"));
        assertEquals(type2.getString("stringtype"), type2a.getString("stringtype"));
        assertEquals(type2.getDouble("floattype"), type2a.getDouble("floattype"), 0.0001);
        assertEquals(type2.getInt("bytetype"), type2a.getInt("bytetype"));
        assertEquals(type2.getBoolean("booleantype"), type2a.getBoolean("booleantype"));
        assertEquals(type2.getInt("enumtype"), type2a.getInt("enumtype"));
    }

    public void testAdvancedJavaToJson()
        throws Exception
    {

        EventWriter<JsonAdvancedTypes> writerJava = null;
        EventReader<JSONObject> readerJson = null;
        EventReader<JsonAdvancedTypes> readerJava = null;
        try {
            TestListener<JSONObject> listenerJson = new TestListener<JSONObject>("listener-json");
            TestListener<JsonAdvancedTypes> listenerJava = new TestListener<JsonAdvancedTypes>("listener-java");
            writerJava = mQeo.createEventWriter(JsonAdvancedTypes.class);
            readerJson = mQeoJson.createEventReader(jsonTdAdvanced, listenerJson);
            readerJava = mQeo.createEventReader(JsonAdvancedTypes.class, listenerJava);

            Thread.sleep(100); // connection time

            JsonTestTypes type1 =
                new JsonTestTypes(1, 2, "type1", (byte) 3, new byte[] {1, 2, 3}, 4, true, TestEnum.ENUM_FIRST);
            JsonTestTypes type2 =
                new JsonTestTypes(5, 6, "type2", (byte) 7, new byte[] {8, 9, 10}, 11, false, TestEnum.ENUM_SECOND);
            JsonAdvancedTypes at = new JsonAdvancedTypes();
            at.types1 = type1;
            at.types2 = type2;

            // write from java
            writerJava.write(at);
            // read from java
            waitForData(listenerJava.onDataSem);
            // read from json
            waitForData(listenerJson.onDataSem);
            compare(at, listenerJson.getLastReceived());

        }
        finally {

            if (readerJson != null) {
                readerJson.close();
            }
            if (readerJava != null) {
                readerJava.close();
            }
            if (writerJava != null) {
                writerJava.close();
            }
        }
    }

    public void atestAdvancedJsonToJava()
        throws Exception
    {

        EventWriter<JSONObject> writerJson = null;
        EventReader<JSONObject> readerJson = null;
        EventReader<JsonAdvancedTypes> readerJava = null;
        try {
            TestListener<JSONObject> listenerJson = new TestListener<JSONObject>("listener-json");
            TestListener<JsonAdvancedTypes> listenerJava = new TestListener<JsonAdvancedTypes>("listener-java");
            writerJson = mQeoJson.createEventWriter(jsonTdAdvanced);
            readerJson = mQeoJson.createEventReader(jsonTdAdvanced, listenerJson);
            readerJava = mQeo.createEventReader(JsonAdvancedTypes.class, listenerJava);

            Thread.sleep(100); // connection time

            JSONObject type1 = new TestTypesJson(1, 2, (short) 11, "type1", (byte) 3, 4, true, TestEnum.ENUM_FIRST);
            JSONObject type2 = new TestTypesJson(5, 6, (short) 12, "type2", (byte) 7, 11, false, TestEnum.ENUM_SECOND);
            JSONObject at = new JSONObject();
            at.put("types1", type1);
            at.put("types2", type2);

            // write from json
            writerJson.write(at);
            // read from json
            waitForData(listenerJson.onDataSem);
            // read from java
            waitForData(listenerJava.onDataSem);
            compare(listenerJava.getLastReceived(), at);

        }
        finally {

            if (readerJson != null) {
                readerJson.close();
            }
            if (readerJava != null) {
                readerJava.close();
            }
            if (writerJson != null) {
                writerJson.close();
            }
        }
    }

    /*
     * Test that creates 2 types that use the same subtype. Test to see if type registration can handle this.
     */
    public void testSubClassReUse()
        throws Exception
    {

        String subClassJson =
            "{topic: 'org::qeo::json::JsonTestEvent_SubClass', properties: {'subClassId' : {type: 'int32'}}}";
        String outerClass1Json =
            "{topic: 'org::qeo::json::JsonTestEvent_OuterClass1', behavior: 'event'," + "properties: {"
                + "'outerClassId1' : {type: 'int32'}," + "'subClass' : {type: 'object', item: " + subClassJson + "}}}";
        String outerClass2Json =
            "{topic: 'org::qeo::json::JsonTestEvent_OuterClass2', behavior: 'event'," + "properties: {"
                + "'outerClassId2' : {type: 'int32'}," + "'subClass' : {type: 'object', item: " + subClassJson + "}}}";

        JSONObject outerClass1 = new JSONObject(outerClass1Json);
        JSONObject outerClass2 = new JSONObject(outerClass2Json);

        JSONIntrospection ji = new JSONIntrospection();
        ObjectType typeJson1 = ji.typeFromTypedesc(outerClass1);
        ObjectType typeJson2 = ji.typeFromTypedesc(outerClass2);

        ReflectionUtil<OuterClass1> ri1 = new ReflectionUtil<OuterClass1>(OuterClass1.class);
        ObjectType typeJava1 = ri1.typeFromTypedesc(OuterClass1.class);
        ReflectionUtil<OuterClass2> ri2 = new ReflectionUtil<OuterClass2>(OuterClass2.class);
        ObjectType typeJava2 = ri2.typeFromTypedesc(OuterClass2.class);

        // validate that types are internally the same
        assertEquals(typeJava1.toString(), typeJson1.toString());
        assertEquals(typeJava2.toString(), typeJson2.toString());

        // create 2 java writers
        EventWriter<OuterClass1> ew1 = mQeo.createEventWriter(OuterClass1.class);
        EventWriter<OuterClass2> ew2 = mQeo.createEventWriter(OuterClass2.class);

        // create 2 json writers
        EventWriter<JSONObject> jw1 = mQeoJson.createEventWriter(outerClass1);
        EventWriter<JSONObject> jw2 = mQeoJson.createEventWriter(outerClass2);

        // if we get here type registration worked.

        ew1.close();
        ew2.close();
        jw1.close();
        jw2.close();
    }

    public static class SubClass
    {
        public int subClassId;
    }

    public static class OuterClass1
    {
        public int outerClassId1;
        public SubClass subClass;
    }

    public static class OuterClass2
    {
        public int outerClassId2;
        public SubClass subClass;
    }
}
