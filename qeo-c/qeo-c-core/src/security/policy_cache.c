/*
 * Copyright (c) 2014 - Qeo LLC
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

/*#######################################################################
 # HEADER (INCLUDE) SECTION                                              #
 ########################################################################*/
#ifndef DEBUG
#define NDEBUG
#endif
#include "policy_cache.h"
#include <assert.h>
#include <stdlib.h>
#include <stdbool.h>
#include "uthash.h"
#include "utlist.h"
#include <stdio.h>
#include <qeo/log.h>
/*#######################################################################
 # TYPES SECTION                                                         #
 ########################################################################*/
typedef enum {
    PARTITION_STRING_LIST_READ = 0,
    PARTITION_STRING_LIST_NOT_READ,
    PARTITION_STRING_LIST_WRITE,
    PARTITION_STRING_LIST_NOT_WRITE,
    PARTITION_STRING_LIST_NUM
} partition_string_list_t;

typedef enum {
    PARTICIPANT_LIST_READ = 0,
    PARTICIPANT_LIST_WRITE,
    PARTICIPANT_LIST_NUM,
} participant_list_t;

#define POLICY_TOPIC    "org.qeo.system.Policy"
struct topic_desc {
    char            *name;          /* key */
    bool            coarse_grained; /* This can change , but only from true --> false (from coarse to fine) */
    UT_hash_handle  hh;             /* hashable */
};

/* TODO: PROBABLY BETTER TO USE A HASHMAP AS IT WILL MAKE LOOKUPS LIKE IN
 * generate_partition_string() FASTER... */
struct participant_list_node {
    const char                    *participant;
    struct participant_list_node  *next;
};

struct rule_entry {
    const struct topic_desc           *tdesc;                                             /* key */
    struct partition_string_list_node *partition_string_list[PARTITION_STRING_LIST_NUM];  /* read, !read, write, !write */

    union {
        struct {
            policy_parser_permission_t perms;
        } coarse;
        struct {
            /* In case you wonder why we don't keep a list to participant_desc:
             * This datastructure is used in the 'first' pass (when the parser runs over the policy file and
             * qeo_policy_cache_add_fine_grained_rule() is called). It is possible that fine-grained rules
             * refer to participants we "don't know", in other words: there simply is no participant_desc object yet.
             * Therefore we keep them as strings. These two lists can safely be deleted (and there objects
             * with their strings at 'finalization'.*/
            /* For each rule-section there will be exactly one list node */
            struct participant_list_node *participant_list[PARTICIPANT_LIST_NUM]; /* read, write */
        } fine;
    }                                 u;
    /* NO, you cannot check tdesc->coarse_grained, as a tdesc->coarse_grained might change! */
    bool                              coarse_grained;

    UT_hash_handle                    hh; /* hashable */
};

struct participant_desc {
    char              *tag; /* key */
    struct rule_entry *rule_entries_hashmap;
    UT_hash_handle    hh; /* hashable */
};

struct qeo_policy_cache {
    uintptr_t               cookie;
    uint64_t                sequence_number;
    struct topic_desc       *topics_hashmap;        /* immutable after first pass */
    struct participant_desc *participants_hashmap;  /* will not grow anymore after first pass */
};
/*#######################################################################
 # STATIC FUNCTION DECLARATION                                           #
 ########################################################################*/
/*#######################################################################
 # STATIC VARIABLE SECTION                                               #
 ########################################################################*/
/*#######################################################################
 # STATIC FUNCTION IMPLEMENTATION                                        #
 ########################################################################*/
#ifdef DEBUG
static void dump_topics(qeo_policy_cache_hndl_t cache)
{
    struct topic_desc *topic;
    struct topic_desc *tmp;

    printf("topics\r\n");
    HASH_ITER(hh, cache->topics_hashmap, topic, tmp)
    {
        printf("%30s(%c)\r\n", topic->name, topic->coarse_grained == true ? 'C' : 'F');
    }
}

static void dump_rule(const struct rule_entry *rentry)
{
    struct partition_string_list_node *part_string_el;
    struct participant_list_node      *participant_specifier_el;
    int                               i;
    const char                        *array[]  = { "r", "!r", "w", "!w" };
    const char                        rwarray[] = { 'r', 'w' };

    printf("\t\t%s(%c)\r\n", rentry->tdesc->name, rentry->tdesc->coarse_grained == true ? 'C' : 'F');

    for (i = 0; i < sizeof(array) / sizeof(array[0]); ++i) {
        unsigned int list_size;
        LL_COUNT(rentry->partition_string_list[i], part_string_el, list_size);
        printf("\t\t\t%s(%u): ", array[i], list_size);
        LL_FOREACH(rentry->partition_string_list[i], part_string_el)
        {
            printf("%s, ", part_string_el->partition_string);
        }
        printf("\r\n");
    }
    if (rentry->coarse_grained == true) {
        printf("\t\t\t[%c%c]\r\n", rentry->u.coarse.perms.read == true ? rwarray[0] : ' ', rentry->u.coarse.perms.write == true ? rwarray[1] : ' ');
    }
    else {
        for (i = 0; i < sizeof(rwarray) / sizeof(rwarray[0]); ++i) {
            printf("\t\t\t[%c(%u): ", rwarray[i], 0);
            LL_FOREACH(rentry->u.fine.participant_list[i], participant_specifier_el)
            {
                printf("%s, ", participant_specifier_el->participant);
            }
            printf("]\r\n");
        }
    }
}

