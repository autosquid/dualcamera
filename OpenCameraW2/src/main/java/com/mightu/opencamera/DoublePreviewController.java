package com.mightu.opencamera;


import android.app.Activity;
import android.hardware.Camera;
import android.hardware.SensorEvent;
import android.location.Location;
import android.os.Bundle;

import java.util.List;

import static java.lang.Math.max;
import static java.lang.Math.min;

public class DoublePreviewController {
    Preview[] previews = null;

    public DoublePreviewController(Preview p1, Preview p2) {
        previews = new Preview[]{p1, p2};
        previews[0].setMain(true);
        previews[1].setMain(false);
    }

     void prepare(){
         previews[0].switchToCamera(0);
         previews[1].switchToCamera(2);
     }

    Preview get(int i) {
        return previews[i];
    }


    void cancelTimer() {
        for (int i = 0; i < 2; i += 1) {
            previews[i].cancelTimer();
        }
    }


    void changeExposure(int change, boolean update_seek_bar) {
        for (int i = 0; i < 1; i += 1) {
            previews[i].changeExposure(change, update_seek_bar);
        }
    }


    void clickedTrash() {
        for (int i = 0; i < 2; i += 1) {
            previews[i].clickedTrash();
        }
    }

    void cycleFlash() {
        previews[0].cycleFlash();
    }

    void cycleFocusMode() {
            previews[0].cycleFocusMode();
    }

    boolean focusIsVideo() {
        for (int i = 0; i < 2; i += 1) {
            if (previews[i].focusIsVideo() == false) return false;
        }
        return true;
    }

    Camera[] getCamera() {
        Camera[] cameras = new Camera[2];
        for (int i = 0; i < 2; i += 1) {
            cameras[i] = previews[i].getCamera();
            if (cameras[i] == null) return null;
        }
        return cameras;
    }

    int[] getCameraId() {
        int[] ids = new int[2];
        for (int i = 0; i < 2; i += 1) {
            ids[i] = previews[i].getCameraId();
        }
        return ids;
    }

    int[] getCurrentExposure() {
        int[] ce = new int[2];
        for (int i = 0; i < 2; i += 1) {
            ce[i] = previews[i].getCurrentExposure();
        }
        return ce;
    }

    String[] getCurrentFocusValue() {
        String[] ce = new String[2];
        for (int i = 0; i < 2; i += 1) {
            ce[i] = previews[i].getCurrentFocusValue();
        }
        return ce;
    }

    String getISOKey() {
        return previews[0].getISOKey();
    }



    int getMaximumExposure() {
        int[] maxexps = new int[2];
        for (int i = 0; i < 2; i += 1) {
            maxexps[i] = previews[i].getMaximumExposure();
        }
        return min(maxexps[0], maxexps[1]);
    }

    int getMinimumExposure() {
        int[] minexps = new int[2];
        for (int i = 0; i < 2; i += 1) {
            minexps[i] = previews[i].getMinimumExposure();
        }
        return max(minexps[0], minexps[1]);
    }


    public List<String> getSupportedFlashValues() {
        List<String> a = previews[0].getSupportedFlashValues();
        a.retainAll(previews[1].getSupportedFlashValues());
        return a;
    }

    List<String> getSupportedFocusValues() {
        List<String> a = previews[0].getSupportedFocusValues();

        return a;
//        if (a==null) return null;
//        a.retainAll(previews[1].getSupportedFocusValues());
//        return a;
    }

    List<String> getSupportedISOs() {
        List<String> a = previews[0].getSupportedISOs();
        return a;
//        if (a==null) return null;
//        a.retainAll(previews[1].getSupportedISOs());
//        return a;
    }

    List<Camera.Size> getSupportedPictureSizes() {
        int i = 0;
        List<Camera.Size> a = previews[i].getSupportedPictureSizes();
        a.retainAll(previews[1].getSupportedPictureSizes());
        return a;
    }

    List<Camera.Size> getSupportedPreviewSizes() {
        int i = 0;
        List<Camera.Size> a = previews[i].getSupportedPreviewSizes();
        i = 1;
        a.retainAll(previews[i].getSupportedPreviewSizes());
        return a;
    }

    List<String> getSupportedWhiteBalances() {
        List<String> a = previews[0].getSupportedWhiteBalances();
        List<String> b = previews[1].getSupportedWhiteBalances();
        a.retainAll(b);
        return a;
    }

    List<Camera.Size> getSupportedVideoSizes(){
        List<Camera.Size> a = previews[0].getSupportedVideoSizes();
        List<Camera.Size> b = previews[1].getSupportedVideoSizes();
        a.retainAll(b);
        return a;
    }

    boolean isVideo() {
        return false;
    }

    void locationChanged(Location location) {
        for (int i = 0; i < 2; i += 1) {
            previews[i].locationChanged(location);
        }
    }

