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

package org.qeo.android.config;

import java.util.logging.Logger;

import org.qeo.android.debugconfig.R;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

public class QeoConfig
    extends Activity
    implements OnClickListener
{
    /**
     * Name of generic config file in debugconfig app.
     */
    public static final String FILE_QEO_PREFS_DEBUGCONFIG = "qeoPrefsDebugconfig";
    /**
     * Configuration file for forwarder specific options.
     */
    public static final String FILE_QEO_PREFS_FORWARDER = "qeoPrefsForwarder";

    private final static String TAG = "QeoConfig";
    private final static Logger log = Logger.getLogger(TAG);

    private EditText inputDomainId;
    private Spinner inputLogLevel;
    private EditText inputTcpServer;
    private EditText inputTcpServerPort;
    private EditText inputPublicIpPort;
    private CheckBox checkBoxDisableOpenDomain;
    private CheckBox checkBoxDisableLocationService;
    private CheckBox checkBoxDisableDdsSecurity;
    private SharedPreferences prefs;
    private SharedPreferences prefsFwd;
    private Button saveButton;

    @SuppressLint("InlinedApi")
    private int getSharedPrefsMultiProcess()
    {
        if (Build.VERSION.SDK_INT >= 11) {
            // this flag is added in api 11
            return Context.MODE_MULTI_PROCESS;
        }
        else {
            // however, before api 11 this is default set to multi_process
            return Context.MODE_PRIVATE;
        }
    }

    private static final String PREF_DOMAIN_ID = "domainId";
    private static final String PREF_LOGLEVEL = "logLevel";
    private static final String PREF_FORWARDING_ADDRESS = "tcpServer";
    private static final String PREF_FORWARDER_TCP_PORT = "tcpPort";
    private static final String PREF_FORWARDER_PUBLIC_ADDRESS = "publicAddress";
    private static final String PREF_OPEN_DOMAIN_DISABLED = "openDomainDisabled";
    private static final String PREF_LOCATION_SERVICE_DISABLED = "locationServiceDisabled";
    private static final String PREF_DDS_SECURITY_DISABLED = "ddsSecurityDisabled";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qeo_config);
        inputDomainId = (EditText) findViewById(R.id.editText1);
        inputLogLevel = (Spinner) findViewById(R.id.spinner1);
        inputTcpServer = (EditText) findViewById(R.id.editText2);
        inputTcpServerPort = (EditText) findViewById(R.id.editText3);
        inputPublicIpPort = (EditText) findViewById(R.id.editText4);
        checkBoxDisableOpenDomain = (CheckBox) findViewById(R.id.checkBoxOpenDomain);
        checkBoxDisableLocationService = (CheckBox) findViewById(R.id.checkBoxLocationService);
        checkBoxDisableDdsSecurity = (CheckBox) findViewById(R.id.checkBoxDdsSecurity);
        prefs = getSharedPreferences(FILE_QEO_PREFS_DEBUGCONFIG, getSharedPrefsMultiProcess());
        prefsFwd = getSharedPreferences(FILE_QEO_PREFS_FORWARDER, getSharedPrefsMultiProcess());
        saveButton = (Button) findViewById(R.id.button1);
        saveButton.setOnClickListener(this);
    }

    @Override
    protected void onStart()
    {
        super.onStart();
        // restore saved values

        // generic settings
        inputDomainId.setText(Integer.toString(prefs.getInt(PREF_DOMAIN_ID, 128)));
        String currentLevel = prefs.getString(PREF_LOGLEVEL, "INFO");
        String[] logLevels = getResources().getStringArray(R.array.loglevel_arrays);
        for (int i = 0; i < logLevels.length; ++i) {
            if (logLevels[i].equals(currentLevel)) {
                inputLogLevel.setSelection(i);
            }
        }
        inputTcpServer.setText(prefs.getString(PREF_FORWARDING_ADDRESS, ""));
        checkBoxDisableOpenDomain.setChecked(prefs.getBoolean(PREF_OPEN_DOMAIN_DISABLED, false));
        checkBoxDisableLocationService.setChecked(prefs.getBoolean(PREF_LOCATION_SERVICE_DISABLED, false));
        checkBoxDisableDdsSecurity.setChecked(prefs.getBoolean(PREF_DDS_SECURITY_DISABLED, false));

        // forwarder settings
        inputTcpServerPort.setText(Integer.toString(prefsFwd.getInt(PREF_FORWARDER_TCP_PORT, 0)));
        inputPublicIpPort.setText(prefsFwd.getString(PREF_FORWARDER_PUBLIC_ADDRESS, ""));
    }

    private void save()
    {

        // regular preferences
        SharedPreferences.Editor editor = prefs.edit();
        // domainId
        final int domainId = Integer.parseInt(inputDomainId.getText().toString());
        editor.putInt(PREF_DOMAIN_ID, domainId);
        log.info("setting domainId: " + domainId);

        // loglevel
        String loglevel = inputLogLevel.getSelectedItem().toString();
        editor.putString(PREF_LOGLEVEL, loglevel);
        log.info("Level set to: " + prefs.getString("logLevel", loglevel));

        // Qeo forwarder address
        final String tcpServer = inputTcpServer.getText().toString();
        editor.putString(PREF_FORWARDING_ADDRESS, tcpServer);
        log.info("Set tcp server to: " + tcpServer);

        // disable open domain check
        boolean openDomainDisabled = checkBoxDisableOpenDomain.isChecked();
        editor.putBoolean(PREF_OPEN_DOMAIN_DISABLED, openDomainDisabled);
        log.info("Open domain is " + (openDomainDisabled ? "disabled" : "enabled"));

        // disable location service check
        boolean locationServiceDisabled = checkBoxDisableLocationService.isChecked();
        editor.putBoolean(PREF_LOCATION_SERVICE_DISABLED, locationServiceDisabled);
        log.info("Location service is " + (locationServiceDisabled ? "disabled" : "enabled"));

        // disable open domain check
        boolean ddsSecurityDisabled = checkBoxDisableDdsSecurity.isChecked();
        editor.putBoolean(PREF_DDS_SECURITY_DISABLED, ddsSecurityDisabled);
        log.info("DDS security is " + (ddsSecurityDisabled ? "disabled" : "enabled"));

        // save
        editor.commit();

        // forwarder preferences
        editor = prefsFwd.edit();

        // tcp port
        final int tcpServerPort = Integer.parseInt(inputTcpServerPort.getText().toString());
        editor.putInt(PREF_FORWARDER_TCP_PORT, tcpServerPort);
        log.info("Set forwarder tcp port: " + tcpServerPort);

        // public ip
        final String publicIpPort = inputPublicIpPort.getText().toString();
        editor.putString(PREF_FORWARDER_PUBLIC_ADDRESS, publicIpPort);
        log.info("Set forwarder public ip: " + publicIpPort);
        // save
        editor.commit();
        Toast.makeText(this, "settings saved", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onClick(View v)
    {
        // currently 1 widget registered
        save();
    }
}
