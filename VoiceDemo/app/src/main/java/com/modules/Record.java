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

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import com.modules.Buffer.BufferData;

public class Record {
    private final static String TAG = "Record";
    private final static int START = 1;
    private final static int STOP = 2;

    private int myState;
    private int myFreq;
    private int myBufferSize;
    private int myChannelConfig;
    private int myAudioEncoding;

    private Listener myListener;
    private Callback myCallback;

    public interface Listener {
        void onStartRecord();
        void onStopRecord();
    }

    public interface Callback {
        BufferData getRecordBuffer();
        void freeRecordBuffer(BufferData buffer);
    }

    public Record(Callback callback) {
        myState = STOP;

        myCallback = callback;
        myFreq = Common.DEFAULT_SAMPLE_RATE;
        myBufferSize = Common.DEFAULT_BUFFER_SIZE;
    }

    public void setListener(Listener listener) {
        myListener = listener;
    }

    public void start() {
        if (myState == STOP) {
            myChannelConfig = AudioFormat.CHANNEL_IN_MONO;
            myAudioEncoding = AudioFormat.ENCODING_PCM_16BIT;

            int minBufferSize = AudioRecord.getMinBufferSize(myFreq, myChannelConfig, myAudioEncoding);
            Log.d(TAG, "minBufferSize:" + minBufferSize);

            if (myBufferSize >= minBufferSize) {
                AudioRecord record = new AudioRecord(MediaRecorder.AudioSource.MIC, myFreq, myChannelConfig, myAudioEncoding, myBufferSize);
                if (record != null) {
                    try {
                        myState = START;
                        record.startRecording();
                        Log.d(TAG, "record start");
                        myListener.onStartRecord();

                        while (myState == START) {
                            BufferData data = myCallback.getRecordBuffer();
                            if (data != null) {
                                if (data.myData != null) {
                                    int bufferReadResult = record.read(data.myData, 0, myBufferSize);
                                    data.setFilledSize(bufferReadResult);
                                    myCallback.freeRecordBuffer(data);
                                }
                                else {
                                    Log.d(TAG, "get end input data, so stop");
                                    break;
                                }
                            }
                            else {
                                Log.e(TAG, "get null data");
                                break;
                            }
                        }

                        myListener.onStopRecord();

                        record.stop();
                        record.release();

                        Log.d(TAG, "record stop");
                    }
                    catch ( IllegalStateException e) {
                        e.printStackTrace();
                        Log.e(TAG, "start record error");
                    }
                    myState = STOP;
                }
            }
            else {
                Log.e(TAG, "bufferSize is too small");
            }
        }
    }

    public void stop() {
        if (myState == START) {
            myState = STOP;
        }
    }
}
