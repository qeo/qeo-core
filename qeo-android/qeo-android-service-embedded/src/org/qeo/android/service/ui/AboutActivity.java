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

package org.qeo.android.service.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.SimpleAdapter;

/**
 * Activity to display about and license text.
 */
public class AboutActivity
    extends Activity
    implements OnItemClickListener
{
    private static final Logger LOG = Logger.getLogger("AboutActivity");
    private static final String TITLE = "title";
    private static final String SUB_TITLE = "subTitle";
    private ListView mListview;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        String[] from = new String[]{TITLE, SUB_TITLE};
        int[] to = new int[]{android.R.id.text1, android.R.id.text2};
        List<Map<String, String>> data = new ArrayList<Map<String, String>>();
        String version = "Unknown";
        try {
            version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        }
        catch (NameNotFoundException e) {
            LOG.log(Level.WARNING, "error fetching version name", e);
        }
        data.add(toMap(getString(R.string.about_version), version)); // 0
        data.add(toMap(getString(R.string.about), "")); // 1
        data.add(toMap(getString(R.string.title_eula), getString(R.string.subtitle_eula))); // 2
        String sub = getString(R.string.subtitle_opensource_licenses);
        data.add(toMap(getString(R.string.title_opensource_licenses), sub)); // 3
        SimpleAdapter adapter = new SimpleAdapter(this, data, android.R.layout.two_line_list_item, from, to);

        mListview = (ListView) findViewById(R.id.aboutActivity_listview_items);
        mListview.setAdapter(adapter);
        mListview.setOnItemClickListener(this);
    }

    private Map<String, String> toMap(String title, String subTitle)
    {
        Map<String, String> map = new HashMap<String, String>();
        map.put(TITLE, title);
        map.put(SUB_TITLE, subTitle);
        return map;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id)
    {
        switch (position) {
            case 0:
                // no click action
                break;
            case 1:
                showAboutDialog();
                break;
            case 2:
                showEulaDialog();
                break;
            case 3:
                showOpenSourceLicenseDialog();
                break;
            default:
                throw new IllegalStateException("id not handled: " + position);
        }

    }

    private void showAboutDialog()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.about);
        builder.setView(getWebview("about.html"));
        builder.show();
    }

    private void showOpenSourceLicenseDialog()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.title_opensource_licenses);
        builder.setView(getWebview("opensourceLicenses.html"));
        builder.show();
    }

    private void showEulaDialog()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.title_eula);
        builder.setView(getWebview("eula.html"));
        builder.show();
    }

    private WebView getWebview(String assetFile)
    {
        WebView webview = new WebView(this);
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.path("/android_asset/" + assetFile);
        uriBuilder.scheme("file");
        Uri uri = uriBuilder.build();
        webview.loadUrl(uri.toString());
        return webview;
    }

}
