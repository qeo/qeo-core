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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;

import com.technicolor.qeo.codegen.type.java.JavaClass;
import com.technicolor.qeo.codegen.type.java.JavaMember;
import com.technicolor.qeo.codegen.type.qdm.QDM;
import com.technicolor.qeo.codegen.type.qdm.QdmEnum;
import com.technicolor.qeo.codegen.type.qdm.QdmMember;
import com.technicolor.qeo.codegen.type.qdm.QdmModule;
import com.technicolor.qeo.codegen.type.qdm.QdmStruct;
import com.technicolor.qeo.codegen.type.qdm.TypeDef;

/**
 * This class contains all the functionality to convert the intermediate parsed object into Java.
 */
public class JavaEngine
    extends Engine
{
    private final Set<String> mPrimitives;
    private final Template mClassTemplate;
    private final Template mEnumTemplate;
    private final List<JavaClass> mClasses;

    /**
     * JavaAdapter constructor. Initialize and populate the set of primitives and the map to the java types.
     */
    public JavaEngine()
    {
        super();
        mPrimitives = new HashSet<String>();
        mPrimitives.add("int");
        mPrimitives.add("long");
        mPrimitives.add("char");
        mPrimitives.add("byte");
        mPrimitives.add("short");
        mPrimitives.add("float");
        mPrimitives.add("double");
        mPrimitives.add("boolean");

        registerType("int32", "int");
        registerType("int16", "short");
        registerType("int64", "long");
        registerType("float32", "float");
        registerType("string", "String");
        registerType("boolean", "boolean");
        registerType("byte", "byte");

        mClassTemplate = mEngine.getTemplate("template/java/class.vm");
        mEnumTemplate = mEngine.getTemplate("template/java/enum.vm");
        mClasses = new ArrayList<JavaClass>();
    }

    private String qdmToJava(String name)
    {
        return name.replaceAll("::", ".");
    }

    private String getFormattedTypeDef(TypeDef td)
    {
        if (td.getType().equals(QDM.STRING_NON_BASIC)) {
            if (td.getNonBasicType().contains("::")) {
                return qdmToJava(td.getNonBasicType());
            }
            else {
                return getType(td.getType());
            }
        }
        else {
            return getType(td.getType());
        }
    }

    private JavaMember generateType(QdmModule module, QdmMember member)
    {
        JavaMember jMember = new JavaMember(member.getName());
        String qdmType = member.getType();
        String javaType = null;
        int arrayDepth = 0;
        if (qdmType.equals(QDM.STRING_NON_BASIC)) {
            // nonbasic type
            String qdmTypeNonBasic = member.getNonBasicTypeName();
            TypeDef td = module.getTypeDef(qdmTypeNonBasic);
            if (td != null) {
                // there is a typedef
                javaType = getFormattedTypeDef(td);
                if (td.getSequenceMaxLength() != null) {
                    arrayDepth++;
                }
            }
            else {
                // no typedef
                javaType = qdmToJava(qdmTypeNonBasic);
            }
        }
        else {
            // basic type
            javaType = getType(qdmType);
        }
        if (javaType == null || javaType.isEmpty()) {
            throw new IllegalStateException("no type name");
        }
        if (member.getSequenceMaxLength() != null) {
            arrayDepth++;
        }
        StringBuilder arrayBrackets = new StringBuilder();
        for (int i = 0; i < arrayDepth; i++) {
            arrayBrackets.append("[]");
        }
        javaType += arrayBrackets.toString();
        jMember.setType(javaType);
        return jMember;
    }

    @Override
    protected void processQdm(QDM qdm)
    {
        mClasses.clear();
        for (QdmModule module : qdm.getModules()) {
            // loop over all modules. A module is a java package
            String pkgName = qdmToJava(module.getName());
            for (QdmStruct struct : module.getStructs()) {
                // loop over all structs. A struct is a java class
                JavaClass clazz = new JavaClass(pkgName, struct.getName());
                clazz.setDoc(struct.getDoc());
                clazz.setBehavior(struct.getBehavior());
                boolean arrays = false;
                boolean keyedarrays = false;
                for (QdmMember member : struct.getMembers()) {
                    // convert qdm type into javatype
                    JavaMember jMember = generateType(module, member);

                    jMember.setKey(member.isKey());
                    jMember.setArray(member.getSequenceMaxLength() != null);
                    if (member.isKey()) {
                        clazz.setHasKeys(true);
                    }
                    if (jMember.isArray()) {
                        arrays = true;
                        if (member.isKey()) {
                            keyedarrays = true;
                        }
                    }
                    jMember.setDoc(member.getDoc());
                    clazz.addMember(jMember);
                }
                /* A class has relevant arrays in case it has arrays inside the key or arrays and no keys at all. */
                clazz.setHasRelevantArrays(clazz.isHasKeys() ? keyedarrays : arrays);
                mClasses.add(clazz);
            }
        }
    }

    private File getOutputFile(File outputDir, String module, String typeName)
        throws IOException
    {
        File f = outputDir;

        for (String s : module.split("\\.")) {
            f = new File(f, s);
        }

        if (!f.exists() && !f.mkdirs()) {
            throw new IOException("Error when creating the directory: " + f.getPath());
        }

        f = new File(f, typeName + ".java");
        return f;
    }

    /**
     * Generate code for a single class.
     * 
     * @param context
     * @param template
     * @param module
     * @param name
     * @throws IOException
     */
    private void generateClass(VelocityContext context, Template template, String module, String name)
        throws IOException
    {
        // create output file writer
        File outputFile = getOutputFile(mOutputDir, module, name);
        Writer writer = null;
        try {
            writer = new OutputStreamWriter(new FileOutputStream(outputFile), Charset.forName("UTF-8"));
            // process template
            Main.printMessage("Creating " + outputFile.getAbsolutePath());
            template.merge(context, writer);
        }
        finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

    @Override
    protected void generateCode(QDM qdm)
        throws IOException
    {
        VelocityContext context = new VelocityContext();

        /* Generate main classes */
        for (JavaClass clazz : mClasses) {
            context.put("class", clazz);
            generateClass(context, mClassTemplate, clazz.getPackageName(), clazz.getName());
        }
        /* Generate enum classes */
        for (QdmModule module : qdm.getModules()) {
            String moduleName = qdmToJava(module.getName());

            for (QdmEnum e : module.getEnums()) {
                context.put("package", moduleName);
                context.put("enum", e);
                generateClass(context, mEnumTemplate, moduleName, e.getName());
            }
        }
    }
}
