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

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <assert.h>
#include <unistd.h>
#include "dds/dds_types.h"
#include "dds/dds_dcps.h"
#include "dds/dds_tsm.h"
#include "dds/dds_dreader.h"
#include "dds/dds_dwriter.h"
#include "dds/dds_seq.h"
#include "dds/dds_debug.h"
#include "libx.h"
#include "crc32.h"
#include "type_data.h"
#include "xdata.h"

#define	USE_BIG_JPEG	/* Set this to use the Big.jpg picture on disk. */
#define	DUMP_PICTURE	/* Set this to use the Big.jpg picture on disk. */
#define TRACE_DATA	/* Trace sent/received data. */

int verbose = 1;

const DDS_TypeSupport_meta _org_qeo_UUID_type[] = {
    { .tc = CDR_TYPECODE_STRUCT, .name = "org.qeo.UUID", .flags = TSMFLAG_GENID|TSMFLAG_MUTABLE, .nelem = 2 },
    { .tc = CDR_TYPECODE_LONGLONG, .name = "lower" },
    { .tc = CDR_TYPECODE_LONGLONG, .name = "upper" },
};

const DDS_TypeSupport_meta _org_qeo_mytribe_Name_type[] = {
    { .tc = CDR_TYPECODE_STRUCT, .name = "org.qeo.mytribe.Name", .flags = TSMFLAG_DYNAMIC|TSMFLAG_GENID|TSMFLAG_MUTABLE, .nelem = 3 },
    { .tc = CDR_TYPECODE_CSTRING, .name = "middle", .flags = TSMFLAG_DYNAMIC },
    { .tc = CDR_TYPECODE_CSTRING, .name = "first", .flags = TSMFLAG_DYNAMIC },
    { .tc = CDR_TYPECODE_CSTRING, .name = "last", .flags = TSMFLAG_DYNAMIC },
};

const DDS_TypeSupport_meta _org_qeo_mytribe_PhoneNumber_type[] = {
    { .tc = CDR_TYPECODE_STRUCT, .name = "org.qeo.mytribe.PhoneNumber", .flags = TSMFLAG_DYNAMIC|TSMFLAG_GENID|TSMFLAG_MUTABLE, .nelem = 2 },
    { .tc = CDR_TYPECODE_CSTRING, .name = "type", .flags = TSMFLAG_DYNAMIC },
    { .tc = CDR_TYPECODE_CSTRING, .name = "phoneNumber", .flags = TSMFLAG_DYNAMIC },
};

DDS_TypeSupport_meta _seq_org_qeo_mytribe_PhoneNumber_type [] = {
    { .tc = CDR_TYPECODE_SEQUENCE, .name = "sequence_org.qeo.mytribe.PhoneNumber", .flags = TSMFLAG_DYNAMIC|TSMFLAG_GENID|TSMFLAG_MUTABLE },
    { .tc = CDR_TYPECODE_TYPEREF, .tsm = _org_qeo_mytribe_PhoneNumber_type },
};

const DDS_TypeSupport_meta _org_qeo_mytribe_Email_type[] = {
    { .tc = CDR_TYPECODE_STRUCT, .name = "org.qeo.mytribe.Email", .flags = TSMFLAG_DYNAMIC|TSMFLAG_GENID|TSMFLAG_MUTABLE, .nelem = 2 },
    { .tc = CDR_TYPECODE_CSTRING, .name = "email", .flags = TSMFLAG_DYNAMIC },
    { .tc = CDR_TYPECODE_CSTRING, .name = "type", .flags = TSMFLAG_DYNAMIC },
};

DDS_TypeSupport_meta _seq_org_qeo_mytribe_Email_type [] = {
    { .tc = CDR_TYPECODE_SEQUENCE, .name = "sequence_org.qeo.mytribe.Email", .flags = TSMFLAG_DYNAMIC|TSMFLAG_GENID|TSMFLAG_MUTABLE },
    { .tc = CDR_TYPECODE_TYPEREF, .tsm = _org_qeo_mytribe_Email_type },
};

const DDS_TypeSupport_meta _org_qeo_mytribe_Address_type[] = {
    { .tc = CDR_TYPECODE_STRUCT, .name = "org.qeo.mytribe.Address", .flags = TSMFLAG_DYNAMIC|TSMFLAG_GENID|TSMFLAG_MUTABLE, .nelem = 6 },
    { .tc = CDR_TYPECODE_CSTRING, .name = "street", .flags = TSMFLAG_DYNAMIC },
    { .tc = CDR_TYPECODE_CSTRING, .name = "country", .flags = TSMFLAG_DYNAMIC },
    { .tc = CDR_TYPECODE_CSTRING, .name = "city", .flags = TSMFLAG_DYNAMIC },
    { .tc = CDR_TYPECODE_CSTRING, .name = "type", .flags = TSMFLAG_DYNAMIC },
    { .tc = CDR_TYPECODE_CSTRING, .name = "zip", .flags = TSMFLAG_DYNAMIC },
    { .tc = CDR_TYPECODE_CSTRING, .name = "state", .flags = TSMFLAG_DYNAMIC },
};

