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

#ifndef FWD_FORWARDER_H_
#define FWD_FORWARDER_H_

/**
 * Set a fixed public IP address and port to be used by the forwarder.  This
 * also disables the mechanism that tries to dynamically discover the address.
 */
void forwarder_config_public_locator(const char *ip,
                                     int port);

/**
 * Set a fixed local TCP port to be used by the forwarder.
 */
void forwarder_config_local_port(const char *port);

/**
 * Disable the notification service.
 */
void forwarder_disable_notification_service(void);

/**
 * Disable the forwarding service on this forwarder (...don't ask...).
 */
void forwarder_disable_forwarding_service(void);

/**
 * Retry when the factory creation of the forwarder failed.
 * This can happen when a remote registration window is finished,
 * but the forwarder is not registered in that time frame.
 * Wait for "delay" seconds before trying again.
 */
void forwarder_retry_on_failed_remote_registration(int delay);

/**
 * Set the time out period for discovering a local forwarder.
 */
void forwarder_config_local_discovery(unsigned int discover_timeout);

/**
 * When calling this function, there will be no portmap added.
 * Either a portmap is not needed (forwarder on gateway listening on public ip)
 * or the user sets his own portmap. The chosen public port is written to path
 */
void forwarder_disable_portmap(char *path);

/**
 * Set the initial UPnP-IGD discovery timeout (in ms) as well as the period
 * (in sec) to be used for requerying the public IP address and the lease
 * period for the port map (in sec).
 */
void forwarder_config_upnp(unsigned int discover_timeout,
                           unsigned int poll_period,
                           unsigned int portmap_lease_period);

/**
 * Start forwarder.  This function will not return unless ::stop_forwarder
 * gets called or an error occurs.
 */
int forwarder_start(void);

/**
 * Stop a previously started forwarder.
 */
int forwarder_stop(void);

#endif /* FWD_FORWARDER_H_ */
