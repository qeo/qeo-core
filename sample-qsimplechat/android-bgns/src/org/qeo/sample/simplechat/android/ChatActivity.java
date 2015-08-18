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

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import org.qeo.sample.qbgnschat.R;
import org.qeo.sample.simplechat.ChatState;

/**
 * This class that extends the android Activity class and uses the ChatMessage class to construct the chat messages that
 * will send around.
 * 
 */
public class ChatActivity
    extends Activity
    implements OnClickListener, OnFocusChangeListener, OnItemSelectedListener
{
    private static final String TAG = "QSimpleChat:Activity";

    /* UI elements */
    private Button mSendButton;
    private EditText mMessage;
    private TextView mMessageLog;
    private ScrollView mScrollableMessageLog;
    private EditText mParticipantName;
    private Spinner mParticipantState;

    /* Service interaction */
    private ChatService mService;
    private Intent mServiceIntent;
    private boolean mBound;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /* Associate the local variables with the right view */
        mSendButton = (Button) findViewById(R.id.buttonSend);
        mMessage = (EditText) findViewById(R.id.messageText);
        mMessageLog = (TextView) findViewById(R.id.textView);
        mScrollableMessageLog = (ScrollView) findViewById(R.id.scrollChatbox);
        /* Prepare the participant name EditText */
        mParticipantName = (EditText) findViewById(R.id.participantName);
        mParticipantName.setEnabled(false);
        mParticipantName.setText(android.os.Build.MODEL);
        mParticipantName.setOnFocusChangeListener(this);
        /* Prepare the participant state Spinner */
        mParticipantState = (Spinner) findViewById(R.id.participantState);
        mParticipantState.setEnabled(false);
        ArrayAdapter<CharSequence> adapter =
            ArrayAdapter.createFromResource(this, R.array.participant_states, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mParticipantState.setAdapter(adapter);
        mParticipantState.setOnItemSelectedListener(this);
        /* Prepare the send button */
        mSendButton.setOnClickListener(this);
        mSendButton.setEnabled(false);
    }

    @Override
    public void onStart()
    {
        Log.d(TAG, "onStart");
        super.onStart();
        /* start/bind to service */
        mServiceIntent = new Intent(getApplicationContext(), ChatService.class);
        Log.d(TAG, "Trying to start local service: " + mServiceIntent);
        startService(mServiceIntent);
        bindService(mServiceIntent, mConnection, 0);
    }

    @Override
    protected void onStop()
    {
        Log.d(TAG, "onStop");
        super.onStop();
        /* Unbind from service */
        if (mBound) {
            mService.setChatDataListener(null);
            unbindService(mConnection);
            mBound = false;
        }
    }

    @Override
    protected void onDestroy()
    {
        Log.d(TAG, "onDestroy");
        /* Disconnect from the service */
        stopService(mServiceIntent);
        super.onDestroy();
    }

    private void updateParticipant()
    {
        if (mService == null) {
            return;
        }
        String name = mParticipantName.getText().toString();
        ChatState state = ChatState.values()[mParticipantState.getSelectedItemPosition()];
        mService.updateParticipant(name, state);
    }

    private void sendMessage()
    {
        /* Publish the message */
        mService.sendMessage(mMessage.getText().toString());
        /* Clear text field */
        mMessage.setText("");
    }

    @Override
    public void onFocusChange(View view, boolean hasFocus)
    {
        if (!hasFocus) {
            /* Update the participant information when focus leaves the participant name text field. */
            updateParticipant();
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
    {
        /* Update the participant information when a new state is selected. */
        updateParticipant();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent)
    {
        /* Not used. */
    }

    @Override
    public void onClick(View view)
    {
        switch (view.getId()) {

            /* Send button */
            case R.id.buttonSend:
                sendMessage();
                break;

            default:
                throw new IllegalArgumentException("Unknown onclick");

        }
    }

    private class MyChatDataListener
        implements ChatService.ChatDataListener
    {
        @Override
        public void onReady(boolean ready)
        {
            Log.d(TAG, "onReady: " + ready);
            /* En/disable UI elements depending on whether Qeo is ready */
            mParticipantName.setEnabled(ready);
            mParticipantState.setEnabled(ready);
            mSendButton.setEnabled(ready);
            /* Publish (updated) availability */
            updateParticipant();
        }

        @Override
        public void onMessage(String msg)
        {
            Log.d(TAG, "onMessage");
            mMessageLog.append(msg);
            /* This line scrolls the view to see the last message sent */
            mScrollableMessageLog.smoothScrollTo(0, mMessageLog.getBottom());
        }
    }

    /** Defines callbacks for service binding, passed to bindService(). */
    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service)
        {
            Log.d(TAG, "onServiceConnected");
            /* We've bound to ChatService, cast the IBinder and get LocalService instance */
            ChatService.LocalBinder binder = (ChatService.LocalBinder) service;
            mService = binder.getService();
            /* Register listener to allow for Service to contact Activity */
            mService.setChatDataListener(new MyChatDataListener());
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0)
        {
            Log.d(TAG, "onServiceDisconnected");
            mBound = false;
        }
    };
}
