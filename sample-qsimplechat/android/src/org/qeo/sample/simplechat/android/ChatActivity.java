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
import org.qeo.sample.qsimplechat.R;
import org.qeo.sample.simplechat.ChatMessage;
import org.qeo.sample.simplechat.ChatParticipant;
import org.qeo.sample.simplechat.ChatState;

import android.app.Activity;
import android.os.Bundle;
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
import android.widget.Toast;

/**
 * This class that extends the android Activity class and uses the ChatMessage class to construct the chat messages that
 * will send around.
 * 
 */
public class ChatActivity
    extends Activity
    implements OnClickListener, OnFocusChangeListener, OnItemSelectedListener
{

    private static final String TAG = "QSimpleChat";
    private EventWriter<ChatMessage> mWriter;
    private EventReader<ChatMessage> mReader;
    private StateWriter<ChatParticipant> mStateWriter;
    private StateChangeReader<ChatParticipant> mStateReader;
    private QeoFactory mQeo = null;
    private QeoConnectionListener mListener = null;
    private Button mSendButton;
    private EditText mMessage;
    private TextView mMessageLog;
    private ScrollView mScrollableMessageLog;
    private EditText mParticipantName;
    private Spinner mParticipantState;
    private boolean mQeoClosed = false;
    private final ChatParticipant mParticipant = new ChatParticipant();

    private void appendMessage(String message)
    {
        mMessageLog.append(message);
        /* This line scrolls the view to see the last message sent */
        mScrollableMessageLog.smoothScrollTo(0, mMessageLog.getBottom());
    }

    /**
     * This class that extends DefaultStateChangeReaderListener to be able to override the method onData and onRemove.
     * The method onData is called when a particpant's data has been updated. The method onRemove is called when a
     * participant has been removed.
     */
    public class MyParticipantListener
        extends DefaultStateChangeReaderListener<ChatParticipant>
    {
        @Override
        public void onData(ChatParticipant data)
        {
            appendMessage("[ " + data.name + " is now  " + data.state.name() + " ]\n");
        }

        @Override
        public void onRemove(ChatParticipant data)
        {
            appendMessage("[ " + data.name + " has left ]\n");
        }
    }

    /**
     * This class that extends DefaultEventReaderListener to be able to override the method onData. The method OnData is
     * called when a message has been sent.
     */
    public class MyMessageListener
        extends DefaultEventReaderListener<ChatMessage>
    {

        @Override
        public void onData(final ChatMessage data)
        {
            appendMessage(data.from + " : " + data.message + "\n");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
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
        /* Set the listener on the send button */
        mSendButton.setOnClickListener(this);
        mSendButton.setEnabled(false);

        initQeo();
    }

    @Override
    public void onStart()
    {
        super.onStart();
        if (mQeoClosed) {
            // Qeo connection got closed while the app was in the background
            initQeo();
        }
    }

    private void initQeo()
    {
        mListener = new QeoConnectionListener() {

            /* When the connection with the Qeo service is ready we can create our reader and writer. */
            @Override
            public void onQeoReady(QeoFactory qeo)
            {
                Log.d(TAG, "onQeoReady");
                mQeo = qeo;
                try {
                    /* Create the Qeo writer and reader for chat participants */
                    mStateReader = mQeo.createStateChangeReader(ChatParticipant.class, new MyParticipantListener());
                    mStateWriter = mQeo.createStateWriter(ChatParticipant.class);
                    /* Create the Qeo writer and reader for chat messages */
                    mReader = mQeo.createEventReader(ChatMessage.class, new MyMessageListener());
                    mWriter = mQeo.createEventWriter(ChatMessage.class);
                    /* Chatting now enabled */
                    mParticipantName.setEnabled(true);
                    mParticipantState.setEnabled(true);
                    mSendButton.setEnabled(true);
                    /* Publish own state */
                    updateParticipant();
                }
                catch (final QeoException e) {
                    Log.e(TAG, "Error creating Qeo reader/writer", e);
                }
                mQeoClosed = false;
            }

            @Override
            public void onQeoError(QeoException ex)
            {

                if (ex.getMessage() != null) {
                    Toast
                        .makeText(ChatActivity.this, "Qeo Service failed due to " + ex.getMessage(), Toast.LENGTH_LONG)
                        .show();
                }
                else {
                    Toast.makeText(ChatActivity.this, "Failed to initialize Qeo Service.Exiting !", Toast.LENGTH_LONG)
                        .show();
                }
                mQeoClosed = true;
                finish();
            }

            @Override
            public void onQeoClosed(QeoFactory qeo)
            {
                Log.d(TAG, "onQeoClosed");
                super.onQeoClosed(qeo);
                mSendButton.setEnabled(false);
                mQeoClosed = true;
                mStateReader = null;
                mStateWriter = null;
                mReader = null;
                mWriter = null;
                mQeo = null;
            }
        };
        /* Start the Qeo service */
        QeoAndroid.initQeo(getApplicationContext(), mListener);
    }

    private void updateParticipant()
    {
        if (mStateWriter != null) {
            String newName = mParticipantName.getText().toString();
            if ((null != mParticipant.name) && !mParticipant.name.equals(newName)) {
                /* The name is key, so if the name changes remove the previous instance before publishing a new one. */
                mStateWriter.remove(mParticipant);
            }
            mParticipant.name = newName;
            mParticipant.state = ChatState.values()[mParticipantState.getSelectedItemPosition()];
            mStateWriter.write(mParticipant);
        }
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
                /* Publish the message */
                ChatMessage message = new ChatMessage();
                message.from = mParticipant.name;
                message.message = mMessage.getText().toString();
                mWriter.write(message);
                /* Clear text field */
                mMessage.setText("");
                break;

            default:
                throw new IllegalArgumentException("Unknown onclick");

        }
    }

    @Override
    protected void onDestroy()
    {
        /* Close the Qeo writers and readers if needed */
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

        /* Disconnect from the service */
        if (mListener != null) {
            QeoAndroid.closeQeo(mListener);
        }

        super.onDestroy();
    }
}
