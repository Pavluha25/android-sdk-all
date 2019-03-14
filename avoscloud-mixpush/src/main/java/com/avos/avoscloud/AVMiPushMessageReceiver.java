package com.avos.avoscloud;

import android.content.Context;
import android.text.TextUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import java.util.List;

/**
 * Created by wli on 16/6/22.
 * 该回调运行在非 UI 线程
 */
public class AVMiPushMessageReceiver extends com.xiaomi.mipush.sdk.PushMessageReceiver {

  private void updateAVInstallation(String miRegId) {
    if (!AVUtils.isBlankString(miRegId)) {
      AVInstallation installation = AVInstallation.getCurrentInstallation();

      if (!"mi".equals(installation.getString(AVInstallation.VENDOR))) {
        installation.put(AVInstallation.VENDOR, "mi");
      }
      if (!miRegId.equals(installation.getString(AVInstallation.REGISTRATION_ID))) {
        installation.put(AVInstallation.REGISTRATION_ID, miRegId);
      }
      String localProfile = installation.getString(AVMixPushManager.MIXPUSH_PROFILE);
      localProfile = (null != localProfile ? localProfile : "");
      if (!localProfile.equals(AVMixPushManager.miDeviceProfile)) {
        installation.put(AVMixPushManager.MIXPUSH_PROFILE, AVMixPushManager.miDeviceProfile);
      }
      installation.saveInBackground(new SaveCallback() {
        @Override
        public void done(AVException e) {
          if (null != e) {
            LogUtil.avlog.e("update installation error!", e);
          } else {
            LogUtil.avlog.d("Xiaomi push registration successful!");
          }
        }
      });
    }
  }

  /**
   * 处理小米推送的透传消息
   * @param miPushMessage
   */
  private void processMiPushMessage(com.xiaomi.mipush.sdk.MiPushMessage miPushMessage) {
    if (null != miPushMessage) {
      String title = miPushMessage.getTitle();
      String description = miPushMessage.getDescription();
      String content = miPushMessage.getContent();

      JSONObject jsonObject = null;
      if (!TextUtils.isEmpty(content)) {
        try {
          jsonObject = JSON.parseObject(content);
        } catch (Exception exception) {
          LogUtil.avlog.e("Parsing json data error, " + content, exception);
        }
      }
      if (null == jsonObject) {
        jsonObject = new JSONObject();
      }

      if (!AVUtils.isBlankString(title)) {
        jsonObject.put("title", title);
      }
      if (!AVUtils.isBlankString(description)) {
        jsonObject.put("alert", description);
      }
      AVNotificationManager.getInstance().processMixPushMessage(jsonObject.toJSONString());
    }
  }

  /**
   * 处理小米推送点击事件
   * @param miPushMessage
   */
  private void processMiNotification(com.xiaomi.mipush.sdk.MiPushMessage miPushMessage) {
    if (null != miPushMessage) {
      String content = miPushMessage.getContent();
      if (!AVUtils.isBlankString(content)) {
        AVNotificationManager.getInstance().processMixNotification(content, AVConstants.AV_MIXPUSH_MI_NOTIFICATION_ACTION);
      }
    }
  }

  /**
   * 注册结果
   */
  @Override
  public void onReceiveRegisterResult(Context context, com.xiaomi.mipush.sdk.MiPushCommandMessage miPushCommandMessage) {
    super.onReceiveRegisterResult(context, miPushCommandMessage);
    String command = miPushCommandMessage.getCommand();
    List<String> arguments = miPushCommandMessage.getCommandArguments();
    String cmdArg1 = ((arguments != null && arguments.size() > 0) ? arguments.get(0) : null);
    if (com.xiaomi.mipush.sdk.MiPushClient.COMMAND_REGISTER.equals(command)) {
      if (miPushCommandMessage.getResultCode() == com.xiaomi.mipush.sdk.ErrorCode.SUCCESS) {
        updateAVInstallation(cmdArg1);
      } else {
        LogUtil.avlog.d("register error, " + miPushCommandMessage.toString());
      }
    } else {
    }
  }

  /**
   * 通知栏消息到达事件
   * @param context
   * @param miPushMessage
   */
  @Override
  public void onNotificationMessageArrived(Context context, com.xiaomi.mipush.sdk.MiPushMessage miPushMessage) {
    if (null != miPushMessage) {
      String content = miPushMessage.getContent();
      if (!AVUtils.isBlankString(content)) {
        AVNotificationManager.getInstance().porcessMixNotificationArrived(content, AVConstants.AV_MIXPUSH_MI_NOTIFICATION_ARRIVED_ACTION);
      }
    }
  }

  /**
   * 透传消息
   * @param context
   * @param miPushMessage
   */
  @Override
  public void onReceivePassThroughMessage(Context context, com.xiaomi.mipush.sdk.MiPushMessage miPushMessage) {
    processMiPushMessage(miPushMessage);
  }

  /**
   * 通知栏消息，用户手动点击后触发
   * @param context
   * @param miPushMessage
   */
  @Override
  public void onNotificationMessageClicked(Context context, com.xiaomi.mipush.sdk.MiPushMessage miPushMessage) {
    processMiNotification(miPushMessage);
  }
}
