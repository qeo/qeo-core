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

#include <pwd.h>
#include <inttypes.h>
#include <stdbool.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <sys/types.h>

#include <qeo/api.h>

#include "QSimpleChat_ChatMessage.h"


/* ===[ Chat message listeners ]============================================= */

static void on_chat_message(const qeo_event_reader_t *reader,
                            const void *data,
                            uintptr_t userdata)
{
    org_qeo_sample_simplechat_ChatMessage_t *msg = (org_qeo_sample_simplechat_ChatMessage_t *)data;

    /* Whenever a new data sample arrives, print it to stdout */
    printf("%s : %s\n", msg->from, msg->message);
}

/* ===[ Policy update listeners ]============================================ */

#define MAX_UID 10
static int64_t _uid[MAX_UID] = { 0 };
static int _uid_num = 0;
static bool _single_peer = false;
static int64_t _single_uid = 0;
static bool _is_first = true;

static bool uid_exists(int64_t uid)
{
    bool exists = false;
    int i;

    for (i = 0; i < _uid_num; i++) {
        if (uid == _uid[i]) {
            exists = true;
            break;
        }
    }
    return exists;
}

static qeo_policy_perm_t on_policy_update(const qeo_policy_identity_t *identity,
                                          uintptr_t userdata)
{
    qeo_policy_perm_t perm = QEO_POLICY_ALLOW;

    if (_is_first) {
        _uid_num = 0;
    }

    _is_first = false;

    if (NULL != identity) {
        int64_t uid = qeo_policy_identity_get_uid(identity);

        /* only send to single peer? */
        if (_single_peer && (uid != _single_uid)) {
            perm = QEO_POLICY_DENY;
        }
        if (_uid_num >= MAX_UID) {
            fprintf(stderr, "max number of users reached\n");
        }
        else {
            /* Save UID to list of known UIDs */
            _uid[_uid_num] = uid;
            _uid_num++;
        }
    }
    else {
            _is_first = true;
    }
    return perm;
}

static qeo_policy_perm_t on_policy_reader_update(const qeo_event_reader_t *reader,
                                                 const qeo_policy_identity_t *identity,
                                                 uintptr_t userdata)
{
    return on_policy_update(identity, userdata);
}

static qeo_policy_perm_t on_policy_writer_update(const qeo_event_writer_t *writer,
                                                 const qeo_policy_identity_t *identity,
                                                 uintptr_t userdata)
{
    return on_policy_update(identity, userdata);
}

/* ===[ Main code ]========================================================== */

static qeo_event_reader_listener_t _listener = { .on_data = on_chat_message,
                                                 .on_policy_update = on_policy_reader_update };

static qeo_event_writer_listener_t _wl = { on_policy_writer_update };

static char *chomp(char *s)
{
    char *p = s;

    while ('\0' != *p) {
        if (('\n' == *p) || ('\r' == *p)) {
            *p = '\0';
            break;
        }
        p++;
    }
    return s;
}

static void help(void)
{
    printf("Available commands:\n");
    printf("  /bye                quit chat application\n");
    printf("  /help               display this help\n");
    printf("  /name <name>        change user name\n");
    printf("  /users              list known user IDs\n");
    printf("  /privatewrite <uid> only write to <uid>\n");
    printf("  /privateread <uid>  only read from <uid>\n");
    printf("  /public             switch to public conversation\n");
}

static void list_users(void)
{
    int i;

    printf("Known users IDs:\n");
    for (i = 0; i < _uid_num; i++) {
        printf("\t%" PRId64 "\n", _uid[i]);
    }
}

static void handle_command(const char *cmd,
                           org_qeo_sample_simplechat_ChatMessage_t *chat_msg,
                           int *done,
                           qeo_event_writer_t *writer,
                           qeo_event_reader_t *reader)
{
    if (0 == strcmp("bye", cmd)) {
        *done = 1;
    }
    else if (0 == strcmp("help", cmd)) {
        help();
    }
    else if (0 == strncmp("name ", cmd, 5)) {
        free(chat_msg->from);
        chat_msg->from = strdup(&cmd[5]);
    }
    else if (0 == strcmp("users", cmd)) {
        list_users();
    }
    else if (0 == strncmp("privatewrite ", cmd, 8)) {
        int64_t uid = strtoll(&cmd[13], NULL, 10);

        if (!uid_exists(uid)) {
            printf("Unknown user ID %" PRId64 "\n", uid);
        }
        else {
            _single_peer = true;
            _single_uid = uid;
            qeo_event_writer_policy_update(writer);
        }
    }
    else if (0 == strncmp("privateread ", cmd, 8)) {
        int64_t uid = strtoll(&cmd[12], NULL, 10);

        if (!uid_exists(uid)) {
            printf("Unknown user ID %" PRId64 "\n", uid);
        }
        else {
            _single_peer = true;
            _single_uid = uid;
            qeo_event_reader_policy_update(reader);
        }
    }
    else if (0 == strcmp("public", cmd)) {
        _single_peer = false;
        qeo_event_reader_policy_update(reader);
        qeo_event_writer_policy_update(writer);
    }
}

static char *default_user(void)
{
    struct passwd *pwd = getpwuid(getuid());
    char *name = "";

    if (NULL != pwd) {
        name = strdup(pwd->pw_name);
    }
    return name;
}

int main(int argc, const char **argv)
{
    qeo_factory_t *qeo;
    qeo_event_writer_t *msg_writer;
    qeo_event_reader_t *msg_reader;
    int done = 0;

    /* local variables for storing the message before sending */
    char buf[128] = "";
    org_qeo_sample_simplechat_ChatMessage_t chat_msg = { .message = buf };

    /* initialize */
    qeo = qeo_factory_create();
    if (qeo != NULL){
        msg_writer = qeo_factory_create_event_writer(qeo, org_qeo_sample_simplechat_ChatMessage_type, &_wl, 0);
        msg_reader = qeo_factory_create_event_reader(qeo, org_qeo_sample_simplechat_ChatMessage_type, &_listener, 0);

        /* set up some defaults */
        chat_msg.from = default_user();

        /* start conversing */
        printf("New chat session opened.  Type '/help' for commands.\n");
        printf("You can now start chatting...\n");
        while (!done) {
            fgets(buf, sizeof(buf), stdin);
            chomp(buf);
            if ('/' == buf[0]) {
                handle_command(&buf[1], &chat_msg, &done, msg_writer, msg_reader);
            }
            else {
                qeo_event_writer_write(msg_writer, &chat_msg);
            }
        }

        /* clean up */
        free(chat_msg.from);
        qeo_event_reader_close(msg_reader);
        qeo_event_writer_close(msg_writer);
        qeo_factory_close(qeo);
    }
    return 0;
}
