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

package com.technicolor.qeo.codegen.type.qdm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Intermediate object that contains the data o the parsed qdm. A QDM object has some other intermediate objects that
 * contains the data of each specific tag of the qdm.
 */
public class QDM
{
    /**
     * String representation of the nonBasic type member.
     */
    public static final String STRING_NON_BASIC = "nonBasic";

    private final String mName;
    private final List<QdmModule> mModules;
    private final List<String> mIncludes;

    /**
     * QDM constructor.
     * 
     * @param name The name of the qdm file.
     */
    public QDM(String name)
    {
        mName = name;
        mIncludes = new ArrayList<String>();
        mModules = new ArrayList<QdmModule>();
    }

    /**
     * Add a QDM module.
     * 
     * @param module the module.
     */
    public void addModule(QdmModule module)
    {
        mModules.add(module);
    }

    /**
     * Get a list of QDM modules.
     * 
     * @return list of modules.
     */
    public List<QdmModule> getModules()
    {
        return Collections.unmodifiableList(mModules);
    }

    /**
     * Get map of all the include tags of the qdm.
     * 
     * @return map with all the includes defined in the qdm
     */
    public List<String> getIncludes()
    {
        return mIncludes;
    }

    /**
     * Get the name of the qdm file.
     * 
     * @return The name.
     */
    public String getName()
    {
        return mName;
    }

    /**
     * Validates if the definitions of all the stucts is done before we reference them, based on the order of the given
     * qdm.
     * 
     * @return true if the order that the structs are defined is correct, false otherwise.
     */
    public boolean validateStructsOrder()
    {
        Set<String> usedStructs = new HashSet<String>();
        for (int i = 0; i < mModules.size(); i++) {
            // usedStructs.clear();
            QdmModule module = mModules.get(i);

            List<QdmEnum> qdmEnums = module.getEnums();
            for (int k = 0; k < qdmEnums.size(); k++) {
                QdmEnum mEnum = qdmEnums.get(k);
                if (usedStructs.contains(module.getName() + "::" + mEnum.getName())) {
                    // the enum is defined after its reference
                    return false;
                }
            }

            List<QdmStruct> qdmStructs = module.getStructs();
            for (int j = 0; j < qdmStructs.size(); j++) {
                QdmStruct struct = qdmStructs.get(j);

                if (usedStructs.contains(module.getName() + "::" + struct.getName())) {
                    // the struct is defined after its reference
                    return false;
                }
                for (QdmMember member : struct.getMembers()) {
                    if (member.getType().equals(QDM.STRING_NON_BASIC)) {
                        // Add all the nonBasic types to the set
                        if (member.getNonBasicTypeName().contains("::")) {
                            usedStructs.add(member.getNonBasicTypeName());
                        }
                        else {
                            usedStructs.add(module.getName() + "::" + member.getNonBasicTypeName());
                        }
                    }
                }
            }
        }
        return true;
    }
}