    void onAccelerometerSensorChanged(SensorEvent event) {
        for (int i = 0; i < 2; i += 1) {
            previews[i].onAccelerometerSensorChanged(event);
        }
    }

    void onMagneticSensorChanged(SensorEvent event) {
        for (int i = 0; i < 2; i += 1) {
            previews[i].onMagneticSensorChanged(event);
        }
    }

    void onPause() {
        for (int i = 0; i < 2; i += 1) {
            previews[i].onPause();
        }
    }

    void onResume() {
        for (int i = 0; i < 2; i += 1) {
            previews[i].onResume();
        }
    }

    void onResume(String toast_message) {
        for (int i = 0; i < 2; i += 1) {
            previews[i].onResume(toast_message);
        }
    }

    void onSaveInstanceState(Bundle state) {
        for (int i = 0; i < 2; i += 1) {
            previews[i].onSaveInstanceState(state);
        }
    }

    void pausePreview() {
        for (int i = 0; i < 2; i += 1) {
            previews[i].pausePreview();
        }
    }

    void requestAutoFocus() {
        for (int i = 0; i < 2; i += 1) {
            previews[i].requestAutoFocus();
        }
    }

    void resetLocation() {
        for (int i = 0; i < 2; i += 1) {
            previews[i].resetLocation();
        }
    }

    void setCameraDisplayOrientation(Activity activity) {
        for (int i = 0; i < 2; i += 1) {
            previews[i].setCameraDisplayOrientation(activity);
        }
    }

    void setExposure(int new_exposure, boolean update_seek_bar) {
        for (int i = 0; i < 2; i += 1) {
            previews[i].setExposure(new_exposure, update_seek_bar);
        }
    }

    void setUIRotation(int ui_rotation) {
        for (int i = 0; i < 2; i += 1) {
            previews[i].setUIRotation(ui_rotation);
        }
    }

    void setupCamera(String toast_message) {
        for (int i = 0; i < 2; i += 1) {
            previews[i].setupCamera(toast_message);
        }
    }

    void showToast(final ToastBoxer clear_toast, final int message_id) {
        for (int i = 0; i < 2; i += 1) {
            previews[i].showToast(clear_toast, message_id);
        }
    }

    void showToast(final ToastBoxer clear_toast, final String message) {
        for (int i = 0; i < 2; i += 1) {
            previews[i].showToast(clear_toast, message);
        }
    }


    public void showToast(final ToastBoxer clear_toast, final String message, final int duration) {
        for (int i = 0; i < 1; i += 1) {
            previews[i].showToast(clear_toast, message, duration);
        }
    }


    void stopVideo(boolean from_restart) {
        for (int i = 0; i < 2; i += 1) {
            previews[i].stopVideo(from_restart);
        }
    }

    boolean supportsExposures() {
        for (int i = 0; i < 2; i += 1) {
            if (previews[i].supportsExposures() == false)
                return false;
        }
        return true;
    }

    boolean supportsFaceDetection() {
        return false;
    }

    List<String> getSupportedSceneModes() {
        List<String> a = previews[0].getSupportedSceneModes();
        List<String> b = previews[1].getSupportedSceneModes();
        a.retainAll(b);
        return a;
    }

    List<String> getSupportedColorEffects() {
        List<String> a = previews[0].getSupportedColorEffects();
        List<String> b = previews[1].getSupportedColorEffects();
        a.retainAll(b);
        return a;
    }

    boolean supportsVideoStabilization() {
        return previews[0].supportsVideoStabilization() && previews[1].supportsVideoStabilization();
    }

    void switchToCamera(int cameraSwitchingTo) {
        previews[0].switchToCamera(0);
        previews[1].switchToCamera(2);
    }

    void takePicturePressed() {
        for (int i = 0; i < 2; i += 1) {
            previews[i].takePicturePressed();
        }
    }

    void toggleExposureLock() {
        for (int i = 0; i < 2; i += 1) {
            previews[i].toggleExposureLock();
        }
    }

    void updateFocus(String focus_value, boolean quiet, boolean auto_focus) {
        for (int i = 0; i < 2; i += 1) {
            previews[i].updateFocus(focus_value, quiet, auto_focus);
        }
    }

    void updateFocusForVideo(boolean auto_focus) {
        for (int i = 0; i < 2; i += 1) {
            previews[i].updateFocusForVideo(auto_focus);
        }
    }

    void updateUIPlacement() {
        for (int i = 0; i < 2; i += 1) {
            previews[i].updateUIPlacement();
        }
    }

    void zoomIn() {
        for (int i = 0; i < 2; i += 1) {
            previews[i].zoomIn();
        }
    }

    void zoomOut() {
        for (int i = 0; i < 2; i += 1) {
            previews[i].zoomOut();
        }
    }

}
