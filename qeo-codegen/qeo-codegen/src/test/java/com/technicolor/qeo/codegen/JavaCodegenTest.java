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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * Java code generation tests.
 */
@RunWith(Parameterized.class)
// this is a parameterized test, meaning that tests get auto-generated
public class JavaCodegenTest
{
    private Engine mEngineJava;
    private Engine mEngineC;
    private Engine mEngineJS;
    private Engine mEngineObjectiveC;

    private final File mTestDir;

    public JavaCodegenTest(File file)
    {
        // the arguments of the constructor are generated the the data() function
        mTestDir = file;
    }

    @Parameters
    public static Collection<Object[]> data()
    {
        // For every iten in the collection, a test will be executed.
        // The Object[] array will be passed as different parameters to the constructor of this test

        // run a test for every folder in the "test" directory
        File testsDir = new File("test");
        assertNotNull(testsDir);
        assertTrue(testsDir.isDirectory());

        Collection<Object[]> data = new ArrayList<Object[]>();
        for (File testDir : testsDir.listFiles()) {
            if (testDir.isDirectory()) {
                Object[] fileArg1 = new Object[] {testDir};
                data.add(fileArg1);
            }
        }
        return data;
    }

    @Rule
    public final TemporaryFolder mTmpFolder = new TemporaryFolder();

    @Before
    public void initialize()
        throws Exception
    {
        mEngineJava = Engine.getInstance(TargetLanguage.JAVA);
        mEngineJava.setOutputDirectory(mTmpFolder.newFolder(this.getClass().getSimpleName() + ".output.java"));
        mEngineC = Engine.getInstance(TargetLanguage.C);
        mEngineC.setOutputDirectory(mTmpFolder.newFolder(this.getClass().getSimpleName() + ".output.c"));
        mEngineJS = Engine.getInstance(TargetLanguage.JS);
        mEngineJS.setOutputDirectory(mTmpFolder.newFolder(this.getClass().getSimpleName() + ".output.js"));
        mEngineObjectiveC = Engine.getInstance(TargetLanguage.OBJECTIVEC);
        mEngineObjectiveC.setOutputDirectory(mTmpFolder.newFolder(this.getClass().getSimpleName()
                + ".output.objectivec"));

    }

    /**
     * Compare two files line-by-line.
     */
    private void compareFiles(File expFile, File genFile)
        throws IOException
    {
        BufferedReader expReader = null;
        BufferedReader genReader = null;
        String line;
        int i = 0;

        try {
            expReader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(expFile), Charset.forName("UTF-8")));
            genReader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(genFile), Charset.forName("UTF-8")));
            while (null != (line = expReader.readLine())) {
                i++;
                assertEquals("line mismatch line in " + expFile.getPath() + " at line " + i + ":", line,
                        genReader.readLine());
            }

            /* expect EOF in generated file */
            assertNull("more lines in generated file than expected", genReader.readLine());
        }
        finally {
            if (null != expReader) {
                try {
                    expReader.close();
                }
                catch (IOException e) {
                    // ignore
                }
            }
            if (null != genReader) {
                genReader.close();
            }
        }
    }

    /**
     * Compare two directories recursively using compareFiles.
     */
    private void compareDirs(File expDir, File genDir)
        throws IOException
    {
        for (File expEntry : expDir.listFiles()) {
            File genEntry = new File(genDir, expEntry.getName());

            assertTrue("missing generated file: " + genEntry.getName(), genEntry.exists());
            if (expEntry.isDirectory()) {
                assertTrue(genEntry.isDirectory());
                compareDirs(expEntry, genEntry);

            }
            else {
                assertTrue(genEntry.isFile());
                compareFiles(expEntry, genEntry);

            }
        }
        for (File genEntry : genDir.listFiles()) {
            File expEntry = new File(expDir, genEntry.getName());
            assertTrue("missing file: " + expEntry.getPath(), expEntry.exists());
            // no need to go recursive here, it will be covered by the previous loop
        }
    }

    @Test
    public void testCompareWithExpectedJava()
        throws IOException
    {
        File qdmDir = new File(mTestDir, "qdm");
        assertNotNull(qdmDir);
        assertTrue(qdmDir.isDirectory());
        for (File qdmFile : qdmDir.listFiles()) {
            /* generate code */
            if (qdmFile.getName().endsWith(".xml")) {
                System.out.println("[JAVA] Processing " + qdmFile.getAbsolutePath());
                mEngineJava.processFile(qdmFile);
            }
        }
        /* compare with expected output */
        File javaDir = new File(mTestDir, "java");
        assertTrue("java directory not found: " + javaDir, javaDir.isDirectory());
        compareDirs(javaDir, mEngineJava.getOutputDirectory());
    }

    @Test
    public void testCompareWithExpectedC()
        throws IOException
    {
        File qdmDir = new File(mTestDir, "qdm");
        assertNotNull(qdmDir);
        assertTrue(qdmDir.isDirectory());
        for (File qdmFile : qdmDir.listFiles()) {
            /* generate code */
            if (qdmFile.getName().endsWith(".xml")) {
                System.out.println("[C] Processing " + qdmFile.getAbsolutePath());
                mEngineC.processFile(qdmFile);
            }
        }
        /* compare with expected output */
        File cDir = new File(mTestDir, "c");
        if (!cDir.isDirectory()) {
            System.out.println("C not yet available");
            return;
        }
        assertTrue("c directory not found: " + cDir, cDir.isDirectory());
        compareDirs(cDir, mEngineC.getOutputDirectory());
    }

    @Test
    public void testCompareWithExpectedJS()
        throws IOException
    {
        File qdmDir = new File(mTestDir, "qdm");
        assertNotNull(qdmDir);
        assertTrue(qdmDir.isDirectory());
        for (File qdmFile : qdmDir.listFiles()) {
            /* generate code */
            if (qdmFile.getName().endsWith(".xml")) {
                System.out.println("[JS] Processing " + qdmFile.getAbsolutePath());
                mEngineJS.processFile(qdmFile);
            }
        }
        /* compare with expected output */
        File jsDir = new File(mTestDir, "js");
        if (!jsDir.isDirectory()) {
            System.out.println("JS not yet available");
            return;
        }
        assertTrue("js directory not found: " + jsDir, jsDir.isDirectory());
        compareDirs(jsDir, mEngineJS.getOutputDirectory());
    }

    @Test
    public void testCompareWithExpectedObjectiveC()
        throws IOException
    {
        File qdmDir = new File(mTestDir, "qdm");
        assertNotNull(qdmDir);
        assertTrue(qdmDir.isDirectory());
        for (File qdmFile : qdmDir.listFiles()) {
            /* generate code */
            if (qdmFile.getName().endsWith(".xml")) {
                System.out.println("[Objective C] Processing " + qdmFile.getAbsolutePath());
                mEngineObjectiveC.processFile(qdmFile);
            }
        }
        /* compare with expected output */
        File objectivecDir = new File(mTestDir, "objectivec");
        if (!objectivecDir.isDirectory()) {
            System.out.println("Objective C not yet available");
            return;
        }
        assertTrue("Objective c directory not found: " + objectivecDir, objectivecDir.isDirectory());
        compareDirs(objectivecDir, mEngineObjectiveC.getOutputDirectory());
    }

}