DDS_TypeSupport_meta _seq_org_qeo_mytribe_Address_type [] = {
    { .tc = CDR_TYPECODE_SEQUENCE, .name = "sequence_org.qeo.mytribe.Address", .flags = TSMFLAG_DYNAMIC|TSMFLAG_GENID|TSMFLAG_MUTABLE },
    { .tc = CDR_TYPECODE_TYPEREF, .tsm = _org_qeo_mytribe_Address_type },
};

const DDS_TypeSupport_meta _org_qeo_mytribe_Picture_type[] = {
    { .tc = CDR_TYPECODE_STRUCT, .name = "org.qeo.mytribe.Picture", .flags = TSMFLAG_DYNAMIC|TSMFLAG_GENID|TSMFLAG_MUTABLE, .nelem = 2 },
    { .tc = CDR_TYPECODE_CSTRING, .name = "mimeType", .flags = TSMFLAG_DYNAMIC },
    { .tc = CDR_TYPECODE_SEQUENCE, .name = "data", .flags = TSMFLAG_DYNAMIC|TSMFLAG_GENID|TSMFLAG_MUTABLE },
    { .tc = CDR_TYPECODE_OCTET },
};

DDS_TypeSupport_meta _org_qeo_mytribe_ContactData_type[] = {
    { .tc = CDR_TYPECODE_STRUCT, .name = "org.qeo.mytribe.ContactData", .flags = TSMFLAG_DYNAMIC|TSMFLAG_GENID|TSMFLAG_MUTABLE, .nelem = 5 },
    { .tc = CDR_TYPECODE_TYPEREF, .name = "name", .tsm = _org_qeo_mytribe_Name_type },
    { .tc = CDR_TYPECODE_TYPEREF, .name = "addresses", .tsm = _seq_org_qeo_mytribe_Address_type, .flags = TSMFLAG_DYNAMIC|TSMFLAG_GENID|TSMFLAG_MUTABLE },
    { .tc = CDR_TYPECODE_TYPEREF, .name = "phoneNumbers", .tsm=_seq_org_qeo_mytribe_PhoneNumber_type, .flags = TSMFLAG_DYNAMIC|TSMFLAG_GENID|TSMFLAG_MUTABLE },
    { .tc = CDR_TYPECODE_TYPEREF, .name = "emails", .tsm=_seq_org_qeo_mytribe_Email_type, .flags = TSMFLAG_DYNAMIC|TSMFLAG_GENID|TSMFLAG_MUTABLE },
    { .tc = CDR_TYPECODE_TYPEREF, .name = "picture", .tsm = _org_qeo_mytribe_Picture_type },
};

DDS_TypeSupport_meta _org_qeo_mytribe_Contact_type[] = {
    { .tc = CDR_TYPECODE_STRUCT, .name = "org.qeo.mytribe.Contact", .flags = TSMFLAG_GENID|TSMFLAG_KEY|TSMFLAG_MUTABLE, .nelem = 3 },
    { .tc = CDR_TYPECODE_TYPEREF, .name = "contactId", .flags = TSMFLAG_KEY, .tsm = _org_qeo_UUID_type },
    { .tc = CDR_TYPECODE_LONGLONG, .name = "providerId", .flags = TSMFLAG_KEY },
    { .tc = CDR_TYPECODE_TYPEREF, .name = "contactData", .tsm = _org_qeo_mytribe_ContactData_type },
};

DDS_TypeSupport_meta _org_qeo_mytribe_ContactRequest_type[] = {
    { .tc = CDR_TYPECODE_STRUCT, .name = "org.qeo.mytribe.ContactRequest", .flags = TSMFLAG_GENID|TSMFLAG_MUTABLE, .nelem = 3 },
    { .tc = CDR_TYPECODE_TYPEREF, .name = "contactId", .tsm = _org_qeo_UUID_type },
    { .tc = CDR_TYPECODE_LONGLONG, .name = "providerId" },
    { .tc = CDR_TYPECODE_TYPEREF, .name = "contactData", .tsm = _org_qeo_mytribe_ContactData_type },
};

static DDS_TypeSupport UUID_type;
static DDS_TypeSupport Name_type;
static DDS_TypeSupport PhoneNumber_type;
static DDS_TypeSupport seq_PhoneNumber_type;
static DDS_TypeSupport Email_type;
static DDS_TypeSupport seq_Email_type;
static DDS_TypeSupport Address_type;
static DDS_TypeSupport seq_Address_type;
static DDS_TypeSupport Picture_type;
static DDS_TypeSupport ContactData_type;
static DDS_TypeSupport Contact_type;
static DDS_TypeSupport ContactRequest_type;

