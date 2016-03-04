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

package org.qeo.sample.gauge.host;

import java.util.List;

import org.qeo.QeoFactory;
import org.qeo.exception.QeoException;
import org.qeo.java.QeoConnectionListener;
import org.qeo.java.QeoJava;
import org.qeo.sample.gauge.NetworkInterfaceSpeedData;
import org.qeo.sample.gauge.SpeedCalculator;
import org.qeo.sample.gauge.SpeedHandler;

/**
 * SpeedPrinter prints all speed measurements to System.out.
 * 
 * 
 */
public class SpeedPrinter
        implements SpeedHandler
{
    private final SpeedCalculator mCalculator;

    public SpeedPrinter(QeoFactory qeo)
        throws QeoException
    {
        mCalculator = new SpeedCalculator(this, qeo);
        mCalculator.start(1000);
    }

    public static void main(String[] args)
        throws QeoException, InterruptedException
    {
        QeoJava.initQeo(new QeoConnectionListener() {
            @Override
            public void onQeoReady(QeoFactory qeo)
            {
                try {
                    new SpeedPrinter(qeo);
                }
                catch (QeoException e) {
                    System.out.print("Error instantiating SpeedPrinter");
                }
            }

            @Override
            public void onQeoError(QeoException ex)
            {
                System.out.print("Error initializing Qeo");
            }
        });
    }

    @Override
    public void onSpeedInterfaceRemoved(List<String> ifNameList, String id)
    {
        for (String name : ifNameList) {
            System.out.print("Interface" + name + "removed ");
        }

    }

    @Override
    public void onSpeedAvailable(List<NetworkInterfaceSpeedData> ifaceSpeedDataList)
    {
        for (NetworkInterfaceSpeedData m : ifaceSpeedDataList) {
            System.out.println(String.format("%s:IN %s: DeviceId: %f kb/s, %f packets/s; OUT: %f kb/s, %f packets/s;",
                    m.getIfaceName(), m.getDeviceId(), m.getKbpsIn(), m.getPcktsIn(), m.getKbpsOut(), m.getPcktsOut()));
        }
    }

    @Override
    public void onSpeedDeviceAvailable(String writerId)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void onSpeedDeviceRemoved(String writerId)
    {
        // TODO Auto-generated method stub

    }
}
