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
import java.util.ArrayList;
import java.util.List;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;

import com.technicolor.qeo.codegen.type.json.JsonMember;
import com.technicolor.qeo.codegen.type.json.JsonStruct;
import com.technicolor.qeo.codegen.type.qdm.QDM;
import com.technicolor.qeo.codegen.type.qdm.QdmEnum;
import com.technicolor.qeo.codegen.type.qdm.QdmMember;
import com.technicolor.qeo.codegen.type.qdm.QdmModule;
import com.technicolor.qeo.codegen.type.qdm.QdmStruct;
import com.technicolor.qeo.codegen.type.qdm.TypeDef;

/**
 * This class contains all the functionality to convert the intermediate parsed object into JavaScript.
 */
public class JSONEngine
    extends Engine
{
    private final Template mTemplate;
    private List<JsonStruct> mJsonStructs;
    private final List<QdmEnum> mJsonEnums;

    /**
     * JavaAdapter constructor. Initialize and populate the set of primitives and the map to the java types.
     */
    public JSONEngine()
    {
        super();
        mTemplate = mEngine.getTemplate("template/js/js.vm");
        mJsonStructs = new ArrayList<JsonStruct>();
        mJsonEnums = new ArrayList<QdmEnum>();
    }

    @Override
    protected void processQdm(QDM qdm)
    {
        mJsonStructs = new ArrayList<JsonStruct>();

        for (QdmModule module : qdm.getModules()) {
            String moduleName = module.getName();
            for (QdmEnum mEnum : module.getEnums()) {
                mEnum.setFullName(Convert.generateNameSpace(moduleName, mEnum.getName()));
                mJsonEnums.add(mEnum);
            }

            for (QdmStruct struct : module.getStructs()) {
                // loop over all structs. A struct is a json var
                String structName = struct.getName();
                JsonStruct jsonStruct = new JsonStruct(Convert.generateNameSpace(moduleName, structName), structName);

                if (null != struct.getBehavior()) {
                    jsonStruct.setBehavior(struct.getBehavior());
                }
                jsonStruct.setDoc(struct.getDoc());

                for (QdmMember member : struct.getMembers()) {
                    JsonMember jsonMember = new JsonMember(member.getName());

                    if (member.getType().equals(QDM.STRING_NON_BASIC)) {
                        // NONBASIC
                        TypeDef testTypeDef = module.getTypeDef(member.getNonBasicTypeName());
                        if (null != testTypeDef) {
                            // TYPEDEF
                            jsonMember.setLevel(iterateTypeDefs(module, member).getLevel());
                            jsonMember.setItemsType(iterateTypeDefs(module, member).getItemsType());
                            jsonMember.setType(iterateTypeDefs(module, member).getType());
                        }
                        else {
                            // NO TYPEDEF
                            if (null != member.getSequenceMaxLength()) {
                                // ARRAY OBJECT
                                jsonMember.setType("array");
                            }
                            else {
                                if (null != module.getEnum(member.getNonBasicTypeName())) {
                                    // ENUM
                                    jsonMember.setType("enum");
                                }
                                else {
                                    // OBJECT
                                    jsonMember.setType("object");
                                }
                            }
                            jsonMember.setItemsType(toStructType(moduleName, member.getNonBasicTypeName()));
                        }
                    }
                    else {
                        // BASIC
                        if (null != member.getSequenceMaxLength()) {
                            // ARRAY PRIMITIVES
                            jsonMember.setType("array");
                            jsonMember.setItemsType(member.getType());
                        }
                        else {
                            jsonMember.setType(member.getType());
                        }
                    }

                    if (member.isKey()) {
                        jsonMember.setKey(true);
                    }
                    jsonStruct.addMember(jsonMember);
                }
                mJsonStructs.add(jsonStruct);
            }
        }
    }

    /**
     * Iterate over the typeDefs and set the type and itemType in a JsonMember.
     * 
     * @param module
     * @param member
     * @return JsonMember
     */
    private JsonMember iterateTypeDefs(QdmModule module, QdmMember member)
    {
        JsonMember returnMember = new JsonMember(null);
        TypeDef typeDefX = module.getTypeDef(member.getNonBasicTypeName());
        TypeDef typeDefY = module.getTypeDef(member.getNonBasicTypeName());

        while (null != typeDefY) {
            if (null != typeDefX.getSequenceMaxLength()) {
                returnMember.setLevel(returnMember.getLevel() + 1);
            }
            typeDefY = module.getTypeDef(typeDefX.getNonBasicType());
            if (null != typeDefY) {
                typeDefX = module.getTypeDef(typeDefX.getNonBasicType());
            }
        }
        returnMember.setType(typeDefX.getType());
        if (returnMember.isBasicType()) {
            // BASIC
            if (returnMember.getLevel() > 0) {
                // ARRAY
                returnMember.setItemsType(returnMember.getType());
                returnMember.setType("array");
            }
        }
        else {
            // NONBASIC
            returnMember.setItemsType(toStructType(module.getName(), returnMember.getType()));
            if (returnMember.getLevel() > 0) {
                // ARRAY
                returnMember.setType("array");
            }
            else {
                returnMember.setType("object");
            }
        }

        return returnMember;
    }

    private String toStructType(String moduleName, String nonBasicTypeName)
    {
        if (nonBasicTypeName.contains("::")) {
            return nonBasicTypeName;
        }
        else {
            return moduleName + "::" + nonBasicTypeName;
        }
    }

    @Override
    protected void generateCode(QDM qdm)
        throws IOException
    {
        VelocityContext context = new VelocityContext();
        context.put("json", mJsonStructs);
        context.put("enums", mJsonEnums);

        // create output file writer
        File outputFile = new File(mOutputDir, qdm.getName() + ".js");
        Writer writer = null;
        try {
            writer = new OutputStreamWriter(new FileOutputStream(outputFile), Charset.forName("UTF-8"));
            // process template
            Main.printMessage("Creating " + outputFile.getAbsolutePath());
            mTemplate.merge(context, writer);
        }
        finally {
            if (writer != null) {
                writer.close();
            }
        }
    }
}
