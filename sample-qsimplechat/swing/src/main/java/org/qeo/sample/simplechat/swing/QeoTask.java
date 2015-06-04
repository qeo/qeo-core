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

package org.qeo.sample.simplechat.swing;

import javax.swing.SwingUtilities;

import org.qeo.DefaultEventReaderListener;
import org.qeo.EventReader;
import org.qeo.EventWriter;
import org.qeo.QeoFactory;
import org.qeo.exception.QeoException;
import org.qeo.java.QeoConnectionListener;
import org.qeo.java.QeoJava;
import org.qeo.sample.simplechat.ChatMessage;

/**
 * QeoTask class takes care of all Qeo interaction. Call's towards the UI need to be done through invokeLater in order
 * to avoid blocking the UI thread.
 */
public class QeoTask
    implements Runnable
{
    private QeoFactory mQeoFactory;
    private EventWriter<ChatMessage> mWriter;
    private EventReader<ChatMessage> mReader;

    /**
     * Default Event listener, acting upon the arrival of ChatMessages.
     */
    public class MyListener
        extends DefaultEventReaderListener<ChatMessage>
    {
        @Override
        public void onData(final ChatMessage message)
        {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run()
                {
                    String mes = message.from + "@says: " + message.message;
                    System.out.println("QeoTask.onData(message arrive): " + mes);
                    QSimpleChat.sTextOutArea.append(mes + "\n");
                    QSimpleChat.sTextOutArea.setCaretPosition(QSimpleChat.sTextOutArea.getDocument().getLength());
                }
            });
        }
    }

    @Override
    public void run()
    {
        QeoJava.initQeo(mQeoConnectionListener);
    }

    /**
     * @param message ChatMessage to send
     */
    public void sendMessage(ChatMessage message)
    {
        if (null != mWriter) {
            System.out.println("QeoTask.sendMessage(going to write message): " + message.message);
            mWriter.write(message);
        }
    }

    public void cleanUpQeoTask()
    {
        System.out.println("QeoTask.cleanUpQeoTask(close all qeo handles)");
        if (null != mReader) {
            mReader.close();
        }
        if (null != mWriter) {
            mWriter.close();
        }
        QeoJava.closeQeo(mQeoConnectionListener);
    }

    private final QeoConnectionListener mQeoConnectionListener = new QeoConnectionListener() {

        @Override
        public void onStatusUpdate(String status, String reason)
        {
            System.out.println("QeoTask.onStatusUpdate(status: " + status + ", reason: " + ")");
        }

        @Override
        public void onQeoReady(QeoFactory qeo)
        {
            mQeoFactory = qeo;

            try {
                mReader = mQeoFactory.createEventReader(ChatMessage.class, new MyListener());
                mWriter = mQeoFactory.createEventWriter(ChatMessage.class);
            }
            catch (QeoException e) {
                e.printStackTrace();
            }

            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run()
                {
                    QSimpleChat.sQeoReady = true;
                    QSimpleChat.sSendButton.setEnabled(true);
                }
            });

            System.out.println("QeoTask.run(Qeo initialized)");

        }

        @Override
        public void onQeoError(QeoException ex)
        {
            System.err.println("Error initializing Qeo");

        }
    };
}
