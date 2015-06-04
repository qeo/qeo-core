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

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.UIManager;

import org.qeo.sample.simplechat.ChatMessage;

/**
 * Main application class constructing the SWING UI and starting the QeoTask Thread.
 */
public final class QSimpleChat
{
    /** */
    static JTextArea sTextOutArea;
    /** */
    static JButton sSendButton;
    /** */
    static boolean sQeoReady = false;

    private static QSimpleChat sWindow;
    private static QeoTask sQeoTask;
    private static Thread sWorkThread;

    private JFrame mFrame;
    private JTextField mTextInField;
    private JButton mExitButton;

    private final ActionListener mSendTextActionListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e)
        {
            String mes = mTextInField.getText();
            if (sQeoReady) {
                if (!mes.equals("")) {
                    ChatMessage message = new ChatMessage();
                    message.from = System.getProperty("user.name") + "_swing";
                    message.message = mes;
                    sQeoTask.sendMessage(message);
                    mTextInField.setText(null);
                }
            }
            else {
                System.out.println("Qeo is not ready yet, ignoring you...");
            }
        }
    };

    private final ActionListener mExitTextActionListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e)
        {
            int confirm = JOptionPane.showOptionDialog(mFrame, "Are You Sure to close QSimpleChat?",
                    "Exit Confirmation", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, null, null);
            if (confirm == JOptionPane.YES_OPTION) {
                sQeoTask.cleanUpQeoTask();
                sWorkThread.interrupt();
                try {
                    sWorkThread.join();
                }
                catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
                mFrame.dispose();
            }
        }
    };

    /**
     * Launch the application.
     * 
     * @param args standard input arguments
     */
    public static void main(String[] args)
    {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run()
            {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                    sWindow = new QSimpleChat();
                    sWindow.mFrame.setVisible(true);
                    sWindow.mFrame.setResizable(false);
                    sWindow.mTextInField.requestFocus();

                    sQeoTask = new QeoTask();
                    sWorkThread = new Thread(sQeoTask);
                    sWorkThread.start();
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Create the application.
     */
    private QSimpleChat()
    {
        initialize();
    }

    /**
     * Initialize the contents of the frame.
     */
    private void initialize()
    {
        mFrame = new JFrame("QSimpleChat");
        mFrame.setBounds(100, 100, 710, 500);
        mFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mFrame.getContentPane().setLayout(null);

        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setBounds(10, 10, 680, 425);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        mFrame.getContentPane().add(scrollPane);

        sTextOutArea = new JTextArea();
        sTextOutArea.setEditable(false);
        scrollPane.setViewportView(sTextOutArea);

        mTextInField = new JTextField();
        mTextInField.setBounds(10, 440, 420, 25);
        mTextInField.addActionListener(mSendTextActionListener);
        mFrame.getContentPane().add(mTextInField);

        sSendButton = new JButton("Send");
        sSendButton.setBounds(440, 440, 120, 25);
        sSendButton.addActionListener(mSendTextActionListener);
        sSendButton.setEnabled(false);
        mFrame.getRootPane().setDefaultButton(sSendButton);
        mFrame.getContentPane().add(sSendButton);

        mExitButton = new JButton("Exit");
        mExitButton.setBounds(570, 440, 120, 25);
        mExitButton.addActionListener(mExitTextActionListener);
        mFrame.getContentPane().add(mExitButton);
    }
}
