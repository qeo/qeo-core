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


package org.qeo.sample.gauge.osgi.host;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;
import org.qeo.QeoFactory;
import org.qeo.exception.QeoException;
import org.qeo.osgi.OSGiQeoFactory;
import org.qeo.sample.gauge.NetStatPublisher;

/**
 * Activator class publishes NetStatMessages.
 */
public class Activator
    implements BundleActivator, Runnable, ServiceListener
{

    private NetStatPublisher mPublisher;
    private BundleContext mContext;
    private ServiceReference mLogRef;
    private static Activator sActivator;

    /*
     * (non-Javadoc)
     * 
     * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext )
     */
    @Override
    public void start(BundleContext bc)
        throws Exception
    {
        sActivator = this;
        mContext = bc;
        bc.addServiceListener(this, "(|(ObjectClass=" + OSGiQeoFactory.class.getName() + ")(ObjectClass="
            + LogService.class.getName() + "))");
        ServiceReference ref = bc.getServiceReference(LogService.class.getName());
        if (ref != null) {
            mLogRef = ref;
        }
        ServiceReference qeoRef = bc.getServiceReference(OSGiQeoFactory.class.getName());
        if (qeoRef != null) {
            startThread();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
     */
    @Override
    public void stop(BundleContext context)
        throws Exception
    {
        cleanRes();
        sActivator = null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.osgi.framework.ServiceListener#serviceChanged(org.osgi.framework.ServiceEvent)
     */
    @Override
    public void serviceChanged(ServiceEvent event)
    {
        System.out.println("Activator.serviceChanged()" + event);
        switch (event.getType()) {
            case ServiceEvent.REGISTERED:
                Object serviceClass = mContext.getService(event.getServiceReference());
                log("Activator.serviceChanged()" + serviceClass);
                System.out.println("Activator.serviceChanged()" + serviceClass);
                if (serviceClass == null) {
                    return;
                }
                if (serviceClass instanceof OSGiQeoFactory) {
                    startThread();
                }
                else {
                    mLogRef = event.getServiceReference();
                }
                break;
            case ServiceEvent.UNREGISTERING:
                // clean up the service.
                cleanRes();
                break;
            default:
                // ignore !
        }
    }

    private void startThread()
    {
        System.out.println("Activator.startThread()");
        log("Found QeoFactory Starting thread...");
        new Thread(this, "OSGi QGauwe Writer thread").start();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run()
    {
        log("Starting QGauge Writer Thread...");
        System.out.println("Activator.run()");
        ServiceReference ref = mContext.getServiceReference(QeoFactory.class.getName());
        if (ref != null) {
            OSGiQeoFactory factory = (OSGiQeoFactory) mContext.getService(ref);
            if (factory != null) {
                try {
                    mPublisher = new NetStatPublisher(new OsgiGaugeWriter(factory.getDeviceID()), 1250, factory);
                }
                catch (QeoException e) {
                    log(e);
                }
            }
        }
    }

    private void cleanRes()
    {
        if (mPublisher != null) {
            mPublisher.close();
        }
        mLogRef = null;
    }

    /**
     * Logs a string message.
     * @param message -message to be displayed as information.
     */
    public static void log(String message)
    {
        if (sActivator != null) {
            sActivator.doLog(message, null);
        }
    }

    /**
     * Logs an exception.
     * 
     * @param exception -error thrown in case of failure.
     */
    public static void log(Throwable exception)
    {
        if (sActivator != null) {
            sActivator.doLog(exception.getMessage(), exception);
        }
    }

    /**
     * Logs a message. 
     * @param message the message to log, can be null
     * @param exception the exception to log, can be null
     */
    public void doLog(String message, Throwable exception)
    {
        ServiceReference ref = mLogRef;
        if (ref != null && mContext != null) {
            LogService service = (LogService) mContext.getService(ref);
            if (service != null) {
                service.log(exception == null ? LogService.LOG_WARNING : LogService.LOG_INFO, message, exception);
                return;
            }
        }
        // Log to standard err if no log service available...
        System.err.println("[SampleGaugeWriterBundle]" + message);
        if (exception != null) {
            exception.printStackTrace();
        }
    }
}