static void dump_participants(qeo_policy_cache_hndl_t cache)
{
    struct participant_desc *part;
    struct participant_desc *ptmp;
    struct rule_entry       *rentry = NULL;
    struct rule_entry       *rtmp   = NULL;

    printf("Participants\r\n");
    HASH_ITER(hh, cache->participants_hashmap, part, ptmp)
    {
        printf("%s\r\n", part->tag);
        HASH_ITER(hh, part->rule_entries_hashmap, rentry, rtmp)
        {
            dump_rule(rentry);
        }
    }
}
#endif //DEBUG

static char *build_partition_string(uint64_t    sequence_number,
                                    const char  *topic_name,
                                    const char  *reader_participant,
                                    const char  *writer_participant)
{
    char *partition_string = NULL;

    assert(topic_name != NULL);
    assert(reader_participant != NULL);
    assert(writer_participant != NULL);

    const char *format = "%" PRIu64 "_%s_%s%s_%s%s";

    const char  *r_prefix = "r";
    const char  *w_prefix = "w";

    if (strcmp(reader_participant, "") == 0) {
        r_prefix = "";
    }

    if (strcmp(writer_participant, "") == 0) {
        w_prefix = "";
    }

    char *wildcard = strchr(topic_name, '*');
    if (wildcard != NULL) { /* wildcard detected */
        format = "%" PRIu64 "_%.*sdef_%s%s_%s%s";
        if (asprintf(&partition_string, format, sequence_number, wildcard - topic_name, topic_name, r_prefix, reader_participant, w_prefix, writer_participant) == -1) {
            return NULL;
        }
    }
    else {    /* no wildcard */
        /* hack for policy */
        if (strcmp(topic_name, POLICY_TOPIC) == 0) {
            sequence_number = 0;
        }

        if (asprintf(&partition_string, format, sequence_number, topic_name, r_prefix, reader_participant, w_prefix, writer_participant) == -1) {
            return NULL;
        }
    }

    return partition_string;
}

/* this function does not take care of allocating the string */
static qeo_retcode_t add_partition_string_list_node(struct partition_string_list_node **head,
                                                    const char                        *partition_string,
                                                    const char                        *tag)
{
    qeo_retcode_t                     ret = QEO_EFAIL;
    struct partition_string_list_node *new_partition_string_el;

    do {
        assert(partition_string != NULL);
        assert(head != NULL);

        new_partition_string_el = calloc(1, sizeof(*new_partition_string_el));
        if (new_partition_string_el == NULL) {
            ret = QEO_ENOMEM;
            qeo_log_e("Out of memory");
            break;
        }

        if (NULL != tag) {
            const char *uid = NULL;

            /* assumption : tag = "uid:<hex>" */
            uid = strstr(tag, "uid:"); /* hard assumption here on uid: because we don't want rid: to be exposed */
            if (NULL != uid) {
                uid += 4;
                new_partition_string_el->fine_grained = 1;
                new_partition_string_el->id.user_id = strtoull(uid, NULL, 16);
            }
        }
        new_partition_string_el->partition_string = partition_string;
        LL_PREPEND(*head, new_partition_string_el);

        ret = QEO_OK;
    } while (0);

    return ret;
}

static qeo_retcode_t add_coarse_grained_to_participant(struct participant_desc          *part,
                                                       const struct topic_desc          *topic,
                                                       const policy_parser_permission_t *perms,
                                                       bool                             existing_is_problem,
                                                       struct rule_entry                **prentry)
{
    struct rule_entry *rentry = NULL;
    qeo_retcode_t     ret     = QEO_EFAIL;

    assert(prentry != NULL);

    do {
        HASH_FIND_INT(part->rule_entries_hashmap, &topic, rentry);
        if (rentry != NULL) {
            if (existing_is_problem == true) {
                assert(false); /* we should actually never come here */
                return QEO_EINVAL;
            }
            else {
                return QEO_OK;
            }
        }

        rentry = calloc(1, sizeof(*rentry));
        if (rentry == NULL) {
            ret = QEO_ENOMEM;
            qeo_log_e("Out of memory");
            break;
        }
        rentry->tdesc           = topic;
        rentry->u.coarse.perms  = *perms;
        rentry->coarse_grained  = true;

        /* we use INT function cause we treat the pointer VALUE as key */
        HASH_ADD_INT(part->rule_entries_hashmap, tdesc, rentry);
        *prentry  = rentry;
        ret       = QEO_OK;
    } while (0);

    return ret;
}

static int cmp_participant_list_node(const struct participant_list_node *p1,
                                     const struct participant_list_node *p2)
{
    return strcmp(p1->participant, p2->participant);
}

static int cmp_partition_list_node(const struct partition_string_list_node  *p1,
                                   const struct partition_string_list_node  *p2)
{
    return strcmp(p1->partition_string, p2->partition_string);
}

/* If things fail in this function, it is because there is no more memory:
 * We don't bother to do clean up then, just focus on proper program termination */
static bool generate_partition_string(qeo_policy_cache_hndl_t       cache,
                                      const struct participant_desc *own_participant,
                                      struct rule_entry             *rule)
{
    qeo_retcode_t ret = QEO_EFAIL;
    char          *partition_string     = NULL;
    char          *partition_string_dup = NULL; /* probably we could avoid duplicating the string with a nifty refcount system, but this is just easier... */
    uint64_t      seqnr = cache->sequence_number;
    partition_string_list_t       partition_string_list_index;

