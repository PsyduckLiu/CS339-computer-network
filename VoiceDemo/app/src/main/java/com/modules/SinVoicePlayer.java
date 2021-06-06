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

import java.util.ArrayList;
import java.util.List;

import android.util.Log;
import android.media.AudioFormat;
import android.text.TextUtils;

import com.modules.Buffer.BufferData;

public class SinVoicePlayer implements Encoder.Listener, Encoder.Callback, PcmPlayer.Listener, PcmPlayer.Callback {
    private final static String TAG = "SinVoicePlayer";

    private final static int STATE_START = 1;
    private final static int STATE_STOP = 2;
    private final static int STATE_PENDING = 3;

    private final static int DEFAULT_GEN_DURATION = 100;

    private List<Integer> myCodes = new ArrayList<Integer>();

    private Encoder myEncoder;
    private PcmPlayer myPlayer;
    private Buffer myBuffer;

    private int myState;
    private Listener myListener;
    private Thread myPlayThread;
    private Thread myEncodeThread;

    public interface Listener {
        void onPlayStart();
        void onPlayEnd();
    }

    public SinVoicePlayer() {
        this(Common.DEFAULT_SAMPLE_RATE, Common.DEFAULT_BUFFER_SIZE, Common.DEFAULT_BUFFER_COUNT);
    }

    public SinVoicePlayer(int sampleRate, int bufferSize, int buffCount) {
        myState = STATE_STOP;
        myBuffer = new Buffer(buffCount, bufferSize);

        myEncoder = new Encoder(this, sampleRate, Common.Bits16, bufferSize);
        myEncoder.setListener(this);
        myPlayer = new PcmPlayer(this, sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);
        myPlayer.setListener(this);
    }

    public void setListener(Listener listener) {
        myListener = listener;
    }

    private boolean convertTextToCodes(String text) {
        boolean ret = true;

        if (!TextUtils.isEmpty(text)) {
            myCodes.clear();
            myCodes.add(Common.START_TOKEN);
            int len = text.length();
            for (int i = 0; i < len; ++i) {
                char ch = text.charAt(i);
                int index = (int)ch - (int)'0';

                if (index == 0 || index == 1) {
                    myCodes.add(index);
                }
                else {
                    ret = false;
                    Log.d(TAG, "invalidate char:" + ch);
                    break;
                }
            }
            if (ret) {
                myCodes.add(Common.STOP_TOKEN);
            }
        } else {
            ret = false;
        }

        return ret;
    }

    public void play(final String text) {
        if (STATE_STOP == myState && convertTextToCodes(text)) {
            myState = STATE_PENDING;

            myPlayThread = new Thread() {
                @Override
                public void run() {
                    myPlayer.start();
                }
            };
            if (myPlayThread != null) {
                myPlayThread.start();
            }

            myEncodeThread = new Thread() {
                @Override
                public void run() {
                    Log.d(TAG, "encode start");
                    myEncoder.encode(myCodes, DEFAULT_GEN_DURATION/*, muteInterval*/);
                    Log.d(TAG, "encode end");

                    myEncoder.stop();
                    stopPlayer();
                }
            };
            if (myEncodeThread != null) {
                myEncodeThread.start();
            }

            Log.d(TAG, "play");
            myState = STATE_START;
        }
    }

    public void stop() {
        if (STATE_START == myState) {
            myState = STATE_PENDING;

            Log.d(TAG, "force stop start");
            myEncoder.stop();
            if (null != myEncodeThread) {
                try {
                    myEncodeThread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    myEncodeThread = null;
                }
            }

            Log.d(TAG, "force stop end");
        }
    }

    private void stopPlayer() {
        if (myEncoder.isStoped()) {
            myPlayer.stop();
        }

        // put end buffer
        myBuffer.putFull(BufferData.getEmptyBuffer());

        if (null != myPlayThread) {
            try {
                myPlayThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                myPlayThread = null;
            }
        }

        myBuffer.reset();
        myState = STATE_STOP;
    }

    @Override
    public void onStartEncode() {
        Log.d(TAG, "Start Encode ");
    }
    @Override
    public void onEndEncode() {
        Log.d(TAG, "End Encode ");
    }
    @Override
    public BufferData getEncodeBuffer() {
        return myBuffer.getEmpty();
    }
    @Override
    public void freeEncodeBuffer(BufferData buffer) {
        if (null != buffer) {
            myBuffer.putFull(buffer);
        }
    }

    @Override
    public BufferData getPlayBuffer() {
        return myBuffer.getFull();
    }

    @Override
    public void freePlayData(BufferData data) {
        myBuffer.putEmpty(data);
    }

    @Override
    public void onPlayStart() {
        if (null != myListener) {
            myListener.onPlayStart();
        }
    }

    @Override
    public void onPlayStop() {
        if (null != myListener) {
            myListener.onPlayEnd();
        }
    }
}
