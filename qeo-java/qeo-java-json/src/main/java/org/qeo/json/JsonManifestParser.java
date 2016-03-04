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

package org.qeo.json;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * <p>
 * Class to parse json manifest into regular manifest format.
 * </p>
 * <p>
 * Example JSON manifest syntax:
 * </p>
 * 
 * <pre>
 * {
 *     "meta": {
 *         "appname": "MyApp",
 *         "version": "1"
 *     },
 *     "application": {
 *         "com::example::my::Topic": "rw"
 *     }
 * }
 * </pre>
 */
public final class JsonManifestParser
{
    private JsonManifestParser()
    {
    }

    /**
     * Convert a manifest in JSON format into regular manifest format.
     * 
     * @param file The file containing the manifest
     * @return an array of lines containing the converted lines.
     * @throws JSONException If the file does not have a valid JSON syntax
     * @throws IOException If the file cannot be read.
     */
    public static String[] getManifest(File file)
        throws JSONException, IOException
    {
        return getManifest(new FileInputStream(file));
    }

    /**
     * Convert a manifest in JSON format into regular manifest format.
     * 
     * @param json the manifest in JSON format.
     * @return an array of lines containing the converted lines.
     * @throws JSONException If the file does not have a valid JSON syntax
     * @throws IOException If the file cannot be read.
     */
    public static String[] getManifest(JSONObject json)
        throws JSONException, IOException
    {
        Iterator<?> keys = json.keys();
        List<String> manifest = new ArrayList<String>();
        while (keys.hasNext()) {
            String key = (String) keys.next();
            manifest.add("[" + key + "]");
            JSONObject values = json.getJSONObject(key);
            Iterator<?> keys2 = values.keys();
            while (keys2.hasNext()) {
                String key2 = (String) keys2.next();
                manifest.add(key2 + " = " + values.getString(key2));
            }
        }
        return manifest.toArray(new String[manifest.size()]);
    }

    /**
     * Convert a manifest in JSON format into regular manifest format.
     * 
     * @param stream The stream to read the JSON data from. The stream will be fully read an closed.
     * @return an array of lines containing the converted lines.
     * @throws JSONException If the file does not have a valid JSON syntax
     * @throws IOException If the file cannot be read.
     */
    public static String[] getManifest(InputStream stream)
        throws JSONException, IOException
    {
        BufferedReader manifestReader = null;
        StringBuilder builder = new StringBuilder();
        try {
            manifestReader = new BufferedReader(new InputStreamReader(stream, "US-ASCII"));
            String line = null;
            while ((line = manifestReader.readLine()) != null) {
                builder.append(line + "\n");
            }
        }
        finally {
            if (manifestReader != null) {
                manifestReader.close();
            }
        }
        return getManifest(new JSONObject(builder.toString()));
    }
}