    if (rule->coarse_grained == true) {
        partition_string = build_partition_string(seqnr, rule->tdesc->name, "", "");
        if (partition_string == NULL) {
            qeo_log_e("Out of memory");
            return false;
        }

        partition_string_list_index = rule->u.coarse.perms.read == true ? PARTITION_STRING_LIST_READ : PARTITION_STRING_LIST_NOT_READ;
        if ((ret = add_partition_string_list_node(&rule->partition_string_list[partition_string_list_index], partition_string, NULL)) != QEO_OK) {
            free(partition_string);
            qeo_log_e("Could not add new list node");
            return false;
        }

        if ((partition_string_dup = strdup(partition_string)) == NULL) {
            qeo_log_e("Out of memory");
            return false;
        }

        partition_string_list_index = rule->u.coarse.perms.write == true ? PARTITION_STRING_LIST_WRITE : PARTITION_STRING_LIST_NOT_WRITE;
        if ((ret = add_partition_string_list_node(&rule->partition_string_list[partition_string_list_index], partition_string_dup, NULL)) != QEO_OK) {
            free(partition_string_dup);
            qeo_log_e("Could not add new list node");
            return false;
        }
    }
    else {
        struct participant_desc *part;
        struct participant_desc *ptmp;
        HASH_ITER(hh, cache->participants_hashmap, part, ptmp)
        {
            for (participant_list_t i = 0; i < PARTICIPANT_LIST_NUM; ++i) {
                struct participant_list_node  *part_el      = NULL;
                struct participant_list_node  part_el_like  = { .participant = part->tag };

                LL_SEARCH(rule->u.fine.participant_list[i], part_el, &part_el_like, cmp_participant_list_node);
                if (part_el != NULL) { /* positive */
                    partition_string_list_index = (i == PARTICIPANT_LIST_READ) ?  PARTITION_STRING_LIST_READ : PARTITION_STRING_LIST_WRITE;
                }
                else {   /* negative */
                    partition_string_list_index = (i == PARTICIPANT_LIST_READ) ?  PARTITION_STRING_LIST_NOT_READ : PARTITION_STRING_LIST_NOT_WRITE;
                }

                if (i == PARTICIPANT_LIST_READ) { /* read */
                    partition_string = build_partition_string(seqnr, rule->tdesc->name, own_participant->tag, part->tag);
                }
                else {       /* write */
                    partition_string = build_partition_string(seqnr, rule->tdesc->name, part->tag, own_participant->tag);
                }

                if (partition_string == NULL) {
                    qeo_log_e("Out of memory");
                    return false;
                }

                if ((ret = add_partition_string_list_node(&rule->partition_string_list[partition_string_list_index], partition_string, part->tag)) != QEO_OK) {
                    free(partition_string);
                    qeo_log_e("Could not add new list node");
                    return false;
                }

                if (own_participant != part) {        /* negative */
                    if (i == PARTICIPANT_LIST_READ) { /* read */
                        partition_string            = build_partition_string(seqnr, rule->tdesc->name, part->tag, "*");
                        partition_string_list_index = PARTITION_STRING_LIST_NOT_READ;
                    }
                    else {       /* write */
                        partition_string            = build_partition_string(seqnr, rule->tdesc->name, "*", part->tag);
                        partition_string_list_index = PARTITION_STRING_LIST_NOT_WRITE;
                    }
                    if (partition_string == NULL) {
                        qeo_log_e("Out of memory");
                        return false;
                    }

                    if ((ret = add_partition_string_list_node(&rule->partition_string_list[partition_string_list_index], partition_string, part->tag)) != QEO_OK) {
                        free(partition_string);
                        qeo_log_e("Could not add new list node");
                        return false;
                    }
                }
            }
        }
    }

    return true;
}

static qeo_retcode_t add_coarse_grained(qeo_policy_cache_hndl_t           cache,
                                        const char                        *participant_tag,
                                        const struct topic_desc           *topic,
                                        const policy_parser_permission_t  *perms,
                                        bool                              existing_is_problem)
{
    struct participant_desc *part = NULL;
    qeo_retcode_t           ret   = QEO_EFAIL;
    struct rule_entry       *rule = NULL;

    do {
        HASH_FIND_STR(cache->participants_hashmap, participant_tag, part);
        if (part == NULL) {
            ret = QEO_EINVAL;
            qeo_log_e("Participant %s not found", participant_tag);
            break;
        }

        if ((ret = add_coarse_grained_to_participant(part, topic, perms, existing_is_problem, &rule)) != QEO_OK) {
            break;
        }

        ret = QEO_OK;
    } while (0);

    return ret;
}

static qeo_retcode_t add_participant_list_node(struct participant_list_node **head,
                                               const char                   *participant)
{
    qeo_retcode_t                 ret = QEO_EFAIL;
    struct participant_list_node  *new_participant_el;

    do {
        assert(participant != NULL);
        assert(head != NULL);

        new_participant_el = calloc(1, sizeof(*new_participant_el));
        if (new_participant_el == NULL) {
            ret = QEO_ENOMEM;
            qeo_log_e("Out of memory");
            break;
        }

        new_participant_el->participant = participant;
        LL_PREPEND(*head, new_participant_el);

        ret = QEO_OK;
    } while (0);

    return ret;
}

