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

package org.qeo.uitest.service;

import org.qeo.uitest.common.Helper;
import org.qeo.uitest.common.LoggerUI;

import android.os.Bundle;
import android.os.RemoteException;
import android.widget.Button;
import android.widget.CheckedTextView;
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
public class Scenario1
    extends UiAutomatorTestCase
{
    private static final LoggerUI LOG = LoggerUI.getLogger(Scenario1.class);
    private static final String REGEXP_UNKNOWN = "^Unknown$";
    private static final String REALM_NAME = "UIAutomator";
    private static final String TESTUSER_NAME = "TestUser";
    private static final String TESTREALM_NAME = "TestRealm";

    @Override
    public void setUp()
        throws Exception
    {
        super.setUp();
        Helper.checkLockscreen();
    }

    private void validateInfoScreen(String realmName, String userName, String url)
        throws UiObjectNotFoundException
    {
        LOG.i("Verify infoscreen");
        Helper.waitForProgressBarToStop();
        UiScrollable listView = new UiScrollable(new UiSelector().className(ListView.class));

        LOG.d("trying to match realm: " + realmName);
        UiSelector realmTitle = new UiSelector().text("Realm");
        listView.scrollIntoView(realmTitle);
        UiObject realmSubTitle = new UiObject(realmTitle.fromParent(new UiSelector().textMatches(realmName)));
        assertTrue("Invalid realm name", realmSubTitle.exists());

        LOG.d("trying to match user: " + userName);
        UiSelector userTitle = new UiSelector().text("User");
        listView.scrollIntoView(userTitle);
        UiObject userSubTitle = new UiObject(realmTitle.fromParent(new UiSelector().textMatches(userName)));
        assertTrue("Invalid username", userSubTitle.exists());

        LOG.d("trying to match url: " + url);
        UiSelector urlTitle = new UiSelector().text("Realm management url");
        listView.scrollIntoView(urlTitle);
        UiObject urlSubTitle = new UiObject(urlTitle.fromParent(new UiSelector().textMatches(url)));
        assertTrue(urlSubTitle.exists());
    }

    private void validateInfoScreenEmpty()
        throws UiObjectNotFoundException
    {
        validateInfoScreen(REGEXP_UNKNOWN, REGEXP_UNKNOWN, REGEXP_UNKNOWN);
    }

    private void validateInfoScreenFilled()
        throws UiObjectNotFoundException
    {
        // validateInfoScreen("^" + REALM_NAME + "\\s*\\(\\d+\\)$", "^.*\\(\\d+\\)$", "^.*join\\.qeo\\.org.*");
        validateInfoScreen("^" + REALM_NAME + "\\s*\\(\\d+\\)$", "^.*\\(\\d+\\)$", "^.*join\\.projectq\\.be.*");
    }

    /**
     * Verify realm not selected dialog.
     * 
     * @param ok true to click ok button, false otherwise.
     * @throws UiObjectNotFoundException
     */
    private void verifyRealmNotSelectedDialog(boolean ok)
        throws UiObjectNotFoundException
    {
        LOG.i("Verify realm not selected dialog");
        UiSelector selectorTitle = new UiSelector().className(TextView.class).text("Setup required");
        Helper.waitForSelectorToAppear(selectorTitle);

        UiObject info = new UiObject(new UiSelector().textContains("You have not selected a realm you want to manage"));
        assertTrue(info.exists());

        UiObject buttonCancel = new UiObject(new UiSelector().className(Button.class).text("Cancel"));
        assertTrue(buttonCancel.exists());
        UiObject buttonOK = new UiObject(new UiSelector().className(Button.class).text("OK"));
        assertTrue(buttonOK.exists());
        if (ok) {
            buttonOK.clickAndWaitForNewWindow();
        }
        else {
            buttonCancel.clickAndWaitForNewWindow();
        }
    }

    private void selectRealm(String from, String to)
        throws Exception
    {
        Thread.sleep(500);
        Helper.waitForProgressBarToStop();
        checkTextView(true, "Note");
        UiObject spinnerRealm = new UiObject(new UiSelector().className(TextView.class).text(from));
        spinnerRealm.clickAndWaitForNewWindow();
        UiScrollable availableRealms = new UiScrollable(new UiSelector().className(ListView.class));
        UiObject viewNewRealm = availableRealms.getChildByText(new UiSelector().className(CheckedTextView.class), to);
        viewNewRealm.clickAndWaitForNewWindow();
    }

    private String selectAddRealm()
        throws UiObjectNotFoundException
    {
        Helper.waitForProgressBarToStop();
        UiObject spinnerRealm = new UiObject(new UiSelector().className(TextView.class).text("UIAutomator"));
        spinnerRealm.clickAndWaitForNewWindow();
        String realm = findNewRealm();
        LOG.i("Going to add realm " + realm);

        UiObject viewNewRealm = new UiObject(new UiSelector().className(CheckedTextView.class).text("Add Realm"));
        viewNewRealm.clickAndWaitForNewWindow();
        return realm;
    }

    private void clickQeoNotification(String msg)
        throws UiObjectNotFoundException
    {
        // open notification
        getUiDevice().openNotification();
        UiObject spinnerRealm = new UiObject(new UiSelector().className(TextView.class).text(msg));
        spinnerRealm.clickAndWaitForNewWindow();
    }

    private String findNewUser()
        throws UiObjectNotFoundException
    {
        LOG.i("Trying to find free user");
        UiScrollable list = new UiScrollable(new UiSelector().className(ListView.class));
        int i = 0;
        UiSelector selector = new UiSelector().className(TextView.class);
        do {
            String name = TESTUSER_NAME + "_" + i;
            if (!list.scrollIntoView(selector.text(name))) {
                // not found, so this is the new username
                return name;
            }
            i++;
        }
        while (true);
    }

    private String findNewRealm()
        throws UiObjectNotFoundException
    {
        LOG.i("Trying to find free realm");
        UiScrollable list = new UiScrollable(new UiSelector().className(ListView.class));
        int i = 0;
        UiSelector selector = new UiSelector().className(CheckedTextView.class);
        do {
            String name = TESTREALM_NAME + "_" + i;
            if (!list.scrollIntoView(selector.text(name))) {
                // not found, so this is the new realmname
                return name;
            }
            i++;
        }
        while (true);
    }

    private void addUserScenario()
        throws UiObjectNotFoundException, InterruptedException
    {
        Helper.waitForProgressBarToStop();
        String user = findNewUser();
        LOG.i("Going to add user " + user);

        UiObject addUserButton = new UiObject(new UiSelector().className(TextView.class).description("Create user"));
        addUserButton.clickAndWaitForNewWindow();

        UiObject userTextField = new UiObject(new UiSelector().className(EditText.class).text("username"));
        userTextField.setText(user);

        UiObject okButton = new UiObject(new UiSelector().className(Button.class).text("OK"));
        okButton.clickAndWaitForNewWindow();

        Helper.waitForProgressBarToStop();
        UiScrollable list = new UiScrollable(new UiSelector().className(ListView.class));
        UiSelector selector = new UiSelector().className(TextView.class).text(user);
        assertTrue("Added user not found", list.scrollIntoView(selector));

    }

    private void addRealmScenario()
        throws UiObjectNotFoundException, InterruptedException
    {
        Helper.waitForProgressBarToStop();

        UiObject addUserButton = new UiObject(new UiSelector().className(TextView.class).description("Settings"));
        addUserButton.clickAndWaitForNewWindow();

        checkTextView(false, "Note");

        String realm = selectAddRealm();

        UiObject userTextField = new UiObject(new UiSelector().className(EditText.class).text("NewRealm"));
        userTextField.setText(realm);

        UiObject okButton = new UiObject(new UiSelector().className(Button.class).text("OK"));
        okButton.clickAndWaitForNewWindow();

        Helper.waitForProgressBarToStop();
        // the added realm is automatically the selected realm
        UiObject spinnerRealm = new UiObject(new UiSelector().className(TextView.class).text(realm));
        assertTrue("Added realm not found", spinnerRealm.exists());
    }

    private void checkTextView(boolean isVisible, String content)
        throws UiObjectNotFoundException
    {
        LOG.i("Check view content:");
        UiObject textViewNote = new UiObject(new UiSelector().className(TextView.class).textContains(content));

        if (textViewNote.exists() && isVisible) {
            assertEquals(textViewNote.getText(), "NOTE: The device will be registered to this realm");
            LOG.i("Check view content - result TRUE: " + textViewNote.getText());
        }
        else {
            if (textViewNote.exists() == false && isVisible == false) {
                assertFalse("Succeed - textWiew not shown", isVisible);
            }
            else {
                assertFalse("Error - Check textView didn't succeed ", true);
            }
        }
    }

    private void rotateLeftAndWait()
        throws RemoteException
    {
        LOG.i("Rotating to left");
        getUiDevice().setOrientationLeft();
        Helper.waitForProgressBarToStop();
    }

    private void rotateNormalAndWait()
        throws RemoteException
    {
        LOG.i("Rotating to normal");
        getUiDevice().setOrientationNatural();
        Helper.waitForProgressBarToStop();
    }

    private void revoke()
        throws UiObjectNotFoundException
    {
        UiObject moreOptions = new UiObject(new UiSelector().description("More options"));
        assertTrue(moreOptions.exists());
        moreOptions.click();

        UiObject accountInfo = new UiObject(new UiSelector().text("Account info"));
        assertTrue(accountInfo.exists());
        accountInfo.clickAndWaitForNewWindow();

        UiObject title = new UiObject(new UiSelector().text("Account details"));
        assertTrue(title.exists());

        UiObject buttonClose = new UiObject(new UiSelector().className(Button.class).text("Close"));
        assertTrue(buttonClose.exists());
        UiObject buttonRevoke = new UiObject(new UiSelector().className(Button.class).text("Revoke access"));
        assertTrue(buttonRevoke.exists());
        UiObject buttonSignOut = new UiObject(new UiSelector().className(Button.class).text("Sign out"));
        assertTrue(buttonSignOut.exists());

        buttonRevoke.clickAndWaitForNewWindow();

    }

    public void testScenario1()
        throws Exception
    {
        LOG.setLevel(LoggerUI.Level.ALL);
        Bundle params = getParams();

        // start app
        Helper.startQeoSettings(getUiDevice());
        Thread.sleep(2000); // give it some time to start for the first time.
        if (!"1".equals(params.getString("noWipe"))) {
            // verify infoscreen, everything should be unknown
            validateInfoScreenEmpty();
            // manage realm, do login
            Helper.manageRealm(getUiDevice(), Helper.LoginType.GOOGLE_PLUS);
            // verify dialog and press cancel
            verifyRealmNotSelectedDialog(false);
            // now infoscreen should be back with unknown
            validateInfoScreenEmpty();
            // select manage realm again, this time no login needed
            Helper.manageRealm(getUiDevice(), Helper.LoginType.NONE);
            // verify dialog and press ok
            verifyRealmNotSelectedDialog(true);
            // select UIAutomator realm
            selectRealm("Select any Realm", REALM_NAME);
            // goto management activity again
            getUiDevice().pressBack();
            // goto info activity
            getUiDevice().pressBack();
            // verify infoscreen
            validateInfoScreenFilled();
            // exit
            getUiDevice().pressHome();
            // click notification
            clickQeoNotification("Device registered to Qeo");
        }
        // infoscreen should come up again
        validateInfoScreenFilled();

        // rotation and validation
        rotateLeftAndWait();
        validateInfoScreenFilled();
        rotateNormalAndWait();
        validateInfoScreenFilled();

        // select manage realm again, this time no login needed
        Helper.manageRealm(getUiDevice(), Helper.LoginType.NONE);
        // add user
        addUserScenario();
        // // add realm
        // addRealmScenario();

        // revoke account
        revoke();

        // infoscreen should still be ok
        validateInfoScreenFilled();

        // some timeout to see final result
        Thread.sleep(2000);
        LOG.i("DONE");

    }
}
