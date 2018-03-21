package com.mediatek.incallui.wfc;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;

import com.android.incallui.Log;
import com.android.incallui.call.CallList;
import com.android.incallui.call.DialerCall;

import com.mediatek.incallui.plugin.ExtensionManager;
import com.mediatek.wfo.IWifiOffloadService;
import com.mediatek.wfo.WifiOffloadManager;

/**
 * RoveOutReceiver.
 */
public class RoveOutReceiver extends WifiOffloadManager.Listener {
    private static final String TAG = "RoveOutReceiver";
    private Context mContext;
    private Message mMsg = null;
    private static final int COUNT_TIMES = 3;
    private static final int EVENT_RESET_TIMEOUT = 1;
    private static final int CALL_ROVE_OUT_TIMER = 1800000;
    private IWifiOffloadService mWfoService = null;

    /**
     * Constructor.
     * @param context context
     */
    public RoveOutReceiver(Context context) {
        mContext = context;
        IBinder b = ServiceManager.getService(WifiOffloadManager.WFO_SERVICE);
        mWfoService = IWifiOffloadService.Stub.asInterface(b);
    }

    /**
     * register RoveOutReceiver for handover events.
     */
    public void register() {
        if (mWfoService != null) {
            try {
                Log.d(TAG, "RoveOutReceiver register mWfoService");
                mWfoService.registerForHandoverEvent(this);
            } catch (RemoteException e) {
                Log.i(TAG, "RemoteException RoveOutReceiver()");
            }
        }
    }

    /**
     * unregister RoveOutReceiver.
     */
    public void unregister() {
        if (mWfoService != null) {
            try {
                Log.d(TAG, "RoveOutReceiver unregister mWfoService ");
                mWfoService.unregisterForHandoverEvent(this);
            } catch (RemoteException e) {
                Log.i(TAG, "RemoteException RoveOutReceiver()");
            }
            WfcDialogActivity.sCount = 0;
            if (mMsg != null) {
                mHandler.removeMessages(mMsg.what);
            }
        }
    }

    @Override
    public void onHandover(int simIdx, int stage, int ratType) {
        Log.d(TAG, "onHandover stage: " + stage + "ratType : " + ratType);
        ExtensionManager.getInCallExt().showHandoverNotification(mHandler, stage, ratType);
    }

    @Override
    public void onRoveOut(int simIdx, boolean roveOut, int rssi) {
        Log.d(TAG, "onRoveOut: " + roveOut);
        DialerCall call = CallList.getInstance().getActiveOrBackgroundCall();
        if (roveOut) {
            if ((call != null && call.hasProperty(android.telecom.Call.Details.PROPERTY_WIFI))
                    && (WfcDialogActivity.sCount < COUNT_TIMES)
                    && !WfcDialogActivity.sIsShowing) {
                final Intent intent1 = new Intent(mContext, WfcDialogActivity.class);
                intent1.putExtra(WfcDialogActivity.SHOW_WFC_ROVE_OUT_POPUP, true);
                intent1.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mContext.startActivity(intent1);
                if (WfcDialogActivity.sCount == 0) {
                    mMsg = mHandler.obtainMessage(EVENT_RESET_TIMEOUT);
                    mHandler.removeMessages(mMsg.what);
                    mHandler.sendMessageDelayed(mMsg, CALL_ROVE_OUT_TIMER);
                    Log.i(TAG, "WfcSignalReceiver sendMessageDelayed ");
                }
            }
        }
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case EVENT_RESET_TIMEOUT:
                    Log.i(TAG, "WfcSignalReceiver EVENT_RESET_TIMEOUT ");
                    WfcDialogActivity.sCount = 0;
                    break;
                default:
                    Log.i(TAG, "Message not expected: ");
                    break;
            }
        }
    };
}