static qeo_retcode_t add_fine_grained_rule_section_to_participant(struct participant_desc           *part,
                                                                  const struct topic_desc           *topic,
                                                                  const policy_parser_permission_t  *perms,
                                                                  const char                        *participant_specifier)
{
    struct rule_entry *rentry = NULL;
    qeo_retcode_t     ret     = QEO_EFAIL;
    char              *participant_specifier_dup;
    bool              new_rule = false;

    do {
        HASH_FIND_INT(part->rule_entries_hashmap, &topic, rentry);

        /* different approach here than for coarse grained */
        if (rentry == NULL) {
            rentry = calloc(1, sizeof(*rentry));
            if (rentry == NULL) {
                ret = QEO_ENOMEM;
                qeo_log_e("Out of memory");
                break;
            }

            rentry->tdesc = topic;
            new_rule      = true;
        }

        if ((participant_specifier_dup = strdup(participant_specifier)) == NULL) {
            ret = QEO_ENOMEM;
            qeo_log_e("Out of memory");
            break;
        }

        if ((ret = add_participant_list_node(&rentry->u.fine.participant_list[perms->write == true ? PARTICIPANT_LIST_WRITE : PARTICIPANT_LIST_READ], participant_specifier_dup)) != QEO_OK) {
            free(participant_specifier_dup);
            qeo_log_e("Could not add new list node");
            break;
        }

        if (new_rule == true) {
            /* we use INT function cause we treat the pointer VALUE as key */
            HASH_ADD_INT(part->rule_entries_hashmap, tdesc, rentry);
        }

        ret = QEO_OK;
    } while (0);

    if (ret != QEO_OK && new_rule == true) {
        free(rentry);
    }

    return ret;
}

static qeo_retcode_t add_fine_grained_rule_section(qeo_policy_cache_hndl_t          cache,
                                                   const char                       *participant_tag,
                                                   const struct topic_desc          *topic,
                                                   const policy_parser_permission_t *perms,
                                                   const char                       *participant_specifier)
{
    struct participant_desc *part = NULL;
    qeo_retcode_t           ret   = QEO_EFAIL;

    do {
        HASH_FIND_STR(cache->participants_hashmap, participant_tag, part);
        if (part == NULL) {
            ret = QEO_EINVAL;
            qeo_log_e("Participant %s not found", participant_tag);
            break;
        }

        if ((ret = add_fine_grained_rule_section_to_participant(part, topic, perms, participant_specifier)) != QEO_OK) {
            break;
        }

        ret = QEO_OK;
    } while (0);

    return ret;
}

static qeo_retcode_t add_topic(qeo_policy_cache_hndl_t  cache,
                               const char               *topic_name,
                               bool                     coarse_grained,
                               struct topic_desc        **ptopic)
{
    assert(ptopic != NULL);
    struct topic_desc *topic  = NULL;
    qeo_retcode_t     ret     = QEO_EFAIL;


    do {
        *ptopic = NULL;
        /* existing entries can occur */
        HASH_FIND_STR(cache->topics_hashmap, topic_name, topic);
        if (topic == NULL) {
            topic = calloc(1, sizeof(*topic));
            if (topic == NULL) {
                ret = QEO_ENOMEM;
                break;
            }

            topic->name = strdup(topic_name);
            if (topic->name == NULL) {
                free(topic);
                ret = QEO_ENOMEM;
                break;
            }
            topic->coarse_grained = coarse_grained;

            HASH_ADD_KEYPTR(hh, cache->topics_hashmap, topic->name, strlen(topic->name), topic);
        }
        else {
            /* reasoning behind this: if they differ in type, we make it fine-grained */
            /* this works because coarse-grained can become fine-grained but never the other way around ! */
            if (topic->coarse_grained != coarse_grained) {
                topic->coarse_grained = false;
            }
        }
        *ptopic = topic;
        ret     = QEO_OK;
    } while (0);


    return ret;
}

static qeo_retcode_t add_default_wildcard(qeo_policy_cache_hndl_t cache)
{
    qeo_retcode_t                     ret = QEO_EFAIL;
    struct topic_desc                 *topic;
    struct participant_desc           *part;
    struct participant_desc           *ptmp;
    const policy_parser_permission_t  perms = { .read = false, .write = false };

    do {
        if ((ret = add_topic(cache, "*", true, &topic)) != QEO_OK) {
            qeo_log_e("Could not add wildcard");
            break;
        }

        /* add to all participants */
        HASH_ITER(hh, cache->participants_hashmap, part, ptmp)
        {
            if ((ret = add_coarse_grained(cache, part->tag, topic, &perms, false)) != QEO_OK) {
                qeo_log_e("Could not add wildcard rule");
                break;
            }
        }

        if (ret != QEO_OK) {
            break;
        }

        ret = QEO_OK;
    } while (0);

    return ret;
}

static bool add_all_participants(struct participant_desc      *participants_hashmap,
                                 struct participant_list_node **head)
{
    struct participant_desc *part;
    struct participant_desc *ptmp;
    qeo_retcode_t           ret = QEO_EFAIL;

