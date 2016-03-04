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

package org.qeo.uitest.test;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.qeo.uitest.common.Helper;
import org.qeo.uitest.common.LoggerUI;
import org.qeo.uitest.common.WebviewLogin;

import android.os.Bundle;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.android.uiautomator.core.UiObject;
import com.android.uiautomator.core.UiObjectNotFoundException;
import com.android.uiautomator.core.UiScrollable;
import com.android.uiautomator.core.UiSelector;
import com.android.uiautomator.testrunner.UiAutomatorTestCase;

/**
 * 
 */
public class IntegratedScenario1
    extends UiAutomatorTestCase
{
    private static final LoggerUI LOG = LoggerUI.getLogger(IntegratedScenario1.class);

    @Override
    public void setUp()
        throws Exception
    {
        super.setUp();
        Helper.checkLockscreen();
    }

    public void testScenario1()
        throws Exception
    {
        // LOG.setLevel(LoggerUI.Level.ALL);

        // parse parameters
        Bundle params = getParams();
        boolean noOpenId = Boolean.parseBoolean(params.getString("noOpenId", "false"));

        // start normal app
        startAppNormal();
        // start qeo
        startQeo(!noOpenId);
        // stop qeo again
        pressStopQeoButton();

        // check that there is currently no device to be registered
        verifyNoNewDeviceNotification();

        // start embedded app
        startAppEmbedded();

        // start qeo
        pressStartQeoButton();

        // new device notification should be there, click it
        clickNewDeviceNotification();
        // select user dialog should open, click create user
        selectAddUser();
        // now create a new user
        String user = addUser();

        // switch to users tab
        selectUsersTab();

        // check that user is added
        Helper.waitForProgressBarToStop(); // users may be refreshing
        veriftyUserAvailable(user);

        // go back to embedded app, should ask for confirmation
        startAppEmbedded();
        acceptRemoteRegistration("QeoHome");
        waitForQeoReady();

        // some timeout to see final result
        Thread.sleep(2000);
        LOG.i("DONE");
    }

    private void pressOther()
        throws UiObjectNotFoundException
    {
        UiObject buttonOther = new UiObject(new UiSelector().text("Other authentication method"));
        assertTrue(buttonOther.exists());
        buttonOther.clickAndWaitForNewWindow();
    }

    private void startAppEmbedded()
        throws UiObjectNotFoundException
    {
        Helper.startApp(getUiDevice(), "UITestEmbedded", "org.qeo.uitest.embedded");
    }

    private void startAppNormal()
        throws UiObjectNotFoundException
    {
        Helper.startApp(getUiDevice(), "UITestNormal", "org.qeo.uitest.normal");
    }

    // private void startQeoSettings()
    // throws UiObjectNotFoundException
    // {
    // Helper.startApp(getUiDevice(), "Qeo Settings", "org.qeo.android.service");
    // }

    private void pressStartQeoButton()
        throws UiObjectNotFoundException
    {
        UiObject startQeoButton = new UiObject(new UiSelector().className(Button.class).text("Start Qeo"));
        startQeoButton.clickAndWaitForNewWindow();
    }

    private void pressStopQeoButton()
        throws UiObjectNotFoundException
    {
        UiObject stopQeoButton = new UiObject(new UiSelector().className(Button.class).text("Stop Qeo"));
        stopQeoButton.click();
    }

    private void waitForQeoReady()
        throws UiObjectNotFoundException
    {
        UiSelector checkboxQeoReady = new UiSelector().className(CheckBox.class).text("Qeo Ready");
        Helper.waitForSelectorToAppear(checkboxQeoReady);
        Helper.waitForChecked(checkboxQeoReady, true);
    }

    private void startQeo(boolean doLogin)
        throws InterruptedException, UiObjectNotFoundException
    {

        if (doLogin) {
            pressStartQeoButton();

            // check webview and OTC
            pressOther();
            verifyWebviewActive();
            Helper.waitForProgressBarToStop();
            // open otc window and close again
            openAndCloseOtc();
            getUiDevice().pressBack();

            Helper.doLogin(getUiDevice(), Helper.LoginType.GOOGLE_PLUS);
        }
        else {
            pressStartQeoButton();
        }
        waitForQeoReady();
    }

    private void verifyWebviewActive()
    {
        Helper.waitForSelectorToAppear(new UiSelector().className(WebView.class));
    }

    private UiObject getUnregisteredDeviceNotification()
    {
        return new UiObject(new UiSelector().className(TextView.class).text("Found unregistered Qeo device"));
    }

    private void verifyNoNewDeviceNotification()
        throws UiObjectNotFoundException, InterruptedException
    {
        // open notification
        getUiDevice().openNotification();
        Thread.sleep(1000);
        UiObject notification = getUnregisteredDeviceNotification();
        getUiDevice().waitForIdle();
        assertFalse(notification.exists());
        getUiDevice().pressHome(); // I can't find a proper way to close the notification screen.
    }

    private void clickNewDeviceNotification()
        throws UiObjectNotFoundException, InterruptedException
    {
        // open notification
        getUiDevice().openNotification();
        Thread.sleep(1000);
        getUiDevice().waitForIdle();
        UiObject notification = getUnregisteredDeviceNotification();
        notification.clickAndWaitForNewWindow();
    }

    private void selectAddUser()
        throws UiObjectNotFoundException
    {
        // wait for dialog
        Helper.waitForSelectorToAppear(new UiSelector().className(TextView.class).text("Select user"));
        UiObject addUserButton = new UiObject(new UiSelector().className(Button.class).text("Create user"));
        addUserButton.clickAndWaitForNewWindow();
    }

    private String addUser()
        throws UiObjectNotFoundException, InterruptedException
    {
        Thread.sleep(500);
        DateFormat df = new SimpleDateFormat("ddMMyyy_HHmmss", Locale.ENGLISH);
        String newUser = "user_" + df.format(new Date());
        // there should be only one editText field
        UiObject userNameField = new UiObject(new UiSelector().className(EditText.class));
        // do a lot of clears. It seems the clear/set stops at - or _ symbols
        int protection = 0;
        while (!userNameField.getText().equals("username")) {
            protection++;
            userNameField.clearTextField();
            if (protection == 20) {
                throw new IllegalStateException("Can't get username field cleared");
            }
        }
        userNameField.setText(newUser);

        Helper.clickOKAndWaitForNewWindow();
        return newUser;
    }

    private void selectUsersTab()
        throws UiObjectNotFoundException
    {
        UiObject usersTab = new UiObject(new UiSelector().className(TextView.class).text("Users"));
        usersTab.click();
    }

    private void veriftyUserAvailable(String user)
        throws UiObjectNotFoundException
    {
        Helper.waitForProgressBarToStop();
        UiScrollable list = new UiScrollable(new UiSelector().className(ListView.class));
        UiSelector selector = new UiSelector().className(TextView.class).text(user);
        assertTrue("Added user not found", list.scrollIntoView(selector));
    }

    private void openAndCloseOtc()
        throws UiObjectNotFoundException
    {
        WebviewLogin.openOTCWindow();
        UiObject help =
            new UiObject(new UiSelector().className(TextView.class).text("Please provide Server URL and One Time Code"));
        help.exists();
        UiObject cancelButton = new UiObject(new UiSelector().className(Button.class).text("Cancel"));
        cancelButton.clickAndWaitForNewWindow();
    }

    private void acceptRemoteRegistration(String realmName)
        throws UiObjectNotFoundException
    {
        UiObject text =
            new UiObject(new UiSelector().className(TextView.class).textContains(
                "Somebody wants to add you to the Qeo realm " + realmName + "."));
        assertTrue(text.exists());
        Helper.clickOKAndWaitForNewWindow();
    }
}