DDS_ReturnCode_t register_contact_types (DDS_DomainParticipant part)
{
	DDS_ReturnCode_t	error;

	DDS_set_generate_callback (crc32_char);

	UUID_type = DDS_DynamicType_register (_org_qeo_UUID_type);
	if (!UUID_type)
		return (DDS_RETCODE_ERROR);

	Name_type = DDS_DynamicType_register (_org_qeo_mytribe_Name_type);
	if (!Name_type)
		return (DDS_RETCODE_ERROR);

	PhoneNumber_type = DDS_DynamicType_register (_org_qeo_mytribe_PhoneNumber_type);
	if (!PhoneNumber_type)
		return (DDS_RETCODE_ERROR);

	DDS_DynamicType_set_type (_seq_org_qeo_mytribe_PhoneNumber_type, 1, PhoneNumber_type);
	seq_PhoneNumber_type = DDS_DynamicType_register (_seq_org_qeo_mytribe_PhoneNumber_type);
	if (!seq_PhoneNumber_type)
		return (DDS_RETCODE_ERROR);

	Email_type = DDS_DynamicType_register (_org_qeo_mytribe_Email_type);
	if (!Email_type)
		return (DDS_RETCODE_ERROR);

	DDS_DynamicType_set_type (_seq_org_qeo_mytribe_Email_type, 1, Email_type);
	seq_Email_type = DDS_DynamicType_register (_seq_org_qeo_mytribe_Email_type);
	if (!seq_Email_type)
		return (DDS_RETCODE_ERROR);

	Address_type = DDS_DynamicType_register (_org_qeo_mytribe_Address_type);
	if (!Address_type)
		return (DDS_RETCODE_ERROR);

	DDS_DynamicType_set_type (_seq_org_qeo_mytribe_Address_type, 1, Address_type);
	seq_Address_type = DDS_DynamicType_register (_seq_org_qeo_mytribe_Address_type);
	if (!seq_Address_type)
		return (DDS_RETCODE_ERROR);

	Picture_type = DDS_DynamicType_register (_org_qeo_mytribe_Picture_type);
	if (!Picture_type)
		return (DDS_RETCODE_ERROR);

	DDS_DynamicType_set_type (_org_qeo_mytribe_ContactData_type, 1, Name_type);
	DDS_DynamicType_set_type (_org_qeo_mytribe_ContactData_type, 2, seq_Address_type);
	DDS_DynamicType_set_type (_org_qeo_mytribe_ContactData_type, 3, seq_PhoneNumber_type);
	DDS_DynamicType_set_type (_org_qeo_mytribe_ContactData_type, 4, seq_Email_type);
	DDS_DynamicType_set_type (_org_qeo_mytribe_ContactData_type, 5, Picture_type);
	ContactData_type = DDS_DynamicType_register (_org_qeo_mytribe_ContactData_type);
	if (!ContactData_type)
		return (DDS_RETCODE_ERROR);

	DDS_DynamicType_set_type (_org_qeo_mytribe_Contact_type, 1, UUID_type);
	DDS_DynamicType_set_type (_org_qeo_mytribe_Contact_type, 3, ContactData_type);
	Contact_type = DDS_DynamicType_register (_org_qeo_mytribe_Contact_type);
	if (!Contact_type)
		return (DDS_RETCODE_ERROR);

	if (verbose)
		printf ("DDS Dynamic type ('%s') registered.\r\n", "org.qeo.mytribe.Contact");

	error = DDS_DomainParticipant_register_type (part, Contact_type, "org.qeo.mytribe.Contact");
	if (error)
		return (error);

	if (verbose)
		printf ("DDS Dynamic type ('%s') added to domain.\r\n", "org.qeo.mytribe.Contact");

	DDS_DynamicType_set_type (_org_qeo_mytribe_ContactRequest_type, 1, UUID_type);
	DDS_DynamicType_set_type (_org_qeo_mytribe_ContactRequest_type, 3, ContactData_type);
	ContactRequest_type = DDS_DynamicType_register (_org_qeo_mytribe_ContactRequest_type);
	if (!ContactRequest_type)
		return (DDS_RETCODE_ERROR);

	if (verbose)
		printf ("DDS Dynamic type ('%s') registered.\r\n", "org.qeo.mytribe.ContactRequest");

	error = DDS_DomainParticipant_register_type (part, ContactRequest_type, "org.qeo.mytribe.ContactRequest");
	if (verbose && !error)
		printf ("DDS Dynamic type ('%s') added to domain.\r\n", "org.qeo.mytribe.ContactRequest");

	DDS_Debug_command ("stypes");

	return (error);
}

