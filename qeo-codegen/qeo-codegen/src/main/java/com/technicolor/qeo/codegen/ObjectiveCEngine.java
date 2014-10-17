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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;

import com.technicolor.qeo.codegen.type.objectivec.OCEnum;
import com.technicolor.qeo.codegen.type.objectivec.OCHeader;
import com.technicolor.qeo.codegen.type.objectivec.OCMember;
import com.technicolor.qeo.codegen.type.objectivec.OCModule;
import com.technicolor.qeo.codegen.type.objectivec.OCSource;
import com.technicolor.qeo.codegen.type.objectivec.OCStruct;
import com.technicolor.qeo.codegen.type.qdm.QDM;
import com.technicolor.qeo.codegen.type.qdm.QdmEnum;
import com.technicolor.qeo.codegen.type.qdm.QdmMember;
import com.technicolor.qeo.codegen.type.qdm.QdmModule;
import com.technicolor.qeo.codegen.type.qdm.QdmStruct;

/**
 * This class contains all the functionality to convert the intermediate parsed object into an Objective C structure.
 */
public class ObjectiveCEngine
    extends TsmEngine
{

    private final Template mTemplateHeader;
    private final Template mTemplateSource;
    private OCHeader mOCHeader;
    private OCSource mOCSource;
    private String mFilenameBase;
    private Map<String, String> mTypeDefNames;

    /**
     * CAdapter constructor. Initialize and populate the set of primitives and the map to the c types.
     */
    public ObjectiveCEngine()
    {
        super();

        // fill the map for the header types
        registerType("boolean", "BOOL");
        registerType("byte", "int8_t");
        registerType("int16", "int16_t");
        registerType("int32", "int32_t");
        registerType("int64", "int64_t");
        registerType("float32", "float");
        registerType("string", "NSString");

        mTemplateHeader = mEngine.getTemplate("template/objectivec/objectiveCheader.vm");
        mTemplateSource = mEngine.getTemplate("template/objectivec/methodes.vm");
        mTypeDefNames = null;
    }

    private OCMember generateType(QdmModule module, QdmStruct struct, OCStruct cStruct, QdmMember member)
    {
        OCMember cMember = new OCMember(member.getName());
        String qdmType = member.getType();
        String cType = null;
        if (qdmType.equals(QDM.STRING_NON_BASIC)) {
            // nonbasic type
            String qdmTypeNonBasic = member.getNonBasicTypeName();
            if (member.getSequenceMaxLength() != null) {
                // dds sequence
                cType = "NSArray";
                String ddsSeqType;
                if (mTypeDefNames.containsKey(qdmTypeNonBasic)) {
                    ddsSeqType = mTypeDefNames.get(qdmTypeNonBasic);
                }
                else {
                    ddsSeqType = Convert.getTypeName(module, qdmTypeNonBasic, TargetLanguage.OBJECTIVEC);
                }
                cMember.setDoc("Array of " + ddsSeqType);
                cStruct.addDdsSequence(ddsSeqType, cType);
            }
            else {
                // normal non-basic type
                if (mTypeDefNames.containsKey(qdmTypeNonBasic)) {
                    // typedef available
                    cType = mTypeDefNames.get(qdmTypeNonBasic);
                }
                else {
                    if (module.containsEnum(qdmTypeNonBasic) != -1) {
                        cMember.setBasic(true);
                    }
                    // construct name
                    cType = Convert.getTypeName(module, qdmTypeNonBasic, TargetLanguage.OBJECTIVEC);
                }
            }
        }
        else {
            // basic type
            if (member.getSequenceMaxLength() != null) {
                // dds sequence
                // cType = qdmToC(module.getName()) + "_" + struct.getName() + "_" + member.getName() + "_seq";
                cType = "NSArray";
                String ddsSeqType = getType(qdmType);
                cStruct.addDdsSequence(ddsSeqType, cType);

                if (ddsSeqType.equals("NSString")) {
                    cMember.setDoc("Array of " + ddsSeqType);
                }
                else {
                    cMember.setDoc("Array of NSNumber (" + ddsSeqType + ")");
                }
            }
            else {
                // regular type
                cType = getType(qdmType);

                if (!cType.equals("NSString")) {
                    cMember.setBasic(true);
                }
            }
        }
        cMember.setType(cType);
        return cMember;
    }

    @Override
    protected void processQdm(QDM qdm)
    {
        // a qdm is a c Header
        mFilenameBase = qdm.getName();
        mOCHeader = new OCHeader();
        mOCSource = new OCSource();

        mOCHeader.addExternalInclude("Foundation/Foundation.h");
        mOCHeader.addExternalInclude("Qeo/Qeo.h");
        mOCSource.addLocalInclude(mFilenameBase + ".h");
        mOCSource.addExternalInclude("Qeo/dds/dds_tsm.h");

        mTypeDefNames = new HashMap<String, String>(); // list to contain known typedef names
        addEnumsToTypes(qdm);

        mOCHeader.setIfDefGuard("QDM_" + qdm.getName().toUpperCase(Locale.US) + "_H_");
        for (String include : qdm.getIncludes()) {
            mOCHeader.addLocalInclude(include.replace(".xml", ".h"));
        }
        for (QdmModule module : qdm.getModules()) {
            String moduleName = Convert.qdmToC(module.getName());

            OCModule mModule = new OCModule(moduleName);
            for (QdmEnum qdmEnum : module.getEnums()) {
                /* wrap QDM enums for use with ObjectiveC */
                mModule.addEnum(new OCEnum(mModule, qdmEnum));
            }
            for (QdmStruct struct : module.getStructs()) {

                // loop over all structs. A struct is a c struct
                OCStruct cStruct = new OCStruct(moduleName, struct.getName());
                cStruct.setDoc(struct.getDoc());

                for (QdmMember member : struct.getMembers()) {
                    OCMember cMember;

                    // convert qdm type into a type for the header
                    cMember = generateType(module, struct, cStruct, member);
                    cMember.setDoc(member.getDoc());

                    if (member.isKey()) {
                        cMember.setDocAtTop("[Key]");
                    }

                    cStruct.addMember(cMember);
                }
                mModule.addStruct(cStruct);
                mOCSource.addStruct(cStruct);
            }

            mOCSource.addTsmList(generateTsmList(moduleName, module));
            mOCHeader.addModule(mModule);
        }
    }

    @Override
    protected void generateCode(QDM qdm)
        throws IOException
    {

        VelocityContext context = new VelocityContext();
        context.put("language", TargetLanguage.OBJECTIVEC.toString());
        context.put("header", mOCHeader);
        context.put("source", mOCSource);

        // generate header
        // create output file writer
        File outputFile = new File(mOutputDir, mFilenameBase + ".h");

        Writer writerHeader = null;
        try {
            writerHeader = new OutputStreamWriter(new FileOutputStream(outputFile), Charset.forName("UTF-8"));
            // process template
            Main.printMessage("Creating " + outputFile.getAbsolutePath());
            mTemplateHeader.merge(context, writerHeader);
        }
        finally {
            if (writerHeader != null) {
                writerHeader.close();
            }
        }

        // generate source
        // create output file writer
        outputFile = new File(mOutputDir, mFilenameBase + ".m");
        Writer writerSource = null;

        try {
            writerSource = new OutputStreamWriter(new FileOutputStream(outputFile), Charset.forName("UTF-8"));
            // process template
            Main.printMessage("Creating " + outputFile.getAbsolutePath());
            mTemplateSource.merge(context, writerSource);
        }
        finally {
            if (writerSource != null) {
                writerSource.close();
            }
        }
    }

    @Override
    public String getTypeForTsm(String moduleName, String baseName)
    {
        return OCStruct.constructType(moduleName, baseName);
    }

    @Override
    public String getNameForTsm(String moduleName, String baseName)
    {
        return baseName;
    }
}
