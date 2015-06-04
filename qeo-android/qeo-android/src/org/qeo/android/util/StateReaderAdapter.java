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

package org.qeo.android.util;

import java.io.Closeable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.qeo.QeoFactory;
import org.qeo.StateChangeReader;
import org.qeo.StateChangeReaderListener;
import org.qeo.android.QeoAndroid;
import org.qeo.exception.QeoException;
import org.qeo.internal.reflection.ReflectionUtil;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

/**
 * Utility class to easily map a state reader with a listener to an Android adapter.
 * 
 * @param <T> The Qeo data type
 */
public abstract class StateReaderAdapter<T>
        extends BaseAdapter
        implements Closeable
{
    private static final Logger LOG = Logger.getLogger(QeoAndroid.TAG);
    private final List<T> mData = new ArrayList<T>();
    private final Map<String, Integer> mKeys = new HashMap<String, Integer>();
    private final int mViewId;
    private StateChangeReader<T> mReader;

    /**
     * Create the adapter.
     * 
     * @param viewId The id of the layout file that should be inflated
     */
    public StateReaderAdapter(int viewId)
    {
        this.mViewId = viewId;
        this.mReader = null;
    }

    @Override
    public int getCount()
    {
        return mData.size();
    }

    @Override
    public T getItem(int position)
    {
        return mData.get(position);
    }

    @Override
    public long getItemId(int position)
    {
        // return unique id for each row. Just use the position as id.
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        if (convertView == null) {
            final LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            convertView = inflater.inflate(mViewId, parent, false);
        }
        fillData(convertView, getItem(position));
        return convertView;
    }

    /**
     * Initialize the adapter. This will create a {@link StateChangeReader} for the provided class and use its data to
     * populate the adapter.
     * 
     * @param qeo the Qeo factory to be used for creating the reader (factory should be ready)
     * @param clazz the class for which to create the adapter
     * @throws QeoException If an error occurs during initialization
     */
    public void init(QeoFactory qeo, Class<T> clazz)
        throws QeoException
    {
        mReader = qeo.createStateChangeReader(clazz, new StateChangeReaderListener<T>() {
            @Override
            public void onData(T t)
            {
                LOG.fine("Listener new data: " + t);
                addItem(t); // add the item. It will automatically update existing item if the keys match
            }

            @Override
            public void onRemove(T t)
            {
                LOG.fine("Listener removed data: " + t);
                removeItem(t);
            }

            @Override
            public void onNoMoreData()
            {
                // callbacks are on UI thread anyway
                notifyDataSetChanged();
            }
        });
    }

    /**
     * Force update a load, iterate over all items again.
     */
    public void forceLoad()
    {
        // not supported
    }

    /**
     * Close the adapter. This will also close the created listener, not the reader itself
     */
    @Override
    public void close()
    {
        // close the reader
        if (mReader != null) {
            mReader.close();
            mReader = null;
        }
        // invalidate data
        synchronized (mData) {
            mData.clear();
            mKeys.clear();
        }
        notifyDataSetInvalidated();
    }

    private void addItem(T t)
    {
        final String key = getKey(t);
        synchronized (mData) {
            if (mKeys.containsKey(key)) {
                // already in the set, update the item
                final int pos = mKeys.get(key);
                mData.remove(pos);
                mData.add(pos, t);
            }
            else {
                // new element
                mData.add(t);
                mKeys.put(key, mData.size() - 1);
            }
        }
    }

    private void removeItem(T t)
    {
        final String key = getKey(t);
        synchronized (mData) {
            if (mKeys.containsKey(key)) {
                final int pos = mKeys.get(key);
                mData.remove(pos);
                mKeys.remove(key);
            }
            else {
                LOG.warning("Trying to remove item that is not available: " + t);
            }
        }
    }

    private String getKey(T t)
    {
        StringBuffer key = new StringBuffer();
        for (final Field field : t.getClass().getFields()) {
            if (ReflectionUtil.fieldIsKey(field)) {
                try {
                    key.append(field.get(t));
                }
                catch (final RuntimeException e) {
                    LOG.severe("RuntimeException when generating key");
                }
                catch (final IllegalAccessException e) {
                    LOG.severe("IllegalAccessException when generating key");
                }
            }
        }
        return key.toString();
    }

    /**
     * The implementing class should implement this function to draw the data on the view.
     * 
     * @param parentView The id of the inflated parent view.
     * @param t The data object
     */
    public abstract void fillData(View parentView, T t);

}