void unregister_contact_types (DDS_DomainParticipant part)
{
	DDS_DomainParticipant_unregister_type (part, Contact_type, "org.qeo.mytribe.Contact");
	if (verbose)
		printf ("DDS Dynamic type ('%s') removed from domain.\r\n", "org.qeo.mytribe.Contact");

	DDS_DomainParticipant_unregister_type (part, ContactRequest_type, "org.qeo.mytribe.ContactRequest");
	if (verbose)
		printf ("DDS Dynamic type ('%s') removed from domain.\r\n", "org.qeo.mytribe.ContactRequest");

	DDS_DynamicType_free (Contact_type);
	if (verbose)
		printf ("DDS Dynamic type ('%s') disposed.\r\n", "org.qeo.mytribe.Contact");

	DDS_DynamicType_free (ContactRequest_type);
	if (verbose)
		printf ("DDS Dynamic type ('%s') disposed.\r\n", "org.qeo.mytribe.ContactRequest");
}

DDS_DataWriter create_writer (DDS_DomainParticipant part,
			      DDS_Topic             *topic,
			      DDS_Publisher         *pub,
			      const char            *name,
			      const char            *type_name,
			      int                   event)
{
	DDS_DataWriterQos	wr_qos;
	DDS_DataWriter		w;
	DDS_StatusMask		sm = 0;

	/* Create topic if not yet done. */
	if (!*topic) {
		*topic = DDS_DomainParticipant_create_topic (part, name, type_name,
								NULL, NULL, sm);
		if (!*topic) {
			DDS_DomainParticipantFactory_delete_participant (part);
			fatal ("DDS_DomainParticipant_create_topic ('HelloWorld') failed!");
		}
		printf ("DDS Topic ('%s') created.\r\n", name);
	}

	/* Create publisher if not yet done. */
	if (!*pub) {
		*pub = DDS_DomainParticipant_create_publisher (part, NULL, NULL, 0);
		if (!*pub)
			fatal ("DDS_DomainParticipant_create_publisher () failed!");

		printf ("DDS Publisher created.\r\n");
	}

	/* Setup writer QoS parameters. */
	DDS_Publisher_get_default_datawriter_qos (*pub, &wr_qos);
	wr_qos.reliability.kind = DDS_RELIABLE_RELIABILITY_QOS;
	if (event) {
		wr_qos.durability.kind = DDS_VOLATILE_DURABILITY_QOS;
		wr_qos.history.kind = DDS_KEEP_ALL_HISTORY_QOS;
		wr_qos.history.depth = DDS_LENGTH_UNLIMITED;
		wr_qos.resource_limits.max_samples_per_instance = DDS_LENGTH_UNLIMITED;
		wr_qos.resource_limits.max_instances = DDS_LENGTH_UNLIMITED;
		wr_qos.resource_limits.max_samples = DDS_LENGTH_UNLIMITED;
	}
	else {
		wr_qos.durability.kind = DDS_TRANSIENT_LOCAL_DURABILITY_QOS;
		wr_qos.history.kind = DDS_KEEP_LAST_HISTORY_QOS;
		wr_qos.history.depth = 1;
	}

	/* Create data writer. */
	w = DDS_Publisher_create_datawriter (*pub, *topic, &wr_qos, NULL, sm);
	if (!w)
		fatal ("DDS_DomainParticipant_create_datawriter () returned an error!");

	if (verbose)
		printf ("DDS Writer ('%s') created.\r\n", name);

	return (w);
}

static uint32_t gen_id (const char* name)
{
	uint32_t ret;

	ret = crc32_char (name);

	ret &= 0x0FFFFFFF;
	if (ret < 2) {
		ret += 2;
	}
	return (ret);
}

static DDS_DynamicType ts2dtype (DDS_TypeSupport ts)
{
	return (DDS_DynamicTypeSupport_get_type ((DDS_DynamicTypeSupport) ts));
}

static void dump_raw_dynamic (DDS_DynamicData d)
{
	DynDataRef_t	*drp;

	drp = (DynDataRef_t *) d;
	xd_dump (2, drp->ddata);
}

