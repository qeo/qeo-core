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

#include <assert.h>
#include <semaphore.h>
#include <signal.h>
#include <stdbool.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <time.h>
#include <inttypes.h>
#include <qeo/api.h>
#include <qeo/device.h>

#include "QGauge_NetStatMessage.h"

static volatile bool _quit = false;
static qeo_platform_device_id _id;

/* ===[ NetStatMessage publication ]========================================= */

/**
 * Publish messages based on the interface statistics from /proc/net/dev.
 *
 * Syntax of /prov/net/dev:
 *
 * Inter-|   Receive                                                |  Transmit
 *  face |bytes    packets errs drop fifo frame compressed multicast|bytes    packets errs drop fifo colls carrier compressed
 *   eth0:  412867    3706    0    0    0     0          0         0     2218      11    0    0    0     0       0          0
 */
static void publish_netstat_messages(const qeo_state_writer_t *writer)
{
    org_qeo_sample_gauge_NetStatMessage_t msg = {};
    FILE *fp = NULL;
    char buf[256], *saveptr, *token;
    int cnt = 0;
    struct timespec time = {};
    int i = 0;

    msg.deviceId.lower = _id.lowerId;
    msg.deviceId.upper = _id.upperId;
    /* now parse /proc/net/dev and publish a message for each interface. */
    fp = fopen("/proc/net/dev", "r");
    assert(NULL != fp);
    while (NULL != fgets(buf, sizeof(buf), fp)) {
        cnt++;
        if (cnt <= 2) {
            continue; /* skip first two header lines */
        }
        /* parse and fill interface name */
        token = strtok_r(buf, ":", &saveptr);
        msg.ifName = strdup(token);
        /* parse and fill received byte count */
        token = strtok_r(NULL, " ", &saveptr);
        msg.bytesIn = atoi(token);
        /* parse and fill received packet count */
        token = strtok_r(NULL, " ", &saveptr);
        msg.packetsIn = atoi(token);
        /* skip some unused fields */
        for (i = 0; i < 6; i++) {
            token = strtok_r(NULL, " ", &saveptr); /* skip */
        }
        /* parse and fill sent byte count */
        token = strtok_r(NULL, " ", &saveptr);
        msg.bytesOut = atoi(token);
        /* parse and fill sent packet count */
        token = strtok_r(NULL, " ", &saveptr);
        msg.packetsOut = atoi(token);
        /* calculate and fill timestamp */
        clock_gettime(CLOCK_MONOTONIC, &time);
        msg.timestamp = ((int64_t) time.tv_nsec) + (((int64_t) time.tv_sec) * 1000000000);
        /* publish sample */
        qeo_state_writer_write(writer, &msg);
        /* clear sample */
        free(msg.ifName);
    }
    fclose(fp);
}

/* ===[ Main code ]========================================================== */

/**
 * Signal handler.  When called the main loop will stop and the process will
 * terminate.
 */
static void sighandler(int sig)
{
    _quit = true;
}

/**
 * Setup a signal handler for intercepting Ctrl-C.  This will allow us to
 * perform a correct resource clean up.
 */
static void setup_sighandler(void)
{
    struct sigaction sa;

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
    qeo_state_writer_t *msg_writer;
    const qeo_platform_device_info *info = qeo_platform_get_device_info();

    /* Setup signal handler to be able to cleanly shutdown. */
    setup_sighandler();
    /* Initialize Qeo (this is a blocking call and will only return when
     * initialization is complete). */
    qeo = qeo_factory_create();
    assert(NULL != qeo);
    /* Create a state writer for NetStatMessage. */
    msg_writer = qeo_factory_create_state_writer(qeo, org_qeo_sample_gauge_NetStatMessage_type, NULL, 0);
    /* Fetch the device ID (will be used in the NetStatMessage) */
    _id = info->qeoDeviceId;
    /* Publish statistics until interrupted (Ctrl-C) */
    while (!_quit) {
        publish_netstat_messages(msg_writer);
        usleep(250*1000); /* every 250ms */
    }
    /* Close our reader. */
    qeo_state_writer_close(msg_writer);
    /* Release the Qeo factory. */
    qeo_factory_close(qeo);
    return 0;
}
