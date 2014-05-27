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

#ifndef __secure_h_
#define	__secure_h_

#include "netinet/in.h"
#include "openssl/ssl.h"
#include "dds_data.h"
#include "dds/dds_security.h"

/* 1. Authentication.
   ------------------ */

typedef struct token_st {
	size_t		size;
	unsigned char	data [1];
} Token_t;

typedef void *Identity_t;

extern int                      dds_openssl_init_global;
extern IdentityHandle_t		local_identity;

IdentityHandle_t validate_local_identity (const char      *name,
					  DDS_Credentials *credentials,
					  size_t          xlength);

/* Validate the credentials of a local DomainParticipant.  If successful, a
   non-zero identity handle is returned. */

DDS_AuthAction_t validate_peer_identity (unsigned char *identity,
				         size_t        identity_length,
				         unsigned char *challenge,
				         size_t        *challenge_length);

/* Validate the identity token of a peer DomainParticipant.  The resulting
   action type is returned.  If the action is AA_CHALLENGE_NEEDED, the challenge
   argument will be populated and *challenge_length is set.
   The *challenge_length argument needs to be set to the buffer size before
   calling this function. */

DDS_AuthAction_t check_DTLS_handshake_initiator (struct sockaddr *addr,
					    unsigned        domain_id);

/* When a Client Hello message is received, it can be useful to check
   if the peer is valid or invalid. When the peer is invalid,
   the rest of the handshake should not be performed */

DDS_AuthAction_t accept_ssl_connection (SSL             *ssl, 
					struct sockaddr *addr, 
					unsigned        domain_id);

/* Validate a peer over an SSL connection. */

DDS_ReturnCode_t get_identity_token (IdentityHandle_t handle,
				     unsigned char    *identity,
				     size_t           *identity_length);

/* Return an identity token from an Identity handle. */

DDS_ReturnCode_t challenge_identity (unsigned char *challenge,
				     size_t        challenge_length,
				     unsigned char *response,
				     size_t        *response_length);

/* Handle an identity challenge from a peer participant. */

DDS_AuthAction_t validate_response (unsigned char *response,
				    size_t        response_length);

/* Handle the response of an Identity challenge from a peer participant. */


/* 2. Access Control.
   ------------------ */

PermissionsHandle_t validate_local_permissions (DDS_DomainId_t   domain_id,
						IdentityHandle_t handle);

/* Validate the permission credentials of a local DomainParticipant.  If
   successful, a non-zero permissions handle is returned. */

PermissionsHandle_t validate_peer_permissions (DDS_DomainId_t domain_id,
					       unsigned char  *permissions,
					       size_t         length);

/* Validate the permissions token of a peer DomainParticipant.  The resulting
   permissions handle is returned, or 0, if the particant is rejected. */

DDS_ReturnCode_t check_create_participant (PermissionsHandle_t            perm,
					   const DDS_DomainParticipantQos *qos,
					   unsigned                       *secure);

/* Check if a local DomainParticipant can be created for the domain.
   If successful, *secure will be set if the domain needs to be secure. */

DDS_ReturnCode_t check_create_topic (PermissionsHandle_t permissions,
				     const char          *topic_name,
				     const DDS_TopicQos  *qos);

/* Check if a topic may be created. */

DDS_ReturnCode_t check_create_writer (PermissionsHandle_t     permissions,
				      Topic_t                 *topic,
				      const DDS_DataWriterQos *qos,
				      const Strings_t         *partitions);

/* Check if a datawriter may be created for the given topic. */

DDS_ReturnCode_t check_create_reader (PermissionsHandle_t     permissions,
				      Topic_t                 *topic,
				      const DDS_DataReaderQos *qos,
				      const Strings_t         *partitions);

/* Check if a datareader may be created for the given topic. */


DDS_ReturnCode_t check_peer_participant (PermissionsHandle_t      perm,
					 String_t                 *user_data);

/* Check if a peer DomainParticipant may be created for the domain. */

DDS_ReturnCode_t check_peer_topic (PermissionsHandle_t      permissions,
				   const char               *topic_name,
				   const DiscoveredTopicQos *qos);

/* Check if a topic may be created. */

DDS_ReturnCode_t check_peer_writer (PermissionsHandle_t       permissions,
				    const char                *topic_name,
				    const DiscoveredWriterQos *qos);

/* Check if a datawriter may be created for the given topic. */

DDS_ReturnCode_t check_peer_reader (PermissionsHandle_t       permissions,
				    const char                *topic_name,
				    const DiscoveredReaderQos *qos);

/* Check if a datareader may be created for the given topic. */

DDS_ReturnCode_t get_permissions_token (PermissionsHandle_t handle,
				        unsigned char       *permissions,
					size_t              *perm_length);

/* Return an Permissions token from a Permissions handle. */

uint32_t get_domain_security (DDS_DomainId_t domain);

/* Return the domain security parameters. */

DDS_ReturnCode_t get_certificate (void *certificate, IdentityHandle_t id_handle);

/* Return a certificate with the specified ID. */

DDS_ReturnCode_t get_CA_certificate_list (void *CAcertificates, IdentityHandle_t id_handle);

/* Return A CA certificate from the certificate with the specified ID. */

DDS_ReturnCode_t sign_with_private_key(int type, const unsigned char *m, unsigned int m_len,
				       unsigned char *sigret, unsigned int *siglen, 
				       IdentityHandle_t id_handle);

/* Sign a piece of data with the private key with the specified ID. */

DDS_ReturnCode_t verify_signature(int type, const unsigned char *m, unsigned int m_len, 
				  unsigned char *sigbuf, unsigned int siglen, 
				  IdentityHandle_t peer_id_handle, void *security_context);

/* Verify the signature of the peer. */

DDS_ReturnCode_t get_private_key(void *privateKey, IdentityHandle_t id_handle);

/* Return the private key. */

DDS_ReturnCode_t get_nb_of_CA_certificates(int *nb, IdentityHandle_t id_handle);

/* Return the number of CA certificates. */

#endif /* !__secure_h_ */