void write_data (DDS_DataWriter w, DDS_TypeSupport ts, size_t size)
{
	DDS_DynamicTypeBuilder	ostb;
	DDS_DynamicType		uuid_t, cd_t, name_t;
	DDS_DynamicType		address_t, seq_address_t;
	DDS_DynamicType		phone_t, seq_phone_t;
	DDS_DynamicType		email_t, seq_email_t;
	DDS_DynamicType		picture_t, contact_t;
	DDS_DynamicType		ot, ost;
	DDS_DynamicData		d, uuid, cd, name, pic, dds;
	DDS_DynamicData		address, address_seq;
	DDS_DynamicData		phoneh, phonem, phone_seq;
	DDS_DynamicData		email, email_seq;
	DDS_OctetSeq		bseq;
	FILE			*f;
	size_t			s;

	contact_t = ts2dtype (ts);
	assert (contact_t);

	d = DDS_DynamicDataFactory_create_data (contact_t);

	DDS_DynamicTypeBuilderFactory_delete_type (contact_t);

	uuid_t = ts2dtype (UUID_type);
	assert (uuid_t);

	uuid = DDS_DynamicDataFactory_create_data (uuid_t);
	assert (uuid);

	DDS_DynamicTypeBuilderFactory_delete_type (uuid_t);

	assert (!DDS_DynamicData_set_int64_value (uuid, gen_id ("lower"), 0x0001020304050607LL));
	assert (!DDS_DynamicData_set_int64_value (uuid, gen_id ("upper"), 0x08090a0b0c0d0e0fLL));

	assert (!DDS_DynamicData_set_complex_value (d, gen_id ("contactId"), uuid));
	assert (!DDS_DynamicData_set_int64_value (d, gen_id ("providerId"), 0xaabbccddeeff9988LL + size));

	cd_t = ts2dtype (ContactData_type);
	assert (cd_t);

	cd = DDS_DynamicDataFactory_create_data (cd_t);
	assert (cd);

	DDS_DynamicTypeBuilderFactory_delete_type (cd_t);

	name_t = ts2dtype (Name_type);
	assert (name_t);

	name = DDS_DynamicDataFactory_create_data (name_t);
	assert (name);

	DDS_DynamicTypeBuilderFactory_delete_type (name_t);

	assert (!DDS_DynamicData_set_string_value (name, gen_id ("middle"), "A"));
	assert (!DDS_DynamicData_set_string_value (name, gen_id ("first"), "Jan"));
	assert (!DDS_DynamicData_set_string_value (name, gen_id ("last"), "Voet"));

	assert (!DDS_DynamicData_set_complex_value (cd, gen_id ("name"), name));

	address_t = ts2dtype (Address_type);
	assert (address_t);

	address = DDS_DynamicDataFactory_create_data (address_t);
	assert (address);

	DDS_DynamicTypeBuilderFactory_delete_type (address_t);

	assert (!DDS_DynamicData_set_string_value (address, gen_id ("street"), "Torfhoeken"));
	assert (!DDS_DynamicData_set_string_value (address, gen_id ("country"), "Belgium"));
	assert (!DDS_DynamicData_set_string_value (address, gen_id ("city"), "Schilde"));
	assert (!DDS_DynamicData_set_string_value (address, gen_id ("type"), "Home"));
	assert (!DDS_DynamicData_set_string_value (address, gen_id ("zip"), "2970"));
	assert (!DDS_DynamicData_set_string_value (address, gen_id ("state"), "Antwerpen"));

	seq_address_t = ts2dtype (seq_Address_type);
	assert (seq_address_t);

	address_seq = DDS_DynamicDataFactory_create_data (seq_address_t);
	assert (address_seq);

	DDS_DynamicTypeBuilderFactory_delete_type (seq_address_t);

	assert (!DDS_DynamicData_set_complex_value (address_seq, 0, address));

	assert (!DDS_DynamicData_set_complex_value (cd, gen_id ("addresses"), address_seq));

	phone_t = ts2dtype (PhoneNumber_type);
	assert (phone_t);

	phoneh = DDS_DynamicDataFactory_create_data (phone_t);
	assert (phoneh);

	assert (!DDS_DynamicData_set_string_value (phoneh, gen_id ("type"), "Home"));
	assert (!DDS_DynamicData_set_string_value (phoneh, gen_id ("phoneNumber"), "+32-3-385-01-07"));

	phonem = DDS_DynamicDataFactory_create_data (phone_t);
	assert (phonem);

	DDS_DynamicTypeBuilderFactory_delete_type (phone_t);

	assert (!DDS_DynamicData_set_string_value (phonem, gen_id ("type"), "Mobile"));
	assert (!DDS_DynamicData_set_string_value (phonem, gen_id ("phoneNumber"), "+32-479-97-51-65"));

	seq_phone_t = ts2dtype (seq_PhoneNumber_type);
	assert (seq_phone_t);

	phone_seq = DDS_DynamicDataFactory_create_data (seq_phone_t);
	assert (phone_seq);
	
	DDS_DynamicTypeBuilderFactory_delete_type (seq_phone_t);

	assert (!DDS_DynamicData_set_complex_value (phone_seq, 0, phoneh));
	assert (!DDS_DynamicData_set_complex_value (phone_seq, 1, phonem));

	assert (!DDS_DynamicData_set_complex_value (cd, gen_id ("phoneNumbers"), phone_seq));

	email_t = ts2dtype (Email_type);
	assert (email_t);

	email = DDS_DynamicDataFactory_create_data (email_t);
	assert (email);

	DDS_DynamicTypeBuilderFactory_delete_type (email_t);

	assert (!DDS_DynamicData_set_string_value (email, gen_id ("email"), "jan.voet@gmail.com"));
	assert (!DDS_DynamicData_set_string_value (email, gen_id ("type"), "Webmail"));

	seq_email_t = ts2dtype (seq_Email_type);
	assert (seq_email_t);

	email_seq = DDS_DynamicDataFactory_create_data (seq_email_t);
	assert (email_seq);

	DDS_DynamicTypeBuilderFactory_delete_type (seq_email_t);

	assert (!DDS_DynamicData_set_complex_value (email_seq, 0, email));

	assert (!DDS_DynamicData_set_complex_value (cd, gen_id ("emails"), email_seq));

	picture_t = ts2dtype (Picture_type);
	assert (picture_t);

	pic = DDS_DynamicDataFactory_create_data (picture_t);
	assert (pic);

	DDS_DynamicTypeBuilderFactory_delete_type (picture_t);

	assert (!DDS_DynamicData_set_string_value (pic, gen_id ("mimeType"), "JPEG"));

	ot = DDS_DynamicTypeBuilderFactory_get_primitive_type (DDS_BYTE_TYPE);
	assert (ot);

	ostb = DDS_DynamicTypeBuilderFactory_create_sequence_type (ot, 
							DDS_UNBOUNDED_COLLECTION);
	assert (ostb);

	ost = DDS_DynamicTypeBuilder_build (ostb);
	assert (ost);

	DDS_DynamicTypeBuilderFactory_delete_type (ostb);

	dds = DDS_DynamicDataFactory_create_data (ost);
	assert (dds);

	DDS_DynamicTypeBuilderFactory_delete_type (ost);

	DDS_SEQ_INIT (bseq);
	if (!size) {
		f = fopen ("Big.jpg", "r");
		assert (f);
		fseek (f, 0L, SEEK_END);
		size = ftell (f);
	}
	else
		f = NULL;
	dds_seq_require (&bseq, size);
	DDS_SEQ_LENGTH (bseq) = size;
	if (f) {
		rewind (f);
		s = fread (DDS_SEQ_DATA (bseq), size, 1, f);
		assert (s == 1);
		fclose (f);
	}
	assert (!DDS_DynamicData_set_byte_values (dds, 0, &bseq));

	assert (!DDS_DynamicData_set_complex_value (pic, gen_id ("data"), dds));

	assert (!DDS_DynamicData_set_complex_value (cd, gen_id ("picture"), pic));

	assert (!DDS_DynamicData_set_complex_value (d, gen_id ("contactData"), cd));

	if (verbose) {
		printf ("Data sample prepared:\r\n");
		DDS_Debug_dump_dynamic (1, (DDS_DynamicTypeSupport) ts, d, 0, 0, 1);
		/*dump_raw_dynamic (d);*/
	}

	assert (!DDS_DynamicDataWriter_write (w, d, 0));

	DDS_DynamicDataFactory_delete_data (dds);
	DDS_DynamicDataFactory_delete_data (pic);
	DDS_DynamicDataFactory_delete_data (email_seq);
	DDS_DynamicDataFactory_delete_data (email);
	DDS_DynamicDataFactory_delete_data (phone_seq);
	DDS_DynamicDataFactory_delete_data (phonem);
	DDS_DynamicDataFactory_delete_data (phoneh);
	DDS_DynamicDataFactory_delete_data (address_seq);
	DDS_DynamicDataFactory_delete_data (address);
	DDS_DynamicDataFactory_delete_data (name);
	DDS_DynamicDataFactory_delete_data (cd);
	DDS_DynamicDataFactory_delete_data (uuid);
	DDS_DynamicDataFactory_delete_data (d);
}

