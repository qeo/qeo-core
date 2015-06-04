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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.technicolor.qeo.codegen.type.Container;
import com.technicolor.qeo.codegen.type.ContainerMember;
import com.technicolor.qeo.codegen.type.c.CEnum;
import com.technicolor.qeo.codegen.type.qdm.QDM;
import com.technicolor.qeo.codegen.type.qdm.QdmEnum;
import com.technicolor.qeo.codegen.type.qdm.QdmMember;
import com.technicolor.qeo.codegen.type.qdm.QdmModule;
import com.technicolor.qeo.codegen.type.qdm.QdmStruct;
import com.technicolor.qeo.codegen.type.qdm.TypeDef;
import com.technicolor.qeo.codegen.type.tsm.Tsm;
import com.technicolor.qeo.codegen.type.tsm.TsmMember;

/**
 * This class contains all the functionality to convert the intermediate parsed object into a Tms structure (Only used
 * by Objective-C and C).
 */
public abstract class TsmEngine
    extends Engine
{
    private final Map<String, String> mSourceTypes;

    /**
     * TSM Constructor, it creates map between tsm vs the type defined in the qdm.
     */
    public TsmEngine()
    {
        mSourceTypes = new HashMap<String, String>();
        mSourceTypes.put("boolean", "CDR_TYPECODE_BOOLEAN");
        mSourceTypes.put("byte", "CDR_TYPECODE_OCTET");
        mSourceTypes.put("int16", "CDR_TYPECODE_SHORT");
        mSourceTypes.put("int32", "CDR_TYPECODE_LONG");
        mSourceTypes.put("int64", "CDR_TYPECODE_LONGLONG");
        mSourceTypes.put("float32", "CDR_TYPECODE_FLOAT");
        mSourceTypes.put("string", "CDR_TYPECODE_CSTRING");
        mSourceTypes.put("nonBasic", "CDR_TYPECODE_TYPEREF");
    }

    private String sourceType(String type)
    {
        String sType = mSourceTypes.get(type);
        if (sType == null) {
            sType = type;
        }
        return sType;
    }

    private String getTypeNameType(QdmModule module, String type, String nonBasicType, String tdType)
    {
        if (type.equals("nonBasic")) {
            // nonBasic
            if (nonBasicType.contains("::")) {
                // fully qualified name
                return Convert.qdmToC(nonBasicType) + "_type";
            }
            else {
                TypeDef td = module.getTypeDef(nonBasicType);
                if (td != null) {
                    return Convert.getFormattedTypeDef(td, "type", tdType);
                }
                else {
                    return Convert.qdmToC(module.getName()) + "_" + nonBasicType + "_type";
                }
            }
        }
        else {
            return tdType;
        }
    }

    /**
     * This function generate the first member of the tsm struct. The first member is a special member with information
     * about the full tsm struct.
     * 
     * @param module of the where the struct is located
     * @param container , qdm struct
     * @param structName , name of the struct
     * @return TsmMember
     */
    public TsmMember generateTsmContainer(QdmModule module, Container<?> container, String structName)
    {
        // a struct also mean a C TSM
        TsmMember cTsmMemberStruct = new TsmMember(Convert.qdmToQeo(module, container.getName()));
        cTsmMemberStruct.setNelem(container.size());
        if (container instanceof QdmEnum) {
            cTsmMemberStruct.setType("CDR_TYPECODE_ENUM");
        }
        else if (container instanceof QdmStruct) {
            cTsmMemberStruct.setType("CDR_TYPECODE_STRUCT");
            cTsmMemberStruct.addFlag("TSMFLAG_MUTABLE");
            cTsmMemberStruct.addFlag("TSMFLAG_GENID");
            String size = "sizeof(";
            size = size.concat(structName);
            size = size.concat(")");
            cTsmMemberStruct.setSize(size);
        }
        return cTsmMemberStruct;

    }

    /**
     * This function generate set the 'key' flags to the members passed as parameters.
     * 
     * @param cTsmMember you want to add the flag
     * @param cTsmMemberStruct , first member of the struct
     */
    public void setKey(TsmMember cTsmMember, TsmMember cTsmMemberStruct)
    {
        cTsmMember.addFlag("TSMFLAG_KEY");
        // if there's at least 1 key field, also add a key flag to the struct entry
        cTsmMemberStruct.addFlag("TSMFLAG_KEY");
    }

    /**
     * This function fill the tsm structure adding the necessary information for each member.
     * 
     * @param module of the struct
     * @param cStructName , qdm struct of teh member
     * @param member , Member to be processed
     * @return TsmMember
     */
    public TsmMember generateTypeSource(QdmModule module, String cStructName, QdmMember member)
    {
        TsmMember tsmMember = null;
        String qdmType = member.getType();
        String tsm = getTypeNameType(module, member.getType(), member.getNonBasicTypeName(), qdmType);

        // NON Basic
        if (qdmType.equals(QDM.STRING_NON_BASIC)) {
            // Member is Sequence

            if (member.getSequenceMaxLength() != null) {
                tsmMember = new TsmMember(member.getName());
                tsmMember.setType("CDR_TYPECODE_SEQUENCE");
                tsmMember.addFlag("TSMFLAG_DYNAMIC");
                tsmMember.addFlag("TSMFLAG_MUTABLE");
                tsmMember.addFlag("TSMFLAG_GENID");
                tsmMember.setNelem(0);

                String qdmTypeNonBasic = member.getNonBasicTypeName();

                // Member has typedef
                if (module.getTypeDef(qdmTypeNonBasic) != null) {
                    TypeDef typedef = module.getTypeDef(qdmTypeNonBasic);

                    // typedef is Sequence
                    if (typedef.getSequenceMaxLength() == null) {
                        TsmMember tsmMember2 = new TsmMember(null);
                        tsmMember2.setType("CDR_TYPECODE_SEQUENCE");
                        tsmMember2.setNelem(0);

                        TsmMember tsmMember3 = new TsmMember(null);
                        tsmMember3.setType(sourceType(typedef.getType()));

                        tsmMember.addSeqElemType(tsmMember2);
                        tsmMember.addSeqElemType(tsmMember3);
                    }
                    // typedef is NOT Sequence
                    else {
                        // Non Basic
                        if (typedef.getType().equals(QDM.STRING_NON_BASIC)) {
                            TsmMember tsmMember2 = new TsmMember(null);
                            tsmMember2.setType("CDR_TYPECODE_TYPEREF");
                            tsmMember2.setTsm(tsm);
                            tsmMember.addSeqElemType(tsmMember2);
                        }
                        // Basic
                        else {
                            TsmMember tsmMember2 = new TsmMember(null);
                            tsmMember2.setType(sourceType(typedef.getType()));
                            tsmMember.addSeqElemType(tsmMember2);
                        }
                    }
                }
                // Member hast't typedef (non basic)
                else {
                    TsmMember tsmMember2 = new TsmMember(null);
                    tsmMember2.setType("CDR_TYPECODE_TYPEREF");
                    tsmMember2.setTsm(tsm);
                    tsmMember.addSeqElemType(tsmMember2);
                }
            }
            else { // member is non-basic but not a sequence
                String qdmTypeNonBasic = member.getNonBasicTypeName();
                int index = module.containsEnum(qdmTypeNonBasic);
                if (index > 0) {
                    QdmEnum mEnumerator = module.getEnum(qdmTypeNonBasic);

                    if (null != mEnumerator) {
                        tsmMember = new TsmMember(member.getName());
                        tsmMember.setType("CDR_TYPECODE_TYPEREF");
                        tsmMember.setTsm(tsm);

                    }
                    /*
                     * if (typedef.getClass().equals(TypeDefSeq.class)) {
                     * 
                     * tsmMember.setType("CDR_TYPECODE_SEQUENCE"); tsmMember.setName(member.getName());
                     * tsmMember.addFlag("TSMFLAG_DYNAMIC"); tsmMember.addFlag("TSMFLAG_MUTABLE");
                     * tsmMember.addFlag("TSMFLAG_GENID"); tsmMember.setNelem(0);
                     * 
                     * TsmMember tsmMember2 = new TsmMember(); tsmMember2.setType(sourceType(typedef.getType()));
                     * tsmMember.addSeqElemType(tsmMember2); } else { if
                     * (typedef.getType().equals(QDM.STRING_NON_BASIC)) {
                     * 
                     * tsmMember.setType("CDR_TYPECODE_TYPEREF"); tsmMember.setName(member.getName());
                     * tsmMember.setTsm(tsm); } else { tsmMember.setType(sourceType(typedef.getType()));
                     * tsmMember.setName(member.getName()); } }
                     */
                }
                else { // member is not a typedef
                    tsmMember = new TsmMember(member.getName());
                    tsmMember.setType(sourceType(member.getType()));
                    tsmMember.setTsm(tsm);
                }
            }
        }
        // member is a basic type
        else {
            if (member.getSequenceMaxLength() != null) {
                tsmMember = new TsmMember(member.getName());
                tsmMember.setType("CDR_TYPECODE_SEQUENCE");
                tsmMember.addFlag("TSMFLAG_DYNAMIC");
                tsmMember.addFlag("TSMFLAG_MUTABLE");
                tsmMember.addFlag("TSMFLAG_GENID");
                tsmMember.setNelem(0);

                TsmMember tsmMember2 = new TsmMember(null);
                tsmMember2.setType(sourceType(member.getType()));
                tsmMember.addSeqElemType(tsmMember2);
            }
            else {
                tsmMember = new TsmMember(member.getName());
                tsmMember.setType(sourceType(member.getType()));
            }
        }
        if (null != tsmMember) {
            String offset = "offsetof(" + cStructName + ", " + member.getName() + ")";
            tsmMember.setOffset(offset);
        }
        return tsmMember;
    }

    /**
     * This function fill the tsm struct adding the necessary information for each member.
     * 
     * @param module of the struct
     * @param cStructName , qdm struct of teh member
     * @param member , Member to be processed
     * @return TsmMember
     */

    /**
     * This function check if the member is a sequence and then add the flags and the necessary information.
     * 
     * @param cTsmMember , member to be evaluated
     * @param cTsmMemberStruct , first member of the tsm struct
     * @param cTsm , tsm struct
     */
    public static void checkIfSeq(TsmMember cTsmMember, TsmMember cTsmMemberStruct, Tsm cTsm)
    {
        if (cTsmMember.getType().equals("CDR_TYPECODE_SEQUENCE")) {
            cTsmMemberStruct.addFlag("TSMFLAG_DYNAMIC");
            for (TsmMember elementType : cTsmMember.getSeqElemType()) {
                if (elementType.getType().equals("CDR_TYPECODE_CSTRING")) {
                    elementType.setSize("0");
                }
                cTsm.addMember(elementType);
            }
        }
    }

    /**
     * This function check if the member is a string and then add the flags and the necessary information.
     * 
     * @param cTsmMember , member to be evaluated
     * @param cTsmMemberStruct , first member of the tsm struct
     */
    public static void checkIfString(TsmMember cTsmMember, TsmMember cTsmMemberStruct)
    {
        if (cTsmMember.getType().equals("CDR_TYPECODE_CSTRING")) {
            cTsmMember.setSize("0");
            cTsmMember.addFlag("TSMFLAG_DYNAMIC");
            cTsmMemberStruct.addFlag("TSMFLAG_DYNAMIC");
        }
    }

    /**
     * This function generate a list of tsms for the given module.
     * 
     * @param moduleName of the module to be parsed
     * @param module to be parsed
     * @return List of tsms
     */
    public List<Tsm> generateTsmList(String moduleName, QdmModule module)
    {
        List<Tsm> tsms = new ArrayList<Tsm>();
        for (QdmEnum qdmEnum : module.getEnums()) {
            CEnum e = new CEnum(module, qdmEnum);
            Tsm tsm = new Tsm(e.getFullName() + "_" + "type");
            TsmMember tsmMemberStruct = generateTsmContainer(module, qdmEnum, qdmEnum.getFullName());
            tsm.addMember(tsmMemberStruct);
            for (ContainerMember member : e.getMembers()) {
                TsmMember tsmMember = new TsmMember(member.getName());
                tsmMember.setLabel(member.getFullName());
                tsm.addMember(tsmMember);
            }

            tsms.add(tsm);
        }
        for (QdmStruct struct : module.getStructs()) {
            String baseName = struct.getName();
            String type = getTypeForTsm(moduleName, baseName);
            // a struct also mean a C TSM
            Tsm cTsm = new Tsm(type);
            cTsm.setBasicName(struct.getName());

            // first entry on the tsm is the tsm scruct itself.
            String cStructName = getNameForTsm(moduleName, baseName);
            TsmMember cTsmMemberStruct = generateTsmContainer(module, struct, cStructName);
            cTsm.addMember(cTsmMemberStruct);

            for (QdmMember member : struct.getMembers()) {

                // fill the intermediate structure for the source template
                TsmMember cTsmMember = generateTypeSource(module, cStructName, member);

                if (member.isKey()) {
                    setKey(cTsmMember, cTsmMemberStruct);
                }

                cTsm.addMember(cTsmMember);

                checkIfSeq(cTsmMember, cTsmMemberStruct, cTsm);
                checkIfString(cTsmMember, cTsmMemberStruct);
            }

            tsms.add(cTsm);
        }
        return tsms;
    }

    /**
     * Get the struct type for the tms, the output depends on the language.
     * 
     * @param moduleName of the module to be parsed
     * @param baseName of the struct
     * @return string , struct type
     */
    abstract String getTypeForTsm(String moduleName, String baseName);

    /**
     * Get the name for the tms, the output depends on the language.
     * 
     * @param moduleName of the module to be parsed
     * @param baseName of the struct
     * @return string , struct name
     */
    abstract String getNameForTsm(String moduleName, String baseName);

}
