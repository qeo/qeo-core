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

import com.technicolor.qeo.codegen.type.c.CEnum;
import com.technicolor.qeo.codegen.type.c.CHeader;
import com.technicolor.qeo.codegen.type.c.CMember;
import com.technicolor.qeo.codegen.type.c.CModule;
import com.technicolor.qeo.codegen.type.c.CSource;
import com.technicolor.qeo.codegen.type.c.CStruct;
import com.technicolor.qeo.codegen.type.qdm.QdmModule;
import com.technicolor.qeo.codegen.type.qdm.QDM;
import com.technicolor.qeo.codegen.type.qdm.QdmEnum;
import com.technicolor.qeo.codegen.type.qdm.QdmMember;
import com.technicolor.qeo.codegen.type.qdm.QdmStruct;

/**
 * This class contains all the functionality to convert the intermediate parsed object into a C.
 */
public class CEngine
    extends TsmEngine

{

    private final Template mTemplateHeader;
    private final Template mTemplateSource;
    private CHeader mCHeader;
    private CSource mCSource;
    private String mFilenameBase;
    private Map<String, String> mTypeDefNames;

    /**
     * CAdapter constructor. Initialize and populate the set of primitives and the map to the c types.
     */
    public CEngine()
    {
        super();

        // fill the map for the header types
        registerType("boolean", "qeo_boolean_t");
        registerType("byte", "int8_t");
        registerType("int16", "int16_t");
        registerType("int32", "int32_t");
        registerType("int64", "int64_t");
        registerType("float32", "float");
        registerType("string", "char *");

        mTemplateHeader = mEngine.getTemplate("template/c/header.vm");
        mTemplateSource = mEngine.getTemplate("template/c/source.vm");
        mTypeDefNames = null;
    }

    private CMember generateType(QdmModule module, QdmStruct struct, CStruct cStruct, QdmMember member)
    {
        CMember cMember = new CMember(member.getName());

        String qdmType = member.getType();
        String cType = null;
        if (qdmType.equals(QDM.STRING_NON_BASIC)) {
            // nonbasic type
            String qdmTypeNonBasic = member.getNonBasicTypeName();
            if (member.getSequenceMaxLength() != null) {
                // dds sequence

                cType = Convert.qdmToC(module.getName()) + "_" + struct.getName() + "_" + member.getName() + "_seq";

                String ddsSeqType;
                if (mTypeDefNames.containsKey(qdmTypeNonBasic)) {
                    ddsSeqType = mTypeDefNames.get(qdmTypeNonBasic);
                }
                else {
                    ddsSeqType = Convert.getTypeName(module, qdmTypeNonBasic, TargetLanguage.C);
                }

                cStruct.addDdsSequence(ddsSeqType, cType);
            }
            else {
                // normal non-basic type
                if (mTypeDefNames.containsKey(qdmTypeNonBasic)) {
                    // typedef available
                    cType = mTypeDefNames.get(qdmTypeNonBasic);
                }
                else {
                    // construct name
                    cType = Convert.getTypeName(module, qdmTypeNonBasic, TargetLanguage.C);
                }
            }
        }
        else {
            // basic type
            if (member.getSequenceMaxLength() != null) {
                // dds sequence
                cType = Convert.qdmToC(module.getName()) + "_" + struct.getName() + "_" + member.getName() + "_seq";
                String ddsSeqType = getType(qdmType);
                cStruct.addDdsSequence(ddsSeqType, cType);
            }
            else {
                // regular type
                cType = getType(qdmType);
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
        mCHeader = new CHeader();
        mCSource = new CSource();

        mCHeader.addExternalInclude("qeo/types.h");
        mCSource.addInclude(mFilenameBase + ".h");

        mTypeDefNames = new HashMap<String, String>(); // list to contain known typedef names
        addEnumsToTypes(qdm);

        mCHeader.setIfDefGuard("QDM_" + qdm.getName().toUpperCase(Locale.US) + "_H_");
        for (String include : qdm.getIncludes()) {
            mCHeader.addLocalInclude(include.replace(".xml", ".h"));
        }
        for (QdmModule module : qdm.getModules()) {
            String moduleName = Convert.qdmToC(module.getName());
            CModule mModule = new CModule(moduleName);
            /*
             * for (TypeDef typeDef : module.getTypeDefs()) { if (typeDef.getSequenceMaxLength() == null) { // not a dds
             * sequence, add typedef in header String typedefname = Convert.getTypeName(module, typeDef.getName(),
             * TargetLanguage.C); mCHeader.addTypeDef(typedefname, Convert.getFormattedTypeDef(typeDef, "t",
             * getType(typeDef.getType()))); mTypeDefNames.put(typeDef.getName(), typedefname); } else { // dds sequence
             * from a typedef if (typeDef.getType().equals(QDM.STRING_NON_BASIC)) { throw new
             * UnsupportedOperationException(
             * "Typedef for type=nonBasic and sequenceMaxLength defined are not yet supported" + " for c codegen"); }
             * String cType = Convert.qdmToC(module.getName()) + "_" + mFilenameBase + "_" + typeDef.getName() + "_t";
             * String ddsSeqType = getType(typeDef.getType());
             * 
             * mCHeader.addDdsSequence(ddsSeqType, cType); mTypeDefNames.put(typeDef.getName(), cType); } }
             */
            for (QdmEnum qdmEnum : module.getEnums()) {
                /* wrap QDM enums for use with C */
                mModule.addEnum(new CEnum(mModule, qdmEnum));
            }
            for (QdmStruct struct : module.getStructs()) {
                // loop over all structs. A struct is a c struct
                CStruct cStruct = new CStruct(moduleName, struct.getName());
                cStruct.setDoc(struct.getDoc());

                for (QdmMember member : struct.getMembers()) {
                    // convert qdm type into ctype for the header
                    CMember cMember = generateType(module, struct, cStruct, member);

                    cMember.setDoc(member.getDoc());

                    if (member.isKey()) {
                        cMember.setDocAtTop("[Key]");
                    }
                    cStruct.addMember(cMember);
                }

                mModule.addStruct(cStruct);
            }

            mCSource.addTsmList(generateTsmList(moduleName, module));
            mCHeader.addModule(mModule);
        }
    }

    @Override
    protected void generateCode(QDM qdm)
        throws IOException
    {

        VelocityContext context = new VelocityContext();
        context.put("language", TargetLanguage.C.toString());
        context.put("header", mCHeader);
        context.put("source", mCSource);

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
        outputFile = new File(mOutputDir, mFilenameBase + ".c");
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
        return CStruct.constructType(moduleName, baseName);
    }

    @Override
    public String getNameForTsm(String moduleName, String baseName)
    {
        return CStruct.constructName(moduleName, baseName);
    }
}
