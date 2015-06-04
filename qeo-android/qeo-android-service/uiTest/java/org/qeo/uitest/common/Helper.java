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

package org.qeo.uitest.common;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import android.graphics.Rect;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.uiautomator.core.UiDevice;
import com.android.uiautomator.core.UiObject;
import com.android.uiautomator.core.UiObjectNotFoundException;
import com.android.uiautomator.core.UiScrollable;
import com.android.uiautomator.core.UiSelector;

/**
 * 
 */
public class Helper
{
    private static final LoggerUI LOG = LoggerUI.getLogger(Helper.class);

    public static void waitForProgressBarToStop()
    {
        LOG.i("Waiting for progressbar indicator to stop");
        boolean timeout = false;
        int i = 0;
        do {
            UiObject progressBar = new UiObject(new UiSelector().className(ProgressBar.class));
            if (progressBar.exists()) {
                ++i;
                if (i == 100) {
                    // 10 sec
                    timeout = true;
                    break;
                }
                LOG.d("Found progressbar, waiting... (" + i + ")");
                try {
                    Thread.sleep(100);
                }
                catch (InterruptedException e) {
                    fail("Got interupted");
                } // 0.1sec
            }
            else {
                LOG.d("No progressbar found");
                break; // no more progressbar
            }

        }
        while (true);
        assertFalse("ProgressBar did not stop", timeout);
    }

    public static void waitForSelectorToAppear(UiSelector selector)
    {
        LOG.i("Waiting for selector to appear: " + selector);
        boolean timeout = false;
        int i = 0;
        do {
            UiObject obj = new UiObject(selector);
            if (!obj.exists()) {
                ++i;
                if (i == 20) {
                    // 20 sec
                    timeout = true;
                    break;
                }
                LOG.i("Not yet found selector, waiting... (" + i + ")");
                try {
                    Thread.sleep(1000);
                }
                catch (InterruptedException e) {
                    fail("Got interupted");
                }
            }
            else {
                LOG.i("selector found");
                break;
            }
        }
        while (true);
        assertFalse("Uiselector did not appear within timeout", timeout);
    }

    public static void waitForChecked(UiSelector selector, boolean enable)
        throws UiObjectNotFoundException
    {
        LOG.i("Waiting for selector to be enabled: " + selector);
        boolean timeout = false;
        int i = 0;
        do {
            UiObject obj = new UiObject(selector);
            if ((enable && !obj.isChecked()) || (!enable && obj.isChecked())) {
                ++i;
                if (i == 20) {
                    // 20 sec
                    timeout = true;
                    break;
                }
                LOG.i("selector not yet checked, waiting... (" + i + ")");
                try {
                    Thread.sleep(1000);
                }
                catch (InterruptedException e) {
                    fail("Got interupted");
                }
            }
            else {
                LOG.i("selector checked");
                break;
            }
        }
        while (true);
        assertFalse("Uiselector did not get checked within timeout", timeout);
    }

    public static void startApp(UiDevice uiDevice, String label, String packageName)
        throws UiObjectNotFoundException
    {
        // start qeosettings
        // Simulate a short press on the HOME button.
        uiDevice.pressHome();

        // We're now in the home screen. Next, we want to simulate
        // a user bringing up the All Apps screen.
        // If you use the uiautomatorviewer tool to capture a snapshot
        // of the Home screen, notice that the All Apps button's
        // content-description property has the value "Apps". We can
        // use this property to create a UiSelector to find the button.
        UiObject allAppsButton = new UiObject(new UiSelector().description("Apps"));

        // Simulate a click to bring up the All Apps screen.
        allAppsButton.clickAndWaitForNewWindow();

        // In the All Apps screen, the Settings app is located in
        // the Apps tab. To simulate the user bringing up the Apps tab,
        // we create a UiSelector to find a tab with the text
        // label "Apps".
        UiObject appsTab = new UiObject(new UiSelector().text("Apps"));

        // Simulate a click to enter the Apps tab.
        appsTab.click();

        // Next, in the apps tabs, we can simulate a user swiping until
        // they come to the Settings app icon. Since the container view
        // is scrollable, we can use a UiScrollable object.
        UiScrollable appViews = new UiScrollable(new UiSelector().scrollable(true));

        // Set the swiping mode to horizontal (the default is vertical)
        appViews.setAsHorizontalList();

        // Create a UiSelector to find the Settings app and simulate
        // a user click to launch the app.
        UiObject settingsApp =
            appViews.getChildByText(new UiSelector().className(android.widget.TextView.class.getName()), label);
        settingsApp.clickAndWaitForNewWindow();

        // Validate that the package name is the expected one
        UiObject settingsValidation = new UiObject(new UiSelector().packageName(packageName));
        assertTrue("Could not start app: " + packageName, settingsValidation.exists());
    }

