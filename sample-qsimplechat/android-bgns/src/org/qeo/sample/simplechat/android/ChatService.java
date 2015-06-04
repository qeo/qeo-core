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

package org.qeo.sample.simplechat.android;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import org.qeo.DefaultEventReaderListener;
import org.qeo.DefaultStateChangeReaderListener;
import org.qeo.EventReader;
import org.qeo.EventWriter;
import org.qeo.QeoFactory;
import org.qeo.StateChangeReader;
import org.qeo.StateWriter;
import org.qeo.android.QeoAndroid;
import org.qeo.android.QeoConnectionListener;
import org.qeo.exception.QeoException;
import org.qeo.sample.simplechat.ChatMessage;
import org.qeo.sample.simplechat.ChatParticipant;
import org.qeo.sample.simplechat.ChatState;

/**
 * 
 */
public class ChatService
    extends Service
{
    private static final String TAG = "QSimpleChat:Service";

    private final IBinder mBinder = new LocalBinder();
    private ChatDataListener mDataListener = null;

    private QeoFactory mQeo;
    private final QeoConnectionListener mQeoConnectionListener = new MyQeoConnectionListener();

    private EventWriter<ChatMessage> mWriter;
    private EventReader<ChatMessage> mReader;
    private StateWriter<ChatParticipant> mStateWriter;
    private StateChangeReader<ChatParticipant> mStateReader;

    private boolean mQeoReady = false; /* is Qeo ready? */
    private boolean mBound = false;

    private final ChatParticipant mParticipant = new ChatParticipant();

    private void suspend()
    {
        /* Suspend all Qeo operations when in background */
        QeoAndroid.suspend();
    }

    @Override
    public void onCreate()
    {
        Log.d(TAG, "onCreate");
        super.onCreate();
        /* Make sure to pass the application context, not the context of this service */
        QeoAndroid.initQeo(getApplicationContext(), mQeoConnectionListener);
    }

    @Override
    public synchronized void onDestroy()
    {
        Log.d(TAG, "onDestroy");
        /* close readers/writers */
        if (mStateWriter != null) {
            mStateWriter.close();
        }
        if (mStateReader != null) {
            mStateReader.close();
        }
        if (mWriter != null) {
            mWriter.close();
        }
        if (mReader != null) {
            mReader.close();
        }
        /* close Qeo */
        if (mQeoConnectionListener != null) {
            QeoAndroid.closeQeo(mQeoConnectionListener);
        }
        mQeoReady = true;
    }

    /**
     * To be called when participant data needs to be updated and published on Qeo.
     * 
     * @param name The name of the participant
     * @param state The state of the participant
     */
    public void updateParticipant(String name, ChatState state)
    {
        if (mStateWriter != null) {
            if ((null != mParticipant.name) && !mParticipant.name.equals(name)) {
                /* The name is key, so if the name changes remove the previous instance before publishing a new one. */
                mStateWriter.remove(mParticipant);
            }
            mParticipant.name = name;
            mParticipant.state = state;
            mStateWriter.write(mParticipant);
        }
    }

    /**
     * To be called when a chat message needs to be published.
     * 
     * @param msg The chat message
     */
    public void sendMessage(String msg)
    {
        ChatMessage message = new ChatMessage();
        message.from = mParticipant.name;
        message.message = msg;
        mWriter.write(message);
    }

    /**
     * This class extends DefaultStateChangeReaderListener to be able to override the method onData and onRemove. The
     * method onData is called when a particpant's data has been updated. The method onRemove is called when a
     * participant has been removed.
     */
    private class MyParticipantListener
        extends DefaultStateChangeReaderListener<ChatParticipant>
    {
        @Override
        public void onData(ChatParticipant data)
        {
            /*
             * Design guideline!
             * 
             * For all types for which background notification is enabled (on a reader) we should take into account that
             * data can arrive even if suspended. This is because:
             * 
             * 1) The backgrounding mechanism on Android will auto-resume when it detects network changes. This is
             * needed to reconnect to the background notification service if needed (e.g. IP address change, switch
             * between WiFi and data, ...). After some time it will auto-suspend again. During the period of resumption
             * data can come in on any reader without the wake-up method having been called.
             * 
             * 2) If the Qeo service allows for managing remote registration of other devices auto-resume will also be
             * triggered if registration requests come in. See also item 1.
             * 
             * 3) There is a race possible between the application thread calling suspend and a data sample arriving in
             * DDS. The sample is added to the cache and the onData or onRemove method will be called even if the
             * application already called suspend (or is in the process of calling it).
             */
            if (data.state == ChatState.AVAILABLE) {
                doWakeUp();
            }
            setMessage("[ " + data.name + " is now  " + data.state.name() + " ]\n");
        }

        @Override
        public void onRemove(ChatParticipant data)
        {
            /* Same comment as above appliews here. */
            if (data.state == ChatState.AVAILABLE) {
                doWakeUp();
            }
            setMessage("[ " + data.name + " has left ]\n");
        }
    }

    /**
     * This class extends DefaultEventReaderListener to be able to override the method onData. The method OnData is
     * called when a message has been received.
     */
    private class MyMessageListener
        extends DefaultEventReaderListener<ChatMessage>
    {
        @Override
        public void onData(final ChatMessage data)
        {
            setMessage(data.from + " : " + data.message + "\n");
        }
    }

    // Service - Activity interaction

    /**
     * Interface to be implemented by the activity to enable the service to pass data to that activity.
     */
    interface ChatDataListener
    {
        /**
         * Called when Qeo becomes ready or gets woken up after a suspend or when the Qeo connection goes down.
         * 
         * @param ready True if Qeo is ready to be used, false otherwise.
         */
        public void onReady(boolean ready);

        /**
         * Called when a new message needs to be added to the activity's message log.
         * 
         * @param msg The message to be added.
         */
        public void onMessage(String msg);
    }

    private void setReady(boolean ready)
    {
        if (null != mDataListener) {
            mDataListener.onReady(ready);
        }
    }

    private void setMessage(String msg)
    {
        if (null != mDataListener) {
            mDataListener.onMessage(msg);
        }
    }

    /**
     * The activity should call this method when connected to the service so data can be passed from the service to the
     * activity.
     * 
     * @param listener The listener to be used for passing the data.
     */
    public void setChatDataListener(ChatDataListener listener)
    {
        mDataListener = listener;
        if (null != mDataListener) {
            mDataListener.onReady(mQeoReady);
        }
    }

    private void doWakeUp()
    {
        if (!mBound) {
            /*
             * We launch the ChatActivity (or bring it to the foreground) when woken up. Note that this is not an ideal
             * solution for a real application. In that case you would want to send a notification and let the user
             * launch the activity if desired.
             */
            Log.d(TAG, "start activity ...");
            Intent intent = new Intent(getBaseContext(), ChatActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
    }

    // Service - Activity binding

    /**
     * Binder returned by service when an activity binds to it. It allows the activity to access the service and its
     * public methods.
     */
    class LocalBinder
        extends Binder
    {
        /**
         * For retrieving the service from within the activity.
         * 
         * @return The chat service.
         */
        ChatService getService()
        {
            return ChatService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        Log.d(TAG, "onBind");
        /* Activity connected so we are available now */
        updateParticipant(mParticipant.name, ChatState.AVAILABLE);
        mBound = true;
        return mBinder;
    }

    @Override
    public void onRebind(Intent intent)
    {
        Log.d(TAG, "onRebind");
        /* Make sure we resume all Qeo operations (that were suspended on unbinding) */
        if (mQeoReady) {
            QeoAndroid.resume();
        }
        /* UI (activity) reconnected so we are available again */
        updateParticipant(mParticipant.name, ChatState.AVAILABLE);
        mBound = true;
        super.onRebind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent)
    {
        Log.d(TAG, "onUnbind");
        /* Only suspend/update state when Qeo is ready */
        if (mQeoReady) {
            suspend();
        }
        mBound = false;
        return true;
    }

    // Qeo connection

    /**
     * Internal class used for handling the connection to Qeo.
     */
    private class MyQeoConnectionListener
        extends QeoConnectionListener
    {
        @Override
        public void onQeoReady(QeoFactory qeo)
        {
            Log.d(TAG, "onQeoReady");
            mQeo = qeo;
            try {
                /* Create the Qeo writer and reader for chat participants */
                mStateReader = mQeo.createStateChangeReader(ChatParticipant.class, new MyParticipantListener());
                mStateWriter = mQeo.createStateWriter(ChatParticipant.class);
                /* Enable notifications for ChatParticipant topic reader */
                mStateReader.setBackgroundNotification(true);
                /* Create the Qeo writer and reader for chat messages */
                mReader = mQeo.createEventReader(ChatMessage.class, new MyMessageListener());
                mWriter = mQeo.createEventWriter(ChatMessage.class);
            }
            catch (final QeoException e) {
                Log.e(TAG, "Error creating Qeo reader/writer", e);
            }
            mQeoReady = true;
            if (!mBound) {
                suspend();
            }
            setReady(mQeoReady);
        }

        @Override
        public void onQeoError(QeoException ex)
        {
            /* handle exceptions */
            if (ex.getMessage() != null) {
                Toast.makeText(ChatService.this, "Qeo Service failed due to " + ex.getMessage(), Toast.LENGTH_LONG)
                    .show();
            }
            else {
                Toast.makeText(ChatService.this, "Failed to initialize Qeo Service.Exiting !", Toast.LENGTH_LONG)
                    .show();
            }
            mQeoReady = false;
            setReady(mQeoReady);
        }

        @Override
        public void onQeoClosed(QeoFactory qeo)
        {
            Log.w(TAG, "Qeo service connection lost");
            super.onQeoClosed(qeo);
            mQeoReady = false;
            setReady(mQeoReady);

            mStateReader = null;
            mStateWriter = null;
            mReader = null;
            mWriter = null;
            mQeo = null;
        }

        @Override
        public void onWakeUp(String typeName)
        {
            Log.w(TAG, "Yawn, someone woke me up: " + typeName);
            if (typeName == null || typeName.equals(ChatParticipant.class.getName())) {
                /* Resume all Qeo operations */
                QeoAndroid.resume();
                if (null != mDataListener) {
                    /* Notify activity if it is already bound. */
                    mDataListener.onReady(true);
                }
                else {
                    doWakeUp();
                }
            }
        }
    }
}