    HASH_ITER(hh, participants_hashmap, part, ptmp)
    {
        char *participant_dup;

        participant_dup = strdup(part->tag);
        if (participant_dup == NULL) {
            qeo_log_e("Out of memory");
            return false;
        }

        if ((ret = add_participant_list_node(head, participant_dup)) != QEO_OK) {
            free(participant_dup);
            qeo_log_e("out of memory");
            return false;
        }
    }

    return true;
}

static bool convert_coarse_to_fine(qeo_policy_cache_hndl_t  cache,
                                   struct rule_entry        *rule)
{
    assert(rule->coarse_grained == true);

    /* WARNING: you MUST make a copy here, otherwise you are messing up the union structure ! */
    policy_parser_permission_t perms = rule->u.coarse.perms;
    rule->coarse_grained = false;

    memset(rule->u.fine.participant_list, 0, sizeof(rule->u.fine.participant_list));
    if (perms.read == true) {
        if (add_all_participants(cache->participants_hashmap, &rule->u.fine.participant_list[PARTICIPANT_LIST_READ]) == false) {
            return false;
        }
    }
    if (perms.write == true) {
        if (add_all_participants(cache->participants_hashmap, &rule->u.fine.participant_list[PARTICIPANT_LIST_WRITE]) == false) {
            return false;
        }
    }

    return true;
}

/* supposes hash map is sorted on prefix length! */
static struct rule_entry *find_match(const struct rule_entry  *rule_entries_hashmap,
                                     const struct topic_desc  *topic,
                                     bool                     *wc_expansion)
{
    struct rule_entry *rentry = NULL;
    struct rule_entry *rtmp   = NULL;

    *wc_expansion = false;

    /* have to delete-cast the const because uthash does not respect constness */
    HASH_ITER(hh, (struct rule_entry *)rule_entries_hashmap, rentry, rtmp)
    {
        char *wildcard = NULL;

        if (rentry->tdesc == topic) {
            return rentry;
        }


        wildcard = strchr(rentry->tdesc->name, '*');
        if (wildcard != NULL) {
            if (strncmp(rentry->tdesc->name, topic->name, wildcard - rentry->tdesc->name) == 0) {
                *wc_expansion = true;
                return rentry;
            }
        }
    }

    return NULL;
}

static int topic_prefix_length_sort(const struct topic_desc *t1,
                                    const struct topic_desc *t2)
{
    size_t  len1  = strlen(t1->name);
    size_t  len2  = strlen(t2->name);

    if (len1 == len2) {
        return strcmp(t1->name, t2->name);
    }
    return len1 < len2;
}

static int topic_prefix_length_inverse_sort(const struct topic_desc *t1,
                                            const struct topic_desc *t2)
{
    size_t  len1  = strlen(t1->name);
    size_t  len2  = strlen(t2->name);

    if (len1 == len2) {
        return strcmp(t2->name, t1->name);
    }
    return len2 < len1;
}

static int rules_topic_prefix_length_sort(const struct rule_entry *r1,
                                          const struct rule_entry *r2)
{
    return topic_prefix_length_sort(r1->tdesc, r2->tdesc);
}

static qeo_retcode_t complete_rules(qeo_policy_cache_hndl_t cache, struct participant_desc *part){

    struct rule_entry       *rule;
    struct rule_entry       *new_rule;
    struct topic_desc       *topic;
    struct topic_desc       *topic_tmp;
    struct rule_entry       *match;
    bool                    wc_expansion;
    qeo_retcode_t           ret = QEO_OK;

    HASH_SORT(part->rule_entries_hashmap, rules_topic_prefix_length_sort); /* needed for correct matching */
    HASH_ITER(hh, cache->topics_hashmap, topic, topic_tmp)
    {
        rule = match = find_match(part->rule_entries_hashmap, topic, &wc_expansion);
        assert(match != NULL);      /* because we added a match-all '*' BEFORE, match can never be NULL */
        if (wc_expansion == true) { /* topic not present yet ! */
            if (match->coarse_grained == true) {
                /* coarse grained */
                if ((ret = add_coarse_grained_to_participant(part, topic, &match->u.coarse.perms, true, &new_rule)) != QEO_OK) {
                    qeo_log_e("Could not add course grained (%s, %s)", part->tag, topic->name);
                    return ret;
                }
                rule = new_rule;
            }
            else {
                /* fine grained */
                struct participant_list_node      *participant_specifier_el;
                const policy_parser_permission_t  perms[PARTICIPANT_LIST_NUM] =
                {
                    { .read = true,  .write = false },
                    { .read = false, .write = true  },
                };
                for (participant_list_t i = 0; i < PARTICIPANT_LIST_NUM; ++i) {
                    LL_FOREACH(match->u.fine.participant_list[i], participant_specifier_el)
                    {
                        if ((ret = add_fine_grained_rule_section_to_participant(part, topic, &perms[i], participant_specifier_el->participant)) != QEO_OK) {
                            qeo_log_e("Could not add fine grained");
                            return ret;
                        }
                    }
                }
            }
        }

        /* we are only interested to expand existing or just added (20 lines above) COARSE-grained rules to fine-grained */
        if (rule->coarse_grained == true) {
            if (match->coarse_grained != topic->coarse_grained) {
                if (convert_coarse_to_fine(cache, rule) == false) {
                    ret = QEO_ENOMEM;
                    return ret;
                }
            }
        }
    }

    return ret;
}

