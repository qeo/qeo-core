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

package org.qeo.android.webview.internal;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.qeo.QeoFactory;
import org.qeo.android.QeoAndroid;
import org.qeo.android.QeoConnectionListener;
import org.qeo.android.internal.QeoConnection;
import org.qeo.android.webview.QeoWebview;
import org.qeo.exception.QeoException;
import org.qeo.json.JsonManifestParser;
import org.qeo.json.QeoJSONProxy;
import org.qeo.policy.AccessRule;
import org.qeo.policy.Identity;
import org.qeo.system.DeviceId;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.SparseArray;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

/**
 * Class to hook Qeo javascript functions into an Android Webview.
 */

public final class QeoWebviewImpl
    extends QeoWebview
    implements QeoJsCallbacks
{
    private static final Logger LOG = Logger.getLogger(QeoAndroid.TAG + ".QeoWebview");

    // name that will be used to hook javascript calls onto
    private static final String JS_OBJECT_NAME = "QeoJS";

    // variables used in json event data
    private static final String MSG_OBJTYPE = "objType";
    private static final String MSG_FACTORYID = "factoryId";
    private static final String MSG_READERID = "readerId";
    private static final String MSG_TYPEDESC = "typeDesc";
    private static final String MSG_ENABLE_POLICY = "enablePolicy";
    private static final String MSG_ID = "id";
    private static final String MSG_DATA = "data";
    private static final String MSG_ERROR = "error";
    private static final String MSG_CREATE = "create";
    private static final String MSG_TERMINATION = "termination";
    private static final String MSG_ITERATE = "iterate";
    private static final String MSG_CLOSE = "close";
    private static final String MSG_MANIFEST = "manifest";
    private static final String MSG_IDENTITY = "identity";
    private static final String MSG_OPEN = "open";
    private static final String MSG_DEFAULT = "default";
    private static final String MSG_POLICYUPDATE = "policyUpdate";
    private static final String MSG_NO_MORE_DATA = "noMoreData";
    private static final String MSG_UPDATE = "update";
    private static final String MSG_REMOVE = "remove";
    private static final String MSG_DEVICE_ID = "deviceId";

    // Main handler. Everything on this handler will be done on the UI thread.
    private static final Handler MAIN_HANDLER;
    // JavaScript handler. Everything on this handler will be done on a worker thread.
    private static final Handler JS_HANDLER;

    private final WebView mWebView;
    private final Context mContext;
    private final ExecutorService mExecutors;
    private final QeoJSONProxy mProxy;
    private final Map<Integer, QeoFactory> mFactories;
    private final Map<Integer, QeoConnectionListener> mListeners;
    private final WebViewCallbacks mWebViewCallbacks;
    private final PolicyHandler mPolicyHandler;
    private boolean mClosed;

    static {
        // create main handler
        MAIN_HANDLER = new Handler(Looper.getMainLooper());

        // create other handler
        // create thread
        HandlerThread thread = new HandlerThread();
        // start thread
        thread.start();
        // wait for thread to have the looper created
        try {
            thread.getSemaphore().acquire();
        }
        catch (InterruptedException e) {
            LOG.severe("Can't create JavaScript handler. Nothing will work.");
        }
        // return the looper
        JS_HANDLER = new Handler(thread.getLooper());
    }

    /**
     * Add qeo javascript hooks to an Android WebView.<br/>
     * NOTE: don't use this class directly, use QeoWebview.enableQeo()
     * 
     * @param context The Android application context.
     * @param webview The webview where the javascript should be attached.
     */
    @SuppressLint("UseSparseArrays")
    public QeoWebviewImpl(Context context, WebView webview)
    {
        mClosed = false;
        mWebView = webview;
        mContext = context;
        mExecutors = Executors.newCachedThreadPool();
        mPolicyHandler = new PolicyHandler();
        mWebViewCallbacks = new WebViewCallbacks();
        mProxy = new QeoJSONProxy(mWebViewCallbacks);
        mFactories = Collections.synchronizedMap(new HashMap<Integer, QeoFactory>());
        mListeners = Collections.synchronizedMap(new HashMap<Integer, QeoConnectionListener>());
        webview.addJavascriptInterface(this, JS_OBJECT_NAME);
    }

    @Override
    public void close()
    {
        mClosed = true;
        cleanup();
        mExecutors.shutdownNow();
    }

    @Override
    public void cleanup()
    {
        // create shallow copy to avoid concurrent modification exceptions
        Set<Integer> factoryIds = new HashSet<Integer>(mFactories.keySet());
        for (int factoryId : factoryIds) {
            // execute immediately, don't schedule on a thread.
            new FactoryCloser(factoryId).run();
        }
    }

    private void notifyJs(String id, String event, JSONObject jsonData, boolean postOnHandler)
    {
        StringBuilder cb = new StringBuilder();
        cb.append("javascript:Qeo.notify(");
        cb.append("\"").append(id).append("\", ");
        cb.append("\"").append(event).append("\", ");
        // fun with java escapes in regexp. You need to escape once for java string and a 2nd time for java regexp!
        // 1st replace replaces \ by \\
        // 2nd replace replaces " by \"
        cb.append("\"").append(jsonData.toString().replaceAll("\\\\", "\\\\\\\\").replaceAll("\"", "\\\\\""))
            .append("\");");
        final String finalcb = cb.toString();
        Runnable r = new Runnable() {
            @Override
            public void run()
            {
                LOG.finest("loading url:" + finalcb);
                mWebView.loadUrl(finalcb);
            }
        };
        if (postOnHandler) {
            MAIN_HANDLER.post(r);
        }
        else {
            r.run();
        }
    }

    private void notifyJs(String id, String event, JSONObject jsonData)
    {
        notifyJs(id, event, jsonData, true);
    }

    private void notifyJsError(String deferId, String msg)
    {
        notifyJs(deferId, MSG_ERROR, createErrorJson(msg.replaceAll(":", ",")));
    }

    private JSONObject createIdJson(int id)
    {
        JSONObject jso = new JSONObject();
        try {
            jso.put("id", id);
        }
        catch (JSONException e) {
            LOG.log(Level.SEVERE, "Json exception", e);
        }
        return jso;
    }

    private JSONObject createErrorJson(String msg)
    {
        JSONObject jso = new JSONObject();
        try {
            jso.put("error", msg);
        }
        catch (JSONException e) {
            LOG.log(Level.SEVERE, "Json exception", e);
        }
        return jso;
    }

    private void createReaderWriter(final ObjType type, final String deferredId, final int factoryId,
        final JSONObject typedesc, final boolean enablePolicy)
    {
        LOG.fine("Creating " + type + " for factory " + factoryId + "(" + deferredId + ") (policy: " + enablePolicy
            + ")");

        // don't schedule this on the executors, but on JS_HANDLER
        // reason: readers can fire onData events immediately after they're created.
        // if this is executed on the executors this onData event might be delivered to js before the reader is
        // delivered to js.
        // however as the onData callbacks also happen on the JS_HANDLER we're guaranteed they cannot happen before the
        // reader is delivered to js.
        JS_HANDLER.post(new Runnable() {
            @Override
            public void run()
            {
                try {
                    int id;
                    switch (type) {
                        case EVENTWRITER:
                            id = mProxy.createEventWriter(factoryId, typedesc, enablePolicy);
                            break;
                        case EVENTREADER:
                            id = mProxy.createEventReader(factoryId, typedesc, enablePolicy);
                            break;
                        case STATEWRITER:
                            id = mProxy.createStateWriter(factoryId, typedesc, enablePolicy);
                            break;
                        case STATEREADER:
                            id = mProxy.createStateReader(factoryId, typedesc, enablePolicy);
                            break;
                        case STATECHANGEREADER:
                            id = mProxy.createStateChangeReader(factoryId, typedesc, enablePolicy);
                            break;
                        default:
                            throw new IllegalStateException("Type not handled: " + type);
                    }

                    LOG.fine("Created " + type + " " + id + "(" + deferredId + ")");
                    notifyJs(deferredId, MSG_CREATE, createIdJson(id));
                }
                catch (Exception e) {
                    LOG.log(Level.FINE, "Error creating " + type + " (" + deferredId + ")", e);
                    notifyJsError(deferredId, "Error creating " + type + ", " + e.getMessage());
                }
            }
        });
    }

    private void createFactory(final String deferId, final JSONObject manifest, final String identity)
    {
        LOG.fine("create JS factory for " + deferId + "(identity: " + identity + ")");
        mExecutors.execute(new Runnable() {

            @Override
            public void run()
            {
                if (manifest != null) {
                    // NOTE: the manifest is set unrelated to the factory.
                    // this means it's not supported to have a different manifest for each factory,
                    // only the first 1 is relevant.
                    // But this is not supported on qeo-android anyway.
                    // This should be fixed sooner or later
                    try {
                        QeoConnection.setManifest(JsonManifestParser.getManifest(manifest));
                    }
                    catch (Exception ex) {
                        notifyJsError(deferId, "Error in manifest handling: " + createErrorJson(ex.getMessage()));
                        return;
                    }
                }
                QeoConnectionListener listener = new QeoConnectionListener() {
                    private int mFactoryId;

                    @Override
                    public void onQeoReady(QeoFactory qeo)
                    {
                        LOG.fine("Factory onQeoReady");
                        mFactoryId = mProxy.createJsonFactory(qeo);
                        mFactories.put(mFactoryId, qeo); // also store real factory locally
                        mListeners.put(mFactoryId, this);
                        notifyJs(deferId, MSG_CREATE, createIdJson(mFactoryId));
                    }

                    @Override
                    public void onQeoError(QeoException ex)
                    {
                        final String errMessage = ex.getMessage() != null ? ex.getMessage() : ex.toString();
                        notifyJsError(deferId, errMessage);
                    }

                    @Override
                    public void onQeoClosed(QeoFactory factory)
                    {
                        LOG.fine("Service connection lost!");
                        closeFactory(mFactoryId);
                        notifyJs(Integer.toString(mFactoryId), MSG_TERMINATION, new JSONObject());
                    }

                };

                int identityId;
                if (identity.equals(MSG_DEFAULT)) {
                    identityId = QeoFactory.DEFAULT_ID;
                }
                else if (identity.equals(MSG_OPEN)) {
                    identityId = QeoFactory.OPEN_ID;
                }
                else {
                    notifyJsError(deferId, "Invalid identity \"" + identity + "\"");
                    return; // abort
                }

                // attach a looper: this call (or any create calls) may happen from
                // threads that do not have a looper associated
                QeoAndroid.initQeo(mContext, listener, JS_HANDLER.getLooper(), identityId);
            }
        });
    }

    private void closeReaderWriter(final int factoryId, final int id)
    {
        mExecutors.execute(new Runnable() {
            @Override
            public void run()
            {
                mProxy.closeReaderWriter(factoryId, id);
                mPolicyHandler.close(id);
            }
        });
    }

    private void write(final int factoryId, final int writerId, final JSONObject data)
    {
        LOG.fine("Writing to writer " + writerId + " -- " + data);
        mExecutors.execute(new Runnable() {
            @Override
            public void run()
            {
                try {
                    mProxy.writeData(factoryId, writerId, data);
                }
                catch (Exception e) {
                    // catch any (runtime)exceptions
                    LOG.log(Level.FINE, "Write error writing to " + writerId, e);
                    notifyJsError(Integer.toString(writerId), "Write error: " + e.getMessage());
                }
            }
        });
    }

    private void removeState(final int factoryId, final int writerId, final JSONObject data)
    {
        mExecutors.execute(new Runnable() {
            @Override
            public void run()
            {
                try {
                    mProxy.removeState(factoryId, writerId, data);
                }
                catch (Exception e) {
                    // catch any (runtime)exceptions
                    LOG.log(Level.FINE, "Write error writing to " + writerId, e);
                    notifyJsError(Integer.toString(writerId), "Write error: " + e.getMessage());
                }
            }
        });
    }

    private void createIterator(final String deferId, final int factoryId, final int readerId)
    {
        mExecutors.execute(new Runnable() {
            @Override
            public void run()
            {
                try {
                    int id = mProxy.getStateIterator(factoryId, readerId);

                    if (id == -1) {
                        throw new IllegalArgumentException("Can't create iterator for reader " + readerId);
                    }
                    // notify iterator id to js
                    notifyJs(deferId, MSG_CREATE, createIdJson(id));

                    // now iterate
                    while (mProxy.iteratorHasNext(id)) {
                        JSONObject data = mProxy.iteratorNext(id);
                        // push data to javascript
                        notifyJs(Integer.toString(id), MSG_ITERATE, data);
                    }
                    // notify that iterator is done
                    notifyJs(Integer.toString(id), MSG_CLOSE, new JSONObject());

                }
                catch (Exception e) {
                    // catch any (runtime)exceptions
                    LOG.log(Level.FINE, "Error in iterator for reader " + readerId, e);
                    notifyJsError(deferId, "Iterator error: " + e.getMessage());
                }
            }
        });
    }

    private void eventCreate(JSONObject options)
    {
        try {
            String objTypeS = options.getString(MSG_OBJTYPE);
            String deferId = options.getString(MSG_ID);

            ObjType objType = ObjType.valueOf(objTypeS.toUpperCase(Locale.ENGLISH));
            switch (objType) {
                case FACTORY:
                    String identity = options.optString(MSG_IDENTITY, MSG_DEFAULT);
                    createFactory(deferId, options.optJSONObject(MSG_MANIFEST), identity);
                    break;
                case EVENTREADER:
                case EVENTWRITER:
                case STATECHANGEREADER:
                case STATEREADER:
                case STATEWRITER:
                    int factoryId = options.getInt(MSG_FACTORYID);
                    JSONObject typeDesc = options.getJSONObject(MSG_TYPEDESC);
                    boolean enablePolicy = options.optBoolean(MSG_ENABLE_POLICY, false);
                    createReaderWriter(objType, deferId, factoryId, typeDesc, enablePolicy);
                    break;
                case ITERATOR:
                    int readerId = options.getInt(MSG_READERID);
                    int factoryId2 = options.getInt(MSG_FACTORYID);
                    createIterator(deferId, factoryId2, readerId);
                    break;
                default:
                    throw new IllegalStateException("objType not handled: " + objType);
            }
        }
        catch (JSONException e) {
            throw new IllegalArgumentException("Can't parse json", e);
        }
    }

    private void eventWrite(JSONObject options)
    {
        try {
            int factoryId = options.getInt(MSG_FACTORYID);
            int writerId = options.getInt(MSG_ID);
            JSONObject data = options.getJSONObject(MSG_DATA);

            write(factoryId, writerId, data);
        }
        catch (JSONException e) {
            throw new IllegalArgumentException("Can't parse json", e);
        }
    }

    private void eventRemove(JSONObject options)
    {
        try {
            int factoryId = options.getInt(MSG_FACTORYID);
            int writerId = options.getInt(MSG_ID);
            JSONObject data = options.getJSONObject(MSG_DATA);

            removeState(factoryId, writerId, data);
        }
        catch (JSONException e) {
            throw new IllegalArgumentException("Can't parse json", e);
        }
    }

    private void closeFactory(int factoryId)
    {
        mExecutors.execute(new FactoryCloser(factoryId));
    }

    private void eventClose(JSONObject options)
    {
        try {
            int id = options.getInt(MSG_ID);
            String objTypeS = options.getString(MSG_OBJTYPE);
            ObjType objType = ObjType.valueOf(objTypeS.toUpperCase(Locale.ENGLISH));
            switch (objType) {
                case FACTORY:
                    closeFactory(id);
                    break;
                case EVENTREADER:
                case EVENTWRITER:
                case STATECHANGEREADER:
                case STATEREADER:
                case STATEWRITER:
                    int factoryId = options.getInt(MSG_FACTORYID);
                    closeReaderWriter(factoryId, id);
                    break;
                default:
                    throw new IllegalStateException("objType not handled: " + objType);
            }

        }
        catch (JSONException e) {
            throw new IllegalArgumentException("Can't parse json", e);
        }
    }

    private void eventPolicyUpdate(JSONObject options)
    {
        try {
            final int factoryId = options.getInt(MSG_FACTORYID);
            final int id = options.getInt(MSG_ID); // id of reader/writer
            JSONObject data = options.getJSONObject(MSG_DATA);
            // parse the data struct, format
            // {"users":[{"id":123, "allow":true},{"id":456, "allow":false},..]}
            JSONArray arr = data.getJSONArray("users");
            final PolicyPermissionMap newPermissions = new PolicyPermissionMap();
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                newPermissions.put(obj.getLong("id"), (obj.getBoolean("allow") ? AccessRule.ALLOW : AccessRule.DENY));
            }
            // parsing done, start the real update
            mExecutors.execute(new Runnable() {

                @Override
                public void run()
                {
                    try {
                        mPolicyHandler.setPolicyPermissions(id, newPermissions);
                        LOG.fine("requesting policyupdate for reader/writer " + id);
                        mProxy.updatePolicy(factoryId, id); // trigger a policy update
                    }
                    catch (RuntimeException ex) {
                        LOG.log(Level.SEVERE, "Error updating policy for reader/writer " + id + ":", ex);
                    }
                }
            });

        }
        catch (JSONException e) {
            throw new IllegalArgumentException("Can't parse json", e);
        }
    }

    private void eventRequestPolicy(JSONObject options)
    {
        try {
            final int factoryId = options.getInt(MSG_FACTORYID);
            final int id = options.getInt(MSG_ID); // id of reader/writer
            mExecutors.execute(new Runnable() {
                @Override
                public void run()
                {
                    try {
                        LOG.fine("requesting policyupdate for reader/writer " + id);
                        mProxy.updatePolicy(factoryId, id); // trigger a policy update
                    }
                    catch (RuntimeException ex) {
                        // catch runtimeexceptions, don't die if they happen
                        LOG.log(Level.SEVERE, "Error requesting policy for reader/writer " + id + ":", ex);
                    }
                }
            });

        }
        catch (JSONException e) {
            throw new IllegalArgumentException("Can't parse json", e);
        }
    }

    private void eventGet(JSONObject options)
    {
        try {
            final String deferId = options.getString(MSG_ID);
            String objTypeS = options.getString(MSG_OBJTYPE);
            if (MSG_DEVICE_ID.equals(objTypeS)) {
                mExecutors.execute(new Runnable() {

                    @Override
                    public void run()
                    {
                        try {
                            DeviceId deviceId = QeoAndroid.getDeviceId();
                            // note: converting manually to json struct.
                            // this could be done by using reflectionUtil/JsonUtil but would require quite a lot of
                            // internal functions for a very simple structure
                            JSONObject deviceIdJson = new JSONObject();
                            deviceIdJson.put("upper", Long.toString(deviceId.upper));
                            deviceIdJson.put("lower", Long.toString(deviceId.lower));
                            notifyJs(deferId, MSG_DATA, deviceIdJson);
                        }
                        catch (Exception ex) {
                            LOG.log(Level.FINE, "Error fetching deviceId", ex);
                            notifyJsError(deferId, "Error fetching deviceId: " + ex.getMessage());
                        }
                    }
                });
            }
            else {
                throw new IllegalArgumentException("Unsupported option: " + objTypeS);
            }
        }
        catch (JSONException e) {
            throw new IllegalArgumentException("Can't parse json", e);
        }
    }

    @Override
    @JavascriptInterface
    public void notify(String eventS, String optionsS)
    {
        try {
            LOG.finest("NOTIFY native: " + eventS + " -- " + optionsS);
            if (mClosed) {
                LOG.fine("Got notify after webview was closed, ignoring");
                return;
            }
            Event event = Event.valueOf(eventS.toUpperCase(Locale.ENGLISH));
            JSONObject options;
            try {
                options = new JSONObject(optionsS);
            }
            catch (JSONException e) {
                throw new IllegalArgumentException("Can't parse options object", e);
            }

            switch (event) {
                case CREATE:
                    eventCreate(options);
                    break;
                case WRITE:
                    eventWrite(options);
                    break;
                case REMOVE:
                    eventRemove(options);
                    break;
                case CLOSE:
                    eventClose(options);
                    break;
                case POLICYUPDATE:
                    eventPolicyUpdate(options);
                    break;
                case REQUESTPOLICY:
                    eventRequestPolicy(options);
                    break;
                case GET:
                    eventGet(options);
                    break;
                default:
                    throw new IllegalStateException("event not handled: " + event);
            }
        }
        catch (RuntimeException ex) {
            LOG.log(Level.SEVERE, "Internal error: ", ex);
            throw ex;
        }
    }

    // ///////////////
    // Helper classes
    // ///////////////

    private static class PolicyPermissionMap
        extends HashMap<Long, AccessRule>
    {
        private static final long serialVersionUID = 1L;
        private boolean mFirst = true;

        public boolean isFirst()
        {
            return mFirst;
        }

        public void setFirst(boolean first)
        {
            mFirst = first;
        }
    }

    private class PolicyHandler
    {
        // current permissions
        private final SparseArray<PolicyPermissionMap> mPolicyPermissions;
        // permissions that have been set from javascript
        private final SparseArray<PolicyPermissionMap> mPolicyPermissionsJS;
        // old permissions. Cached for later use.
        private final SparseArray<PolicyPermissionMap> mPolicyPermissionsOld;

        PolicyHandler()
        {
            mPolicyPermissions = new SparseArray<PolicyPermissionMap>();
            mPolicyPermissionsJS = new SparseArray<PolicyPermissionMap>();
            mPolicyPermissionsOld = new SparseArray<PolicyPermissionMap>();
        }

        synchronized AccessRule onPolicyUpdate(final int id, Identity identity)
        {
            LOG.finest("onPolicyUpdate: " + id + " -- " + (identity == null ? "null" : identity.getUserID()));
            // policy updates get buffered for javascript.
            PolicyPermissionMap perms = mPolicyPermissions.get(id);
            PolicyPermissionMap permsNew = mPolicyPermissionsJS.get(id);
            PolicyPermissionMap permsOld = mPolicyPermissionsOld.get(id);

            if (perms == null) {
                // first time a policyupdate for this reader/writer is seen, create object.
                perms = new PolicyPermissionMap();
                mPolicyPermissions.put(id, perms);
            }
            if (identity == null) {
                // last item in identity update callbacks
                LOG.fine("last onPolicy callback for reader/writer" + id);
                perms.setFirst(true); // mark in perms that next item will be the first
                mPolicyPermissionsOld.delete(id); // no need to keep old values longer

                // build json data, format:
                // {"users":[{"id":123, "allow":true},{"id":456, "allow":false},..]}</b>
                try {
                    JSONArray array = new JSONArray();
                    for (Entry<Long, AccessRule> entry : perms.entrySet()) {
                        JSONObject obj = new JSONObject();
                        obj.put("id", entry.getKey());
                        obj.put("allow", (entry.getValue() == AccessRule.ALLOW ? true : false));
                        array.put(obj);
                    }
                    final JSONObject data = new JSONObject();
                    data.put("users", array);

                    // post this on the JS_HANDLER
                    // this to avoid to loose the first policy update on reader creation
                    JS_HANDLER.post(new Runnable() {
                        @Override
                        public void run()
                        {
                            notifyJs(Integer.toString(id), MSG_POLICYUPDATE, data);
                        }
                    });

                }
                catch (JSONException ex) {
                    LOG.log(Level.SEVERE, "Error in JSON object", ex);
                }

                return null; // not relevant
            }
            else {
                if (perms.isFirst()) {
                    LOG.fine("first onPolicy callback for reader/writer" + id);
                    // first item in update row
                    // copy to old permissions
                    permsOld = new PolicyPermissionMap();
                    for (Entry<Long, AccessRule> entry : perms.entrySet()) {
                        permsOld.put(entry.getKey(), entry.getValue());
                    }
                    mPolicyPermissionsOld.put(id, permsOld);
                    // clear list
                    perms.clear();
                    perms.setFirst(false);
                }
                AccessRule perm = null;
                if (permsNew != null) {
                    // permission set already from javascript
                    perm = permsNew.get(identity.getUserID());
                }
                if (perm == null) {
                    // not yet set, look in cached old value
                    perm = permsOld.get(identity.getUserID());
                }
                if (perm == null) {
                    // not yet set, use default
                    perm = AccessRule.ALLOW; // default ALLOW;
                }
                perms.put(identity.getUserID(), perm);
                return perm;
            }
        }

        synchronized void setPolicyPermissions(int id, PolicyPermissionMap newPermissions)
        {
            mPolicyPermissionsJS.put(id, newPermissions);
        }

        synchronized void close(int id)
        {
            // close reader/writer. Do cleanup
            mPolicyPermissions.delete(id);
            mPolicyPermissionsJS.delete(id);
        }
    }

    private class WebViewCallbacks
        implements QeoJSONProxy.Callbacks
    {

        @Override
        public void onData(int readerId, JSONObject data)
        {
            notifyJs(Integer.toString(readerId), MSG_DATA, data);
        }

        @Override
        public void onNoMoreData(int readerId)
        {
            notifyJs(Integer.toString(readerId), MSG_NO_MORE_DATA, new JSONObject());
        }

        @Override
        public void onStateUpdate(int readerId)
        {
            notifyJs(Integer.toString(readerId), MSG_UPDATE, createIdJson(readerId));
        }

        @Override
        public void onRemove(int readerId, JSONObject data)
        {
            notifyJs(Integer.toString(readerId), MSG_REMOVE, data);
        }

        @Override
        public AccessRule onPolicyUpdate(int id, Identity identity)
        {
            return mPolicyHandler.onPolicyUpdate(id, identity);
        }
    }

    private static class HandlerThread
        extends Thread
    {
        private Looper mLooper;
        private final Semaphore mSem = new Semaphore(0);

        public Looper getLooper()
        {
            return mLooper;
        }

        public Semaphore getSemaphore()
        {
            return mSem;
        }

        @Override
        public void run()
        {
            Looper.prepare();
            mLooper = Looper.myLooper();
            mSem.release();
            Looper.loop();
        }
    }

    private class FactoryCloser
        implements Runnable
    {

        private final int mFactoryId;

        public FactoryCloser(int id)
        {
            mFactoryId = id;
        }

        @Override
        public void run()
        {
            Set<Integer> readerWriters = mProxy.getReaderWriterIds(mFactoryId);
            if (readerWriters == null) {
                // this means an invalid factory ID. nothing more to be done.
                return;
            }
            for (int id : readerWriters) {
                // close policy updatehandlers (if any)
                mPolicyHandler.close(id);
            }
            mProxy.closeFactory(mFactoryId); // close json factory
            mFactories.remove(mFactoryId);
            QeoAndroid.closeQeo(mListeners.remove(mFactoryId)); // close qeo java factory
        }

    }
}
