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

#include <assert.h>
#include <semaphore.h>
#include <signal.h>
#include <stdbool.h>
#include <stdio.h>
#include <stdlib.h>
#include <inttypes.h>
#include <unistd.h>

#include <qeo/api.h>

#include "qeo_DeviceInfo.h"

static sem_t _quit;

/* ===[ DeviceInfo listener ]================================================ */

static qeo_iterate_action_t device_info_iterate_callback(const void *data,
                                                         uintptr_t userdata)
{
    org_qeo_system_DeviceInfo_t *info = (org_qeo_system_DeviceInfo_t *)data;

    printf("\t%016" PRIx64 "%016" PRIx64" [manufacturer: %s; model: %s]\n",
           info->deviceId.upper, info->deviceId.lower,
           info->manufacturer, info->modelName);
    return QEO_ITERATE_CONTINUE;
}

static void on_device_info_update(const qeo_state_reader_t *reader,
                                  uintptr_t userdata)
{
    qeo_retcode_t rc;

    printf("Updated device list:\n");
    rc = qeo_state_reader_foreach(reader, device_info_iterate_callback, 0);
    assert(QEO_OK == rc);
}

static qeo_state_reader_listener_t _listener = { .on_update = on_device_info_update };

/* ===[ Main code ]========================================================== */

static void sighandler(int sig)
{
    sem_post(&_quit);
}

static void setup_sighandler(void)
{
    struct sigaction sa;

    sem_init(&_quit, 0, 0);
    sa.sa_handler = sighandler;
    sa.sa_flags = 0;
    sigemptyset(&sa.sa_mask);
    if ((sigaction(SIGTERM, &sa, NULL) == -1) ||
        (sigaction(SIGINT, &sa, NULL) == -1)) {
        perror("sigaction");
        exit(-1);
    }
}

int main(int argc, const char **argv)
{
    qeo_factory_t *qeo;
    qeo_state_reader_t *info_reader;

    /* Setup signal handler to be able to cleanly shutdown. */
    setup_sighandler();
    /* Initialize Qeo (this is a blocking call and will only return when
     * initialization is complete). */
    qeo = qeo_factory_create();
    assert(NULL != qeo);
    /* Create a state reader for DeviceInfo and listen for updates using a
     * qeo_state_reader_listener_t. */
    info_reader = qeo_factory_create_state_reader(qeo, org_qeo_system_DeviceInfo_type, &_listener, 0);
    /* Wait for interrupt from user (Ctrl-C) */
    sem_wait(&_quit);
    /* Close our reader. */
    qeo_state_reader_close(info_reader);
    /* Release the Qeo factory. */
    qeo_factory_close(qeo);
    return 0;
}
