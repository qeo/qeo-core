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

package com.technicolor.qeo.codegen.type.objectivec;

import java.util.Locale;

import com.technicolor.qeo.codegen.type.ContainerMember;
import com.technicolor.qeo.codegen.type.ContainerMemberAdapter;
import com.technicolor.qeo.codegen.type.qdm.QdmEnumerator;

/**
 * Representation of an enumerator constant as needed by the Objective-C templates.
 */
public class OCEnumerator
    extends ContainerMember
    implements ContainerMemberAdapter<QdmEnumerator, OCEnumerator>
{
    private final OCEnum mEnum;
    private QdmEnumerator mEnumerator = null;
    private String mFullName = null;

    /**
     * Create an undefined Objective-C enumerator as part of the given enum.
     * 
     * @param e Containing enum.
     */
    protected OCEnumerator(OCEnum e)
    {
        super(null);
        mEnum = e;
    }

    /**
     * Create a Objective-C enumerator from the provided QDM enumerator which is part of the given enum.
     * 
     * @param e Containing enum.
     * @param enumerator QDM enumerator value.
     */
    public OCEnumerator(OCEnum e, QdmEnumerator enumerator)
    {
        super(enumerator.getName());
        mEnum = e;
        mEnumerator = enumerator;
    }

    @Override
    public synchronized String getFullName()
    {
        if (null == mFullName) {
            mFullName = mEnum.getFullName() + "_" + mEnumerator.getName();
            mFullName = mFullName.toUpperCase(Locale.ENGLISH);
        }
        return mFullName;
    }

    @Override
    public String getDoc()
    {
        return mEnumerator.getDoc();
    }

    @Override
    public OCEnumerator wrap(QdmEnumerator member)
    {
        return new OCEnumerator(mEnum, member);
    }
}