#ifdef DUMP_PICTURE

static void dump_picture (DDS_DynamicData d, unsigned index)
{
	DDS_DynamicData	cd, pic, dds;
	DDS_MemberId	id;
	DDS_ByteSeq	bseq;
	FILE		*f;
	char		name [32];

	id = DDS_DynamicData_get_member_id_by_name (d, "contactData");
	assert (id != DDS_MEMBER_ID_INVALID);

	assert (!DDS_DynamicData_get_complex_value (d, &cd, id));

	id = DDS_DynamicData_get_member_id_by_name (cd, "picture");
	assert (id != DDS_MEMBER_ID_INVALID);

	assert (!DDS_DynamicData_get_complex_value (cd, &pic, id));

	id = DDS_DynamicData_get_member_id_by_name (pic, "data");
	assert (id != DDS_MEMBER_ID_INVALID);

	assert (!DDS_DynamicData_get_complex_value (pic, &dds, id));

	DDS_SEQ_INIT (bseq);
	assert (!DDS_DynamicData_get_byte_values (dds, &bseq, 0));
	assert (DDS_SEQ_LENGTH (bseq) <= 4000000000UL && DDS_SEQ_LENGTH (bseq) > 0);

	sprintf (name, "pict%u.jpg", index);
	f = fopen (name, "w");
	assert (fwrite (DDS_SEQ_DATA (bseq), DDS_SEQ_LENGTH (bseq), 1, f) == 1);
	fclose (f);
}

