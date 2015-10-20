/*
 * Created by Angel Leon (@gubatron), Alden Torres (aldenml)
 * Copyright (c) 2011-2015, FrostWire(R). All rights reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.frostwire.android.gui.util;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import com.frostwire.android.R;
import com.frostwire.android.core.ConfigurationManager;
import com.frostwire.android.gui.activities.MainActivity;
import com.frostwire.util.Ref;

import java.lang.ref.WeakReference;

/**
 * @author gubatron
 * @author aldenml
 */
public final class DangerousPermissionsChecker implements ActivityCompat.OnRequestPermissionsResultCallback {

    public enum PermissionCheck {
        ExternalStorage,
        PhoneState
    }

    public static final int EXTERNAL_STORAGE_PERMISSIONS_REQUEST_CODE = 0xAAAA;
    public static final int PHONE_STATE_PERMISSIONS_REQUEST_CODE = 0xBBBB;
    private final WeakReference<Activity> activityRef;
    private final PermissionCheck checkType;

    public DangerousPermissionsChecker(Activity activity, PermissionCheck requestType) {
        checkType = requestType;
        this.activityRef = Ref.weak(activity);
    }

    public boolean noAccess() {
        if (checkType == PermissionCheck.ExternalStorage) {
            return noExternalStorageAccess();
        } else if (checkType == PermissionCheck.PhoneState) {
            return noPhoneStateAccess();
        }
        return false;
    }

    public void showPermissionsRationale() {
        if (checkType == PermissionCheck.ExternalStorage) {
            showExternalStoragePermissionsRationale();
        } else if (checkType == PermissionCheck.PhoneState) {
            showPhoneStatePermissionsRationale();
        }
    }

    private boolean noExternalStorageAccess() {
        if (!Ref.alive(activityRef)) {
            return true;
        }
        Activity activity = activityRef.get();
        return ActivityCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED ||
                ActivityCompat.checkSelfPermission(activity,  Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED;
    }

    private boolean noPhoneStateAccess() {
        if (!Ref.alive(activityRef)) {
            return true;
        }
        Activity activity = activityRef.get();
        return ActivityCompat.checkSelfPermission(activity, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_DENIED;
    }

    private void showExternalStoragePermissionsRationale() {
        if (!Ref.alive(activityRef)) {
            return;
        }
        final Activity activity = activityRef.get();
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setIcon(R.drawable.sd_card_notification);
        builder.setTitle(R.string.why_we_need_storage_permissions);
        builder.setMessage(R.string.why_we_need_storage_permissions_summary);
        builder.setNegativeButton(R.string.exit, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                shutdown();
            }
        });
        builder.setPositiveButton(R.string.accept, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, EXTERNAL_STORAGE_PERMISSIONS_REQUEST_CODE);
            }
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void showPhoneStatePermissionsRationale() {
        if (!Ref.alive(activityRef)) {
            return;
        }
        final Activity activity = activityRef.get();
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setIcon(R.drawable.device_type_type_phone);
        builder.setTitle(R.string.why_we_need_phone_state_permissions);
        builder.setMessage(R.string.why_we_need_phone_state_permissions_summary);
        builder.setNegativeButton(R.string.exit, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                UIUtils.showInformationDialog(activity, R.string.frostwire_warning_no_phone_state_permissions, 0, true, null);
            }
        });
        builder.setPositiveButton(R.string.accept, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.READ_PHONE_STATE}, PHONE_STATE_PERMISSIONS_REQUEST_CODE);
            }
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case EXTERNAL_STORAGE_PERMISSIONS_REQUEST_CODE:
                onExternalPermissionsResult(permissions, grantResults);
                break;
            case PHONE_STATE_PERMISSIONS_REQUEST_CODE:
                onPhoneStatePermissionsResult(permissions, grantResults);
                break;
            default:
                break;
        }
    }

    private void onPhoneStatePermissionsResult(String[] permissions, int[] grantResults) {
        if (!Ref.alive(activityRef)) {
            return;
        }
        final Activity activity = activityRef.get();
        for (int i=0; i<permissions.length; i++) {
            if (grantResults[i]== PackageManager.PERMISSION_DENIED) {
                if (permissions[i].equals(Manifest.permission.READ_PHONE_STATE)) {
                    UIUtils.showInformationDialog(activity, R.string.frostwire_warning_no_phone_state_permissions, 0, true, null);
                    return;
                }
            }

// TODO: Test if we have to restart, will need a phone, or a way to trigger a fake call on emulator.
//            UIUtils.showInformationDialog(mainActivity, R.string.restarting_summary, R.string.restarting, false, new DialogInterface.OnClickListener() {
//                @Override
//                public void onClick(DialogInterface dialog, int which) {
//                    mainActivity.restart(2000);
//                }
//            });
        }

    }

    private void onExternalPermissionsResult(String[] permissions, int[] grantResults) {
        if (!Ref.alive(activityRef)) {
            return;
        }
        final Activity activity = activityRef.get();
        for (int i=0; i<permissions.length; i++) {
            if (grantResults[i]== PackageManager.PERMISSION_DENIED) {
                if (permissions[i].equals(Manifest.permission.WRITE_EXTERNAL_STORAGE) ||
                        permissions[i].equals(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    UIUtils.showInformationDialog(activity, R.string.frostwire_shutting_down_no_permissions, 0, true,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    shutdown();
                                }
                            });
                    return;
                }
            }
            UIUtils.showInformationDialog(activity, R.string.restarting_summary, R.string.restarting, false, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    restart();
                }
            });
        }
    }

    private void shutdown() {
        if (!Ref.alive(activityRef)) {
            return;
        }
        final Activity activity = activityRef.get();
        Intent shutdownIntent = new Intent();
        shutdownIntent.putExtra("shutdown-" + ConfigurationManager.instance().getUUIDString(), true);
        activity.startActivity(shutdownIntent);
    }

    private void restart() {
        if (!Ref.alive(activityRef)) {
            return;
        }
        final Activity activity = activityRef.get();
        Intent shutdownIntent = new Intent();
        shutdownIntent.putExtra("restart-" + ConfigurationManager.instance().getUUIDString(), true);
        activity.startActivity(shutdownIntent);
    }
}
