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

package org.qeo.android.webview.internal;

/**
 * Interface that defines all javascript callbacks defined in qeo.js.
 */
public interface QeoJsCallbacks
{
    /**
     * List of event values that are allowed in the notify callback.
     */
    public static enum Event {
        /**
         * <p>
         * create a factory/reader/writer/iterator. Options format:
         * </p>
         * 
         * <pre>
         * {
         *   id: deferId,
         *   objtype: one or the ObjType enum values
         * }
         * </pre>
         */
        CREATE,

        /**
         * <p>
         * Write data to a Qeo writer. Options format:
         * </p>
         * 
         * <pre>
         * {
         *   factoryid: id of the factory,
         *   id: writerId,
         *   data: data in json format
         * }
         * </pre>
         */
        WRITE,

        /**
         * <p>
         * Remove data from a state writer. Options format:
         * </p>
         * 
         * <pre>
         * {
         *   factoryid: id of the factory,
         *   id: writerId,
         *   data: data in json format
         * }
         * </pre>
         */
        REMOVE,

        /**
         * <p>
         * Close a reader/writer/factory.
         * </p>
         * 
         * <pre>
         * {
         *   factoryid: id of the factory,
         *   id: reader/writer/factory id,
         *   objtype: one or the ObjType enum values
         * }
         * </pre>
         */
        CLOSE,

        /**
         * <p>
         * Request a policy update.
         * </p>
         * 
         * <pre>
         * {
         *   factoryid: id of the factory,
         *   id: reader/writer id
         * }
         * </pre>
         */
        REQUESTPOLICY,

        /**
         * <p>
         * Apply new policy settings.
         * </p>
         * 
         * <pre>
         * {
         *   factoryid: id of the factory,
         *   id: reader/writer id,
         *   data: json object with new policy. Format:
         *     {"users":[{"id":123, "allow":true},{"id":456, "allow":false},..]}
         * }
         * </pre>
         */
        POLICYUPDATE,

        /**
         * <p>
         * Get an object (currently only deviceId supported).
         * </p>
         * 
         * <pre>
         * {
         *   objtype: currently only "deviceid" supported
         * }
         * </pre>
         */
        GET;
    }

    /**
     * objtype options in json events.
     */
    public static enum ObjType {
        /** Factory. */
        FACTORY,
        /** Event reader. */
        EVENTREADER,
        /** Event writer. */
        EVENTWRITER,
        /** State writer. */
        STATEWRITER,
        /** State reader. */
        STATEREADER,
        /** State change reader. */
        STATECHANGEREADER,
        /** Iterator for a state reader. */
        ITERATOR;
    }

    /**
     * Function to handle javascript events. It should never be called directly!
     * 
     * @param event event name.
     * @param options options object in json format.
     */
    void notify(String event, String options);

}