static qeo_retcode_t complete_partition_strings(qeo_policy_cache_hndl_t cache)
{
    struct participant_desc *part;
    struct participant_desc *ptmp;
    struct rule_entry       *rule;
    struct rule_entry       *rule_tmp;
    qeo_retcode_t           ret = QEO_OK;

    HASH_ITER(hh, cache->participants_hashmap, part, ptmp)
    {
        /* 1. Add all rules to all participants */
        if ((ret = complete_rules(cache, part)) != QEO_OK){
            qeo_log_e("Could not complete rules");
            return ret;
        }

        HASH_SORT(part->rule_entries_hashmap, rules_topic_prefix_length_sort); /* not really needed but is nicer in print */ /* also simplifies testing */

        /* At this moment the participant is guaranteed to have all the expanded rules,
         * Now let's generate the partition strings ! */

        /* 2. Generate partition strings for all rules */
        HASH_ITER(hh, part->rule_entries_hashmap, rule, rule_tmp)
        {
            /* normal rules */
            if ((generate_partition_string(cache, part, rule)) == false) {
                qeo_log_e("Could not generate partition string");
                ret = QEO_ENOMEM;
                return ret;
            }

            /* sort for predictable output */
            for (partition_string_list_t i = 0; i < PARTITION_STRING_LIST_NUM; ++i) {
                LL_SORT(rule->partition_string_list[i], cmp_partition_list_node);
            }
        }
    }

    return ret;
}

static void free_topic_desc(struct topic_desc *topic)
{
    free(topic->name);
#ifndef NDEBUG
    memset(topic, 0xAA, sizeof(*topic));
#endif
    free(topic);
}

static void free_partition_string_list_node(struct partition_string_list_node *partition_string_el)
{
    free((char *)partition_string_el->partition_string);
#ifndef NDEBUG
    memset(partition_string_el, 0xBB, sizeof(*partition_string_el));
#endif
    free(partition_string_el);
}

static void free_participant_list_node(struct participant_list_node *participant_el)
{
    free((char *)participant_el->participant);
#ifndef NDEBUG
    memset(participant_el, 0xCC, sizeof(*participant_el));
#endif
    free(participant_el);
}

static void free_rule_entry(struct rule_entry *rentry)
{
    struct partition_string_list_node *part_string_el;
    struct partition_string_list_node *part_string_el_tmp;

    struct participant_list_node  *participant_el;
    struct participant_list_node  *participant_el_tmp;

    for (partition_string_list_t i = 0; i < PARTITION_STRING_LIST_NUM; ++i) {
        LL_FOREACH_SAFE(rentry->partition_string_list[i], part_string_el, part_string_el_tmp)
        {
            LL_DELETE(rentry->partition_string_list[i], part_string_el);
            free_partition_string_list_node(part_string_el);
        }
    }

    if (rentry->coarse_grained == false) {
        for (participant_list_t i = 0; i < PARTICIPANT_LIST_NUM; ++i) {
            LL_FOREACH_SAFE(rentry->u.fine.participant_list[i], participant_el, participant_el_tmp)
            {
                LL_DELETE(rentry->u.fine.participant_list[i], participant_el);
                free_participant_list_node(participant_el);
            }
        }
    }

#ifndef NDEBUG
    memset(rentry, 0xDD, sizeof(*rentry));
#endif
    free(rentry);
}

static void free_participant_desc(struct participant_desc *part)
{
    struct rule_entry *rentry = NULL;
    struct rule_entry *rtmp   = NULL;


    HASH_ITER(hh, part->rule_entries_hashmap, rentry, rtmp)
    {
        HASH_DEL(part->rule_entries_hashmap, rentry);
        free_rule_entry(rentry);
    }
    free(part->tag);
#ifndef NDEBUG
    memset(part, 0xEE, sizeof(*part));
#endif
    free(part);
}

static void call_partition_string_cb(qeo_policy_cache_update_partition_string_cb  update_cb,
                                     qeo_policy_cache_hndl_t                      cache,
                                     uintptr_t                                    cookie,
                                     const char                                   *participant_tag,
                                     const char                                   *topic_name,
                                     unsigned int                                 selector_mask,
                                     const struct rule_entry                      *rule)
{
    const unsigned int all_masks[] =
    {
        PARTITION_STRING_SELECTOR_READ,
        PARTITION_STRING_SELECTOR_NOT_READ,
        PARTITION_STRING_SELECTOR_WRITE,
        PARTITION_STRING_SELECTOR_NOT_WRITE,
    };

    for (int i = 0; i < sizeof(all_masks) / sizeof(all_masks[0]); ++i) {
        if (selector_mask & all_masks[i]) {
            update_cb(cache, cookie, participant_tag, topic_name, all_masks[i], rule->partition_string_list[i]);
        }
    }
}

