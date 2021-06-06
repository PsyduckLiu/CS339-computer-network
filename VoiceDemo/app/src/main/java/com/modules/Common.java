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

public class Common {
    public final static int maxRange = 94;
    public final static int defaultCount = 5;

    public final static int Bits16 = 32768;
    // 0 1 start insert end
    // sampling point Count 25, 22, 19, 15, 10
    public final static int[] Frequency = { 1764, 2004, 2321, 2940, 4410 };

    public final static int START_TOKEN =2;
    public final static int STOP_TOKEN = 4;

    public final static int DEFAULT_BUFFER_SIZE = 4096;
    public final static int DEFAULT_BUFFER_COUNT = 3;
    public final static int DEFAULT_SAMPLE_RATE = 44100;

    public static int ACK = 0;
    public static int SEND = 0;
}
