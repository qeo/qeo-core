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

package org.qeo.uitest.service;

import org.qeo.uitest.common.Helper;
import org.qeo.uitest.common.LoggerUI;

import android.os.Bundle;
import android.widget.Button;
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
public class Scenario2
    extends UiAutomatorTestCase
{
    private static final LoggerUI LOG = LoggerUI.getLogger(Scenario2.class);
    private static final String REGEXP_UNKNOWN = "^Unknown$";

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

    public void testScenario2()
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
            Helper.manageRealm(getUiDevice(), Helper.LoginType.SSOFI);
            // verify dialog and press cancel
            verifyRealmNotSelectedDialog(false);
            // now infoscreen should be back with unknown
            validateInfoScreenEmpty();
            // since uiautomator tests have been done already using google login
            // it is pointless to redo these tests for the 3rd party SSOFI openid provider.
        }
        Thread.sleep(2000);
        LOG.i("DONE");

    }
}
