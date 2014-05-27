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

/* dds_security.h -- DDS Security interface. */

#ifndef __dds_security_h_
#define	__dds_security_h_

#include "dds/dds_dcps.h"
#include "openssl/safestack.h"
#include "openssl/ossl_typ.h"
#ifdef  __cplusplus
extern "C" {
#endif

typedef unsigned IdentityHandle_t;
typedef unsigned PermissionsHandle_t;

typedef enum {
	DDS_FORMAT_X509,	/* X.509 format. */
	DDS_FORMAT_RSA,		/* RSA format. */
	DDS_FORMAT_ASN1		/* ASN.1 format. */
} DDS_CredentialsFormat;

typedef struct {
	DDS_CredentialsFormat	format;		/* Format of credentials data. */
	const void		*data;
	size_t			length;
} DDS_Credential;

typedef struct {
	DDS_Credential	private_key;		/* Private key. */
	unsigned	num_certificates;	/* # of certificates. */
	DDS_Credential	certificate [1];	/* Certificates list. */
} DDS_DataCredentials;

typedef struct {
	const char	*private_key_file;
	const char	*certificate_chain_file;
} DDS_FileCredentials;

typedef struct {
	const char	*engine_id;
	const char	*cert_id;
	const char	*priv_key_id;
} DDS_SecurityEngine;

typedef struct {
	STACK_OF(X509) *certificate_list;
	EVP_PKEY *private_key;
} DDS_SSLDataCredentials;
	
typedef enum {
	DDS_DATA_BASED,
	DDS_FILE_BASED,
	DDS_ENGINE_BASED,
	DDS_SSL_BASED
} DDS_CredentialKind;
	
typedef struct {
	DDS_CredentialKind      credentialKind;
	union {
	  DDS_DataCredentials	data;
	  DDS_FileCredentials	filenames;
	  DDS_SecurityEngine	engine;
	  DDS_SSLDataCredentials sslData;	
	}			info;
} DDS_Credentials;

typedef struct {
	const char		*id;
	void			*data;
} dataMap;

/* Set user credentials.
   This function is mandatory if access to secure domains is needed.
   It is allowed to set the credentials more than once, since they are used
   only when a new DomainParticipant is created.  Changes do not have an effect
   on existing domains.
   Note that the credentials information will not be stored locally, but is
   instead delivered to the security agent for safekeeping.  Only a reference
   is stored in DDS. */
DDS_EXPORT DDS_ReturnCode_t DDS_Security_set_credentials (
	const char      *name,
	DDS_Credentials *credentials
);

typedef enum {
	DDS_SECURITY_UNSPECIFIED,	/* Unset security policy. */
	DDS_SECURITY_LOCAL,		/* Local, i.e. via callback functions.*/
	DDS_SECURITY_AGENT		/* Remote via secure channel. */
} DDS_SecurityPolicy;

/* Security plugin function types: */
typedef enum {
	
	/* Encryption: */
	DDS_GET_PRIVATE_KEY,            /* handle -> data. */
	DDS_SIGN_WITH_PRIVATE_KEY,      /* handle, lenght, data, secure -> rdata, rlength. */
	DDS_GET_NB_CA_CERT,             /* handle -> data. */
	DDS_VERIFY_SIGNATURE,           /* handle, length, data, rdata, rlength, name, secure. */

	/* Authentication: */
	DDS_VALIDATE_LOCAL_ID,		/* data, length -> handle. */
	DDS_SET_LOCAL_HANDLE,
	DDS_VALIDATE_PEER_ID,		/* data, length -> action, rdata, rlength. */
	DDS_SET_PEER_HANDLE,
	DDS_ACCEPT_SSL_CX,		/* data, rdata -> action. */
	DDS_GET_ID_TOKEN,		/* handle -> rdata, rlength. */
	DDS_CHALLENGE_ID,		/* data, length -> rdata, rlength. */
	DDS_VALIDATE_RESPONSE,		/* data, length -> action. */
	DDS_GET_CERT,                   /* handle -> data. */
	DDS_GET_CA_CERT,                /* handle -> data. */
	
	/* Access Control: */
	DDS_VALIDATE_LOCAL_PERM,	/* handle -> handle. */
	DDS_VALIDATE_PEER_PERM,		/* data, length -> handle. */
	DDS_CHECK_CREATE_PARTICIPANT,	/* domain_id, handle, data -> secure. */
	DDS_CHECK_CREATE_TOPIC,		/* handle, name, data. */
	DDS_CHECK_CREATE_WRITER,	/* handle, name, data, rdata. */
	DDS_CHECK_CREATE_READER,	/* handle, name, data, rdata. */
	DDS_CHECK_PEER_PARTICIPANT,	/* domain_id, handle, data. */
	DDS_CHECK_PEER_TOPIC,		/* handle, name, data. */
	DDS_CHECK_PEER_WRITER,		/* handle, name, data */
	DDS_CHECK_PEER_READER,		/* handle, name, data. */
	DDS_GET_PERM_TOKEN,		/* handle -> rdata, rlength. */

	/* Domain parameters: */
	DDS_GET_DOMAIN_SEC_CAPS		/* domain_id -> secure. */
} DDS_SecurityRequest;

/* Authentication actions: */
typedef enum {
	DDS_AA_REJECTED,
	DDS_AA_CHALLENGE_NEEDED,
	DDS_AA_ACCEPTED
} DDS_AuthAction_t;

/* Plugin function parameters: */
typedef struct {
	DDS_AuthAction_t	action;
	unsigned		handle;
	size_t			length;
	size_t			rlength;
	DDS_DomainId_t		domain_id;
	void			*data;
	void			*rdata;
	const char		*name;
	unsigned		secure;
} DDS_SecurityReqData;

typedef DDS_ReturnCode_t (*DDS_SecurityPluginFct) (
	DDS_SecurityRequest code,
	DDS_SecurityReqData *data
);

/* Set the security policy.  This should be done only once and before any
   credentials are assigned or any DomainParticipants are created! */
DDS_EXPORT DDS_ReturnCode_t DDS_Security_set_policy (
	DDS_SecurityPolicy policy,
	DDS_SecurityPluginFct plugin
);

DDS_EXPORT void DDS_Security_set_library_init (int val);

/* Either have DDS do the init of the security library
   or tell DDS not to do this and do it yourself.
   val = 0 --> DDS does not init the security library */

DDS_EXPORT void DDS_Security_set_library_lock (void);

/* Let DDS do the locking of the security library */

DDS_EXPORT void DDS_Security_unset_library_lock (void);

/* Unset the locking mechanism of the security library */

DDS_EXPORT DDS_ReturnCode_t DDS_revoke_participant (DDS_DomainId_t id,
						    DDS_InstanceHandle_t part
);

#ifdef  __cplusplus
}
#endif

#endif /* __dds_security_h_ */

