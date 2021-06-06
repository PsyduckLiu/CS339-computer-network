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

public class VoiceRecognition {
    private final static String TAG = "Recognition";

    private final static int START = 1;
    private final static int STOP = 2;
    private final static int STEP1 = 1;
    private final static int STEP2 = 2;

    private final static int START_INDEX = 2;
    private final static int INTERVAL_INDEX = 3;
    private final static int MIN_CONFIRM_CIRCLE = 10;
    private int myState;

    private Listener myListener;
    private Callback myCallback;

    private int mySamplingPointCount = 0;
    private boolean myIsStartCounting;
    private int myStep;
    private boolean myIsBeginning;
    private boolean myStartingDet;
    private int myStartingDetCount;

    private int myRegIndex;
    private int myRegCount;
    private int myPreRegIndex;
    private boolean myIsRegStart;

    public interface Listener {
        void onStartRecognition();
        void onRecognition(int index);
        void onStopRecognition();
    }

    public interface Callback {
        BufferData getRecognitionBuffer();
        void freeRecognitionBuffer(BufferData buffer);
    }

    public VoiceRecognition(Callback callback) {
        myState = STOP;
        myCallback = callback;
    }

    public void setListener(Listener listener) {
        myListener = listener;
    }

    public void start() {
        if (myState == STOP) {

            if (myCallback != null) {
                myState = START;
                mySamplingPointCount = 0;

                myIsStartCounting = false;
                myStep = STEP1;
                myIsBeginning = false;
                myStartingDet = false;
                myStartingDetCount = 0;
                myPreRegIndex = -1;
                myIsRegStart = false;

                myListener.onStartRecognition();

                while (myState == START) {
                    BufferData data = myCallback.getRecognitionBuffer();
                    if (data != null) {
                        if (data.myData != null) {
                            process(data);

                            myCallback.freeRecognitionBuffer(data);
                        }
                        else {
                            Log.d(TAG, "end input buffer, so stop");
                            break;
                        }
                    }
                    else {
                        Log.e(TAG, "get null recognition buffer");
                        break;
                    }
                }

                myState = STOP;
                myListener.onStopRecognition();
            }
        }
    }

    public void stop() {
        if (myState == START) {
            myState = STOP;
        }
    }

    private void process(BufferData data) {
        int size = data.getFilledSize() - 1;
        short sh = 0;
        for (int i = 0; i < size; i++) {
            short sh1 = data.myData[i];
            sh1 &= 0xff;
            short sh2 = data.myData[++i];
            sh2 <<= 8;
            sh = (short) ((sh1) | (sh2));

            if (!myIsStartCounting) {
                if (STEP1 == myStep) {
                    if (sh < 0) {
                        myStep = STEP2;
                    }
                }
                else if (STEP2 == myStep) {
                    if (sh > 0) {
                        myIsStartCounting = true;
                        mySamplingPointCount = 0;
                        myStep = STEP1;
                    }
                }
            }
            else {
                ++mySamplingPointCount;
                if (STEP1 == myStep) {
                    if (sh < 0) {
                        myStep = STEP2;
                    }
                }
                else if (STEP2 == myStep) {
                    if (sh > 0) {
                        // preprocess the circle recognise voice
                        reg(mySamplingPointCount);

                        mySamplingPointCount = 0;
                        myStep = STEP1;
                    }
                }
            }
        }
    }

    private void reg(int samplingPointCount) {
        if (!myIsBeginning) {
            if (!myStartingDet) {
                if (getIndex(samplingPointCount) == START_INDEX) {
                    myStartingDet = true;
                    myStartingDetCount = 0;
                }
            }
            else {
                if (getIndex(samplingPointCount) == START_INDEX) {
                    ++myStartingDetCount;

                    if (myStartingDetCount >= MIN_CONFIRM_CIRCLE) {
                        myIsBeginning = true;
                        myIsRegStart = false;
                        myRegCount = 0;
                    }
                }
                else {
                    myStartingDet = false;
                }
            }
        }
        else {
            if (!myIsRegStart) {
                if (samplingPointCount > 0) {
                    //myRegValue = samplingPointCount;
                    myRegIndex = getIndex(samplingPointCount);
                    myIsRegStart = true;
                    myRegCount = 1;
                }
            }
            else {
                if (myRegIndex == getIndex(samplingPointCount)) {
                    ++myRegCount;

                    if (myRegCount >= MIN_CONFIRM_CIRCLE) {
                        if (myRegIndex != myPreRegIndex) {
                            if (null != myListener) {
                                if(myRegIndex != INTERVAL_INDEX) {
                                    myListener.onRecognition(myRegIndex);
                                }
                            }
                            myPreRegIndex = myRegIndex;
                        }

                        myIsRegStart = false;
                    }
                }
                else {
                    myIsRegStart = false;
                }
            }
        }
    }

    private int getIndex(int samplingPointCount) {
        switch (samplingPointCount) {
            case 8:
            case 9:
            case 10:
            case 11:
            case 12:
                return 4;

            case 13:
            case 14:
            case 15:
            case 16:
            case 17:
                return 3;

            case 18:
            case 19:
            case 20:
                return 2;

            case 21:
            case 22:
            case 23:
                return 1;

            case 24:
            case 25:
            case 26:
                return 0;

            default:
                return -1;
        }
    }
}
