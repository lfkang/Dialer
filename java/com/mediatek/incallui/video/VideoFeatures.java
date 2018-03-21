package com.mediatek.incallui.video;

import android.content.Context;
import android.os.PersistableBundle;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telephony.CarrierConfigManager;
import android.telephony.TelephonyManager;
import com.android.incallui.call.DialerCall;
import com.android.incallui.call.CallList;
import com.android.incallui.InCallPresenter;

import java.util.Objects;

import mediatek.telephony.MtkCarrierConfigManager;

/**
 * M: management for video call features.
 */
public class VideoFeatures {
    private final DialerCall mCall;
    /**
     * M: [video call]for management of video call features.
     *
     * @param call the call associate with current VideoFeatures instance.
     */
    public VideoFeatures(DialerCall call) {
        mCall = call;
    }

    /**
     * M: whether this call supports rotation.
     * make sure this is a video call before checking this feature.
     *
     * @return true if support.
     */
    public boolean supportsRotation() {
        return !isCsCall();
    }

    /**
     * M: whether this call supports downgrade.
     * make sure this is a video call before checking this feature.
     *
     * @return true if support.
     */
    public boolean supportsDowngrade() {
        return !isCsCall();
    }

    /**
     * M: whether this call supports answer as voice.
     * make sure this is a video call before checking this feature.
     *
     * @return true if support.
     */
    public boolean supportsAnswerAsVoice() {
        return !isCsCall();
    }

    /**
     * M: whether this call supports pause (turn off camera).
     * make sure this is a video call before checking this feature.
     *
     * @return
     */
    public boolean supportsPauseVideo() {
        return !isCsCall()
                && isContainCarrierConfig(
                    MtkCarrierConfigManager.MTK_KEY_ALLOW_ONE_WAY_VIDEO_BOOL);
    }

    /**
     * M: whether this video call supports reject call by SMS .
     * make sure this is a video call before checking this feature.
     *
     * @return true if support.
     */
    public boolean supportsRejectVideoCallBySms() {
        return !isCsCall()
                && isContainCarrierConfig(
                    MtkCarrierConfigManager.MTK_KEY_ALLOW_ONE_VIDEO_CALL_ONLY_BOOL);
    }

    /**
     * M: whether this call supports cancel upgrade .
     * make sure this is a video call before checking this feature.
     *
     * @return true if support
     */
    public boolean supportsCancelUpgradeVideo() {
        return !isCsCall()
                && isContainCarrierConfig(
                    MtkCarrierConfigManager.MTK_KEY_ALLOW_CANCEL_VIDEO_UPGRADE_BOOL);
    }

    /// M: Add for video feature change due to camera error @{
    private boolean mCameraErrorHappened = false;
    public void setCameraErrorHappened(boolean happen) {
        mCameraErrorHappened = happen;
    }
    /// @}

    /**
     * M: check the current call can upgrade to video call or not
     *
     * @return false if without subject call, active and hold call with the same account which
     * belongs to some operator, else depends on whether it is not a CS Call.
     */
    public boolean canUpgradeToVideoCall() {
        if (mCall == null) {
            return false;
        }

        if (isContainCarrierConfig(MtkCarrierConfigManager.
            MTK_KEY_ALLOW_ONE_VIDEO_CALL_ONLY_BOOL)) {
            //FIXME: support vilte capability when multi calls exist with different accounts.
            if (CallList.getInstance().getBackgroundCall() == null
                    || !Objects.equals(mCall.getTelecomCall().getDetails().getAccountHandle(),
                    CallList.getInstance().getBackgroundCall().getTelecomCall().getDetails()
                            .getAccountHandle())) {
                return !isCsCall() && !mCameraErrorHappened;
            } else {
                // return false if Active and Hold Calls from the same account
                return false;
            }
        }

        return !isCsCall() && !mCameraErrorHappened;
    }

    /**
     * M: Check whether it's ims(video) call or 3gvt call
     */
    public boolean isImsCall() {
        return !isCsCall();
    }

    /**
     * M: whether this call supports hold.
     * make sure this is a video call before checking this feature.
     *
     * @return
     */
    public boolean supportsHold() {
        return !isCsCall();
    }

    public boolean supportsHidePreview() {
        return !isCsCall();
    }

    private boolean isCsCall() {
        return !mCall.hasProperty(mediatek.telecom.MtkCall.MtkDetails.MTK_PROPERTY_VOLTE);
    }

  /**
   * TODO: This is bind call progress may effect the UI performance.
   * cache it ? but how to update it?
   * @param key
   * @return
   */
  public boolean isContainCarrierConfig(String key) {
    PhoneAccountHandle phoneAccountHandle = mCall.getAccountHandle();
    if (phoneAccountHandle == null) {
      return false;
    }
    Context context = InCallPresenter.getInstance().getContext();
    TelecomManager telecomManager = (TelecomManager)context.
        getSystemService(Context.TELECOM_SERVICE);
    PhoneAccount phoneAccount = telecomManager
        .getPhoneAccount(phoneAccountHandle);
    if (phoneAccount == null) {
      return false;
    }
    TelephonyManager telephonyManager = context
        .getSystemService(TelephonyManager.class);
    CarrierConfigManager configManager = (CarrierConfigManager) context
        .getSystemService(Context.CARRIER_CONFIG_SERVICE);

    int subId = telephonyManager.getSubIdForPhoneAccount(phoneAccount);
    PersistableBundle bundle = configManager.getConfigForSubId(subId);
    if (bundle != null
        && bundle
            .getBoolean(key)) {
      return true;
    }
    return false;
  }

  /**
   * AOSP default not show Dialpd for video call.
   * Less effect to the AOSP UI change, add it for some special OP.
   * @return
   */
  public boolean supportShowVideoDialpad() {
    return isContainCarrierConfig(MtkCarrierConfigManager.MTK_KEY_ALLOW_ONE_VIDEO_CALL_ONLY_BOOL);
  }
}
