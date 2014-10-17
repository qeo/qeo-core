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

package com.technicolor.qeo.codegen;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;

import com.technicolor.qeo.codegen.type.qdm.QdmModule;
import com.technicolor.qeo.codegen.type.qdm.QDM;
import com.technicolor.qeo.codegen.type.qdm.QdmEnum;
import com.technicolor.qeo.codegen.type.qdm.QdmEnumerator;

/**
 * This class is the engine of the code generator. It is in charged of setup Velocity and process the input file.
 */
public abstract class Engine
{
    /**
     * The output directory where everything should be written.
     */
    protected File mOutputDir;

    /**
     * The velocity engine.
     */
    protected final VelocityEngine mEngine;

    private final Map<String, String> mTypes;

    /**
     * Initialize Velocity, set up properties and defaults.
     */
    protected Engine()
    {
        mEngine = new VelocityEngine();
        // configure velocity to load templates from classpath
        mEngine.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
        mEngine.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
        mEngine.init();

        // set up defaults
        mOutputDir = new File(System.getProperty("user.dir"));
        mTypes = new HashMap<String, String>();
    }

    /**
     * Get an instance of the engine for a specific target language.
     * 
     * @param targetLanguage The target language.
     * @return An engine instance.
     */
    public static Engine getInstance(TargetLanguage targetLanguage)
    {
        switch (targetLanguage) {
            case JAVA:
                return new JavaEngine();
            case C:
                return new CEngine();
            case JS:
                return new JSONEngine();
            case OBJECTIVEC:
                return new ObjectiveCEngine();
            default:
                throw new IllegalArgumentException("invalid language: " + targetLanguage);
        }
    }

    /**
     * Translate qdm type into native type for this engine.
     * 
     * @param type The qdm type
     * @return The native type
     */
    protected String getType(String type)
    {
        if (mTypes.containsKey(type)) {
            return mTypes.get(type);
        }
        else {
            throw new IllegalArgumentException("member with type \"" + type + "\" is not supported");
        }

    }

    /**
     * Register a type mapping between qdm and native.
     * 
     * @param qdmType The qdm type.
     * @param nativeType The native type.
     */
    protected void registerType(String qdmType, String nativeType)
    {
        mTypes.put(qdmType, nativeType);
    }

    /**
     * Set the output directory where the generated files will be allocated.
     * 
     * @param dir file
     */
    void setOutputDirectory(File dir)
    {
        mOutputDir = dir;
    }

    /**
     * Get the output directory where the generated files will be allocated.
     * 
     * @return File path
     */
    File getOutputDirectory()
    {
        return mOutputDir;
    }

    /**
     * Process the qdm for the specific language.
     * 
     * @param qdm The QDM object. The values will be altered.
     */
    protected abstract void processQdm(QDM qdm);

    /**
     * Generate code and write to disk.
     * 
     * @param qdm the processed qdm to write.
     * @throws IOException If the files cannot be written.
     */
    protected abstract void generateCode(QDM qdm)
        throws IOException;

    /**
     * Process the input file, assign the correct template depending of the language and generate the output file(s).
     * 
     * @param inputFile qdm you want to use to generate the code
     * @throws IOException when creation of output file(s) fails
     */
    void processFile(File inputFile)
        throws IOException
    {
        QDM qdm = new QDMParser(inputFile).parse();
        processQdm(qdm);
        generateCode(qdm);
    }

    /**
     * Generate code and write to disk.
     * 
     * @param qdm the processed qdm to write.
     */
    protected void addEnumsToTypes(QDM qdm)
    {
        for (QdmModule module : qdm.getModules()) {
            for (QdmEnum qdmEnum : module.getEnums()) {
                if (qdmEnum.getClass().equals(QdmEnumerator.class)) {
                    String value = Convert.qdmToC(module.getName()) + "_" + qdmEnum.getName() + "_t";
                    registerType(qdmEnum.getName(), value);
                }
            }
        }
    }
}
