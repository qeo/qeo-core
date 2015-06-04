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

package com.technicolor.qeo.codegen;

import java.io.File;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.technicolor.qeo.codegen.type.qdm.QdmModule;
import com.technicolor.qeo.codegen.type.qdm.QDM;
import com.technicolor.qeo.codegen.type.qdm.QdmEnum;
import com.technicolor.qeo.codegen.type.qdm.QdmEnumerator;
import com.technicolor.qeo.codegen.type.qdm.QdmMember;
import com.technicolor.qeo.codegen.type.qdm.QdmStruct;

/**
 * This class extends DefautHandeler and overrides the necessary methods to parse your qdm.
 */
public class QDMParser
    extends DefaultHandler
{
    private static SAXParser sParser = null;

    private StringBuilder mDoc;
    private final QDM mQdm;
    private final File mFile;
    private QdmModule mModule;
    private QdmMember mCurrentMember;
    private QdmStruct mCurrentStruct;
    private QdmEnum mCurrentEnum;
    private QdmEnumerator mCurrentEnumerator;

    // private TypeDef mTypeDef;

    private static synchronized SAXParser getParser()
        throws ParserConfigurationException, SAXException
    {
        if (null == sParser) {
            sParser = SAXParserFactory.newInstance().newSAXParser();
        }
        return sParser;
    }

    /**
     * QDMParser constructor.
     * 
     * @param file The qdm file.
     */
    public QDMParser(File file)
    {
        mFile = file;
        mQdm = new QDM(file.getName().replace(".xml", ""));
        mModule = null;
        mCurrentStruct = null;
        mDoc = null;
        mCurrentEnum = null;
        mCurrentEnumerator = null;
    }

    /**
     * When the parser encounters plain text (not XML elements), it calls(this method, which accumulates them in a
     * string buffer.
     * 
     * @param buffer the characters from the XML document
     * @param start the start position in the array
     * @param length the number of characters to read from the array
     */
    @Override
    public void characters(char[] buffer, int start, int length)
    {
        if (mDoc != null) {
            String[] words = new String(buffer, start, length).split("[ \r\n]+");
            StringBuilder sb = new StringBuilder();
            boolean first = true;
            for (String word : words) {
                if (word.length() > 0) {
                    if (first) {
                        first = false;
                    }
                    else {
                        sb.append(' ');
                    }
                    sb.append(word);
                }
            }
            if (sb.length() > 0) {
                if (mDoc.length() > 0) {
                    mDoc.append(' ');
                }
                mDoc.append(sb.toString());
            }
        }
    }

    /**
     * Every time the parser encounters the beginning of a new element, it calls this method, which resets the string
     * buffer.
     * 
     * @param uri the Namespace URI, or the empty string if the element has no Namespace URI or if Namespace processing
     *            is not being performed
     * @param localName the local name (without prefix), or the empty string if Namespace processing is not being
     *            performed
     * @param qName the qualified name (with prefix), or the empty string if qualified names are not available
     * @param attributes the attributes attached to the element. If there are no attributes, it shall be an empty
     *            Attributes object. The value of this object after startElement returns is undefined
     * @throws SAXException If the parsing fails
     * 
     */
    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes)
        throws SAXException
    {
        mDoc = null;
        if (qName.equalsIgnoreCase("module")) {
            if (mModule != null) {
                throw new IllegalArgumentException("A <module> tag inside a <module> tag is not supported");
            }
            mModule = new QdmModule(attributes.getValue("name"));
        }
        else if (qName.equalsIgnoreCase("enum")) {
            mCurrentEnum = new QdmEnum(attributes.getValue("name"));
        }
        else if (qName.equalsIgnoreCase("enumerator")) {
            mCurrentEnumerator = new QdmEnumerator(attributes.getValue("name"));
        }
        else if (qName.equalsIgnoreCase("struct")) {
            String name = attributes.getValue("name");
            if (Character.isLowerCase(name.charAt(0))) {
                throw new ExitException("The struct name must start with a capital letter. Invalid: \"" + name + "\"");
            }
            mCurrentStruct = new QdmStruct(name);
            mCurrentStruct.setBehavior(attributes.getValue("behavior"));
        }
        else if (qName.equalsIgnoreCase("member")) {
            mCurrentMember = new QdmMember(attributes.getValue("name"));
            mCurrentMember.setType(attributes.getValue("type"));
            if (attributes.getValue("key") != null && attributes.getValue("key").equals("true")) {
                mCurrentMember.setKey(true);
            }
            else {
                mCurrentMember.setKey(false);
            }
            if (mCurrentMember.getType().equals(QDM.STRING_NON_BASIC)) {
                mCurrentMember.setNonBasicTypeName(attributes.getValue("nonBasicTypeName"));
            }
            if (attributes.getValue("sequenceMaxLength") != null) {
                mCurrentMember.setSequenceMaxLength(attributes.getValue("sequenceMaxLength"));
            }
        }
        else if (qName.equalsIgnoreCase("doc")) {
            mDoc = new StringBuilder();
        }
        else if (qName.equalsIgnoreCase("typedef")) {

            throw new ExitException("typedef is not yet supported");
            // TODO: enable again if typedefs are supported
            // mTypeDef = new TypeDef(attributes.getValue("name"));
            // mTypeDef.setType(attributes.getValue("type"));
            // if (attributes.getValue("sequenceMaxLength") != null) {
            // mTypeDef.setSequenceMaxLength(attributes.getValue("sequenceMaxLength"));
            // if (mTypeDef.getType().equals(QDM.STRING_NON_BASIC)) {
            // throw new ExitException("typedef with type nonbasic and sequenceMaxLength is not yet supported");
            // }
            // if (!mTypeDef.getSequenceMaxLength().equals("-1")) {
            // throw new ExitException("only sequenceMaxLength=\"-1\" is currently supported");
            // }
            // }
            // if (attributes.getValue("nonBasicTypeName") != null) {
            // mTypeDef.setNonBasicType(attributes.getValue("nonBasicTypeName"));
            // }
            // else {
            // if (mTypeDef.getType().equals(QDM.STRING_NON_BASIC)) {
            // throw new ExitException("Typedef with type=nonBasic requires nonBasicTypeName to be defined");
            // }
            // }
        }
        else if (qName.equalsIgnoreCase("include")) {
            mQdm.getIncludes().add(attributes.getValue("file"));
        }
    }

    /**
     * When the parser encounters the end of an element, it calls this method.
     * 
     * @param uri the Namespace URI, or the empty string if the element has no Namespace URI or if Namespace processing
     *            is not being performed
     * @param localName the local name (without prefix), or the empty string if Namespace processing is not being
     *            performed
     * @param qName the qualified XML name (with prefix), or the empty string if qualified names are not available
     * @throws SAXException If the parsing fails
     */
    @Override
    public void endElement(String uri, String localName, String qName)
        throws SAXException
    {
        if (qName.equalsIgnoreCase("module")) {
            mQdm.addModule(mModule);
            mModule = null;
        }
        else if (qName.equalsIgnoreCase("struct")) {
            mModule.addStruct(mCurrentStruct);
            mCurrentStruct = null;
        }
        else if (qName.equalsIgnoreCase("member")) {
            mCurrentStruct.addMember(mCurrentMember);
            mCurrentMember = null;
        }
        else if (qName.equalsIgnoreCase("enumerator")) {
            mCurrentEnum.addValue(mCurrentEnumerator);
            mCurrentEnumerator = null;
        }
        else if (qName.equalsIgnoreCase("doc")) {
            if (mCurrentMember != null) {
                mCurrentMember.setDoc(mDoc.toString());
            }
            else if (mCurrentEnumerator != null) {
                mCurrentEnumerator.setDoc(mDoc.toString());
            }
            else if (mCurrentEnum != null) {
                mCurrentEnum.setDoc(mDoc.toString());
            }
            else if (mCurrentStruct != null) {
                mCurrentStruct.setDoc(mDoc.toString());
            }
        }
        else if (qName.equalsIgnoreCase("enum")) {
            mModule.addEnum(mCurrentEnum);
            mCurrentEnum = null;
        }

        /*
         * else if (qName.equalsIgnoreCase("typedef")) { mModule.addTypeDef(mTypeDef); }
         */
    }

    /**
     * This method sets things up for parsing, parses and return a ParsedQdm object.
     * 
     * @return intermediate object generated with all the information of the qdmFile passed as parameter
     */
    public QDM parse()
    {
        try {
            getParser().parse(mFile, this);
        }
        catch (Exception e) {
            // Exit program. Call an ExitException for proper termination
            throw new ExitException("Can't parse file " + mFile + ": " + e.getMessage(), e);
        }
        if (!mQdm.validateStructsOrder()) {
            // Exit program. Call an ExitException for proper termination
            throw new ExitException("The order of the struct of the given file " + mFile.getName()
                + " is not correct. There are references to structs/enums that are defined later.");
        }
        return mQdm;
    }
}
