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

package com.technicolor.qeo.codegen.type.c;

import java.util.Locale;

import com.technicolor.qeo.codegen.type.ContainerMember;
import com.technicolor.qeo.codegen.type.ContainerMemberAdapter;
import com.technicolor.qeo.codegen.type.qdm.QdmEnumerator;

/**
 * Representation of an enumerator constant as needed by the C templates.
 */
public class CEnumerator
    extends ContainerMember
    implements ContainerMemberAdapter<QdmEnumerator, CEnumerator>
{
    private final CEnum mEnum;
    private QdmEnumerator mEnumerator = null;
    private String mFullName = null;

    /**
     * Create an undefined C enumerator as part of the given enum.
     * 
     * @param e Containing enum.
     */
    protected CEnumerator(CEnum e)
    {
        super(null);
        mEnum = e;
    }

    /**
     * Create a C enumerator from the provided QDM enumerator which is part of the given enum.
     * 
     * @param e Containing enum.
     * @param enumerator QDM enumerator value.
     */
    public CEnumerator(CEnum e, QdmEnumerator enumerator)
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
    public CEnumerator wrap(QdmEnumerator member)
    {
        return new CEnumerator(mEnum, member);
    }
}
