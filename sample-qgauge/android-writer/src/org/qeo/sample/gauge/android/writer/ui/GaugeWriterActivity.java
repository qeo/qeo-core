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

package org.qeo.sample.gauge.android.writer.ui;

import org.qeo.sample.gauge.android.writer.R;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

/**
 * 
 * GaugeWriterActivity displays the UI to start/stop the Writer. <br/>
 * The writer will keep on running even if the application is sent to background.
 */
public class GaugeWriterActivity
        extends FragmentActivity
{
    private static final String FRAGMENT_NAME = "WriterUIFragment";
    private GaugeWriterFragment mFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gaugewriter_layout);

        if (savedInstanceState == null) {
            mFragment = new GaugeWriterFragment();
            getSupportFragmentManager().beginTransaction().add(R.id.writer_container, mFragment, FRAGMENT_NAME)
                    .commit();
        }
        else {
            // Do Nothing
        }
    }
}
