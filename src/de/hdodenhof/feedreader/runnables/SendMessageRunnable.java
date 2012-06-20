package de.hdodenhof.feedreader.runnables;

import java.util.ArrayList;

import android.os.Handler;
import android.os.Message;

import de.hdodenhof.feedreader.misc.RSSMessage;

public class SendMessageRunnable implements Runnable {
        private RSSMessage mMessage;
        private long mDelay;
        private ArrayList<Handler> mHandlers;

        public SendMessageRunnable(ArrayList<Handler> handlers, RSSMessage message, long delay) {
                this.mHandlers = handlers;
                this.mMessage = message;
                this.mDelay = delay;
        }

        public void run() {
                for (Handler mHandler : mHandlers) {
                        Message mMSG = mHandler.obtainMessage();
                        mMSG.obj = mMessage;
                        mHandler.sendMessageDelayed(mMSG, mDelay);
                }
        }
}