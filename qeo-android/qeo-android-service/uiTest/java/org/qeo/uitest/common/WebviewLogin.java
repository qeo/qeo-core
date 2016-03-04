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

package org.qeo.uitest.common;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.widget.TextView;

import com.android.uiautomator.core.UiDevice;
import com.android.uiautomator.core.UiObject;
import com.android.uiautomator.core.UiObjectNotFoundException;
import com.android.uiautomator.core.UiSelector;

/**
 * Class to login into qeo with openid.
 */
public class WebviewLogin
{
    private static final Pattern mPatternLogcat = Pattern
        .compile("^I/WebView(Activity|Fragment)\\(\\s*\\d+\\):\\s*loaded url \\[(.*)\\]$");
    private static final LoggerUI LOG = LoggerUI.getLogger(WebviewLogin.class);
    private final LogcatParserThread logcatThread;
    private final UiDevice mUiDevice;
    private static final String GOOGLE_USER = "qeolokiuiautomator";
    private static final String GOOGLE_PASS = "uitestpass";
    private static final String SSOFI_OPEN_ID = "http://54.87.100.191:8080/ssofi-provider/";
    private static final String SSOFI_USER = "id1@test.com";
    private static final String SSOFI_PASS = "ssofiid1";

    /**
     * Start the webview class. This must be called before actually launching the webview activity.
     */
    public WebviewLogin(UiDevice uiDevice)
        throws InterruptedException
    {
        LOG.setLevel(LoggerUI.Level.ALL);
        logcatThread = new LogcatParserThread();
        logcatThread.start();
        Thread.sleep(1000); // wait a little to let current logcat output pass
        // Set the first page url
        // testje.setPattern(Pattern.compile("^.*/oauth.qeo.org/.*client_id=easy-install.*$"));
        // use instead following pattern when using projectq dev server
        logcatThread.setPattern(Pattern.compile("^.*/oauth.projectq.be/.*client_id=projectq-oauth-embedded.*$"));
        mUiDevice = uiDevice;
    }

    /**
     * Login to google using default user.
     */
    public void doGoogleLogin()
        throws InterruptedException, UiObjectNotFoundException
    {
        doGoogleLogin(GOOGLE_USER, GOOGLE_PASS);
    }

    /**
     * Login to ssofi openid provider using default user.
     */
    public void doSsofiLogin()
        throws InterruptedException, UiObjectNotFoundException
    {
        doSsofiLogin(SSOFI_USER, SSOFI_PASS);
    }

    /**
     * Login to google
     * 
     * @param user the username
     * @param pass the password
     */
    public void doGoogleLogin(String user, String pass)
        throws InterruptedException, UiObjectNotFoundException
    {

        LOG.d("webview loaded");
        logcatThread.waitForPattern();
        logcatThread.setPattern(Pattern.compile("^.*/accounts.google.com/.*$"));

        KeyEventHelper keyEventHelper = new KeyEventHelper(mUiDevice);
        LOG.i("Select google");
        keyEventHelper.pressTab();
        keyEventHelper.pressEnter();
        logcatThread.waitForPattern();
        logcatThread.setPattern(Pattern.compile("^.*OAuth token ready.*$"));
        LOG.i("Do login");
        keyEventHelper.pressTab();
        keyEventHelper.sendString(user);
        keyEventHelper.pressTab();
        keyEventHelper.sendString(pass);
        keyEventHelper.pressTab();
        keyEventHelper.pressEnter();
        logcatThread.waitForPattern();

        logcatThread.stopLogcat();
        LOG.i("Webview login done");

    }

    /**
     * Login to ssofi
     * 
     * @param user the username
     * @param pass the password
     */
    public void doSsofiLogin(String user, String pass)
        throws InterruptedException, UiObjectNotFoundException
    {
        LOG.d("webview loaded");
        logcatThread.waitForPattern();
        logcatThread.setPattern(Pattern.compile("^.*54.87.100.191:8080/ssofi-provider/.*$"));

        KeyEventHelper keyEventHelper = new KeyEventHelper(mUiDevice);
        LOG.i("Select openid");
        keyEventHelper.pressTab();
        keyEventHelper.pressTab();
        keyEventHelper.pressTab();
        keyEventHelper.pressEnter();
        // enter the SSOFI_OPEN_ID
        keyEventHelper.sendString(SSOFI_OPEN_ID);
        keyEventHelper.pressEnter();
        logcatThread.waitForPattern();
        logcatThread.setPattern(Pattern.compile("^.*OAuth token ready.*$"));
        LOG.i("Do login");
        keyEventHelper.sendString(user);
        keyEventHelper.pressTab();
        keyEventHelper.sendString(pass);
        keyEventHelper.pressEnter();
        logcatThread.waitForPattern();
        logcatThread.stopLogcat();
        LOG.i("Webview login done");
    }

    private class LogcatParserThread
        extends Thread
    {
        private final AtomicBoolean isActive;
        private Pattern mPatternUrl;
        private final Semaphore mSem;
        private Process process;

        public LogcatParserThread()
        {
            isActive = new AtomicBoolean(false);
            mSem = new Semaphore(0);
        }

        @Override
        public void run()
        {
            try {
                process = Runtime.getRuntime().exec("logcat");
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));

                String line;
                LOG.d("Starting logcat parser");
                while ((line = bufferedReader.readLine()) != null) {
                    if (isActive.get()) {
                        Matcher m = mPatternLogcat.matcher(line);
                        if (m.matches()) {
                            String url = m.group(2);
                            LOG.d("URL:->" + url + "<-");
                            if (mPatternUrl.matcher(line).matches()) {
                                mSem.release();
                            }
                        }
                    }
                }
                LOG.d("Stopping logcat parser");
            }
            catch (Exception e) {
                LOG.w("ex: ", e);
            }
        }

        public void setPattern(Pattern p)
        {
            mSem.drainPermits();
            isActive.set(false);
            mPatternUrl = p;
            isActive.set(true);
        }

        public void waitForPattern()
            throws InterruptedException
        {
            LOG.i("waiting for: " + mPatternUrl.pattern());
            if (!mSem.tryAcquire(30, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Can't get semaphore");
            }
            isActive.set(false);
            Thread.sleep(500);
        }

        public void stopLogcat()
        {
            process.destroy();
        }
    }

    private static UiObject getOTCButton()
    {
        return new UiObject(new UiSelector().className(TextView.class).text("OTC Login"));
    }

    public static boolean hasOTCLogin()
    {
        return getOTCButton().exists();
    }

    public static void openOTCWindow()
        throws UiObjectNotFoundException
    {
        getOTCButton().clickAndWaitForNewWindow();
    }
}
