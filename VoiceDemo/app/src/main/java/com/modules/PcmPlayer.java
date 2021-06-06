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

import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

import com.modules.Buffer.BufferData;

public class PcmPlayer {
    private final static String TAG = "PcmPlayer";
    private final static int STATE_START = 1;
    private final static int STATE_STOP = 2;

    private int myState;
    private AudioTrack myAudio;
    private long myPlayedLen;
    private Listener myListener;
    private Callback myCallback;

    public interface Listener {
        void onPlayStart();
        void onPlayStop();
    }

    public interface Callback {
        BufferData getPlayBuffer();
        void freePlayData(BufferData data);
    }

    public PcmPlayer(Callback callback, int sampleRate, int channel, int format, int bufferSize) {
        myPlayedLen = 0;
        myCallback = callback;
        myState = STATE_STOP;
        myAudio = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, channel, format, bufferSize, AudioTrack.MODE_STREAM);
    }

    public void setListener(Listener listener) {
        myListener = listener;
    }

    public void start() {
        Log.d(TAG, "start");

        if (STATE_STOP == myState && myAudio != null) {
            myPlayedLen = 0;

            if (myCallback != null) {
                myState = STATE_START;
                Log.d(TAG, "start");
                if (myListener != null) {
                    myListener.onPlayStart();
                }
                while (STATE_START == myState) {
                    Log.d(TAG, "start getbuffer");

                    BufferData data = myCallback.getPlayBuffer();
                    if (null != data) {
                        if (null != data.myData) {
                            int len = myAudio.write(data.myData, 0, data.getFilledSize());

                            if (0 == myPlayedLen) {
                                myAudio.play();
                            }
                            myPlayedLen += len;
                            myCallback.freePlayData(data);
                        }
                        else {
                            // it is the end of input, so need stop
                            Log.d(TAG, "it is the end of input, so need stop");
                            break;
                        }
                    }
                    else {
                        Log.e(TAG, "get null data");
                        break;
                    }
                }

                if (myAudio != null) {
                    myAudio.pause();
                    myAudio.flush();
                    myAudio.stop();
                }
                myState = STATE_STOP;
                if (myListener != null) {
                    myListener.onPlayStop();
                }
                Log.d(TAG, "end");
            }
        }
    }

    public void stop() {
        if (STATE_START == myState && myAudio != null) {
            myState = STATE_STOP;
        }
    }
}
