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
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * This class contains different methods to handle the command-line arguments to launch the code generator.
 */
public final class Main
{
    private static final String OPT_HELP_SHORT = "h";
    private static final String OPT_HELP_LONG = "help";
    private static final String OPT_LANGUAGE_SHORT = "l";
    private static final String OPT_LANGUAGE_LONG = "language";
    private static final String OPT_OUTPUT_SHORT = "o";
    private static final String OPT_OUTPUT_LONG = "output";

    private static final Options OPTIONS = new Options()
        .addOption(OPT_HELP_SHORT, OPT_HELP_LONG, false, "Display this help.")
        .addOption(OPT_LANGUAGE_SHORT, OPT_LANGUAGE_LONG, true,
                    "Specify the language for which to generate code. Available languages: java, c, js, objectiveC ")
        .addOption(OPT_OUTPUT_SHORT, OPT_OUTPUT_LONG, true,
            "Specify the output directory for the generated code.  Default is the current directory.");

    private Engine mEngine;

    private Main()
    {
        // make constructor private
    }

    /**
     * Print a progress message to the console.
     * 
     * @param msg The message to print
     */
    public static void printMessage(String msg)
    {
        System.out.println(msg);
    }

    private void usageExit(String message)

    {
        HelpFormatter hf = new HelpFormatter();
        PrintWriter pw = null;

        if (null == message) {
            pw = new PrintWriter(System.out, true);
        }
        else {
            pw = new PrintWriter(System.err, true);
            pw.println("ERROR : " + message);
            pw.println();
        }
        pw.println("Usage: qeo-codegen [options ...] [file ...]");
        pw.println();
        pw.println("Options:");
        hf.printOptions(pw, 80, OPTIONS, 4, 2);
        System.exit(1);
    }

    private TargetLanguage getTargetLanguage(String langStr)
    {
        TargetLanguage lang = TargetLanguage.JAVA;

        try {
            lang = TargetLanguage.valueOf(langStr.toUpperCase(Locale.getDefault()));
        }
        catch (IllegalArgumentException iae) {
            usageExit("Invalid language: " + langStr);
        }
        return lang;
    }

    private File getOutputDirectory(String dirStr)
    {
        File dir = new File(dirStr);

        if (dir.exists()) {
            if (!dir.isDirectory()) {
                usageExit("Output argument is not a directory: " + dirStr);
            }
            else if (!dir.canWrite()) {
                usageExit("Output directory is not writable: " + dirStr);
            }
        }
        else {
            if (!dir.mkdirs()) {
                usageExit("Unable to create output directory: " + dirStr);
            }
        }
        return dir;
    }

    /**
     * Parses and validates the command-line arguments and configures the engine accordingly.
     * 
     * @param mEngine The engine to be configured.
     * @param args The command-line arguments.
     * 
     * @return An array list of QDM files for which to generate code.
     */
    private List<File> parseArguments(String[] args)
    {
        CommandLineParser p = new GnuParser();
        List<File> files = new ArrayList<File>();

        // list of options that can be set
        TargetLanguage language = null;
        File outputDirectory = null;

        try {
            CommandLine cl = p.parse(OPTIONS, args);
            String[] fileNames = cl.getArgs();

            if (cl.hasOption(OPT_HELP_SHORT)) {
                usageExit(null);
            }
            if (fileNames.length < 1) {
                usageExit("At least one input file is needed");
            }
            // language selection
            if (cl.hasOption(OPT_LANGUAGE_SHORT)) {
                language = getTargetLanguage(cl.getOptionValue(OPT_LANGUAGE_SHORT));
            }
            // output directory selection
            if (cl.hasOption(OPT_OUTPUT_SHORT)) {
                outputDirectory = getOutputDirectory(cl.getOptionValue(OPT_OUTPUT_SHORT));
            }
            // input file validation
            for (String fileName : fileNames) {
                File f = new File(fileName);

                if (!f.exists() || !f.isFile()) {
                    usageExit("Invalid input file: " + fileName);
                }
                files.add(f);
            }
        }
        catch (ParseException ex) {
            usageExit(ex.getMessage());
        }

        // create engine
        if (language == null) {
            usageExit("Please specify a language");
        }
        else {
            mEngine = Engine.getInstance(language);
            if (outputDirectory != null) {
                mEngine.setOutputDirectory(outputDirectory);
            }
        }

        return files;
    }

    private void start(String[] args)
    {

        List<File> inputFiles = parseArguments(args);

        for (File f : inputFiles) {
            Main.printMessage("Processing " + f.getAbsolutePath());
            try {
                mEngine.processFile(f);
            }
            catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
        Main.printMessage("Done");
    }

    /**
     * Main program. This will parse the command-line arguments and trigger code generation for each input file.
     * 
     * @param args Command-line arguments.
     */
    public static void main(String[] args)
    {
        try {
            Main main = new Main();
            main.start(args);
        }
        catch (ExitException e) {
            // Check if somewhere in the code an abort happened. If so, print the message and set exit code to 1
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }
}
