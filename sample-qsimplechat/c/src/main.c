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

#include <pwd.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <sys/types.h>

#include <qeo/api.h>

#include "QSimpleChat_ChatMessage.h"
#include "QSimpleChat_ChatParticipant.h"

/* ===[ Chat message listeners ]============================================= */

static void on_chat_message(const qeo_event_reader_t *reader,
                            const void *data,
                            uintptr_t userdata)
{
    org_qeo_sample_simplechat_ChatMessage_t *msg = (org_qeo_sample_simplechat_ChatMessage_t *)data;

    /* Whenever a new data sample arrives, print it to stdout */
    printf("%s : %s\n", msg->from, msg->message);
}

static qeo_event_reader_listener_t _msg_listener = {
    .on_data = on_chat_message
};

/* ===[ Chat participant listeners ]========================================= */

static void on_chat_participant_update(const qeo_state_change_reader_t *reader,
                                       const void *data,
                                       uintptr_t userdata)
{
    const org_qeo_sample_simplechat_ChatParticipant_t *part = (const org_qeo_sample_simplechat_ChatParticipant_t *)data;
    char state_name[16];

    /* Whenever a participant's data changes, print it to stdout */
    qeo_enum_value_to_string(org_qeo_sample_simplechat_ChatState_type, part->state, state_name, sizeof(state_name));
    printf("[ %s is now %s ]\n", part->name, state_name);
}

static void on_chat_participant_remove(const qeo_state_change_reader_t *reader,
                                       const void *data,
                                       uintptr_t userdata)
{
    const org_qeo_sample_simplechat_ChatParticipant_t *part = (const org_qeo_sample_simplechat_ChatParticipant_t *)data;

    /* Whenever a participant leaves, print it to stdout */
    printf("[ %s has left ]\n", part->name);
}

static qeo_state_change_reader_listener_t _part_listener = {
    .on_data = on_chat_participant_update,
    .on_remove = on_chat_participant_remove
};

/* ===[ Main code ]========================================================== */

/* The user's participant data. */
static org_qeo_sample_simplechat_ChatParticipant_t _me;

/* Participant data writer. */
static qeo_state_writer_t *_part_writer;

/* Participant data iterating reader. */
static qeo_state_reader_t *_part_reader;

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
    printf("  /bye           quit chat application\n");
    printf("  /help          display this help\n");
    printf("  /name <name>   change user name\n");
    printf("  /state <state> change state\n");
    printf("  /participants  list on-line participants\n");
}

static void update_participant(const char *name,
                               org_qeo_sample_simplechat_ChatState_t state)
{
    _me.state = state;
    if (NULL != name) {
        /* use provided name */
        qeo_state_writer_remove(_part_writer, &_me);
        free(_me.name);
        _me.name = strdup(name);
    }
    if (NULL == _me.name) {
        /* use default name */
        struct passwd *pwd = getpwuid(getuid());

        if (NULL != pwd) {
            _me.name = strdup(pwd->pw_name);
        }
    }
    /* publish updated participant data */
    qeo_state_writer_write(_part_writer, &_me);
}

static qeo_iterate_action_t print_participant(const void *data,
                                              uintptr_t userdata)
{
    const org_qeo_sample_simplechat_ChatParticipant_t *part = (const org_qeo_sample_simplechat_ChatParticipant_t *)data;
    char state_name[16];

    /* Print information about a single participant */
    qeo_enum_value_to_string(org_qeo_sample_simplechat_ChatState_type, part->state, state_name, sizeof(state_name));
    printf("\t %s is %s\n", part->name, state_name);
    return QEO_ITERATE_CONTINUE;
}

static void handle_command(const char *cmd,
                           int *done)
{
    if (0 == strcmp("bye", cmd)) {
        *done = 1;
    }
    if (0 == strcmp("help", cmd)) {
        help();
    }
    else if (0 == strncmp("name ", cmd, 5)) {
        update_participant(&cmd[5], _me.state);
    }
    else if (0 == strncmp("state ", cmd, 6)) {
        qeo_enum_value_t state;

        /* convert enumerator  name to its corresponding value */
        if (QEO_OK != qeo_enum_string_to_value(org_qeo_sample_simplechat_ChatState_type, &cmd[6], &state)) {
            printf("error - invalid state '%s'\n", &cmd[6]);
        }
        else {
            update_participant(NULL, state);
        }
    }
    else if (0 == strcmp("participants", cmd)) {
        printf("Participant list:\n");
        qeo_state_reader_foreach(_part_reader, print_participant, 0);
    }
}

int main(int argc, const char **argv)
{
    int done = 0;
    qeo_factory_t *qeo;
    /* R/W for chat participants */
    qeo_state_change_reader_t *part_change_reader;
    /* R/W for chat messages */
    qeo_event_writer_t *msg_writer;
    qeo_event_reader_t *msg_reader;

    /* local variables for storing the message before sending */
    char buf[128];
    org_qeo_sample_simplechat_ChatMessage_t chat_msg = { .message = buf };

    /* initialize */
    qeo = qeo_factory_create();
    if (qeo != NULL){
        /* initialize the state R/W */
        _part_writer = qeo_factory_create_state_writer(qeo, org_qeo_sample_simplechat_ChatParticipant_type, NULL, 0);
        _part_reader = qeo_factory_create_state_reader(qeo, org_qeo_sample_simplechat_ChatParticipant_type, NULL, 0);
        part_change_reader = qeo_factory_create_state_change_reader(qeo, org_qeo_sample_simplechat_ChatParticipant_type,
                                                                    &_part_listener, 0);
        /* initialize the message R/W */
        msg_writer = qeo_factory_create_event_writer(qeo, org_qeo_sample_simplechat_ChatMessage_type, NULL, 0);
        msg_reader = qeo_factory_create_event_reader(qeo, org_qeo_sample_simplechat_ChatMessage_type,
                                                     &_msg_listener, 0);

        /* set up some defaults */
        update_participant(NULL, ORG_QEO_SAMPLE_SIMPLECHAT_CHATSTATE_AVAILABLE);

        /* start conversing */
        printf("New chat session opened.  Type '/help' for commands.\n");
        printf("You can now start chatting...\n");
        while (!done) {
            if(fgets(buf, sizeof(buf), stdin) != NULL) {
                chomp(buf);
                if ('/' == buf[0]) {
                    handle_command(&buf[1], &done);
                }
                else {
                    chat_msg.from = _me.name;
                    qeo_event_writer_write(msg_writer, &chat_msg);
                }
            }
        }

        /* clean up */
        qeo_event_reader_close(msg_reader);
        qeo_event_writer_close(msg_writer);
        qeo_state_change_reader_close(part_change_reader);
        qeo_state_reader_close(_part_reader);
        qeo_state_writer_close(_part_writer);
        qeo_factory_close(qeo);
        free(_me.name);
    }
    return 0;
}
