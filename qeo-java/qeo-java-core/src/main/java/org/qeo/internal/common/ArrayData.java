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

package org.qeo.internal.common;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Representation of (complex) array Data consumed and produced throughout Qeo.
 * 
 * This representation is reflection and DDS agnostic
 * 
 * The ArrayData class contains an array of Data objects.
 */
public class ArrayData
    extends Data
{
    private static final Logger LOG = Logger.getLogger(ArrayData.class.getName());

    /**
     * ArrayList containing all the data elements of this ArrayData instance.
     */
    private final ArrayList<Data> mElements;

    /**
     * Construct a new array data instance. Members will be added later.
     * 
     * @param id Id of the object
     */
    public ArrayData(int id)
    {
        super(id);
        this.mElements = new ArrayList<Data>();
    }

    /**
     * Copy constructor.
     * 
     * @param data the object to cpoy from
     */
    public ArrayData(ArrayData data)
    {
        super(data.getId());
        this.mElements = data.mElements;
    }

    /**
     * Add a element to this array data object.
     * 
     * @param element element to add
     */
    public void addElement(Data element)
    {
        this.mElements.add(element);
    }

    /**
     * Retrieve an element of the array list that has this id.
     * 
     * @param id The id of the data object to search for
     * @return The data object or null in case not found
     */
    public Data getElement(int id)
    {
        return mElements.get(id);
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append("ArrayData: ");
        for (Data data : mElements) {
            if (LOG.isLoggable(Level.FINEST)) {
                // this is a very expensive operation, only do this at the finest level
                sb.append(data.toString());
            }
            else {
                sb.append("Size: ").append(mElements.size());
            }
        }
        return sb.toString();
    }

    /**
     * Get the size of the array.
     * 
     * @return the number of elements.
     */
    public int size()
    {
        return mElements.size();
    }

    /**
     * Get the elements of the array.
     * 
     * @return The elements or null if none.
     */
    public Iterable<Data> getElements()
    {
        return mElements;
    }

    /**
     * Construct and object from a data sample.
     * 
     * @param <T> The class of the Qeo data type.
     * @param clazz The class of the object.
     * @param type The type of the data sample.
     * 
     * @return The constructed object.
     */
    @Override
    public <T> Object toObject(Class<T> clazz, Type type)
    {
        Object array = Array.newInstance(clazz.getComponentType(), size());
        Type elementType = ((ArrayType) type).getElementType();
        for (int i = 0; i < size(); i++) {
            Array.set(array, i, getElement(i).toObject(clazz.getComponentType(), elementType));
        }
        return array;
    }
}
