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

/** \file
 * Qeo factory API
 */

#ifndef FACTORY_H_
#define FACTORY_H_

#include <stdbool.h>
#include <qeo/types.h>

#ifdef __cplusplus
extern "C"
{
#endif

/* ===[ Qeo factory ]======================================================== */

/// \name Qeo Factory
/// \{

/**
 * Returns a Qeo factory instance for the default realm that can be used for
 * creating Qeo readers and writers.  The factory instance should be properly
 * closed if none of the readers and/or writers that have been created, are
 * needed anymore.  This will free any allocated resources associated with that
 * factory.
 *
 * \return The factory or \c NULL on failure.
 *
 * \see ::qeo_factory_close
 */
qeo_factory_t *qeo_factory_create();

/**
 * Returns a Qeo factory instance that can be used for creating Qeo readers and
 * writers.  The factory instance should be properly closed if none of the
 * readers and/or writers that have been created, are needed anymore.  This will
 * free any allocated resources associated with that factory.
 *
 * \param[in] id  The identity for which you want to create the factory.
 *                Use QEO_IDENTITY_DEFAULT for the default identity.
 *
 * \return The factory or \c NULL on failure.
 *
 * \see ::qeo_factory_close
 */
qeo_factory_t *qeo_factory_create_by_id(const qeo_identity_t *id);

/**
 * Close the factory and release any resources associated with it.
 *
 * \warning Make sure that any readers and/or writers created with this factory
 *          have been closed before calling this function.
 *
 * \param[in] factory  The factory to be closed.
 */
void qeo_factory_close(qeo_factory_t *factory);

/// \}

/* ===[ Background Notification Service ]==================================== */

/// \name Background Notification Service
/// \{

/**
 * Called when data arrives on one of the readers that has been configured to
 * be monitored for this.
 *
 * \param[in] userdata opaque user data as provided during registration in
 *                     ::qeo_bgns_register
 * \param[in] type_name the name of the type for which data arrived
 *
 * \warning Make sure to resume Qeo in this callback if needed.
 *
 * \see ::qeo_state_reader_bgns_notify
 * \see ::qeo_state_change_reader_bgns_notify
 * \see ::qeo_bgns_register
 * \see ::qeo_bgns_resume
 */
typedef void (*qeo_bgns_on_wakeup)(uintptr_t userdata, const char *type_name);

/**
 * Called when connecting to or disconnecting from the Background Notification
 * Service.
 *
 * \param[in] userdata  opaque user data as provided during registration
 * \param[in] fd        the file descriptor of the socket that is used
 * \param[in] connected \c true when connecting, \c false when disconnecting
 *
 * \see ::qeo_bgns_register
 */
typedef void (*qeo_bgns_on_connect)(uintptr_t userdata, int fd, bool connected);

/**
 * Background notification service listener callback structure.
 */
typedef struct {
    qeo_bgns_on_wakeup on_wakeup;      /**< \see ::qeo_bgns_on_wakeup */
    qeo_bgns_on_connect on_connect;    /**< \see ::qeo_bgns_on_connect */
} qeo_bgns_listener_t;

/**
 * Register or unregister Background Notification Service callbacks.
 *
 * \param[in] listener structure with callbacks to be used, may be \c NULL in
                       which case all listeners will be unregistered
 * \param[in] userdata opaque user data passed to the callbacks
 *
 * \warning When a wake-up callback has been registered auto-resuming of Qeo
 *          will not happend.  You need to call ::qeo_bgns_resume in the
 *          callback otherwise Qeo will remain suspended.
 */
void qeo_bgns_register(const qeo_bgns_listener_t *listener,
                       uintptr_t userdata);

/**
 * Suspend Qeo operations.  All non-vital network connections are closed and
 * timers are stopped.
 */
void qeo_bgns_suspend(void);

/**
 * Resume Qeo operations.  Re-establishes all connections as if nothing
 * happened.
 */
void qeo_bgns_resume(void);

/// \}

/* ===[ Miscellaneous ]====================================================== */

/// \name Miscellaneous
/// \{

/**
 * Returns a string representation of the Qeo library version.
 *
 * \return The Qeo library version string.
 */
const char *qeo_version_string(void);

/**
 * Set whether Qeo should initialize OpenSSL or not.
 * By default Qeo will initialize OpenSSL.
 *
 * This must be called before any other Qeo function.
 *
 * WARNING: Qeo uses multi-threaded calls to OpenSSL. It's important to provide
 * all required callbacks for OpenSSL to support this. Info on:
 * https://www.openssl.org/docs/crypto/threads.html
 *
 * \param[in] value 1 to let Qeo do the init, 0 to skip this.
 */
void qeo_security_set_init_openssl(int value);

/// \}

#ifdef __cplusplus
}
#endif

#endif /* FACTORY_H_ */
