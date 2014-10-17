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

import com.technicolor.qeo.codegen.type.qdm.QdmModule;
import com.technicolor.qeo.codegen.type.qdm.QDM;
import com.technicolor.qeo.codegen.type.qdm.TypeDef;

/**
 * This class tries to be an utility class where we can find methods to give or change into a specific format.
 */
public final class Convert
{
    private Convert()
    {
    }

    /**
     * This method gets a string name, normally this name is complete name space separated by "::". This function
     * replace this separator by _. Example: hello::world -> hello_world
     * 
     * @param name , namespace
     * @return String with the new format.
     */
    public static String qdmToC(String name)
    {
        return name.replaceAll("::", "_");
    }

    /**
     * This method gets an object Module 'module' and a string 'name' and concatenate them with the "." as a separator.
     * It also replace the "::" of the name into ".". Example: module1, hello::world -> module1Name.hello.world.
     * 
     * @param module , qdm module
     * @param name , namespace
     * @return String with the new format.
     */
    public static String qdmToQeo(QdmModule module, String name)
    {
        return module.getName().replaceAll("::", ".") + "." + name;
    }

    /**
     * This method gives the format type depending of the language.
     * 
     * @param module , qdm module
     * @param name , namespace
     * @param lang , language code to be generated
     * @return String with the new format.
     */
    public static String getTypeName(QdmModule module, String name, TargetLanguage lang)
    {
        if (name.contains("::")) {
            // fully qualified name
            if (lang.equals(TargetLanguage.OBJECTIVEC)) {
                return qdmToC(name);
            }
            else {
                return qdmToC(name) + "_t";
            }
        }
        else {
            // prepend module namespace
            if (lang.equals(TargetLanguage.OBJECTIVEC)) {
                return qdmToC(module.getName()) + "_" + name;
            }
            else {
                return qdmToC(module.getName()) + "_" + name + "_t";
            }
        }
    }

    /**
     * This method gives the formatted typeDef.
     * 
     * @param td TypeDef
     * @param ext , String extension to be concatenated as a sufix.
     * @param tdType , String type of td
     * @return String with the new format.
     */
    public static String getFormattedTypeDef(TypeDef td, String ext, String tdType)
    {
        if (td.getType().equals(QDM.STRING_NON_BASIC)) {
            if (td.getNonBasicType().contains("::")) {
                return Convert.qdmToC(td.getNonBasicType()) + "_" + ext;
            }
            else {
                return tdType;
            }
        }
        else {
            return tdType;
        }
    }

    /**
     * This method generate the namespace full name. It concatenate the given parameters with a "::".
     * 
     * @param module , String module name
     * @param struct , String struct name
     * @return String with the new format.
     */
    public static String generateNameSpace(String module, String struct)
    {
        return module + "::" + struct;
    }

}
