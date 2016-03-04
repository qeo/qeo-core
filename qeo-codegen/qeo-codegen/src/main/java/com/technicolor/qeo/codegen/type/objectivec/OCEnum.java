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

package com.technicolor.qeo.codegen.type.objectivec;

import com.technicolor.qeo.codegen.Convert;
import com.technicolor.qeo.codegen.type.Container;
import com.technicolor.qeo.codegen.type.ContainerIterableAdapter;
import com.technicolor.qeo.codegen.type.qdm.QdmEnum;
import com.technicolor.qeo.codegen.type.qdm.QdmEnumerator;
import com.technicolor.qeo.codegen.type.qdm.QdmModule;

/**
 * Representation of an enumerated type as needed by the Objective-C templates.
 */
public class OCEnum
    extends Container<OCEnumerator>
{
    private final QdmEnum mEnum;
    private final QdmModule mModule;

    /**
     * Create an Objective-C enum from the provided QDM enum which is part of the given module.
     * 
     * @param module Containing module.
     * @param e QDM enumeration.
     */
    public OCEnum(QdmModule module, QdmEnum e)
    {
        super(e.getName());
        mModule = module;
        mEnum = e;
        mFullName = Convert.qdmToC(mModule.getName()) + "_" + mEnum.getName();
    }

    @Override
    public String getDoc()
    {
        return mEnum.getDoc();
    }

    @Override
    public Iterable<OCEnumerator> getMembers()
    {
        return new ContainerIterableAdapter<QdmEnumerator, OCEnumerator>(mEnum.getMembers(), new OCEnumerator(this));
    }
}
