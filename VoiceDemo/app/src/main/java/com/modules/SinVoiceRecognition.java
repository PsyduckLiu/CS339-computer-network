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

import java.util.Timer;
import java.util.TimerTask;

public class SinVoiceRecognition implements Record.Listener, Record.Callback, VoiceRecognition.Listener, VoiceRecognition.Callback {
    private final static String TAG = "SinVoiceRecognition";

    private final static int START = 1;
    private final static int STOP = 2;
    private final static int PEND = 3;
    private int myState;
    private int end = 0;

    private Buffer myBuffer;
    private Record myRecord;
    private VoiceRecognition myRecognition;
    private SinVoicePlayer mySinVoicePlayer;

    private Thread myRecordThread;
    private Thread myRecognitionThread;
    private Listener myListener;


    private int[] myChar = new int[8];
    private StringBuilder myTmpStr = new StringBuilder();
    private StringBuilder myFinalStr = new StringBuilder();
    private CRCChecker myCRCChecker = new CRCChecker();

    public interface Listener {
        void onRecognitionStart();
        void onRecognition(char ch);
        void onRecognitionEnd();
    }

    public SinVoiceRecognition() {
        myState = STOP;
        myBuffer = new Buffer(Common.DEFAULT_BUFFER_COUNT, Common.DEFAULT_BUFFER_SIZE);

        myRecord = new Record(this);
        myRecord.setListener(this);
        myRecognition = new VoiceRecognition(this);
        myRecognition.setListener(this);
        mySinVoicePlayer = new SinVoicePlayer();
    }

    public void setListener(Listener listener) {
        myListener = listener;
    }

    public void start() {
        if (myState == STOP) {
            myState = PEND;

            myRecognitionThread = new Thread() {
                @Override
                public void run() {
                    myRecognition.start();
                }
            };
            myRecognitionThread.start();

            myRecordThread = new Thread() {
                @Override
                public void run() {
                    myRecord.start();

                    Log.d(TAG, "record thread end");
                    Log.d(TAG, "stop recognition start");
                    stopRecognition();
                    Log.d(TAG, "stop recognition end");
                }
            };
            myRecordThread.start();

            myState = START;
        }
    }

    private void stopRecognition() {
        myRecognition.stop();

        // put end buffer
        BufferData data = new BufferData(0);
        myBuffer.putFull(data);

        if (null != myRecognitionThread) {
            try {
                myRecognitionThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                myRecognitionThread = null;
            }
        }

        myBuffer.reset();
    }

    public void stop() {
        if (myState == START) {
            myState = PEND;

            Log.d(TAG, "force stop start");
            myRecord.stop();
            if (null != myRecordThread) {
                try {
                    myRecordThread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    myRecordThread = null;
                }
            }

            myState = STOP;
            Log.d(TAG, "force stop end");
        }
    }

    @Override
    public void onStartRecord() {
        Log.d(TAG, "start record");
    }
    @Override
    public void onStopRecord() {
        Log.d(TAG, "stop record");
    }

    @Override
    public BufferData getRecordBuffer() {
        BufferData buffer = myBuffer.getEmpty();
        if (null == buffer) {
            Log.e(TAG, "get null empty buffer");
        }
        return buffer;
    }
    @Override
    public void freeRecordBuffer(BufferData buffer) {
        if (null != buffer) {
            if (!myBuffer.putFull(buffer)) {
                Log.e(TAG, "put full buffer failed");
            }
        }
    }

    @Override
    public BufferData getRecognitionBuffer() {
        BufferData buffer = myBuffer.getFull();
        if (null == buffer) {
            Log.e(TAG, "get null full buffer");
        }
        return buffer;
    }
    @Override
    public void freeRecognitionBuffer(BufferData buffer) {
        if (null != buffer) {
            if (!myBuffer.putEmpty(buffer)) {
                Log.e(TAG, "put empty buffer failed");
            }
        }
    }

    @Override
    public void onStartRecognition() {
        Log.d(TAG, "start recognition");
    }
    @Override
    public void onRecognition(int index) {
        Log.d(TAG, "recognition:" + index);
        if (null != myListener) {
            if (Common.START_TOKEN == index) {
                end = 1;
                myTmpStr.setLength(0);
                myFinalStr.setLength(0);
                myListener.onRecognitionStart();
            }
            if (Common.STOP_TOKEN == index && end == 1) {
                end = 0;

                String message = "CRC Right!";
                String text = myTmpStr.toString();
                boolean result = myCRCChecker.CheckCRCCode(text);

                int length = text.length();
                int num = length/8;

                for (int i = 0;i < num;i++) {
                    for (int j = 0;j < 8;j++) {
                        int pos = 8*i+j;
                        myChar[j] = (int)(text.charAt(pos)) - (int)('0');
                    }
                    int mCharASCII = 0;
                    for (int k = 0; k < 8; k++) {
                        mCharASCII += myChar[k] * Math.pow(2, 7 - k);
                    }
                    char aChar = (char)mCharASCII;
                    myFinalStr.append(aChar);
                }

                if (myFinalStr.toString().equals("ACK")) {
                    Common.ACK =1;
                }

                if (!result) {
                    message = "CRC Wrong!";
                }
                else {
                    if (!myFinalStr.toString().equals("ACK") && Common.SEND == 0)  {
                    TimerTask task = new TimerTask() {
                        @Override
                        public void run() {
                            GenBinaryText myGenBinaryText = new GenBinaryText();
                            String initText = "ACK";
                            String binaryText = myGenBinaryText.convertTextToBinary(initText);
                            mySinVoicePlayer.play(binaryText);
                        }
//                        }

                    };
                    Timer myTimer = new Timer();
                    myTimer.schedule(task, 2000);
                }
                }

                myFinalStr.append(message);

                Log.d(TAG,myFinalStr.toString());

                for(int i = 0; i < myFinalStr.length(); ++i) {
                    myListener.onRecognition(myFinalStr.charAt(i));
                }

                myTmpStr.setLength(0);
                myFinalStr.setLength(0);
                myListener.onRecognitionEnd();
            }
            if (index == 0 || index == 1) {
                myTmpStr.append((char)(index + '0'));
            }
        }
    }

    @Override
    public void onStopRecognition() {
        Log.d(TAG, "stop recognition");
    }
}
