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

package org.qeo.sample.gauge.android.reader.ui;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.model.TimeSeries;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;
import org.qeo.sample.gauge.android.reader.GaugeReaderApplication;
import org.qeo.sample.gauge.android.reader.R;
import org.qeo.sample.gauge.android.reader.helper.IfaceGraphDetailsHelper;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * 
 * A layout class to display the graphical representation of the traffic statistics for the selected interface.
 * 
 */
public class SpeedGraphView
        extends LinearLayout
{
    private final int mMaxValueX = 100;
    private final int mMaxValueY = 1000;

    private final XYMultipleSeriesRenderer mRenderer;
    private final GraphicalView mGraphView;
    private TimeSeries mDownstreamSeries;
    private TimeSeries mUpstreamSeries;
    private int mCount;
    private int mCurrentValueX = mMaxValueY;
    private final TextView mIfnameTextView;
    private final TextView mSpeedTextView;
    private final ImageView mIfDetailsImageView;
    private final IfaceGraphDetailsHelper mIfaceDetails;
    private int mClickedState;

    /**
     * Comstructs a SpeedGraphView.
     *
     * @param context Activity context
     * @param interfaceName interface name
     */
    public SpeedGraphView(Context context, final String interfaceName)
    {
        super(context);
        mIfaceDetails = new IfaceGraphDetailsHelper();
        this.setOrientation(LinearLayout.VERTICAL);
        mDownstreamSeries = new TimeSeries("Downstream");
        mUpstreamSeries = new TimeSeries("Upstream");
        XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.component_speedgraph, this);

        final LinearLayout speedGraphParent = (LinearLayout) this.findViewById(R.id.speedGraph);
        mIfnameTextView = (TextView) this.findViewById(R.id.speedGraph_intfName);
        mIfnameTextView.setText(interfaceName);

        mSpeedTextView = (TextView) this.findViewById(R.id.speedGraph_speed);
        mSpeedTextView.setText("In: 0 kbs - Out: 0 kbs");

        mIfDetailsImageView = (ImageView) this.findViewById(R.id.speedGraph_intfDetails);

        if (GaugeReaderApplication.sIfaceGraphHelperMap.get(interfaceName) != null) {

            mClickedState = GaugeReaderApplication.sIfaceGraphHelperMap.get(interfaceName).getGraphClickState();
            mCount = GaugeReaderApplication.sIfaceGraphHelperMap.get(interfaceName).getCount();
        }
        if (GaugeReaderApplication.sCurrentOrientation != GaugeReaderApplication.sLastOrientation) {

            if (GaugeReaderApplication.sIfaceGraphHelperMap.get(interfaceName) != null) {

                if (GaugeReaderApplication.sIfaceGraphHelperMap.get(interfaceName).getGraphClickState() == 1) {
                    speedGraphParent.setVisibility(View.VISIBLE);
                    mIfDetailsImageView.setRotation(-90);
                }
                else {
                    speedGraphParent.setVisibility(View.GONE);
                    mIfDetailsImageView.setRotation(0);
                }

                mDownstreamSeries =
                        GaugeReaderApplication.sIfaceGraphHelperMap.get(interfaceName).getCurrentdownstreamSeries();
                mUpstreamSeries =
                        GaugeReaderApplication.sIfaceGraphHelperMap.get(interfaceName).getCurrentupstreamSeries();
            }
        }

        dataset.addSeries(mDownstreamSeries);
        dataset.addSeries(mUpstreamSeries);

        mIfDetailsImageView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v)
            {
                if (speedGraphParent.getVisibility() == View.GONE) {
                    mClickedState = 1;
                    speedGraphParent.setVisibility(View.VISIBLE);
                    v.setRotation(-90);

                }
                else {
                    mClickedState = 0;
                    speedGraphParent.setVisibility(View.GONE);
                    v.setRotation(0);
                }

            }
        });

        mRenderer = new XYMultipleSeriesRenderer();

        mRenderer.setYAxisMin(0);
        mRenderer.setXAxisMin(0);
        mRenderer.setXAxisMax(mMaxValueX);
        mRenderer.setYAxisMax(mMaxValueY);
        mRenderer.setPanEnabled(false, false);
        mRenderer.setZoomRate(0.0f);
        mRenderer.setZoomEnabled(false, false);

        XYSeriesRenderer downstreamRenderer = new XYSeriesRenderer();
        downstreamRenderer.setLineWidth(5.0f);
        downstreamRenderer.setColor(Color.WHITE);

        XYSeriesRenderer upstreamRenderer = new XYSeriesRenderer();
        upstreamRenderer.setLineWidth(5.0f);
        upstreamRenderer.setColor(Color.RED);

        mRenderer.addSeriesRenderer(downstreamRenderer);
        mRenderer.addSeriesRenderer(upstreamRenderer);

        mGraphView = ChartFactory.getLineChartView(context, dataset, mRenderer);
        mGraphView.setMinimumHeight(400);

        mGraphView.setBackgroundColor(Color.BLACK);

        ViewGroup.LayoutParams params =
                new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        mGraphView.setLayoutParams(params);

        speedGraphParent.addView(mGraphView, params);
        speedGraphParent.setBackgroundColor(Color.BLACK);
    }

    /**
     * Adds the new data to the speedGraph displayed.
     * 
     * @param downstream received data in kilobytes
     * @param upstream transferred data in kilobytes
     */
    public void addNewSpeedData(int downstream, int upstream)
    {
        mDownstreamSeries.add(mCount, downstream);
        mUpstreamSeries.add(mCount, upstream);

        mIfaceDetails.setGraphClickState(mClickedState);
        mIfaceDetails.setCurrentdownstreamSeries(mDownstreamSeries);
        mIfaceDetails.setCurrentupstreamSeries(mUpstreamSeries);
        mCount++;
        mIfaceDetails.setCount(mCount);
        mSpeedTextView.setText("In: " + downstream + " kbs - Out: " + upstream + " kbs");

        if (mCount > mMaxValueX) {
            mDownstreamSeries.remove(0);
            mRenderer.setRange(new double[] {mCount - mMaxValueX, mCount, 0, mCurrentValueX});
        }
        // Adjust the Y-Axis when mMaxValueY has been reached
        if (downstream > mMaxValueY && downstream > mCurrentValueX) {
            mCurrentValueX = downstream;
            mRenderer.setYAxisMax(mCurrentValueX);

        }
        if (upstream > mMaxValueY && upstream > mCurrentValueX) {
            mCurrentValueX = upstream;
            mRenderer.setYAxisMax(mCurrentValueX);

        }

        mGraphView.refreshDrawableState();
        mGraphView.repaint();
    }

    /**
     * Getter method for details of clicked interface state.
     * 
     * @return returns object containing details for currently selected and clicked interface.
     */
    public IfaceGraphDetailsHelper getIfaceCurerntDetails()
    {

        return mIfaceDetails;
    }

}
