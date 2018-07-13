package com.kopirealm.permissionjedi;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import com.kopirealm.permissionjedi.databinding.ActivityMainBinding;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends Activity {

    private ActivityMainBinding mBinding;
    private Activity self = this;
    private final String[] requestPerms = new String[]{
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    mBinding.message.setText(R.string.title_home);
                    PermissionJedi.init(self)
                            .addPermissions(requestPerms)
                            .setActions(new PermissionJedi.PermissionJediActions() {
                                @Override
                                public void onPermissionReviewed(@NonNull HashMap<String, Boolean> permits) {
                                    for (Map.Entry<String, Boolean> entry : permits.entrySet()) {
                                        Log.d("kopihao", entry.getKey() + " is revoked by policy : " + entry.getValue());
                                    }
                                }
                            })
                            .isPermissionRevokedByPolicy();
                    return true;
                case R.id.navigation_dashboard:
                    mBinding.message.setText(R.string.title_check);
                    checkPermission();
                    return true;
                case R.id.navigation_notifications:
                    mBinding.message.setText(R.string.title_request);
                    requestPermission();
                    return true;
            }
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        mBinding.navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
    }

    private void requestPermission() {
        PermissionJedi
                .init(self)
                .checkNotifications()
                .addPermissions(requestPerms)
                .setActions(new PermissionJedi.PermissionJediActions() {
                    @Override
                    public void onPermissionReviewed(@NonNull HashMap<String, Boolean> permits) {
                        if (permits.isEmpty()) {
                            sbNothingChecked();
                            return;
                        }
                        ArrayList<String> denied = new ArrayList<>(getDeniedPermissions(permits));
                        if (denied.isEmpty()) {
                            sbAllAllowed();
                        } else {
                            retryPermissions(denied);
                        }
                        mBinding.message.setText(denied.isEmpty() ? getString(R.string.txt_warn_0) : getString(R.string.txt_warn_4).concat(permissionsToStrings(denied)));
                    }
                })
                .request();
    }

    private void checkPermission() {
        PermissionJedi.init(self)
                .checkPermissionStrictly()
                .addPermissions(requestPerms)
                .checkNotifications()
                .setActions(new PermissionJedi.PermissionJediActions() {
                    @Override
                    public void onPermissionReviewed(@NonNull HashMap<String, Boolean> permits) {
                        if (permits.isEmpty()) {
                            sbNothingChecked();
                            return;
                        }
                        ArrayList<String> denied = new ArrayList<>(getDeniedPermissions(permits));
                        if (denied.isEmpty()) {
                            sbAllAllowed();
                        } else {
                            sbSomeAllowed(denied);
                        }
                        mBinding.message.setText(denied.isEmpty() ? getString(R.string.txt_warn_0) : getString(R.string.txt_warn_4).concat(permissionsToStrings(denied)));
                    }
                })
                .check();
    }

    private void retryPermissions(ArrayList<String> denied) {
        String rationale = String.format(getString(R.string.txt_warn_2), getApplicationInfo().loadLabel(getPackageManager()).toString())
                .concat(permissionsToStrings(denied));
        if (denied.isEmpty()) {
            rationale = getString(R.string.txt_warn_1).concat(permissionsToStrings(new ArrayList<String>(Arrays.asList(requestPerms))));
        }
        PermissionJedi.init(self)
                .addPermissions(requestPerms)
                .setActions(new PermissionJedi.PermissionJediActions() {
                    @Override
                    public void onPermissionReviewed(@NonNull HashMap<String, Boolean> permits) {
                        ArrayList<String> denied = new ArrayList<>(getDeniedPermissions(permits));
                        if (denied.isEmpty()) {
                            sbAllAllowed();
                        } else {
                            deliverRationale(denied);
                        }
                        mBinding.message.setText(denied.isEmpty() ? getString(R.string.txt_warn_0) : getString(R.string.txt_warn_4).concat(permissionsToStrings(denied)));
                    }
                })
//                .gotoAppPermissionsSettings();
                .gotoAppPermissionsSettingsDialog(rationale);

    }

    private void retryNotification() {
        String rationale = String.format(getString(R.string.txt_warn_5), getApplicationInfo().loadLabel(getPackageManager()).toString());
        PermissionJedi.init(self)
                .setActions(new PermissionJedi.PermissionJediActions() {
                    @Override
                    public void onPermissionReviewed(@NonNull HashMap<String, Boolean> permits) {
                        ArrayList<String> denied = new ArrayList<>(getDeniedPermissions(permits));
                        if (denied.isEmpty()) {
                            sbAllAllowed();
                        } else {
                            sbSomeAllowed(denied);
                        }
                        mBinding.message.setText(denied.isEmpty() ? getString(R.string.txt_warn_0) : getString(R.string.txt_warn_4).concat(permissionsToStrings(denied)));
                    }
                })
//                .gotoNotificationsSettings()
                .showGotoNotificationsSettingsDialog(rationale);

    }

    private void deliverRationale(final ArrayList<String> denied) {
        DialogInterface.OnClickListener diPos = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                sbSomeAllowed(denied);
            }
        };
        PermissionJedi
                .init(self)
                .addPermissions(denied.toArray(new String[denied.size()]))
                .showRationaleDialog(
                        getString(R.string.txt_warn_1).concat(permissionsToStrings(denied)),
                        getString(R.string.title_noted),
                        null,
                        diPos,
                        null);
    }

    private ArrayList<String> getDeniedPermissions(HashMap<String, Boolean> permits) {
        ArrayList<String> denied = new ArrayList<>();
        for (Map.Entry<String, Boolean> entry : permits.entrySet()) {
            Log.d("kopihao", entry.getKey() + " has permission : " + entry.getValue());
            if (!entry.getValue()) {
                denied.add(entry.getKey());
            }
        }
        return denied;
    }

    private String permissionsToStrings(ArrayList<String> permissions) {
        return ("\n" + android.text.TextUtils.join(",\n", permissions)).replace("android.permission.", "").replace(PermissionJedi.permission.GROUP_ID + ".", "");
    }

    private void sbNothingChecked() {
        Snackbar.make(
                mBinding.coordinatorLayout,
                String.format(getString(R.string.txt_warn_6)),
                Snackbar.LENGTH_SHORT)
                .show();
    }

    private void sbAllAllowed() {
        Snackbar.make(
                mBinding.coordinatorLayout,
                String.format(getString(R.string.txt_warn_0)),
                Snackbar.LENGTH_SHORT)
                .setAction(getString(R.string.title_review), new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        retryPermissions(new ArrayList<String>());
                    }
                }).show();
    }

    private void sbSomeAllowed(final ArrayList<String> denied) {
        boolean notificationOnly = denied.size() == 1 && denied.contains(PermissionJedi.permission.LOCAL_NOTIFICATION);
        if (notificationOnly) {
            Snackbar.make(
                    mBinding.coordinatorLayout,
                    String.format(getString(R.string.txt_warn_3), denied.size()),
                    Snackbar.LENGTH_LONG)
                    .setAction(getString(R.string.title_retry), new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            retryNotification();
                        }
                    }).show();

        } else {
            Snackbar.make(
                    mBinding.coordinatorLayout,
                    String.format(getString(R.string.txt_warn_3), denied.size()),
                    Snackbar.LENGTH_LONG)
                    .setAction(getString(R.string.title_retry), new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            requestPermission();
                        }
                    }).show();

        }
    }

}