#endif

static void reader_data_available (DDS_DataReaderListener *l,
				   DDS_DataReader         dr)
{
	static DDS_DynamicDataSeq drx_sample = DDS_SEQ_INITIALIZER (void *);
	static DDS_SampleInfoSeq rx_info = DDS_SEQ_INITIALIZER (DDS_SampleInfo *);
	DDS_SampleStateMask	ss = DDS_NOT_READ_SAMPLE_STATE;
	DDS_ViewStateMask	vs = DDS_ANY_VIEW_STATE;
	DDS_InstanceStateMask	is = DDS_ANY_INSTANCE_STATE;
	DDS_SampleInfo		*info;
	DDS_DynamicData		dd;
	DDS_ReturnCode_t	error;
	static int		index = 0;

	ARG_NOT_USED (l)

	if (verbose)
		printf ("\r\n");

	for (;;) {
		error = DDS_DynamicDataReader_read (dr, &drx_sample, &rx_info, 1, ss, vs, is);
		if (error) {
			if (error != DDS_RETCODE_NO_DATA)
				printf ("Unable to read samples: error = %s!\r\n", DDS_error (error));
			break;
		}
		if (DDS_SEQ_LENGTH (rx_info)) {
			info = DDS_SEQ_ITEM (rx_info, 0);
			if (info->valid_data) {
				printf ("Data Sample received:\r\n");
				dd = DDS_SEQ_ITEM (drx_sample, 0);
				if (!dd)
					fatal ("Empty dynamic sample!");
#ifdef DUMP_PICTURE
				dump_picture (dd, index++);
#endif
				if (verbose) {
					DDS_Debug_dump_dynamic (1, (DDS_DynamicTypeSupport) l->cookie, dd, 0, 0, 1);
					/*dump_raw_dynamic (dd);*/
				}
			}
			DDS_DynamicDataReader_return_loan (dr, &drx_sample, &rx_info);
		}
		else
			break;
	}
}

static DDS_DataReaderListener dr_listeners [2] = {
{
	NULL,			/* Sample rejected. */
	NULL,			/* Liveliness changed. */
	NULL,			/* Requested Deadline missed. */
	NULL,			/* Requested incompatible QoS. */
	reader_data_available,	/* Data available. */
	NULL,			/* Subscription matched. */
	NULL,			/* Sample lost. */
	NULL			/* Cookie */
},
{
	NULL,			/* Sample rejected. */
	NULL,			/* Liveliness changed. */
	NULL,			/* Requested Deadline missed. */
	NULL,			/* Requested incompatible QoS. */
	reader_data_available,	/* Data available. */
	NULL,			/* Subscription matched. */
	NULL,			/* Sample lost. */
	NULL			/* Cookie */
}
};

DDS_DataReader create_reader (DDS_DomainParticipant part,
			      DDS_Topic             *topic,
			      DDS_TopicDescription  *topic_desc,
			      DDS_Subscriber        *sub,
			      DDS_TypeSupport       ts,
			      const char            *name,
			      const char            *type_name,
			      int                   event)
{
	DDS_DataReaderQos	r_qos;
	DDS_StatusMask		sm = 0;
	DDS_DataReader		r;

	/* Create topic if not yet done. */
	if (!*topic) {
		*topic = DDS_DomainParticipant_create_topic (part, name, type_name,
								NULL, NULL, sm);
		if (!*topic)
			fatal ("DDS_DomainParticipant_create_topic failed!");

		if (verbose)
			printf ("DDS Topic ('%s') created.\r\n", name);
	}

	/* Create Topic Description if not yet done. */
	if (!*topic_desc) {
		*topic_desc = DDS_DomainParticipant_lookup_topicdescription (part, name);
		if (!*topic_desc)
			fatal ("Unable to create topic description!");

		if (verbose)
			printf ("DDS TopicDescription ('%s') created.\r\n", name);
	}

	/* Create subscriber if not yet done. */
	if (!*sub) {
		*sub = DDS_DomainParticipant_create_subscriber (part, NULL, NULL, 0);
		if (!*sub)
			fatal ("DDS_DomainParticipant_create_subscriber () failed!");

		if (verbose)
			printf ("DDS Subscriber created.\r\n");
	}

	/* Setup reader QoS parameters. */
	DDS_Subscriber_get_default_datareader_qos (*sub, &r_qos);
	r_qos.reliability.kind = DDS_RELIABLE_RELIABILITY_QOS;
	if (event) {
		r_qos.durability.kind = DDS_VOLATILE_DURABILITY_QOS;
		r_qos.history.kind = DDS_KEEP_ALL_HISTORY_QOS;
		r_qos.history.depth = DDS_LENGTH_UNLIMITED;
		r_qos.resource_limits.max_samples_per_instance = DDS_LENGTH_UNLIMITED;
		r_qos.resource_limits.max_instances = DDS_LENGTH_UNLIMITED;
		r_qos.resource_limits.max_samples = DDS_LENGTH_UNLIMITED;
	}
	else {
		r_qos.durability.kind = DDS_TRANSIENT_LOCAL_DURABILITY_QOS;
		r_qos.history.kind = DDS_KEEP_LAST_HISTORY_QOS;
		r_qos.history.depth = 1;
	}
	dr_listeners [event].cookie = ts;
	sm = DDS_DATA_AVAILABLE_STATUS;
	r = DDS_Subscriber_create_datareader (*sub, *topic_desc, &r_qos, &dr_listeners [event], sm);
	if (!r)
		fatal ("DDS_DomainParticipant_create_datareader () returned an error!");

	if (verbose)
		printf ("DDS Reader ('%s') created.\r\n", name);

	return (r);
}

