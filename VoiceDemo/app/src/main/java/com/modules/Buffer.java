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

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import android.util.Log;

public class Buffer {
    private final static String TAG = "Buffer";

    private BlockingQueue<BufferData> myProducerQueue;
    private BlockingQueue<BufferData> myConsumeQueue;
    private int myBufferCount;
    private int myBufferSize;

    // when myData is null, means it is end of input
    public static class BufferData {
        public byte myData[];
        private int myFilledSize;
        private int myMaxBufferSize;
        private static BufferData s_EmptyBuffer = new BufferData(0);

        public BufferData(int maxBufferSize) {
            myMaxBufferSize = maxBufferSize;
            reset();

            if (maxBufferSize > 0) {
                myMaxBufferSize = maxBufferSize;
                myData = new byte[myMaxBufferSize];
            } else {
                myData = null;
            }
        }

        public static BufferData getEmptyBuffer() {
            return s_EmptyBuffer;
        }

        final public void reset() {
            myFilledSize = 0;
        }

        final public int getMaxBufferSize() {
            return myMaxBufferSize;
        }

        final public void setFilledSize(int size) {
            myFilledSize = size;
        }

        final public int getFilledSize() {
            return myFilledSize;
        }
    }

    public Buffer(int bufferCount, int bufferSize) {
        myBufferSize = bufferSize;
        myBufferCount = bufferCount;
        myProducerQueue = new LinkedBlockingQueue<BufferData>(myBufferCount);
        // we want to put the end buffer, so need to add 1
        myConsumeQueue = new LinkedBlockingQueue<BufferData>(myBufferCount + 1);

        try {
            for (int i = 0; i < myBufferCount; ++i) {
                myProducerQueue.put(new BufferData(myBufferSize));
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            Log.e(TAG, "put buffer data error");
        }
    }

    public void reset() {
        int size = myProducerQueue.size();
        for (int i = 0; i < size; ++i) {
            BufferData data = myProducerQueue.peek();
            if (null == data || null == data.myData) {
                myProducerQueue.poll();
            }
        }

        size = myConsumeQueue.size();
        for (int i = 0; i < size; ++i) {
            BufferData data = myConsumeQueue.poll();
            if (null != data && null != data.myData) {
                myProducerQueue.add(data);
            }
        }

        Log.d(TAG, "reset ProducerQueue Size:" + myProducerQueue.size() + "    ConsumeQueue Size:" + myConsumeQueue.size());
    }

    public BufferData getEmpty() {
        return getImpl(myProducerQueue);
    }

    public boolean putEmpty(BufferData data) {
        return putImpl(data, myProducerQueue);
    }

    public BufferData getFull() {
        return getImpl(myConsumeQueue);
    }

    public boolean putFull(BufferData data) {
        return putImpl(data, myConsumeQueue);
    }

    private BufferData getImpl(BlockingQueue<BufferData> queue) {
        if (null != queue) {
            try {
                return queue.take();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private boolean putImpl(BufferData data, BlockingQueue<BufferData> queue) {
        if (null != queue && null != data) {
            try {
                queue.put(data);
                return true;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return false;
    }
}
