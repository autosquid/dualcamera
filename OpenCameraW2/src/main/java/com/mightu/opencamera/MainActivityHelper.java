package com.mightu.opencamera;

import android.Manifest;
import android.app.ActivityManager;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.Toast;

import com.mightu.opencamera.CameraController.CameraControllerManager2;

import static android.content.Context.ACTIVITY_SERVICE;

/**
 * Created by uuplusu on 24/06/2017.
 */

public class MainActivityHelper {
    static final String TAG = "MainActivityHelper";

    MainActivity mainActivity;

    public MainActivityHelper(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
    }

    void setupPermissions() {
        String[] permissionsWeNeed = new String[]{
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.RECORD_AUDIO};

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            for (String permission : permissionsWeNeed) {
                if (ActivityCompat.checkSelfPermission(mainActivity, permission) != PackageManager.PERMISSION_GRANTED) {
                    mainActivity.requestPermissions(permissionsWeNeed, mainActivity.MY_PERMISSION_SEND_MSG_REQUEST_CODE);
                    break;
                }
            }
        }
    }


    void initCamera2Support() {
        if (MyDebug.LOG)
            Log.d(TAG, "initCamera2Support");
        mainActivity.supports_camera2 = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            CameraControllerManager2 manager2 = new CameraControllerManager2(mainActivity);
            mainActivity.supports_camera2 = true;
            if (manager2.getNumberOfCameras() == 0) {
                if (MyDebug.LOG)
                    Log.d(TAG, "Camera2 reports 0 cameras");
                mainActivity.supports_camera2 = false;
            }
            for (int i = 0; i < manager2.getNumberOfCameras() && mainActivity.supports_camera2; i++) {
                if (!manager2.allowCamera2Support(i)) {
                    if (MyDebug.LOG)
                        Log.d(TAG, "camera " + i + " doesn't have limited or full support for Camera2 API");
                    mainActivity.supports_camera2 = false;
                }
            }
        }
        if (MyDebug.LOG)
            Log.d(TAG, "supports_camera2? " + mainActivity.supports_camera2);
    }

     void inspect() {
        initCamera2Support();

        Toast.makeText(mainActivity, "camera 2 supported?" + String.valueOf(mainActivity.supports_camera2), Toast.LENGTH_LONG);

        ActivityManager activityManager = (ActivityManager) mainActivity.getSystemService(ACTIVITY_SERVICE);
        if (MyDebug.LOG) {
            Log.d(TAG, "standard max memory = " + activityManager.getMemoryClass() + "MB");
            Log.d(TAG, "large max memory = " + activityManager.getLargeMemoryClass() + "MB");
        }
        if (activityManager.getLargeMemoryClass() >= 128) {
            mainActivity.supports_auto_stabilise = true;
        }
        if (MyDebug.LOG)
            Log.d(TAG, "supports_auto_stabilise? " + mainActivity.supports_auto_stabilise);

        // hack to rule out phones unlikely to have 4K video, so no point even offering the option!
        // both S5 and Note 3 have 128MB standard and 512MB large heap (tested via Samsung RTL), as does Galaxy K Zoom
        // also added the check for having 128MB standard heap, to support modded LG G2, which has 128MB standard, 256MB large - see https://sourceforge.net/p/opencamera/tickets/9/
        if (activityManager.getMemoryClass() >= 128 || activityManager.getLargeMemoryClass() >= 512) {
            mainActivity.supports_force_video_4k = true;
        }
        if (MyDebug.LOG)
            Log.d(TAG, "supports_force_video_4k? " + mainActivity.supports_force_video_4k);
    }

}