static bool complete_topics(qeo_policy_cache_hndl_t cache)
{
    /* sort with least specific first */
    HASH_SORT(cache->topics_hashmap, topic_prefix_length_inverse_sort);

    struct topic_desc *topic;
    struct topic_desc *topic_tmp;
    HASH_ITER(hh, cache->topics_hashmap, topic, topic_tmp)
    {
        char *wildcard;

        if ((topic->coarse_grained == false) && ((wildcard = strchr(topic->name, '*')) != NULL)) {
            /* Ah, we are fine-grained. Do we have any other rules that match with us that are still coarse-grained ? */

            /* As we have sorted, we only have to check the topics ahead of us ! */
            struct topic_desc *topic_test;
            struct topic_desc *topic_tmp_test;
            HASH_ITER(hh, topic, topic_test, topic_tmp_test)
            {
                /* Note the exception for POLICY_TOPIC --> it will and must never become fine-grained !! */
                if ((topic_test->coarse_grained == true) && (strcmp(topic_test->name, POLICY_TOPIC) != 0) && (strncmp(topic_test->name, topic->name, wildcard - topic->name) == 0)) {
                    qeo_log_d("We convert topic '%s' to fine-grained based on topic '%s'\n", topic_test->name, topic->name);
                    topic_test->coarse_grained = false;
                }
            }
        }
    }

    return true;
}

/*#######################################################################
 # PUBLIC FUNCTION IMPLEMENTATION                                        #
 ########################################################################*/

qeo_retcode_t qeo_policy_cache_construct(uintptr_t                cookie,
                                         qeo_policy_cache_hndl_t  *cache)
{
    *cache = calloc(1, sizeof(**cache));
    if (*cache == NULL) {
        return QEO_ENOMEM;
    }
    (*cache)->cookie = cookie;

    return QEO_OK;
}

qeo_retcode_t qeo_policy_cache_get_cookie(qeo_policy_cache_hndl_t cache,
                                          uintptr_t               *cookie)
{
    qeo_retcode_t ret = QEO_EFAIL;

    do {
        if (cache == NULL || cookie == NULL) {
            ret = QEO_EINVAL;
            break;
        }

        *cookie = cache->cookie;
        ret     = QEO_OK;
    } while (0);
    return ret;
}

qeo_retcode_t qeo_policy_cache_set_seq_number(qeo_policy_cache_hndl_t cache,
                                              uint64_t                sequence_number)
{
    qeo_retcode_t ret = QEO_EFAIL;

    do {
        if (cache == NULL) {
            ret = QEO_EINVAL;
            break;
        }

        cache->sequence_number = sequence_number;
        ret = QEO_OK;
    } while (0);
    return ret;
}

qeo_retcode_t qeo_policy_cache_get_number_of_participants(qeo_policy_cache_hndl_t cache,
                                                          unsigned int            *number_of_participants)
{
    if (cache == NULL || number_of_participants == NULL){
        return QEO_EINVAL;
    } 
    *number_of_participants = HASH_COUNT(cache->participants_hashmap);
    return QEO_OK;
}

qeo_retcode_t qeo_policy_cache_get_number_of_topics(qeo_policy_cache_hndl_t cache,
                                                    unsigned int            *number_of_topics)
{
    if (cache == NULL || number_of_topics == NULL){
        return QEO_EINVAL;
    } 
    *number_of_topics = HASH_COUNT(cache->topics_hashmap);
    return QEO_OK;
}

qeo_retcode_t qeo_policy_cache_add_participant_tag(qeo_policy_cache_hndl_t  cache,
                                                   const char               *participant_tag)
{
    struct participant_desc *part = NULL;
    qeo_retcode_t           ret   = QEO_EFAIL;

    if (cache == NULL || participant_tag == NULL) {
        return QEO_EINVAL;
    }

    do {
/* existing entries should never occur */
#ifndef NDEBUG
        HASH_FIND_STR(cache->participants_hashmap, participant_tag, part);
        assert(part == NULL);
#endif


        part = calloc(1, sizeof(*part));
        if (part == NULL) {
            ret = QEO_ENOMEM;
            break;
        }

        part->tag = strdup(participant_tag);
        if (part->tag == NULL) {
            free(part);
            ret = QEO_ENOMEM;
            break;
        }

        HASH_ADD_KEYPTR(hh, cache->participants_hashmap, part->tag, strlen(part->tag), part);
        ret = QEO_OK;
    } while (0);

    return ret;
}

qeo_retcode_t qeo_policy_cache_add_coarse_grained_rule(qeo_policy_cache_hndl_t          cache,
                                                       const char                       *participant_tag,
                                                       const char                       *topic_name,
                                                       const policy_parser_permission_t *perms)
{
    qeo_retcode_t     ret = QEO_EFAIL;
    struct topic_desc *topic;

    do {
        if (cache == NULL) {
            ret = QEO_EINVAL;
            break;
        }

        if ((ret = add_topic(cache, topic_name, true, &topic)) != QEO_OK) {
            qeo_log_e("topic_name %s could not be added", topic_name);
            break;
        }

        if ((ret = add_coarse_grained(cache, participant_tag, topic, perms, true)) != QEO_OK) {
            qeo_log_e("Could not add course grained (%s, %s)", participant_tag, topic_name);
            break;
        }

        ret = QEO_OK;
    } while (0);

    return ret;
}

qeo_retcode_t qeo_policy_cache_add_fine_grained_rule_section(qeo_policy_cache_hndl_t          cache,
                                                             const char                       *participant_tag,
                                                             const char                       *topic_name,
                                                             const policy_parser_permission_t *perms,
                                                             const char                       *participant_specifier)
{
    qeo_retcode_t     ret = QEO_EFAIL;
    struct topic_desc *topic;

    do {
        if (cache == NULL) {
            ret = QEO_EINVAL;
            break;
        }

        if ((ret = add_topic(cache, topic_name, false, &topic)) != QEO_OK) {
            qeo_log_e("topic_name %s could not be added", topic_name);
            break;
        }

        if ((ret = add_fine_grained_rule_section(cache, participant_tag, topic, perms, participant_specifier)) != QEO_OK) {
            qeo_log_e("Could not add fine grained (%s, %s)", participant_tag, topic_name);
            break;
        }


        ret = QEO_OK;
    } while (0);

    return ret;
}