    public static void startQeoSettings(UiDevice uiDevice)
        throws UiObjectNotFoundException
    {
        startApp(uiDevice, "Qeo Settings", "org.qeo.android.service");
    }

    public static void clickOKAndWaitForNewWindow()
        throws UiObjectNotFoundException
    {
        UiObject okButton = new UiObject(new UiSelector().className(Button.class).text("OK"));
        okButton.clickAndWaitForNewWindow();
    }

    public static void checkLockscreen()
        throws UiObjectNotFoundException, InterruptedException
    {
        LOG.i("CheckLockScreen");
        UiSelector selector = new UiSelector().className(View.class).description("Slide area.");
        UiObject obj = new UiObject(selector);
        if (obj.exists()) {
            LOG.i("Unlocking device");
            Rect bounds = obj.getBounds();

            // swipe to unlock
            obj.dragTo(bounds.right, bounds.centerY(), 10);
            Thread.sleep(2000);

            // check if it worked
            obj = new UiObject(selector);
            assertFalse("Could not unlock device", obj.exists());
        }

        // check for crashed app screen. This happens a lot due to reinstalling that there is a crash popup open.
        // Close it.
        for (int i = 0; i < 5; i++) {
            UiObject crashText =
                new UiObject(new UiSelector().className(TextView.class).textMatches(
                    "^.*Unfortunately, .* has stopped.*$"));
            if (crashText.exists()) {
                LOG.w("Crashed app during startup, closing: " + crashText.getText());
                clickOKAndWaitForNewWindow();
                Thread.sleep(1000);
            }
            else {
                break;
            }
        }
    }

    public static enum LoginType {
        NONE, GOOGLE, YAHOO, SSOFI, GOOGLE_PLUS;
    }

    public static void manageRealm(UiDevice uiDevice, LoginType loginType)
        throws UiObjectNotFoundException, InterruptedException
    {
        LOG.i("Select manage realm: " + loginType);
        UiObject manageRealmButton = new UiObject(new UiSelector().text("Manage realm"));
        manageRealmButton.clickAndWaitForNewWindow();

        if (loginType == LoginType.NONE) {
            // no login needed
            uiDevice.waitForIdle();
            return;
        }
        doLogin(uiDevice, loginType);
    }

    public static void doLogin(UiDevice uiDevice, LoginType loginType)
        throws UiObjectNotFoundException, InterruptedException
    {
        WebviewLogin webviewLogin = new WebviewLogin(uiDevice);
        Thread.sleep(1000); // give webviewLogin thread time to start
        UiObject gPlusButton = new UiObject(new UiSelector().text("Sign in with Google"));
        assertTrue(gPlusButton.exists());
        UiObject otherButton = new UiObject(new UiSelector().text("Other authentication method"));
        assertTrue(otherButton.exists());

        switch (loginType) {
            case GOOGLE: {
                // do google login
                assertFalse(WebviewLogin.hasOTCLogin());
                webviewLogin.doGoogleLogin();
                break;
            }
            case SSOFI: {
                // custom provider
                otherButton.clickAndWaitForNewWindow();
                assertFalse(WebviewLogin.hasOTCLogin());
                webviewLogin.doSsofiLogin();
                break;
            }
            case GOOGLE_PLUS:
                LOG.i("click google plus button");
                gPlusButton.click();

                UiObject setupWindow = new UiObject(new UiSelector().className(TextView.class).text("Setup required"));
                UiObject qeoReady = new UiObject(new UiSelector().className(CheckBox.class).text("Qeo Ready"));
                int i = 0;
                UiObject okButton =
                    new UiObject(new UiSelector().className(Button.class).resourceId(
                        "com.google.android.gms:id/accept_button"));
                while (!setupWindow.exists() && !qeoReady.exists()) {
                    if (okButton.exists()) {
                        LOG.i("gplus consent screen shown");
                        okButton.click();
                    }
                    if (i == 60) {
                        throw new IllegalStateException("Google+ login failed");
                    }
                    i++;
                    LOG.i("Waiting for google+ to login (" + i + " sec)");
                    Thread.sleep(1000);
                }
                LOG.i("Google+ login ok!");
                break;
            default:
                throw new IllegalStateException("can't do login " + loginType);

        }

        // give management activity time to start
        uiDevice.waitForIdle();
    }
}
