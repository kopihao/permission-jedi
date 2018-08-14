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

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class PermissionJediActivity extends Activity implements PermissionJedi.PermissionJediDelegate {

    private static final int REQUEST_CODE_ASK_PERMISSIONS = 91001;
    private static final int REQUEST_CODE_GOTO_APP_PERMISSION_SETTINGS = 91002;
    private static final int REQUEST_CODE_GOTO_APP_NOTIFICATIONS_SETTINGS = 91003;
    public static final String EXTRA_ACTION = "EXTRA_ACTION";
    public static final String EXTRA_PERMISSIONS = "EXTRA_PERMISSIONS";

    private Activity self = this;
    private String action = "";
    private String[] permissions = null;
    private Button btnMayTheForceBeWithYou = null;
    private HashMap<String, Boolean> delegateResult = new HashMap<>();


    private void logj(String s) {
        PermissionJedi.logj(s);
    }

    private void logj(Exception e) {
        PermissionJedi.logj(e);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle extras = getIntent().getExtras();
        action = extras.getString(EXTRA_ACTION, "");
        permissions = extras.getStringArray(EXTRA_PERMISSIONS);
        mayTheForceBeWithYou();
        runtimePermissions();
    }

    private void mayTheForceBeWithYou() {
        try {
            btnMayTheForceBeWithYou = new Button(this);
            btnMayTheForceBeWithYou.setOnClickListener(new View.OnClickListener() {
                final PermissionJedi.PermissionJediDelegate delegate = PermissionJedi.getJedi().clone().getDelegate();

                @Override
                public void onClick(View v) {
                    delegate.onPermissionReviewed(delegateResult);
                }
            });
        } catch (Exception e) {
            logj(e);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle extras) {
        extras.putString(EXTRA_ACTION, action);
        extras.putStringArray(EXTRA_PERMISSIONS, permissions);
        super.onSaveInstanceState(extras);
        logj("run onSaveInstanceState()");
    }

    @Override
    protected void onRestoreInstanceState(Bundle extras) {
        super.onRestoreInstanceState(extras);
        if (extras != null) {
            action = extras.getString(EXTRA_ACTION, "");
            permissions = extras.getStringArray(EXTRA_PERMISSIONS);
        }
        logj("run onRestoreInstanceState()");
    }

    private void runtimePermissions() {
        switch (action) {
            case PermissionJedi.ACTION_CHECK:
                hasPermissions(permissions);
                break;
            case PermissionJedi.ACTION_REQUEST:
                requestPermissions(permissions);
                break;
            case PermissionJedi.ACTION_REVOKE:
                checkPolicy(permissions);
                break;
            case PermissionJedi.ACTION_APP_PERMISSIONS_SETTINGS:
                gotoAppSettings();
                break;
            case PermissionJedi.ACTION_APP_NOTIFICATIONS_SETTINGS:
                gotoNotificationSettings();
                break;
            default:
                finish();
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private HashMap<String, Boolean> isPermissionRevokedByPolicy(@NonNull String... permissions) {
        HashMap<String, Boolean> permits = new HashMap<>();
        for (final String p : permissions) {
            if (PermissionJedi.isAndroidPreM()) {
                permits.put(p, false);
            } else {
                try {
                    permits.put(p, self.getPackageManager().isPermissionRevokedByPolicy(p, self.getPackageName()));
                } catch (Exception e) {
                    logj(e);
                    permits.put(p, false);
                }
                logj("checkPolicy()::" + p + "::" + permits.get(p));
            }
        }
        return permits;
    }
    private void checkPolicy(@NonNull String... permissions) {
        final HashMap<String, Boolean> permits = isPermissionRevokedByPolicy(permissions);
        onPermissionReviewed(permits);
    }

    private HashMap<String, Boolean> checkPermission(@NonNull String... permissions) {
        HashMap<String, Boolean> permits = new HashMap<>();
        for (final String p : permissions) {
            if (PermissionJedi.isAndroidPreM()) {
                permits.put(p, true);
            } else {
                if (p.equals(PermissionJedi.permission.LOCAL_NOTIFICATION)) {
                    permits.put(p, (NotificationManagerCompat.from(self).areNotificationsEnabled()));
                } else {
                    permits.put(p, (ContextCompat.checkSelfPermission(self, p) == PackageManager.PERMISSION_GRANTED));
                }
            }
            logj("checkPermission()::" + p + "::" + permits.get(p));
        }
        return permits;
    }

    private void hasPermissions(@NonNull String... permissions) {
        final HashMap<String, Boolean> permits = checkPermission(permissions);
        onPermissionReviewed(permits);
    }

    private void requestPermissions(@NonNull String... permissions) {
        ArrayList<String> missingPermissions = new ArrayList<String>();
        HashMap<String, Boolean> permits = checkPermission(permissions);
        for (final String p : permissions) {
            if (!permits.get(p)) {
                missingPermissions.add(p);
            }
        }
        if (missingPermissions.isEmpty()) {
            grantAllPermissions(permissions);
        } else {
            if (PermissionJedi.isAndroidPreM()) {
                grantAllPermissions(permissions);
            } else {
                ActivityCompat.requestPermissions(self, missingPermissions
                        .toArray(new String[missingPermissions.size()]), REQUEST_CODE_ASK_PERMISSIONS);
            }
        }
    }

    private void grantAllPermissions(String... permissions) {
        final int[] grantResults = new int[permissions.length];
        Arrays.fill(grantResults, PackageManager.PERMISSION_GRANTED);
        onRequestPermissionsResult(REQUEST_CODE_ASK_PERMISSIONS, permissions, grantResults);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        final HashMap<String, Boolean> permits = new HashMap<>();
        if (requestCode == REQUEST_CODE_ASK_PERMISSIONS) {
            for (int i = 0; i < permissions.length; i++) {
                permits.put(permissions[i], grantResults[i] == PackageManager.PERMISSION_GRANTED);
            }
        }
        onPermissionReviewed(permits);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_GOTO_APP_PERMISSION_SETTINGS ||
                requestCode == REQUEST_CODE_GOTO_APP_NOTIFICATIONS_SETTINGS) {
            hasPermissions(permissions);
            return;
        }
    }

    public void gotoAppSettings() {
        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", self.getPackageName(), null);
        intent.setData(uri);
        self.startActivityForResult(intent, REQUEST_CODE_GOTO_APP_PERMISSION_SETTINGS);
    }

    public void gotoNotificationSettings() {
        gotoNotificationSettings(null);
    }

    public void gotoNotificationSettings(String channel) {
        try {
            Intent intent = new Intent();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                intent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                if (!TextUtils.isEmpty(channel)) {
                    intent.putExtra(Settings.EXTRA_CHANNEL_ID, channel);
                }
                intent.putExtra(Settings.EXTRA_APP_PACKAGE, self.getPackageName());
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                intent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                intent.putExtra(Settings.EXTRA_APP_PACKAGE, self.getPackageName());
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                intent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                intent.putExtra("app_package", self.getPackageName());
                intent.putExtra("app_uid", self.getApplicationInfo().uid);
            } else {
                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.addCategory(Intent.CATEGORY_DEFAULT);
                intent.setData(Uri.parse("package:" + self.getPackageName()));
            }
            self.startActivityForResult(intent, REQUEST_CODE_GOTO_APP_NOTIFICATIONS_SETTINGS);
        } catch (Exception e) {
            logj(e);
        }
    }

    private boolean isDooming() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            if (this.isDestroyed()) {
                return true;
            }
        } else {
            if (this.isFinishing()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onPermissionReviewed(@NonNull HashMap<String, Boolean> permits) {
        try {
            if (!isDooming()) {
                try {
                    delegateResult.clear();
                    delegateResult.putAll(permits);
                    if (PermissionJedi.getJedi() != null && PermissionJedi.getJedi().getDelegate() != null) {
                        PermissionJedi.getJedi().getDelegate().onPermissionReviewed(permits);
                    } else {
                        btnMayTheForceBeWithYou.performClick();
                    }
                } catch (Exception e) {
                    throw new Exception("Jedi is crippled");
                }
            } else {
                throw new Exception("Jedi Activity Aborted");
            }
        } catch (Exception e) {
            logj(e);
        } finally {
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        System.gc();
    }
}
