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

//*********
//This test tests using both the open domain and the closed domain in a single process
//*********

#include <assert.h>
#include <semaphore.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdbool.h>

#include <qeo/api.h>
#include "common.h"
#include "tsm_types.h"
#include "verbose.h"

static sem_t _sync;
static sem_t _notify_sync;

static void on_data(const qeo_state_change_reader_t *reader,
                           const void *data,
                           uintptr_t userdata)
{
    log_verbose("%s entry", __FUNCTION__);
    types_t *t = (types_t *)data;
    int id = (int) userdata;
    if (id == 0) {
        log_verbose("Got sample in closed domain");
        //closed domain
        assert(0 == strcmp(_types1.string, t->string));
        assert(_types1.i8 == t->i8);
    }
    else if (id == 1) {
        log_verbose("Got sample in open domain");
        //open domain
        assert(0 == strcmp(_types2.string, t->string));
        assert(_types2.i8 == t->i8);
    }
    else {
        //unknown id
        assert(false);
    }
    /* release main thread */
    sem_post(&_sync);
    log_verbose("%s exit", __FUNCTION__);
}

static void on_no_more_data(const qeo_state_change_reader_t *reader,
                            uintptr_t userdata)
{
    /* nop */
}

static void on_remove(const qeo_state_change_reader_t *reader,
                             const void *data,
                             uintptr_t userdata)
{
    log_verbose("%s entry", __FUNCTION__);
    /* release main thread */
    sem_post(&_notify_sync);
    log_verbose("%s exit", __FUNCTION__);
}

int main(int argc, const char **argv)
{
    unsigned int i = 0;
    qeo_factory_t *factory;
    qeo_factory_t *factory_open;
    qeo_state_change_reader_t *change[2];
    qeo_state_writer_t *writer[2];
    qeo_state_change_reader_listener_t scr_cb = {
        .on_data = on_data,
        .on_no_more_data = on_no_more_data,
        .on_remove = on_remove
    };

    _tsm_types[0].flags |= TSMFLAG_KEY;
    _tsm_types[1].flags |= TSMFLAG_KEY; /* makes 'string' key */

    /* initialize */
    log_verbose("initialization start - closed");
    sem_init(&_sync, 0, 0);
    sem_init(&_notify_sync, 0, 0);
    assert(NULL != (factory = qeo_factory_create()));
    assert(NULL != (change[0] = qeo_factory_create_state_change_reader(factory, _tsm_types, &scr_cb, 0)));
    assert(NULL != (writer[0] = qeo_factory_create_state_writer(factory, _tsm_types, NULL, 0)));

    log_verbose("initialization start - open");
    assert(NULL != (factory_open = qeo_factory_create_by_id(QEO_IDENTITY_OPEN)));
    assert(NULL != (change[1] = qeo_factory_create_state_change_reader(factory_open, _tsm_types, &scr_cb, 1)));
    assert(NULL != (writer[1] = qeo_factory_create_state_writer(factory_open, _tsm_types, NULL, 0)));
    log_verbose("initialization done");

    log_verbose("Write on closed domain");
    qeo_state_writer_write(writer[0], &_types1);
    sem_wait(&_sync);
    log_verbose("Write on open domain");
    qeo_state_writer_write(writer[1], &_types2);
    sem_wait(&_sync);

    log_verbose("Remove on closed domain");
    qeo_state_writer_remove(writer[0], &_types1);
    sem_wait(&_notify_sync);
    log_verbose("Remove on open domain");
    qeo_state_writer_remove(writer[1], &_types2);
    sem_wait(&_notify_sync);

    log_verbose("clean up");
    for (i = 0; i < 2; i++) {
        qeo_state_writer_close(writer[i]);
        qeo_state_change_reader_close(change[i]);
    }

    log_verbose("close factory");
    qeo_factory_close(factory);
    qeo_factory_close(factory_open);
    sem_destroy(&_sync);
    sem_destroy(&_notify_sync);
    return 0;
}
