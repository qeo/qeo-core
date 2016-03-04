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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;

import org.json.JSONException;
import org.json.JSONObject;
import org.qeo.StateChangeReader;
import org.qeo.StateChangeReaderListener;
import org.qeo.StateReader;
import org.qeo.StateReaderListener;
import org.qeo.StateWriter;
import org.qeo.json.QeoFactoryJSON;
import org.qeo.json.QeoJSON;
import org.qeo.json.types.JsonState;

public class JsonTestState
    extends JsonAndroidTestCase
{
    private StateWriter<JsonState> sw;
    private StateWriter<JSONObject> jsw;

    private StateReader<JsonState> sr;
    private StateChangeReader<JsonState> scr;
    private StateReader<JSONObject> jsr;
    private StateChangeReader<JSONObject> jscr;

    private QeoFactoryJSON mQeoJson;

    private static final String TD_STATE = "{ topic: 'org.qeo.json.types.JsonState', behavior: 'STATE', "
        + "properties: {" + "id: {type: 'STRING', key: true }," + "name: { type: 'STRING' }," + "i: {type: 'int32' }"
        + "}}";

    private static class StateUpdate
    {
        public JsonState mState;
        public boolean mRemoved;

        StateUpdate(JsonState state, boolean removed)
        {
            this.mState = state;
            this.mRemoved = removed;
        }
    }

    private final LinkedList<StateUpdate> recvStatesNormal = new LinkedList<StateUpdate>();
    private final LinkedList<StateUpdate> recvStatesJSON = new LinkedList<StateUpdate>();

    private static class TestStates
    {
        private JsonState stateJson;
        private JsonState stateJava;
    }

    private Map<String, TestStates> getStatesFromReaders()
    {
        Map<String, TestStates> map = new HashMap<String, TestStates>();
        for (JsonState s : sr) {
            // iterator over java reader
            TestStates t = new TestStates();
            t.stateJava = s;
            t.stateJson = null;
            map.put(s.id, t);
        }

        for (JSONObject s : jsr) {
            // iterate over json reader
            JsonState t = jsonToTestState(s);
            assertTrue(map.containsKey(t.id));
            TestStates ts = map.get(t.id);
            ts.stateJson = t;
        }
        return map;
    }

    private JsonState jsonToTestState(JSONObject json)
    {
        try {
            JsonState st = new JsonState();
            st.id = json.getString("id");
            st.name = json.optString("name", null);
            st.i = json.optInt("i", 0);
            return st;
        }
        catch (Exception e) {
            println("jsonToTestState failed: " + e);
        }
        return null;
    }

    private boolean compareTestState(JsonState one, JsonState two)
    {
        boolean ok = one.id.equals(two.id) && one.i == two.i;
        if (!ok) {
            return false;
        }
        if (one.name == null) {
            return (two.name == null);
        }
        return one.name.equals(two.name);
    }

    private class NormalStateChangeListener
        implements StateChangeReaderListener<JsonState>
    {
        @Override
        public void onData(JsonState s)
        {
            recvStatesNormal.add(new StateUpdate(s, false));
        }

        @Override
        public void onRemove(JsonState s)
        {
            recvStatesNormal.add(new StateUpdate(s, true));
        }

        @Override
        public void onNoMoreData()
        {
        }
    }

    private class JSONStateChangeListener
        implements StateChangeReaderListener<JSONObject>
    {
        @Override
        public void onData(JSONObject json)
        {
            recvStatesJSON.add(new StateUpdate(jsonToTestState(json), false));
        }

        @Override
        public void onRemove(JSONObject json)
        {
            recvStatesJSON.add(new StateUpdate(jsonToTestState(json), true));
        }

        @Override
        public void onNoMoreData()
        {
        }
    }

    private static class NormalStateListener
        implements StateReaderListener
    {
        @Override
        public void onUpdate()
        {
        }
    }

    private static class JSONStateListener
        implements StateReaderListener
    {
        @Override
        public void onUpdate()
        {
        }
    }

    @Override
    public void setUp()
        throws Exception
    {
        super.setUp(); // call as first
        println("setup");
        mQeoJson = QeoJSON.getFactory(mQeo);
        JSONObject jsonTdState = null;

        jsonTdState = new JSONObject(TD_STATE);
        sw = mQeo.createStateWriter(JsonState.class);
        jsw = mQeoJson.createStateWriter(jsonTdState);
        assertNotNull(sw);
        assertNotNull(jsw);

        sr = mQeo.createStateReader(JsonState.class, new NormalStateListener());
        scr = mQeo.createStateChangeReader(JsonState.class, new NormalStateChangeListener());
        jsr = mQeoJson.createStateReader(jsonTdState, new JSONStateListener());
        jscr = mQeoJson.createStateChangeReader(jsonTdState, new JSONStateChangeListener());
        assertNotNull(sr);
        assertNotNull(scr);
        assertNotNull(jsr);
        assertNotNull(jscr);
    }

    @Override
    public void tearDown()
        throws Exception
    {
        println("tearDown");
        if (sw != null) {
            sw.close();
        }
        if (jsw != null) {
            jsw.close();
        }
        if (sr != null) {
            sr.close();
        }
        if (scr != null) {
            scr.close();
        }
        if (jsr != null) {
            jsr.close();
        }
        if (jscr != null) {
            jscr.close();
        }
        super.tearDown(); // call as last
    }

    public void testStates()
        throws Exception
    {
        println("testStates");
        JsonState state = new JsonState("java", "javaname", 12345);
        JsonState stateupdate = new JsonState("java", "javarenamed", 56789);
        JSONObject json = null;
        JSONObject jsonupdate = null;
        try {
            json = new JSONObject("{id:'json', name:'jsonname', i:54321}");
            jsonupdate = new JSONObject("{id:'json', name:'jsonrenamed', i:98765}");
        }
        catch (JSONException e) {
            // cannot happen
        }

        println("step 1: publish");
        for (int i = 0; i < 5; ++i) {
            sw.write(state);
            Thread.sleep(50);
            jsw.write(json);
            Thread.sleep(50);
            sw.write(stateupdate);
            Thread.sleep(50);
            jsw.write(jsonupdate);
            Thread.sleep(50);
        }
        Thread.sleep(200);
        /* compare the received updates at both sides */
        assertTrue(recvStatesNormal.size() == recvStatesJSON.size());
        while (recvStatesNormal.size() > 0) {
            StateUpdate normal = recvStatesNormal.removeFirst();
            StateUpdate jsonstate = recvStatesJSON.removeFirst();
            assertTrue(compareTestState(normal.mState, jsonstate.mState));
            assertTrue(normal.mRemoved == jsonstate.mRemoved);
        }
        /* compare the StateReader local caches */

        for (Entry<String, TestStates> statesEntry : getStatesFromReaders().entrySet()) {
            TestStates states = statesEntry.getValue();
            assertNotNull(states.stateJava);
            assertNotNull(states.stateJson);
            assertTrue(compareTestState(states.stateJava, states.stateJson));
        }

        println("step 2: dispose");
        sw.remove(state);
        Thread.sleep(50);
        try {
            jsw.remove(new JSONObject("{id:'json', name:'', i:0}"));
        }
        catch (JSONException e) {
            // cannot happen
        }

        Thread.sleep(200);
        /* compare the received updates at both sides */
        assertTrue(recvStatesNormal.size() == recvStatesJSON.size());
        while (recvStatesNormal.size() > 0) {
            StateUpdate normal = recvStatesNormal.removeFirst();
            StateUpdate jsonstate = recvStatesJSON.removeFirst();
            assertTrue(normal.mRemoved == jsonstate.mRemoved);
            assertTrue(compareTestState(normal.mState, jsonstate.mState));
        }
        /* compare the StateReader local caches */
        for (Entry<String, TestStates> statesEntry : getStatesFromReaders().entrySet()) {
            TestStates states = statesEntry.getValue();
            assertNotNull(states.stateJava);
            assertNotNull(states.stateJson);
            assertTrue(compareTestState(states.stateJava, states.stateJson));
        }
    }
}
