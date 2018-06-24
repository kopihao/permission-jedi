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
import android.os.Handler;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

public class PermissionJedi {

    private static PermissionJedi jedi = null;

    public static PermissionJedi getJedi() {
        System.gc();
        jedi = (jedi != null) ? jedi : new PermissionJedi();
        return jedi;
    }

    public static final String ACTION_CHECK = "ACTION_CHECK";
    public static final String ACTION_REQUEST = "ACTION_REQUEST";
    public static final String ACTION_APPSETTINGS = "ACTION_APPSETTINGS";

    private Activity activity;
    private PermissionJediActions actions;
    private HashSet<String> permissions = new HashSet<>();
    private boolean strictMode = false;

    public static PermissionJedi init(Activity activity) {
        return PermissionJedi.getJedi().setActivity(activity);
    }

    protected boolean isValidContext() {
        if (activity == null || activity.isFinishing()) {
            return false;
        }
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN) {
            if (activity.isDestroyed()) {
                return false;
            }
        }
        return true;
    }

    protected boolean notifyPermissionStatus(final HashMap<String, Boolean> permits) {
        try {
            if (!isValidContext()) {
                return false;
            }
            if (actions == null) {
                return false;
            }
            new Handler(activity.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    actions.onPermissionReviewed(permits);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public PermissionJedi useStrictMode() {
        this.strictMode = true;
        return this;
    }

    public PermissionJedi addPermissions(@NonNull String... permissions) {
        this.permissions.addAll(Arrays.asList(permissions));
        return this;
    }

    public void bind(PermissionJediActivity activity) {
        activity.notifier = actions;
    }

    public PermissionJedi setActions(PermissionJediActions actions) {
        this.actions = actions;
        return this;
    }

    public PermissionJedi setActivity(Activity activity) {
        this.activity = activity;
        return this;
    }

    public void check() {
        if (permissions == null || permissions.isEmpty()) {
            return;
        }
        execute(ACTION_CHECK);
    }

    public void request() {
        if (permissions == null || permissions.isEmpty()) {
            return;
        }
        execute(ACTION_REQUEST);
    }

    private void execute(String action) {
        try {
            final String[] request = permissions.toArray(new String[permissions.size()]);
            if (strictMode && !hasValidPermissions()) {
                throw new IllegalAndroidPermissionException();
            }
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

    public void gotoAppSettings() {
        execute(ACTION_APPSETTINGS);
    }

    public HashSet<String> getAndroidPermissions() {
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
        final HashSet<String> androidPermissions = getAndroidPermissions();
        final HashSet<String> runtimePermissions = permissions;
        if (!androidPermissions.isEmpty() && !androidPermissions.containsAll(runtimePermissions)) {
            return false;
        }
        return true;
    }

    private class IllegalAndroidPermissionException extends IllegalArgumentException {
        public IllegalAndroidPermissionException() {
            super("Illegal Android Permission Found.");
        }
    }

    public interface PermissionJediActions {
        public void onPermissionReviewed(@NonNull HashMap<String, Boolean> permits);
    }

    public void showRationaleDialog(@NonNull String rationale, @NonNull String btnPos, @NonNull String btnNeg, @NonNull DialogInterface.OnClickListener diPos, @NonNull DialogInterface.OnClickListener diNeg) {
        customDialog(rationale, btnPos, btnNeg, diPos, diNeg).show();
    }

    public void showGotoSettingsDialog(@NonNull String rationale) {
        rationale = (!TextUtils.isEmpty(rationale)) ? rationale : activity.getString(R.string.txt_permission_required);
        customDialog(
                rationale,
                activity.getString(R.string.btn_go_now),
                activity.getString(android.R.string.cancel),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        gotoAppSettings();
                    }
                },
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                }).show();
    }

    public void showGotoSettingsDialog(@NonNull String rationale, @NonNull String btnPos, @NonNull String btnNeg, @NonNull DialogInterface.OnClickListener diPos, @NonNull DialogInterface.OnClickListener diNeg) {
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

}
