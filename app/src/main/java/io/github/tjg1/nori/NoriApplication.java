/*
 * This file is part of nori.
 * Copyright (c) 2014-2016 Tomasz Jan G&oacute;ralczyk <tomg@fastmail.uk>
 * License: GNU GPLv2
 */
package io.github.tjg1.nori;


import android.app.Application;
import android.content.Context;
import android.util.Log;

import org.acra.ACRA;
import org.acra.ReportField;
import org.acra.ReportingInteractionMode;
import org.acra.config.ACRAConfiguration;
import org.acra.config.ACRAConfigurationException;
import org.acra.config.ConfigurationBuilder;

import io.github.tjg1.nori.util.HockeyIonSender;

import static org.acra.ReportField.*;

/**
 * Base class for maintaining global application state.
 */
public class NoriApplication extends Application {
  public static final String LOG_TAG = "io.github.tjg1.nori";

  @Override
  protected void attachBaseContext(Context base) {
    super.attachBaseContext(base);

    try {
      final ACRAConfiguration acraConfig = new ConfigurationBuilder(this)
          .setReportSenderFactoryClasses(HockeyIonSender.SenderFactory.class)
          .setReportingInteractionMode(ReportingInteractionMode.DIALOG)
          .setResDialogIcon(0)
          .setResToastText(R.string.crash_toast_text)
          .setResDialogText(R.string.crash_dialog_text)
          .setResDialogTitle(R.string.crash_dialog_title)
          .setResDialogCommentPrompt(R.string.crash_dialog_comment_prompt)
          .setResDialogPositiveButtonText(R.string.crash_dialog_positive_button_text)
          .setResDialogNegativeButtonText(R.string.crash_dialog_negative_button_text)
          .setResDialogOkToast(R.string.crash_dialog_ok_toast)
          .setResDialogTheme(R.style.CrashDialogTheme)
          .setSendReportsInDevMode(true)
          .setCustomReportContent(new ReportField[]{
              REPORT_ID, APP_VERSION_CODE, APP_VERSION_NAME, PACKAGE_NAME, PHONE_MODEL,
              BRAND, PRODUCT, ANDROID_VERSION, BUILD, TOTAL_MEM_SIZE, AVAILABLE_MEM_SIZE,
              CUSTOM_DATA, STACK_TRACE, INITIAL_CONFIGURATION, CRASH_CONFIGURATION, DISPLAY,
              USER_COMMENT, USER_APP_START_DATE, USER_CRASH_DATE, IS_SILENT, INSTALLATION_ID,
              DEVICE_FEATURES, ENVIRONMENT, MEDIA_CODEC_LIST, THREAD_DETAILS, BUILD_CONFIG
          })
          .build();

      // Initialise ACRA crash reporting.
      ACRA.init(this, acraConfig);
    } catch (ACRAConfigurationException e) {
      Log.e(LOG_TAG, "Failed to initialise ACRA", e);
    }
  }
}