void read_data (unsigned delay, DDS_DataReader r1, DDS_DataReader r2)
{
	ARG_NOT_USED (r1);
	ARG_NOT_USED (r2);

	if (verbose) {
		printf ("Waiting for incoming data ...");
		fflush (stdout);
	}
	usleep (delay);
	if (verbose)
		printf (" done.\r\n");
}

int main (int argc, char *argv [])
{
	DDS_DomainParticipant	part;
	DDS_Topic		t_c, t_cr;
	DDS_TopicDescription	td_c, td_cr;
	DDS_Publisher		pub;
	DDS_Subscriber		sub;
	DDS_DataWriter		w_c, w_cr;
	DDS_DataReader		r_c, r_cr;
	unsigned		i;

	if (argc > 2)
		return (1);

	part = DDS_DomainParticipantFactory_create_participant (
						6, NULL, NULL, 0);
	if (!part)
		fatal ("DDS_DomainParticipantFactory_create_participant () failed!");

	if (verbose)
		printf ("DDS Domain Participant created.\r\n");

	pub = NULL;
	sub = NULL;
	t_c = t_cr = NULL;
	td_c = td_cr = NULL;

	register_contact_types (part);
#ifdef DDS_DEBUG
	DDS_Debug_server_start (2, 0);
#ifdef TRACE_DATA
	DDS_Trace_defaults_set (DDS_TRACE_ALL);
#endif
#endif
	if (argc == 2 && *argv [1] == '-' && argv [1][1] == 'w') {

		/* Create writers. */
		w_c = create_writer (part,
				     &t_c,
				     &pub,
				     "Contact",
				     "org.qeo.mytribe.Contact",
				     0);
		w_cr = create_writer (part,
				      &t_cr,
				      &pub,
				      "ContactRequest",
				      "org.qeo.mytribe.ContactRequest",
				      1);

		/* Write some data on each writer. */
		for (i = 0; i < 10; i++) {
			write_data (w_c, Contact_type, 187346);
			usleep (1000000);
			write_data (w_c, Contact_type, 444639);
			usleep (1000000);
			write_data (w_cr, ContactRequest_type, 0 /*95368*/);
			usleep (1000000);
			write_data (w_cr, ContactRequest_type, 88396);
			usleep (1000000);
			write_data (w_cr, ContactRequest_type, 187346);
			usleep (1000000);
			write_data (w_c, Contact_type, 95368);
			usleep (1000000);
			write_data (w_c, Contact_type, 88396);
			usleep (2000000);
			write_data (w_cr, ContactRequest_type, 0 /*444639*/);
			usleep (1000000);
		}
	}
	else {

		/* Create readers. */
		r_c = create_reader (part,
				     &t_c,
				     &td_c,
				     &sub,
				     Contact_type,
				     "Contact",
				     "org.qeo.mytribe.Contact",
				     0);
		r_cr = create_reader (part,
				      &t_cr,
				      &td_cr,
				      &sub,
				      ContactRequest_type,
				      "ContactRequest",
				      "org.qeo.mytribe.ContactRequest",
				      1);

		/* Wait some time for data to come in and read it. */
		read_data (100000000, r_c, r_cr);
	}

	unregister_contact_types (part);

	DDS_DomainParticipant_delete_contained_entities (part);
	if (verbose)
		printf ("DDS contained domain entities deleted.\r\n");

	DDS_DomainParticipantFactory_delete_participant (part);
	if (verbose)
		printf ("DDS Domain Participant deleted.\r\n");

	return (0);
}
