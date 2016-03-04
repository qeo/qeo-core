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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;

import junit.framework.TestCase;

import org.qeo.sample.gauge.android.writer.GaugeWriter;

/**
 * This class represents the unit test cases to test the correct behavior for QGaugeWriter.
 */
public class IfaceFileTest
        extends TestCase
{
    private GaugeWriter mGaugeWriter;
    private File mNonNumericData;
    private File mNumericData;
    private FileWriter mNumwriter;
    private FileWriter mNonNumwriter;

    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();
        mGaugeWriter = new GaugeWriter();
        mNonNumericData = new File("testFile");
        mNonNumwriter = new FileWriter(mNonNumericData);
        mNonNumwriter.write("Hello I am not numeric");
        mNonNumwriter.flush();

        mNumericData = new File("testNumericFile");
        mNumwriter = new FileWriter(mNumericData);
        mNumwriter.write("1234567");
        mNumwriter.flush();

    }

    /**
     * Tests if the Interface file exists on the device.
     */
    public void testIfaceFileExist()
    {
        String filename = "testname";
        boolean thrown = false;
        try {
            long data = mGaugeWriter.readDataFromIfaceFile(filename);
        }
        catch (FileNotFoundException fnf) {
            thrown = true;
        }

        assertTrue(thrown);
    }

    /**
     * Tests the contents of Interface file.Test is passed if the contents are non-numeric.
     */
    public void testContentofIfaceFileNonNumeric()
    {

        boolean thrown = false;
        try {
            long data = mGaugeWriter.readDataFromIfaceFile(mNonNumericData.getAbsolutePath());
        }
        catch (NumberFormatException nf) {
            thrown = true;
        }
        catch (FileNotFoundException e) {
            System.out.print(e.getMessage());
        }
        assertTrue(thrown);
    }

    /**
     * Tests the contents of Interface file.Test is passed if the contents are numeric.
     */
    public void testContentofIfaceFileNumeric()
    {

        boolean thrown = false;
        try {
            long data = mGaugeWriter.readDataFromIfaceFile(mNumericData.getAbsolutePath());
            if (String.valueOf(data) != null) {
                thrown = true;
            }
        }
        catch (NumberFormatException nf) {
            System.out.print(nf.getMessage());
        }
        catch (FileNotFoundException e) {
            System.out.print(e.getMessage());
        }
        assertTrue(thrown);
    }

    @Override
    protected void tearDown()
        throws Exception
    {
        super.tearDown();
        mNonNumericData.delete();
        mNumericData.delete();
        mNonNumwriter.close();
        mNumwriter.close();
    }

}