qeo_retcode_t qeo_policy_cache_get_partition_strings(qeo_policy_cache_hndl_t                      cache,
                                                     uintptr_t                                    cookie,
                                                     const char                                   *participant_id,
                                                     const char                                   *topic_name,
                                                     unsigned int                                 selector_mask,
                                                     qeo_policy_cache_update_partition_string_cb  update_cb)
{
    qeo_retcode_t           ret = QEO_EFAIL;
    struct participant_desc *part;
    struct participant_desc *ptmp;
    struct rule_entry       *rtmp = NULL;

    do {
        if (cache == NULL) {
            ret = QEO_EINVAL;
            break;
        }

        if (update_cb == NULL) {
            ret = QEO_EINVAL;
            break;
        }

        HASH_ITER(hh, cache->participants_hashmap, part, ptmp)
        {
            if ((participant_id == NULL) || (strcmp(part->tag, participant_id) == 0)) {
                if (topic_name != NULL) {
                    const struct rule_entry *rentry     = NULL;
                    struct topic_desc       *topic      = NULL;
                    struct topic_desc       temp_topic  = { .name = (char *) topic_name };
                    /* 1. Find topic in cache->topics_hashmap */
                    HASH_FIND_STR(cache->topics_hashmap, topic_name, topic);

                    /* If topic was not found, use topic_desc on the stack based on topic_name */
                    if (topic == NULL) {
                        topic = &temp_topic;
                    }

                    bool wc_expansion;
                    rentry = find_match(part->rule_entries_hashmap, topic, &wc_expansion);
                    assert(rentry != NULL);
                    call_partition_string_cb(update_cb, cache, cookie, part->tag, topic_name, selector_mask, rentry);
                }
                else {
                    struct rule_entry *rentry = NULL;
                    HASH_ITER(hh, part->rule_entries_hashmap, rentry, rtmp)
                    {
                        call_partition_string_cb(update_cb, cache, cookie, part->tag, rentry->tdesc->name, selector_mask, rentry);
                    }
                }
            }
        }

        ret = QEO_OK;
    } while (0);

    return ret;
}

qeo_retcode_t qeo_policy_cache_finalize(qeo_policy_cache_hndl_t cache)
{
    qeo_retcode_t ret = QEO_EFAIL;

    do {
        if (cache == NULL) {
            ret = QEO_EINVAL;
            break;
        }
        /* first add * if it was not there yet to topics */
        if ((ret = add_default_wildcard(cache)) != QEO_OK) {
            qeo_log_e("Could not add default rule");
            break;
        }

        if (complete_topics(cache) == false) {
            qeo_log_e("Could not complete topics");
            break;
        }

        /* sort the topic names */
        HASH_SORT(cache->topics_hashmap, topic_prefix_length_sort);

        /* Make sure all topics are present for all participants */
        if ((ret = complete_partition_strings(cache)) != QEO_OK) {
            qeo_log_e("complete_rules failed");
            break;
        }

#ifdef DEBUG
        if (getenv("POLICY_DUMP") != NULL){
            dump_topics(cache);
            dump_participants(cache);
        }
#endif

        ret = QEO_OK;
    } while (0);

    return ret;
}

qeo_retcode_t qeo_policy_cache_get_participants(qeo_policy_cache_hndl_t         cache,
                                                qeo_policy_cache_participant_cb participant_cb)
{
    qeo_retcode_t           ret = QEO_EFAIL;
    struct participant_desc *part;
    struct participant_desc *ptmp;

    do {
        if (cache == NULL) {
            ret = QEO_EINVAL;
            break;
        }

        if (participant_cb == NULL) {
            ret = QEO_EINVAL;
            break;
        }

        HASH_ITER(hh, cache->participants_hashmap, part, ptmp)
        {
            participant_cb(cache, part->tag);
        }
        ret = QEO_OK;
    } while (0);

    return ret;
}

qeo_retcode_t qeo_policy_cache_reset(qeo_policy_cache_hndl_t cache)
{
    struct topic_desc       *topic;
    struct topic_desc       *tmp;
    struct participant_desc *part;
    struct participant_desc *ptmp;

    if (cache == NULL) {
        return QEO_EINVAL;
    }

    HASH_ITER(hh, cache->participants_hashmap, part, ptmp)
    {
        HASH_DEL(cache->participants_hashmap, part);
        free_participant_desc(part);
    }

    HASH_ITER(hh, cache->topics_hashmap, topic, tmp)
    {
        HASH_DEL(cache->topics_hashmap, topic);
        free_topic_desc(topic);
    }
    return QEO_OK;
}

qeo_retcode_t qeo_policy_cache_destruct(qeo_policy_cache_hndl_t *cache)
{
    if ((cache == NULL) || (*cache == NULL)) {
        return QEO_OK;
    }

    qeo_policy_cache_reset(*cache);
    free(*cache);
    *cache = NULL;
    return QEO_OK;
}
