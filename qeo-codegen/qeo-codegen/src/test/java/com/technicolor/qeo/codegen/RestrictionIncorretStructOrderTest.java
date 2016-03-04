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

package com.technicolor.qeo.codegen;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

/**
 * Java code generation tests.
 */
public class RestrictionIncorretStructOrderTest
{
    private QDMParser mQdmParser;
    private File mRestrictionsDir = null;

    @Before
    public void setup()
    {
        mRestrictionsDir = new File("Restrictions");
    }

    @Test(expected = ExitException.class)
    public void testValidateTheStructOrder()
        throws IOException
    {
        File qdmFile = new File(mRestrictionsDir, "EventWithSeveralStructsIncorrectOrder.xml");
        assertNotNull(mRestrictionsDir);
        assertTrue(mRestrictionsDir.isDirectory());
        assertNotNull(qdmFile);
        assertTrue(qdmFile.isFile());
        mQdmParser = new QDMParser(qdmFile);
        mQdmParser.parse();
    }

    @Test(expected = ExitException.class)
    public void testValidateTheModuleOrder()
        throws IOException
    {
        File qdmFile = new File(mRestrictionsDir, "EventWithSeveralModulesIncorrectOrder.xml");
        assertNotNull(qdmFile);
        assertTrue(qdmFile.isFile());
        mQdmParser = new QDMParser(qdmFile);
        mQdmParser.parse();
    }

    @Test(expected = ExitException.class)
    public void typedefNotAllowed()
        throws IOException
    {
        File qdmFile = new File(mRestrictionsDir, "Typedef.xml");
        assertNotNull(qdmFile);
        assertTrue(qdmFile.isFile());
        mQdmParser = new QDMParser(qdmFile);
        mQdmParser.parse();
    }

    @Test(expected = ExitException.class)
    public void testValidateTheEnumDefinitionModuleOrder()
        throws IOException
    {
        File qdmFile = new File(mRestrictionsDir, "EnumsMultipleModulesIncorrectOrder.xml");
        assertNotNull(qdmFile);
        assertTrue(qdmFile.isFile());
        mQdmParser = new QDMParser(qdmFile);
        mQdmParser.parse();
    }

}
