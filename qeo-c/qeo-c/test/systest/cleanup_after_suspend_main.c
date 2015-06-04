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
#include <stdio.h>
#include <time.h>
#include <qeo/api.h>
#include <qeo/factory.h>
#define _GNU_SOURCE
#include <sys/wait.h>
#include <string.h>
#include <stdlib.h>
#include "tsm_types.h"
#include <semaphore.h>

#include "verbose.h"

/** 
 * This test tests that qeo is properly resumed if
 * 1. qeo suspended
 * 2. qeo closed
 * 3. qeo started again -> suspend should be undone.
 */

static types_t *_current = NULL;
static sem_t _sync;
static pid_t _pid;
static char * string1 = "string 1";
static char * string2 = "string 2";
static char * string3 = "string 3";

static qeo_iterate_action_t count_instances_callback(const void *data,
                                                     uintptr_t userdata)
{

    types_t *t = (types_t *)data;

    log_verbose("[PID %d] %s entry: %d", _pid, __FUNCTION__, t->i8);
    if (t->i8 == 1) {
        //initial sample, OK
    }
    else if (t->i8 == 2) {
        //correct sample!
        sem_post(&_sync);
    }
    else {
        //unknown sample
        printf("[PID %d] Unknown sample: %s -- %d", _pid, t->string, t->i8);
    }
    return QEO_ITERATE_CONTINUE;
}


static void on_update(const qeo_state_reader_t *reader,
                      uintptr_t userdata)
{
    log_verbose("[PID %d] on_update", _pid);
    assert(QEO_OK == qeo_state_reader_foreach(reader, count_instances_callback, userdata));
}

static bool run_test_write()
{
    qeo_factory_t *factory;
    qeo_state_writer_t *writer;
    int status;
    int ok = 0;

    pid_t pid = getpid();

    log_verbose("[PID %d] Creating factory", pid);
    assert(NULL != (factory = qeo_factory_create_by_id(QEO_IDENTITY_DEFAULT)));
    assert(NULL != (writer = qeo_factory_create_state_writer(factory, _tsm_types, NULL, 0)));
    _current = &_types1;
    
    log_verbose("[PID %d] Writing sample 1", pid);
    _current->string = string1;
    _current->i8 = 1;
    qeo_state_writer_write(writer, _current);
    sleep(1);

    log_verbose("go to suspend");
    qeo_bgns_suspend();
    log_verbose("[PID %d] Writing sample 3", pid); //should not arrive since suspended!
    _current->string = string3;
    _current->i8 = 3;
    qeo_state_writer_write(writer, _current);
    sleep(1);

    log_verbose("[PID %d] Closing factory", pid);
    qeo_state_writer_close(writer);
    qeo_factory_close(factory);

    log_verbose("[PID %d] Creating factory", pid);
    assert(NULL != (factory = qeo_factory_create_by_id(QEO_IDENTITY_DEFAULT)));
    assert(NULL != (writer = qeo_factory_create_state_writer(factory, _tsm_types, NULL, 0)));

    log_verbose("[PID %d] Writing sample 2", pid);
    _current->string = string2;
    _current->i8 = 2;
    qeo_state_writer_write(writer, _current);


        log_verbose("Waiting for process with ID %d", _pid);
        assert(_pid == waitpid(_pid, &status, 0));
        if (WIFEXITED(status)) {
            ok = 1;
            printf("ok\n");
        } else if (WIFSIGNALED(status)) {
            printf("killed by signal %s\n", strsignal(WTERMSIG(status)));
        } else if (WIFSTOPPED(status)) {
            printf("stopped by signal %s\n", strsignal(WSTOPSIG(status)));
        } else if (WIFCONTINUED(status)) {
            printf("continued\n");
        }


    log_verbose("[PID %d] Closing factory", pid);
    qeo_state_writer_close(writer);
    qeo_factory_close(factory);
    return (ok == 1);

}

static void run_test_read()
{
    qeo_factory_t *factory;
    qeo_state_reader_t *reader;
    qeo_state_reader_listener_t sr_cbs = {
        .on_update = on_update
    };
    sem_init(&_sync, 0, 0);

    _pid = getpid();

    log_verbose("[PID %d] Creating factory", _pid);
    assert(NULL != (factory = qeo_factory_create_by_id(QEO_IDENTITY_DEFAULT)));
    assert(NULL != (reader = qeo_factory_create_state_reader(factory, _tsm_types, &sr_cbs, 0)));
    sem_wait(&_sync);

    log_verbose("[PID %d] Closing factory", _pid);
    //qeo_state_change_reader_close(change);
    qeo_state_reader_close(reader);
    qeo_factory_close(factory);
}

int main(int argc, char **argv)
{
    setenv("QEO_FWD_DISABLE_LOCATION_SERVICE", "1", 0);

    _tsm_types[0].flags |= TSMFLAG_KEY;
    _tsm_types[1].flags |= TSMFLAG_KEY; /* makes 'string' key */

        _pid = fork();
        assert(-1 != _pid);
        if (0 == _pid) {
            run_test_read();
            return EXIT_SUCCESS;
        }
        else {
            if (run_test_write()) {
                return EXIT_SUCCESS;
            }
            else {
                return EXIT_FAILURE;
            }
        }

}
