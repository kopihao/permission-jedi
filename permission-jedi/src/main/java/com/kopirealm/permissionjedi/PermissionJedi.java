/*
 * Copyright (C) 2018 Kopihao
 * <p/>
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.kopirealm.permissionjedi;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

public class PermissionJedi {

    public static final boolean DEBUG = true;
    private static final String versionName = BuildConfig.VERSION_NAME;
    private static PermissionJedi jediPalace = null;

    synchronized static PermissionJedi getJedi() {
        if (jediPalace == null) {
            PermissionJedi.logj("jediPalace == null");
            jediPalace = new PermissionJedi(null);
        }
        return jediPalace;
    }

    protected static final class permission {
        public static final String GROUP_ID = "permissionjedi.permission";
        public static final String LOCAL_NOTIFICATION = GROUP_ID + ".LOCAL_NOTIFICATION";
    }

    public static final String ACTION_CHECK = "ACTION_CHECK";
    public static final String ACTION_REQUEST = "ACTION_REQUEST";
    public static final String ACTION_REVOKE = "ACTION_REVOKE";
    public static final String ACTION_APP_PERMISSIONS_SETTINGS = "ACTION_APP_PERMISSIONS_SETTINGS";
    public static final String ACTION_APP_NOTIFICATIONS_SETTINGS = "ACTION_APP_NOTIFICATIONS_SETTINGS";

    private Activity activity;
    private PermissionJediDelegate delegate;
    private HashSet<String> permissions = new HashSet<>();
    private boolean strictMode = false;

    public synchronized static PermissionJedi init(Activity activity) {
        PermissionJedi jediBaby = new PermissionJedi(activity);
        jediPalace = jediBaby;
        return PermissionJedi.getJedi();
    }

    public static boolean isAndroidPreM() {
        return !(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M);
    }

    private PermissionJedi(Activity activity) {
        this.activity = activity;
    }

    public PermissionJedi onComplete(PermissionJediDelegate delegate) {
        this.delegate = delegate;
        return this;
    }

    PermissionJediDelegate getDelegate() {
        return delegate;
    }

    public PermissionJedi checkPermissionStrictly() {
        this.strictMode = true;
        return this;
    }

    public PermissionJedi addPermissions(@NonNull String... permissions) {
        this.permissions.addAll(Arrays.asList(permissions));
        return this;
    }

    public PermissionJedi checkNotifications() {
        this.addPermissions(permission.LOCAL_NOTIFICATION);
        return this;
    }

    static <E extends Exception> void logj(E e) {
        if (!DEBUG) return;
        e.printStackTrace();
        Log.d("kopihao", PermissionJedi.class.getSimpleName() + ":" + versionName + "\t" + e.getMessage());
    }

    static void logj(String s) {
        if (!DEBUG) return;
        Log.d("kopihao", PermissionJedi.class.getSimpleName() + ":" + versionName + "\t" + s);
    }

    private void execute(String action) {
        try {
            if (strictMode) {
                if (permissions == null || permissions.isEmpty() || (!hasValidPermissions())) {
                    throw new IllegalAndroidPermissionException();
                }
            }
            ArrayList<String> preRequest = new ArrayList<>(Arrays.asList(permissions.toArray(new String[permissions.size()])));
            if (preRequest.contains(permission.LOCAL_NOTIFICATION)) {
                preRequest.remove(permission.LOCAL_NOTIFICATION);
                preRequest.add(0, permission.LOCAL_NOTIFICATION);
            }
            final String[] request = preRequest.toArray(new String[preRequest.size()]);
            final Intent intent = new Intent(activity, PermissionJediActivity.class);
            Bundle extras = new Bundle();
            extras.putString(PermissionJediActivity.EXTRA_ACTION, action);
            extras.putStringArray(PermissionJediActivity.EXTRA_PERMISSIONS, request);
            intent.putExtras(extras);
            activity.startActivity(intent);
        } catch (IllegalAndroidPermissionException e) {
            e.printStackTrace();
        }
    }

    public void gotoAppPermissionsSettings() {
        execute(ACTION_APP_PERMISSIONS_SETTINGS);
    }

    public void gotoNotificationsSettings() {
        this.addPermissions(permission.LOCAL_NOTIFICATION);
        execute(ACTION_APP_NOTIFICATIONS_SETTINGS);
    }

    public void check() {
        execute(ACTION_CHECK);
    }

    public void request() {
        execute(ACTION_REQUEST);
    }

    public void isPermissionRevokedByPolicy() {
        execute(ACTION_REVOKE);
    }

    public HashSet<String> getDeviceAndroidPermissions() {
        final HashSet<String> androidPermissions = new HashSet<>();
        try {
            for (Field field : Manifest.permission.class.getFields()) {
                String permission = (String) field.get("");
                if (permission.startsWith("android.permission")) {
                    androidPermissions.add(permission);
                }
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return androidPermissions;
    }

    private boolean hasValidPermissions() {
        final HashSet<String> androidPermissions = getDeviceAndroidPermissions();
        final HashSet<String> runtimePermissions = new HashSet<>(permissions);
        runtimePermissions.remove(permission.LOCAL_NOTIFICATION);
        if (!androidPermissions.isEmpty() && !androidPermissions.containsAll(runtimePermissions)) {
            return false;
        }
        return true;
    }

    public void showRationaleDialog(@NonNull String rationale, @NonNull String btnPos, @NonNull String btnNeg, @NonNull DialogInterface.OnClickListener diPos, @NonNull DialogInterface.OnClickListener diNeg) {
        customDialog(rationale, btnPos, btnNeg, diPos, diNeg).show();
    }

    public void gotoAppPermissionsSettingsDialog(@NonNull String rationale) {
        rationale = (!TextUtils.isEmpty(rationale)) ? rationale : activity.getString(R.string.txt_permission_required);
        this.gotoAppPermissionsSettingsDialog(
                rationale,
                activity.getString(R.string.btn_go_now),
                activity.getString(android.R.string.cancel),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        gotoAppPermissionsSettings();
                    }
                },
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
    }

    public void gotoAppPermissionsSettingsDialog(@NonNull String rationale, @NonNull String btnPos, @NonNull String btnNeg, @NonNull DialogInterface.OnClickListener diPos, @NonNull DialogInterface.OnClickListener diNeg) {
        customDialog(
                rationale,
                TextUtils.isEmpty(btnPos) ? activity.getString(R.string.btn_go_now) : btnPos,
                TextUtils.isEmpty(btnNeg) ? activity.getString(R.string.btn_not_now) : btnNeg,
                diPos,
                diNeg).show();
    }

    public void showGotoNotificationsSettingsDialog(@NonNull String rationale) {
        this.addPermissions(permission.LOCAL_NOTIFICATION);
        rationale = (!TextUtils.isEmpty(rationale)) ? rationale : activity.getString(R.string.txt_notification_required);
        this.showGotoNotificationsSettingsDialog(
                rationale,
                activity.getString(R.string.btn_go_now),
                activity.getString(android.R.string.cancel),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        gotoNotificationsSettings();
                    }
                },
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
    }

    public void showGotoNotificationsSettingsDialog(@NonNull String rationale, @NonNull String btnPos, @NonNull String btnNeg, @NonNull DialogInterface.OnClickListener diPos, @NonNull DialogInterface.OnClickListener diNeg) {
        customDialog(
                rationale,
                TextUtils.isEmpty(btnPos) ? activity.getString(R.string.btn_go_now) : btnPos,
                TextUtils.isEmpty(btnNeg) ? activity.getString(R.string.btn_not_now) : btnNeg,
                diPos,
                diNeg).show();
    }

    private AlertDialog.Builder customDialog(@NonNull String rationale, @NonNull String btnPos, @NonNull String btnNeg, @NonNull DialogInterface.OnClickListener diPos, @NonNull DialogInterface.OnClickListener diNeg) {
        AlertDialog.Builder adb = new AlertDialog.Builder(activity);
        adb.setMessage(rationale);
        if (!(TextUtils.isEmpty(btnPos) && diPos == null)) {
            btnPos = TextUtils.isEmpty(btnPos) ? activity.getString(android.R.string.ok) : btnPos;
            adb.setPositiveButton(btnPos, diPos);
        }
        if (!(TextUtils.isEmpty(btnNeg) && diNeg == null)) {
            btnNeg = TextUtils.isEmpty(btnNeg) ? activity.getString(android.R.string.cancel) : btnNeg;
            adb.setNegativeButton(btnNeg, diNeg);
        }
        adb.setCancelable(false);
        return adb;
    }

    public class IllegalAndroidPermissionException extends IllegalArgumentException {
        public IllegalAndroidPermissionException() {
            super("Illegal Android Permission Found.");
        }
    }

    public interface PermissionJediDelegate {
        void onPermissionReviewed(@NonNull HashMap<String, Boolean> permits);
    }

}
