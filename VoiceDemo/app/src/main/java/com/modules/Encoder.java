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

import java.util.List;
import android.util.Log;
import com.modules.Buffer.BufferData;

public class Encoder implements SinGenerator.Listener, SinGenerator.Callback {
    private final static String TAG = "Encoder";
    private final static int range  = Common.Frequency.length;
    private final static int encoding = 1;
    private final static int stoped   = 2;
    private int State = 0;

    private SinGenerator mySinGenerator;
    private Listener myListener;
    private Callback myCallback;

    public interface Listener {
        void onStartEncode();
        void onEndEncode();
    }
    public interface Callback {
        void freeEncodeBuffer(BufferData buffer);
        BufferData getEncodeBuffer();
    }

    public Encoder(Callback callback, int sampleRate, int bits, int bufferSize) {
        State = stoped;
        myCallback = callback;
        mySinGenerator = new SinGenerator(this, sampleRate, bits, bufferSize);
        mySinGenerator.setListener(this);
    }

    public void setListener(Listener listener) {
        myListener = listener;
    }

    public final boolean isStoped() {
        return (State == stoped);
    }

    // content of input from 0 to (CODE_FREQUENCY.length-1)
    public void encode(List<Integer> codes, int duration) {
        if (State == stoped) {
            State = encoding;
            myListener.onStartEncode();

            mySinGenerator.start();
            for (int element : codes) {
                if (State == encoding) {
                    Log.d(TAG, "encode:" + element);

                    if (element >= 0 && element < range) {
                        mySinGenerator.genData(Common.Frequency[element], duration);
                        mySinGenerator.genData(Common.Frequency[3], duration/5);
                    }
                    else {
                        Log.e(TAG, element + "Index error");
                    }
                }
                else {
                    Log.d(TAG, "Encode force stop");
                    break;
                }
            }

            stop();
            myListener.onEndEncode();
        }
    }

    public void stop() {
        if (State == encoding) {
            State = stoped;
            mySinGenerator.stop();
        }
    }

    @Override
    public void onStartGen() {
        Log.d(TAG, "Start generate");
    }
    @Override
    public void onStopGen() {
        Log.d(TAG, "End generate");
    }

    @Override
    public BufferData getGenBuffer() {
        return myCallback.getEncodeBuffer();
    }
    @Override
    public void freeGenBuffer(BufferData buffer) {
        myCallback.freeEncodeBuffer(buffer);
    }
}
