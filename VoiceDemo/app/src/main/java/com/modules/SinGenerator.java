/*
 * Copyright (C) 2013 gujicheng
 * 
 * Licensed under the GPL License Version 2.0;
 * you may not use this file except in compliance with the License.
 * 
 * If you have any question, please contact me.
 * 
 *************************************************************************
 **                   Author information                                **
 *************************************************************************
 ** Email: gujicheng197@126.com                                         **
 ** QQ   : 29600731                                                     **
 ** Weibo: http://weibo.com/gujicheng197                                **
 *************************************************************************
 */
package com.modules;

import android.util.Log;
import com.modules.Buffer.BufferData;

public class SinGenerator {
    private static final String TAG = "SinGenerator";

    private static final int start = 1;
    private static final int stop = 2;
    private int State = 0;

    private int SampleRate;
    private int StoredBits;
    private int Duration;
    private int GenRate;
    private int FilledSize;
    private int BufferSize;

    private Listener myListener;
    private Callback myCallback;

    public interface Listener {
        void onStartGen();
        void onStopGen();
    }
    public interface Callback {
        BufferData getGenBuffer();
        void freeGenBuffer(BufferData buffer);
    }

    public SinGenerator(Callback callback, int sampleRate, int bits, int bufferSize) {
        State = stop;
        SampleRate = sampleRate;
        StoredBits = bits;
        Duration = 0;
        FilledSize = 0;
        BufferSize = bufferSize;

        myCallback = callback;
    }

    public void setListener(Listener listener) {
        myListener = listener;
    }

    public void start() {
        if (State == stop) {
            State = start;
        }
    }
    public void stop() {
        if (State == start) {
            State = stop;
        }
    }

    public void genData(int genRate, int duration) {
        if (State == start) {
            myListener.onStartGen();
            Log.d(TAG, "genRate:" + genRate);

            GenRate = genRate;
            Duration = duration;
            int n = StoredBits / 2;
            int totalCount = (Duration * SampleRate) / 1000;
            double step = (GenRate / (double) SampleRate) * 2 * Math.PI;
            double d = 0;
            FilledSize = 0;
            // Get a buffer from the ProducerQueue
            BufferData buffer = myCallback.getGenBuffer();
            if (buffer != null) {
                for (int i = 0; i < totalCount; ++i) {
                    if (State == start) {
                        int out = (int) (Math.sin(d) * n) + 128;
                        // Put the current full buffer in the ConsumeQueue
                        if (FilledSize >= BufferSize - 1) {
                            buffer.setFilledSize(FilledSize);
                            myCallback.freeGenBuffer(buffer);

                            FilledSize = 0;
                            buffer = myCallback.getGenBuffer();

                            if (buffer == null) {
                                Log.e(TAG, "Get a null buffer");
                                break;
                            }
                        }

                        buffer.myData[FilledSize++] = (byte) (out & 0xff);
                        buffer.myData[FilledSize++] = (byte) ((out >> 8) & 0xff);
                        d += step;
                    }
                    else {
                        Log.d(TAG, "Gen force stop");
                        break;
                    }
                }
            }
            else {
                Log.e(TAG, "Get a null buffer");
            }

            //Put the last buffer in the ConsumeQueue
            if (buffer != null) {
                buffer.setFilledSize(FilledSize);
                myCallback.freeGenBuffer(buffer);
            }
            FilledSize = 0;

            myListener.onStopGen();
        }
    }
}
