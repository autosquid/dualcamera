package com.mightu.opencamera;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.provider.MediaStore.Images.ImageColumns;
import android.provider.MediaStore.Video;
import android.provider.MediaStore.Video.VideoColumns;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Toast;

import com.mightu.opencamera.CameraController.CameraControllerManager;
import com.mightu.opencamera.XMLTasks.WriteConfXmlTask;
import com.mightu.opencamera.XMLTasks.WriteDataXmlTask;

import org.simpleframework.xml.core.Persister;
import org.simpleframework.xml.stream.Format;

import java.io.File;
import java.io.FilenameFilter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    static final String TAG = "MainActivity";
    static final int MY_PERMISSION_SEND_MSG_REQUEST_CODE = 88;
    private SensorManager mSensorManager = null;
    private Sensor mSensorAccelerometer = null;
    private Sensor mSensorMagnetic = null;
    private LocationManager mLocationManager = null;
    private LocationListener locationListener = null;
    private DoublePreviewController preview = null;
    private int current_orientation = 0;
    private OrientationEventListener orientationEventListener = null;
    boolean supports_auto_stabilise = false;
    boolean supports_force_video_4k = false;
    private ArrayList<String> save_location_history = new ArrayList<String>();
    private boolean camera_in_background = false; // whether the camera is covered by a fragment/dialog (such as settings or folder picker)
    private GestureDetector gestureDetector;
    private boolean screen_is_locked = false;
    private Map<Integer, Bitmap> preloaded_bitmap_resources = new Hashtable<Integer, Bitmap>();
    private PopupView popup_view = null;

    private ToastBoxer screen_locked_toast = new ToastBoxer();
    ToastBoxer changed_auto_stabilise_toast = new ToastBoxer();

    //zhangxaochen:
    boolean _isSensorSplit = false;
    boolean _isSensorOn = false;
    boolean _saveDataXmlFinished = false;
    boolean _savePicFinished = false;

    //{照片名, 时间戳} pair
    public List<String> _picNames = new ArrayList<String>();
    public List<Double> _picTimestamps = new ArrayList<Double>();
    MySensorListener _listener = new MySensorListener();
    NewSessionNode _newSessionNode = new NewSessionNode();

    //2015-4-13 21:33:27， 采集 IMU数据变为增加采集 BSSS 数据
    TelephonyManager _tManager;

    // for testing:
    public boolean is_test = false;
    public Bitmap gallery_bitmap = null;
    boolean supports_camera2 = false;
    private int ncam;

    //2015-4-14 23:02:36
    boolean _captureStarted = false;
    Persister _persister = new Persister(new Format("<?xml version=\"1.0\" encoding= \"UTF-8\" ?>"));

    final String _projXmlName = "collection-proj.xml";
    final String _dataXmlPrefix = "sensor";
    final String _dataXmlExt = "xml";

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSION_SEND_MSG_REQUEST_CODE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                } else {
                    Toast.makeText(this, "Permission for audio not granted. Visualizer can't run." + String.valueOf(grantResults[0]), Toast.LENGTH_LONG).show();
                    finish();
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    //zhangxaochen:
    void startCaptureSensor(NewSessionNode nsNode) {
        //2015-4-14 23:03:51： 加这个 _captureStarted flag， 防止先点录像按钮，导致 conf xml 文件中多记录了video文件名：
        _captureStarted = true;
        double beginTime = System.currentTimeMillis() * Consts.MS2S;
        nsNode.setBeginTime(beginTime);
        this._listener.set_baseTimestamp(beginTime / Consts.MS2S);
        this._listener._allowStoreData = true;
        System.out.println("setBeginTime: " + beginTime);
    }//startCaptureSensor

    void stopCaptureSensor(NewSessionNode nsNode) {
        /* 结束sensor数据采集 */
        //2015-4-14 23:05:03
        _captureStarted = false;
        nsNode.setEndTime(System.currentTimeMillis() * Consts.MS2S);
        nsNode.addNode(this._listener.getSensorData());
        System.out.println("+++++++++++++++1");

        try {
            String dataFolderName = this.getSaveLocation();
            File projFolder = new File(dataFolderName);
            System.out.println("projFolder: " + projFolder + ", " + projFolder.isDirectory() + ", " + dataFolderName);
            if (!projFolder.isDirectory() && !dataFolderName.contains("/")) { //最可能“OpenCamera”
                this._listener.reset();
                Builder builder = new Builder(this);
                builder
                        .setTitle("保存位置错误")
                        .setMessage("请在'OpenCamera'目录下新建子工程目录，以免不同次采集的数据混杂")
                        .setNegativeButton("放弃本次", null);
                builder.show();

                return;
            }
            //计数： 当前目录有多少 sensor_xxx.xml?
            int cntDataXml = projFolder.list(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String filename) {
                    return filename.contains(_dataXmlPrefix) && filename.endsWith(_dataXmlExt);
                }
            }).length;
            String dataXmlName = _dataXmlPrefix + "_" + cntDataXml + "." + _dataXmlExt;
            File dataXmlFile = new File(projFolder, dataXmlName);
            System.out.println("+++++++++++++++2");

            //--------------- 传感器数据 异步存文件
            WriteDataXmlTask dataXmlTask = new WriteDataXmlTask() {
                @Override
                protected void onPostExecute(Void result) {
                    super.onPostExecute(result);
                    _saveDataXmlFinished = true;
//				enableCaptureButton();
                }//onPostExecute
            };
            dataXmlTask.setXmlRootNode(nsNode)
                    .setFile(dataXmlFile)
                    .setPersister(_persister)
                    .execute();

            Toast.makeText(this, "picNames, etc" +
                    this._picNames.size() + ", " +
                    this._picTimestamps.size(), Toast.LENGTH_SHORT).show();
            Log.i(TAG, "---------------" + this.getSaveLocation() + ", " + _projXmlName + ", " + dataXmlName + ", " + this._picNames.get(0));

            //---------------异步写配置文件
            WriteConfXmlTask confXmlTask = new WriteConfXmlTask(
                    this, _persister,
                    new File(this.getSaveLocation(), _projXmlName),
                    dataXmlName);
            confXmlTask.execute();
            System.out.println("+++++++++++++++3");
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        submitjob();

        // _listener 不 unregister， 但是 reset 以禁止存数据到内存
        this._listener.reset();
    }//stopCaptureSensor


    CameraControllerManager camera_controller_manager = null;

    MainActivityHelper helper = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        helper = new MainActivityHelper(this);
        helper.setupPermissions();

        long time_s = System.currentTimeMillis();
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        if (getIntent() != null && getIntent().getExtras() != null) {
            is_test = getIntent().getExtras().getBoolean("test_project");
            if (MyDebug.LOG)
                Log.d(TAG, "is_test: " + is_test);
        }

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        helper.inspect();

        setWindowFlagsForCamera();

        // read save locations
        save_location_history.clear();
        int save_location_history_size = sharedPreferences.getInt("save_location_history_size", 0);
        if (MyDebug.LOG)
            Log.d(TAG, "save_location_history_size: " + save_location_history_size);
        for (int i = 0; i < save_location_history_size; i++) {
            String string = sharedPreferences.getString("save_location_history_" + i, null);
            if (string != null) {
                save_location_history.add(string);
            }
        }
        // also update, just in case a new folder has been set
        updateFolderHistory();

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
            if (MyDebug.LOG) Log.d(TAG, "found accelerometer");
            mSensorAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        } else {
            if (MyDebug.LOG) Log.d(TAG, "no support for accelerometer");
        }
        if (mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null) {
            if (MyDebug.LOG) Log.d(TAG, "found magnetic sensor");
            mSensorMagnetic = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        } else {
            if (MyDebug.LOG) Log.d(TAG, "no support for magnetic sensor");
        }

        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        clearSeekBar();

        Preview p1 = new Preview(this, savedInstanceState);
        ((ViewGroup) findViewById(R.id.preview)).addView(p1);

        Preview p2 = new Preview(this, savedInstanceState);
        ;
        ((ViewGroup) findViewById(R.id.preview2)).addView(p2);


        preview = new DoublePreviewController(p1, p2);

        preview.prepare();
        preview.setupCamera(null);

        orientationEventListener = new OrientationEventListener(this) {
            @Override
            public void onOrientationChanged(int orientation) {
                MainActivity.this.onOrientationChanged(orientation);
            }
        };


        gestureDetector = new GestureDetector(this, new MyGestureDetector());

        final String done_first_time_key = "done_first_time";
        boolean has_done_first_time = sharedPreferences.contains(done_first_time_key);
        if (!has_done_first_time && !is_test) {
            AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
            alertDialog.setTitle(R.string.app_name);
            alertDialog.setMessage(R.string.intro_text);
            alertDialog.setPositiveButton(R.string.intro_ok, null);
            alertDialog.show();

            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(done_first_time_key, true);
            editor.apply();
        }

        preloadIcons(R.array.flash_icons);
        preloadIcons(R.array.focus_mode_icons);

        // this is to overcome file share problem
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());

        if (MyDebug.LOG)
            Log.d(TAG, "time for Activity startup: " + (System.currentTimeMillis() - time_s));
    }


    private void preloadIcons(int icons_id) {
        long time_s = System.currentTimeMillis();
        String[] icons = getResources().getStringArray(icons_id);
        for (int i = 0; i < icons.length; i++) {
            int resource = getResources().getIdentifier(icons[i], null, this.getApplicationContext().getPackageName());
            if (MyDebug.LOG)
                Log.d(TAG, "load resource: " + resource);
            Bitmap bm = BitmapFactory.decodeResource(getResources(), resource);
            this.preloaded_bitmap_resources.put(resource, bm);
        }
        if (MyDebug.LOG) {
            Log.d(TAG, "time for preloadIcons: " + (System.currentTimeMillis() - time_s));
            Log.d(TAG, "size of preloaded_bitmap_resources: " + preloaded_bitmap_resources.size());
        }
    }

    @Override
    protected void onDestroy() {
        if (MyDebug.LOG) {
            Log.d(TAG, "onDestroy");
            Log.d(TAG, "size of preloaded_bitmap_resources: " + preloaded_bitmap_resources.size());
        }
        // Need to recycle to avoid out of memory when running tests - probably good practice to do anyway
        for (Map.Entry<Integer, Bitmap> entry : preloaded_bitmap_resources.entrySet()) {
            if (MyDebug.LOG)
                Log.d(TAG, "recycle: " + entry.getKey());
            entry.getValue().recycle();
        }
        preloaded_bitmap_resources.clear();
        super.onDestroy();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (MyDebug.LOG)
            Log.d(TAG, "onKeyDown: " + keyCode);
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_VOLUME_DOWN: {
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
                String volume_keys = sharedPreferences.getString("preference_volume_keys", "volume_take_photo");
                if (volume_keys.equals("volume_take_photo")) {
                    View view = findViewById(R.id.take_photo);
                    clickedTakePhoto(view);
                    //takePicture();
                    return true;
                } else if (volume_keys.equals("volume_focus")) {
                    preview.requestAutoFocus();
                    return true;
                } else if (volume_keys.equals("volume_zoom")) {
                    if (keyCode == KeyEvent.KEYCODE_VOLUME_UP)
                        this.preview.zoomIn();
                    else
                        this.preview.zoomOut();
                    return true;
                } else if (volume_keys.equals("volume_exposure")) {
//                    if (keyCode == KeyEvent.KEYCODE_VOLUME_UP)
//                        this.preview.changeExposure(1, true);
//                    else
//                        this.preview.changeExposure(-1, true);
                    return true;
                } else if (volume_keys.equals("volume_auto_stabilise")) {
                    if (this.supports_auto_stabilise) {
                        boolean auto_stabilise = sharedPreferences.getBoolean("preference_auto_stabilise", false);
                        auto_stabilise = !auto_stabilise;
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putBoolean("preference_auto_stabilise", auto_stabilise);
                        editor.apply();
                        String message = getResources().getString(R.string.preference_auto_stabilise) + ": " + getResources().getString(auto_stabilise ? R.string.on : R.string.off);
                        preview.showToast(changed_auto_stabilise_toast, message);
                    } else {
                        preview.showToast(changed_auto_stabilise_toast, R.string.auto_stabilise_not_supported);
                    }
                    return true;
                } else if (volume_keys.equals("volume_really_nothing")) {
                    // do nothing, but still return true so we don't change volume either
                    return true;
                }
                // else do nothing here, but still allow changing of volume (i.e., the default behaviour)
                break;
            }
            case KeyEvent.KEYCODE_MENU: {
                // needed to support hardware menu button
                // tested successfully on Samsung S3 (via RTL)
                // see http://stackoverflow.com/questions/8264611/how-to-detect-when-user-presses-menu-key-on-their-android-device
                openSettings();
                return true;
            }
            case KeyEvent.KEYCODE_CAMERA: {
                if (event.getRepeatCount() == 0) {
                    View view = findViewById(R.id.take_photo);
                    clickedTakePhoto(view);
                    return true;
                }
            }
            case KeyEvent.KEYCODE_FOCUS: {
                preview.requestAutoFocus();
                return true;
            }
            case KeyEvent.KEYCODE_ZOOM_IN: {
                preview.zoomIn();
                return true;
            }
            case KeyEvent.KEYCODE_ZOOM_OUT: {
                preview.zoomOut();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    private SensorEventListener accelerometerListener = new SensorEventListener() {
        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            preview.onAccelerometerSensorChanged(event);
        }
    };

    private SensorEventListener magneticListener = new SensorEventListener() {
        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            preview.onMagneticSensorChanged(event);
        }
    };


    private void setupLocationListener() {
        if (MyDebug.LOG)
            Log.d(TAG, "setupLocationListener");
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        // Define a listener that responds to location updates
        boolean store_location = sharedPreferences.getBoolean("preference_location", false);
        if (store_location && locationListener == null) {
            locationListener = new LocationListener() {
                public void onLocationChanged(Location location) {
                    if (MyDebug.LOG)
                        Log.d(TAG, "onLocationChanged");
                    preview.locationChanged(location);
                }

                public void onStatusChanged(String provider, int status, Bundle extras) {
                }

                public void onProviderEnabled(String provider) {
                }

                public void onProviderDisabled(String provider) {
                }
            };

            // see https://sourceforge.net/p/opencamera/tickets/1/
            if (mLocationManager.getAllProviders().contains(LocationManager.NETWORK_PROVIDER)) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                        && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
            }
            if (mLocationManager.getAllProviders().contains(LocationManager.GPS_PROVIDER)) {
                mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
            }
        } else if (!store_location && locationListener != null) {
            if (this.locationListener != null) {
                mLocationManager.removeUpdates(locationListener);
                locationListener = null;
            }
        }
    }

    @Override
    protected void onResume() {
        if (MyDebug.LOG)
            Log.d(TAG, "onResume");
        super.onResume();

        mSensorManager.registerListener(accelerometerListener, mSensorAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(magneticListener, mSensorMagnetic, SensorManager.SENSOR_DELAY_NORMAL);
        orientationEventListener.enable();

        setupLocationListener();

        if (!this.camera_in_background) {
            // immersive mode is cleared when app goes into background
            setImmersiveMode(true);
        }

        layoutUI();

        preview.onResume();

        Button btn = (Button) findViewById(R.id.bt_new_save);
        btn.setText(getButtonText());

        //zhangxaochen:
        _listener.reset();
//		_listener.registerWithSensorManager(mSensorManager, Consts.aMillion / 30);
        //2015-4-13 21:34:44
        _listener.registerWithSensorManager(mSensorManager, SensorManager.SENSOR_DELAY_FASTEST);

    }

    @Override
    protected void onPause() {
        if (MyDebug.LOG)
            Log.d(TAG, "onPause");
        super.onPause();
        closePopup();
        mSensorManager.unregisterListener(accelerometerListener);
        mSensorManager.unregisterListener(magneticListener);
        orientationEventListener.disable();
        if (this.locationListener != null) {
            mLocationManager.removeUpdates(locationListener);
            locationListener = null;
        }
        // reset location, as may be out of date when resumed - the location listener is reinitialised when resuming
        preview.resetLocation();
        preview.onPause();

        //zhangxaochen:
        _listener.unregisterWithSensorManager(mSensorManager);
    }

    public void layoutUI() {
        if (MyDebug.LOG)
            Log.d(TAG, "layoutUI");
        this.preview.updateUIPlacement();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String ui_placement = sharedPreferences.getString("preference_ui_placement", "ui_right");
        boolean ui_placement_right = ui_placement.equals("ui_right");
        if (MyDebug.LOG)
            Log.d(TAG, "ui_placement: " + ui_placement);
        // new code for orientation fixed to landscape
        // the display orientation should be locked to landscape, but how many degrees is that?
        int rotation = this.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }
        // getRotation is anti-clockwise, but current_orientation is clockwise, so we add rather than subtract
        // relative_orientation is clockwise from landscape-left
        //int relative_orientation = (current_orientation + 360 - degrees) % 360;
        int relative_orientation = (current_orientation + degrees) % 360;
        int ui_rotation = (360 - relative_orientation) % 360;
        preview.setUIRotation(ui_rotation);
        int align_left = RelativeLayout.ALIGN_LEFT;
        int align_right = RelativeLayout.ALIGN_RIGHT;
        //int align_top = RelativeLayout.ALIGN_TOP;
        //int align_bottom = RelativeLayout.ALIGN_BOTTOM;
        int left_of = RelativeLayout.LEFT_OF;
        int right_of = RelativeLayout.RIGHT_OF;
        int above = RelativeLayout.ABOVE;
        int below = RelativeLayout.BELOW;
        int align_parent_left = RelativeLayout.ALIGN_PARENT_LEFT;
        int align_parent_right = RelativeLayout.ALIGN_PARENT_RIGHT;
        int align_parent_top = RelativeLayout.ALIGN_PARENT_TOP;
        int align_parent_bottom = RelativeLayout.ALIGN_PARENT_BOTTOM;
        if (!ui_placement_right) {
            //align_top = RelativeLayout.ALIGN_BOTTOM;
            //align_bottom = RelativeLayout.ALIGN_TOP;
            above = RelativeLayout.BELOW;
            below = RelativeLayout.ABOVE;
            align_parent_top = RelativeLayout.ALIGN_PARENT_BOTTOM;
            align_parent_bottom = RelativeLayout.ALIGN_PARENT_TOP;
        }
        {
            View view = findViewById(R.id.settings);
            view.setRotation(ui_rotation);

            view = findViewById(R.id.popup);
            view.setRotation(ui_rotation);

            view = findViewById(R.id.exposure_lock);
            view.setRotation(ui_rotation);

            view = findViewById(R.id.exposure);
            view.setRotation(ui_rotation);

            view = findViewById(R.id.bt_new_save);
            view.setRotation(ui_rotation);

            view = findViewById(R.id.switch_sensor_split);
            view.setRotation(ui_rotation);

            view = findViewById(R.id.sensor_start);
            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) view.getLayoutParams();
            layoutParams.addRule(align_parent_top, RelativeLayout.TRUE);
            layoutParams.addRule(align_parent_bottom, 0);
            layoutParams.addRule(left_of, R.id.switch_sensor_split);
            layoutParams.addRule(right_of, 0);
            view.setLayoutParams(layoutParams);
            view.setRotation(ui_rotation);

            //------------------------------

            view = findViewById(R.id.share);
            layoutParams = (RelativeLayout.LayoutParams) view.getLayoutParams();
            layoutParams.addRule(align_parent_top, RelativeLayout.TRUE);
            layoutParams.addRule(align_parent_bottom, 0);
            layoutParams.addRule(left_of, R.id.trash);
            layoutParams.addRule(right_of, 0);
            view.setLayoutParams(layoutParams);
            view.setRotation(ui_rotation);

            view = findViewById(R.id.take_photo);
            layoutParams = (RelativeLayout.LayoutParams) view.getLayoutParams();
            layoutParams.addRule(align_parent_left, 0);
            layoutParams.addRule(align_parent_right, RelativeLayout.TRUE);
            view.setLayoutParams(layoutParams);
            view.setRotation(ui_rotation);

            view = findViewById(R.id.zoom);
            layoutParams = (RelativeLayout.LayoutParams) view.getLayoutParams();
            layoutParams.addRule(align_parent_left, 0);
            layoutParams.addRule(align_parent_right, RelativeLayout.TRUE);
            layoutParams.addRule(align_parent_top, 0);
            layoutParams.addRule(align_parent_bottom, RelativeLayout.TRUE);
            view.setLayoutParams(layoutParams);
            view.setRotation(180.0f); // should always match the zoom_seekbar, so that zoom in and out are in the same directions

            view = findViewById(R.id.zoom_seekbar);
            layoutParams = (RelativeLayout.LayoutParams) view.getLayoutParams();
            layoutParams.addRule(align_left, 0);
            layoutParams.addRule(align_right, R.id.zoom);
            layoutParams.addRule(above, R.id.zoom);
            layoutParams.addRule(below, 0);
            view.setLayoutParams(layoutParams);
        }

        {
            View view = findViewById(R.id.popup_container);
            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) view.getLayoutParams();
            //layoutParams.addRule(left_of, R.id.popup);
            layoutParams.addRule(align_right, R.id.popup);
            layoutParams.addRule(below, R.id.popup);
            layoutParams.addRule(align_parent_bottom, RelativeLayout.TRUE);
            layoutParams.addRule(above, 0);
            layoutParams.addRule(align_parent_top, 0);
            view.setLayoutParams(layoutParams);

            view.setRotation(ui_rotation);
            // reset:
            view.setTranslationX(0.0f);
            view.setTranslationY(0.0f);
            if (MyDebug.LOG) {
                Log.d(TAG, "popup view width: " + view.getWidth());
                Log.d(TAG, "popup view height: " + view.getHeight());
            }
            if (ui_rotation == 0 || ui_rotation == 180) {
                view.setPivotX(view.getWidth() / 2.0f);
                view.setPivotY(view.getHeight() / 2.0f);
            } else {
                view.setPivotX(view.getWidth());
                view.setPivotY(ui_placement_right ? 0.0f : view.getHeight());
                if (ui_placement_right) {
                    if (ui_rotation == 90)
                        view.setTranslationY(view.getWidth());
                    else if (ui_rotation == 270)
                        view.setTranslationX(-view.getHeight());
                } else {
                    if (ui_rotation == 90)
                        view.setTranslationX(-view.getHeight());
                    else if (ui_rotation == 270)
                        view.setTranslationY(-view.getWidth());
                }
            }
        }

        {
            // set icon for taking photos vs videos
            ImageButton view = (ImageButton) findViewById(R.id.take_photo);
            if (preview != null) {
                view.setImageResource(preview.isVideo() ? R.drawable.take_video_selector : R.drawable.take_photo_selector);
            }
        }
    }

    private void onOrientationChanged(int orientation) {
        /*if( MyDebug.LOG ) {
            Log.d(TAG, "onOrientationChanged()");
			Log.d(TAG, "orientation: " + orientation);
			Log.d(TAG, "current_orientation: " + current_orientation);
		}*/
        if (orientation == OrientationEventListener.ORIENTATION_UNKNOWN)
            return;
        int diff = Math.abs(orientation - current_orientation);
        if (diff > 180)
            diff = 360 - diff;
        // only change orientation when sufficiently changed
        if (diff > 60) {
            orientation = (orientation + 45) / 90 * 90;
            orientation = orientation % 360;
            if (orientation != current_orientation) {
                this.current_orientation = orientation;
                if (MyDebug.LOG) {
                    Log.d(TAG, "current_orientation is now: " + current_orientation);
                }
                layoutUI();
            }
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        if (MyDebug.LOG)
            Log.d(TAG, "onConfigurationChanged()");
        // configuration change can include screen orientation (landscape/portrait) when not locked (when settings is open)
        // needed if app is paused/resumed when settings is open and device is in portrait mode
        preview.setCameraDisplayOrientation(this);
        super.onConfigurationChanged(newConfig);
    }

    int currentjobs = 0;

    void startalljobs() {
        currentjobs = 3;
    }

    String getButtonText() {
        String dataFolderName = this.getSaveLocation();
        try {
            File projFolder = new File(dataFolderName);
            int cntDataXml = projFolder.list(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String filename) {
                    return filename.contains(_dataXmlPrefix) && filename.endsWith(_dataXmlExt);
                }
            }).length;

            return dataFolderName.substring(dataFolderName.lastIndexOf('/') + 12)
                    + "(" + String.valueOf(cntDataXml) + ")";
        } catch (Exception e){
            return getString(R.string.button_default_text);
        }
    }

    void submitjob() {
        synchronized (this) {
            currentjobs -= 1;
        }
        if (alljobdone()) {
            ImageButton takebutton = (ImageButton) findViewById(R.id.take_photo);
            takebutton.setVisibility(View.VISIBLE);

            Button nbt = (Button) findViewById(R.id.bt_new_save);

            nbt.setText(getButtonText());
        }
    }

    boolean alljobdone() {
        return currentjobs == 0;
    }

    public void clickedTakePhoto(View view) {
        //todo: here we should start the sensor capture task
        startalljobs();

        ImageButton takebutton = (ImageButton) findViewById(R.id.take_photo);
        takebutton.setVisibility(View.INVISIBLE);

        preview.takePicturePressed();
    }

    //zhangxaochen:
    public void clickedSwitchSensorSplit(View view) {
        if (MyDebug.LOG)
            Log.d(TAG, "clickedSwitchSensorSplit");

        //切换 "分/合" 图标:
        this._isSensorSplit = !this._isSensorSplit;
        ImageButton btnSensorSplit = (ImageButton) findViewById(R.id.switch_sensor_split);
        btnSensorSplit.setImageResource(this._isSensorSplit ? R.drawable.sensor_fen
                : R.drawable.sensor_he);

        //"分" 时, 单独显示一个 "sensor_on/off" 图标, 用于控制采集：
        ImageButton btnSensorStart = (ImageButton) findViewById(R.id.sensor_start);
        btnSensorStart.setVisibility(_isSensorSplit ? View.VISIBLE : View.GONE);

    }//clickedSwitchCamera

    /**
     * 触摸"△/||"按钮, 回调
     *
     * @param view
     */
    public void clickedSensorStart(View view) {
        if (MyDebug.LOG)
            Log.d(TAG, "clickedSensorStart");

        //TODO:
        this._isSensorOn = !this._isSensorOn;
        ImageButton btnSensorStart = (ImageButton) findViewById(R.id.sensor_start);
        btnSensorStart.setImageResource(this._isSensorOn ? R.drawable.sensor_on
                : R.drawable.sensor_off);
        //此时 "分合" 禁用:
        ImageButton btnSensorSplit = (ImageButton) findViewById(R.id.switch_sensor_split);
        //btnSensorSplit.setClickable(!is_sensor_on);
        btnSensorSplit.setEnabled(!_isSensorOn);

        if (_isSensorOn)
            startCaptureSensor(_newSessionNode);
        else {
            stopCaptureSensor(_newSessionNode);
        }
    }//clickedSensorStart

    public void fastDefaultNewFolder(View view) {
        Log.d(TAG, "fast new folder");
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        Date d = new Date();
        String new_save_location = DateFormat.format("yyyy-MM-dd-HH-mm-ss", d.getTime()).toString();

        File new_folder = new File(new File(getBaseFolder(), getString(R.string.parent_folder_name)), new_save_location);

        if (!new_folder.exists()) {
            if (MyDebug.LOG)
                Log.d(TAG, "create new folder" + new_folder);
            if (!new_folder.mkdirs()) {
                if (MyDebug.LOG)
                    Log.d(TAG, "failed to create new folder");
                // don't do anything yet, this is handled below
            }
        }

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("preference_save_location", new_folder.getAbsolutePath());
        editor.apply();

        Button nbt = (Button) view;
        nbt.setText(getButtonText());
    }

    public void clickedFlash(View view) {
        if (MyDebug.LOG)
            Log.d(TAG, "clickedFlash");
        this.preview.cycleFlash();
    }

    public void clickedFocusMode(View view) {
        if (MyDebug.LOG)
            Log.d(TAG, "clickedFocusMode");
        this.preview.cycleFocusMode();
    }

    void clearSeekBar() {
        View view = findViewById(R.id.seekbar);
        view.setVisibility(View.GONE);
        view = findViewById(R.id.seekbar_zoom);
        view.setVisibility(View.GONE);
    }

    void setSeekBarExposure() {
//        SeekBar seek_bar = ((SeekBar) findViewById(R.id.seekbar));
//        final int min_exposure = preview.getMinimumExposure();
//        seek_bar.setMax(preview.getMaximumExposure() - min_exposure);
//        seek_bar.setProgress(preview.getCurrentExposure() - min_exposure);
    }

    public void clickedExposure(View view) {
        if (MyDebug.LOG)
            Log.d(TAG, "clickedExposure");
        this.closePopup();
        SeekBar seek_bar = ((SeekBar) findViewById(R.id.seekbar));
        int visibility = seek_bar.getVisibility();
        if (visibility == View.GONE && preview.getCamera() != null && preview.supportsExposures()) {
            final int min_exposure = preview.getMinimumExposure();
            seek_bar.setVisibility(View.VISIBLE);
            setSeekBarExposure();
            seek_bar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (MyDebug.LOG)
                        Log.d(TAG, "exposure seekbar onProgressChanged");
                    preview.setExposure(min_exposure + progress, false);
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                }
            });
        } else if (visibility == View.VISIBLE) {
            clearSeekBar();
        }
    }

    public void clickedExposureLock(View view) {
        if (MyDebug.LOG)
            Log.d(TAG, "clickedExposureLock");
        this.preview.toggleExposureLock();
    }


    public void clickedSettings(View view) {
        if (MyDebug.LOG)
            Log.d(TAG, "clickedSettings");
        openSettings();
    }

    public boolean popupIsOpen() {
        return popup_view != null;
    }


    // for testing
    public View getPopupButton(String key) {
        return popup_view.getPopupButton(key);
    }

    void closePopup() {
        if (MyDebug.LOG)
            Log.d(TAG, "close popup");
        if (popupIsOpen()) {
            ViewGroup popup_container = (ViewGroup) findViewById(R.id.popup_container);
            popup_container.removeAllViews();
            popup_view.close();
            popup_view = null;
        }
    }

    Bitmap getPreloadedBitmap(int resource) {
        Bitmap bm = this.preloaded_bitmap_resources.get(resource);
        return bm;
    }

    public void clickedPopupSettings(View view) {
        if (MyDebug.LOG)
            Log.d(TAG, "clickedPopupSettings");
        final ViewGroup popup_container = (ViewGroup) findViewById(R.id.popup_container);
        if (popupIsOpen()) {
            closePopup();
            return;
        }
        if (preview.getCamera() == null) {
            if (MyDebug.LOG)
                Log.d(TAG, "camera not opened!");
            return;
        }

        if (MyDebug.LOG)
            Log.d(TAG, "open popup");

        clearSeekBar();
        preview.cancelTimer(); // best to cancel any timer, in case we take a photo while settings window is open, or when changing settings

        final long time_s = System.currentTimeMillis();

        {
            // prevent popup being transparent
            popup_container.setBackgroundColor(Color.BLACK);
            popup_container.setAlpha(0.95f);
        }

        popup_view = new PopupView(this);
        popup_container.addView(popup_view);

        // need to call layoutUI to make sure the new popup is oriented correctly
        // but need to do after the layout has been done, so we have a valid width/height to use
        popup_container.getViewTreeObserver().addOnGlobalLayoutListener(
                new OnGlobalLayoutListener() {
                    @SuppressWarnings("deprecation")
                    @SuppressLint("NewApi")
                    @Override
                    public void onGlobalLayout() {
                        if (MyDebug.LOG)
                            Log.d(TAG, "onGlobalLayout()");
                        if (MyDebug.LOG)
                            Log.d(TAG, "time after global layout: " + (System.currentTimeMillis() - time_s));
                        layoutUI();
                        if (MyDebug.LOG)
                            Log.d(TAG, "time after layoutUI: " + (System.currentTimeMillis() - time_s));
                        // stop listening - only want to call this once!
                        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                            popup_container.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        } else {
                            popup_container.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                        }

                        ScaleAnimation animation = new ScaleAnimation(0.0f, 1.0f, 0.0f, 1.0f, Animation.RELATIVE_TO_SELF, 1.0f, Animation.RELATIVE_TO_SELF, 0.0f);
                        animation.setDuration(100);
                        popup_container.setAnimation(animation);
                    }
                }
        );

        if (MyDebug.LOG)
            Log.d(TAG, "time to create popup: " + (System.currentTimeMillis() - time_s));
    }

    private void openSettings() {
        if (MyDebug.LOG)
            Log.d(TAG, "openSettings");
        closePopup();
        preview.cancelTimer(); // best to cancel any timer, in case we take a photo while settings window is open, or when changing settings
        preview.stopVideo(false); // important to stop video, as we'll be changing camera parameters when the settings window closes

        Bundle bundle = new Bundle();
        //bundle.putInt("cameraId", this.preview.getCameraId());
        bundle.putBoolean("supports_auto_stabilise", this.supports_auto_stabilise);
        bundle.putBoolean("supports_force_video_4k", this.supports_force_video_4k);
        bundle.putBoolean("supports_face_detection", this.preview.supportsFaceDetection());
        bundle.putBoolean("supports_video_stabilization", this.preview.supportsVideoStabilization());

        putBundleExtra(bundle, "color_effects", this.preview.getSupportedColorEffects());
        putBundleExtra(bundle, "scene_modes", this.preview.getSupportedSceneModes());
        putBundleExtra(bundle, "white_balances", this.preview.getSupportedWhiteBalances());
        putBundleExtra(bundle, "isos", this.preview.getSupportedISOs());
        bundle.putString("iso_key", this.preview.getISOKey());
        if (this.preview.getCamera() != null) {
            bundle.putString("parameters_string", this.preview.get(0).getCamera().getParameters().flatten());
        }

        List<Camera.Size> preview_sizes = this.preview.getSupportedPreviewSizes();
        if (preview_sizes != null) {
            int[] widths = new int[preview_sizes.size()];
            int[] heights = new int[preview_sizes.size()];
            int i = 0;
            for (Camera.Size size : preview_sizes) {
                widths[i] = size.width;
                heights[i] = size.height;
                i++;
            }
            bundle.putIntArray("preview_widths", widths);
            bundle.putIntArray("preview_heights", heights);
        }

        List<Camera.Size> sizes = this.preview.getSupportedPictureSizes();
        if (sizes != null) {
            int[] widths = new int[sizes.size()];
            int[] heights = new int[sizes.size()];
            int i = 0;
            for (Camera.Size size : sizes) {
                widths[i] = size.width;
                heights[i] = size.height;
                i++;
            }
            bundle.putIntArray("resolution_widths", widths);
            bundle.putIntArray("resolution_heights", heights);
        }


        List<Camera.Size> video_sizes = this.preview.getSupportedVideoSizes();
        if (video_sizes != null) {
            int[] widths = new int[video_sizes.size()];
            int[] heights = new int[video_sizes.size()];
            int i = 0;
            for (Camera.Size size : video_sizes) {
                widths[i] = size.width;
                heights[i] = size.height;
                i++;
            }
            bundle.putIntArray("video_widths", widths);
            bundle.putIntArray("video_heights", heights);
        }

        putBundleExtra(bundle, "flash_values", this.preview.getSupportedFlashValues());
        putBundleExtra(bundle, "focus_values", this.preview.getSupportedFocusValues());

        setWindowFlagsForSettings();
        MyPreferenceFragment fragment = new MyPreferenceFragment();
        fragment.setArguments(bundle);
        getFragmentManager().beginTransaction().add(R.id.prefs_container, fragment, "PREFERENCE_FRAGMENT").addToBackStack(null).commit();
    }

    public void updateForSettings() {
        updateForSettings(null);
    }

    public void updateForSettings(String toast_message) {
        if (MyDebug.LOG) {
            Log.d(TAG, "updateForSettings()");
            if (toast_message != null) {
                Log.d(TAG, "toast_message: " + toast_message);
            }
        }
        String saved_focus_value = null;
        if (preview.getCamera() != null && preview.isVideo() && !preview.focusIsVideo()) {
//            saved_focus_value = preview.getCurrentFocusValue(); // n.b., may still be null
//            // make sure we're into continuous video mode
//            // workaround for bug on Samsung Galaxy S5 with UHD, where if the user switches to another (non-continuous-video) focus mode, then goes to Settings, then returns and records video, the preview freezes and the video is corrupted
//            // so to be safe, we always reset to continuous video mode, and then reset it afterwards
//            preview.updateFocusForVideo(false);
        }
        if (MyDebug.LOG)
            Log.d(TAG, "saved_focus_value: " + saved_focus_value);

        updateFolderHistory();

        // update camera for changes made in prefs - do this without closing and reopening the camera app if possible for speed!
        // but need workaround for Nexus 7 bug, where scene mode doesn't take effect unless the camera is restarted - I can reproduce this with other 3rd party camera apps, so may be a Nexus 7 issue...
        boolean need_reopen = false;
        if (preview.getCamera() != null) {
            Camera.Parameters parameters = preview.getCamera()[0].getParameters();
            if (MyDebug.LOG)
                Log.d(TAG, "scene mode was: " + parameters.getSceneMode());
            String key = Preview.getSceneModePreferenceKey();
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
            String value = sharedPreferences.getString(key, Camera.Parameters.SCENE_MODE_AUTO);
            if (!value.equals(parameters.getSceneMode())) {
                if (MyDebug.LOG)
                    Log.d(TAG, "scene mode changed to: " + value);
                need_reopen = true;
            }
        }

        layoutUI(); // needed in case we've changed left/right handed UI
        setupLocationListener(); // in case we've enabled GPS
        if (need_reopen || preview.getCamera() == null) { // if camera couldn't be opened before, might as well try again
            preview.onPause();
            preview.onResume(toast_message);
        } else {
            preview.setCameraDisplayOrientation(this); // need to call in case the preview rotation option was changed
            preview.pausePreview();
            preview.setupCamera(toast_message);
        }

        if (saved_focus_value != null) {
            if (MyDebug.LOG)
                Log.d(TAG, "switch focus back to: " + saved_focus_value);
            preview.updateFocus(saved_focus_value, true, false);
        }
    }

    boolean cameraInBackground() {
        return this.camera_in_background;
    }

    MyPreferenceFragment getPreferenceFragment() {
        MyPreferenceFragment fragment = (MyPreferenceFragment) getFragmentManager().findFragmentByTag("PREFERENCE_FRAGMENT");
        return fragment;
    }

    @Override
    public void onBackPressed() {
        final MyPreferenceFragment fragment = getPreferenceFragment();
        if (screen_is_locked) {
            preview.showToast(screen_locked_toast, R.string.screen_is_locked);
            return;
        }
        if (fragment != null) {
            if (MyDebug.LOG)
                Log.d(TAG, "close settings");
            setWindowFlagsForCamera();
            updateForSettings();
        } else {
            if (popupIsOpen()) {
                closePopup();
                return;
            }
        }
        super.onBackPressed();
    }

    //@TargetApi(Build.VERSION_CODES.KITKAT)
    private void setImmersiveMode(boolean on) {
        // Andorid 4.4 immersive mode disabled for now, as not clear of a good way to enter and leave immersive mode, and "sticky" might annoy some users
        /*if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT ) {
            if( on )
        		getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN);
        	else
        		getWindow().getDecorView().setSystemUiVisibility(0);
        }*/
        if (on)
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
        else
            getWindow().getDecorView().setSystemUiVisibility(0);
    }

    private void setWindowFlagsForCamera() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        // force to landscape mode
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        // keep screen active - see http://stackoverflow.com/questions/2131948/force-screen-on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (sharedPreferences.getBoolean("preference_show_when_locked", true)) {
            // keep Open Camera on top of screen-lock (will still need to unlock when going to gallery or settings)
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        }

        // set screen to max brightness - see http://stackoverflow.com/questions/11978042/android-screen-brightness-max-value
        // done here rather than onCreate, so that changing it in preferences takes effect without restarting app
        {
            WindowManager.LayoutParams layout = getWindow().getAttributes();
            if (sharedPreferences.getBoolean("preference_max_brightness", true)) {
                layout.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL;
            } else {
                layout.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
            }
            getWindow().setAttributes(layout);
        }

        setImmersiveMode(true);

        camera_in_background = false;
    }

    private void setWindowFlagsForSettings() {
        // allow screen rotation
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        // revert to standard screen blank behaviour
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        // settings should still be protected by screen lock
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);

        {
            WindowManager.LayoutParams layout = getWindow().getAttributes();
            layout.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
            getWindow().setAttributes(layout);
        }

        setImmersiveMode(false);

        camera_in_background = true;
    }

    class Media {
        public long id;
        public boolean video;
        public Uri uri;
        public long date;
        public int orientation;

        Media(long id, boolean video, Uri uri, long date, int orientation) {
            this.id = id;
            this.video = video;
            this.uri = uri;
            this.date = date;
            this.orientation = orientation;
        }
    }

    private Media getLatestMedia(boolean video) {
        Media media = null;
        Uri baseUri = video ? Video.Media.EXTERNAL_CONTENT_URI : MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        Uri query = baseUri.buildUpon().appendQueryParameter("limit", "1").build();
        String[] projection = video ? new String[]{VideoColumns._ID, VideoColumns.DATE_TAKEN} : new String[]{ImageColumns._ID, ImageColumns.DATE_TAKEN, ImageColumns.ORIENTATION};
        String selection = video ? "" : ImageColumns.MIME_TYPE + "='image/jpeg'";
        String order = video ? VideoColumns.DATE_TAKEN + " DESC," + VideoColumns._ID + " DESC" : ImageColumns.DATE_TAKEN + " DESC," + ImageColumns._ID + " DESC";
        Cursor cursor = null;
        try {
            cursor = getContentResolver().query(query, projection, selection, null, order);
            if (cursor != null && cursor.moveToFirst()) {
                long id = cursor.getLong(0);
                long date = cursor.getLong(1);
                int orientation = video ? 0 : cursor.getInt(2);
                Uri uri = ContentUris.withAppendedId(baseUri, id);
                if (MyDebug.LOG)
                    Log.d(TAG, "found most recent uri for " + (video ? "video" : "images") + ": " + uri);
                media = new Media(id, video, uri, date, orientation);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return media;
    }

    private Media getLatestMedia() {
        Media image_media = getLatestMedia(false);
        Media video_media = getLatestMedia(true);
        Media media = null;
        if (image_media != null && video_media == null) {
            if (MyDebug.LOG)
                Log.d(TAG, "only found images");
            media = image_media;
        } else if (image_media == null && video_media != null) {
            if (MyDebug.LOG)
                Log.d(TAG, "only found videos");
            media = video_media;
        } else if (image_media != null && video_media != null) {
            if (MyDebug.LOG) {
                Log.d(TAG, "found images and videos");
                Log.d(TAG, "latest image date: " + image_media.date);
                Log.d(TAG, "latest video date: " + video_media.date);
            }
            if (image_media.date >= video_media.date) {
                if (MyDebug.LOG)
                    Log.d(TAG, "latest image is newer");
                media = image_media;
            } else {
                if (MyDebug.LOG)
                    Log.d(TAG, "latest video is newer");
                media = video_media;
            }
        }
        return media;
    }


    public void updateFolderHistory() {
        String folder_name = getSaveLocation();
        updateFolderHistory(folder_name);
    }

    private void updateFolderHistory(String folder_name) {
        while (save_location_history.remove(folder_name)) {
        }
        save_location_history.add(folder_name);
        while (save_location_history.size() > 6) {
            save_location_history.remove(0);
        }
        writeSaveLocations();
    }

    public void clearFolderHistory() {
        save_location_history.clear();
        updateFolderHistory(); // to re-add the current choice, and save
    }

    private void writeSaveLocations() {
        if (MyDebug.LOG)
            Log.d(TAG, "writeSaveLocations");
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("save_location_history_size", save_location_history.size());
        if (MyDebug.LOG)
            Log.d(TAG, "save_location_history_size = " + save_location_history.size());
        for (int i = 0; i < save_location_history.size(); i++) {
            String string = save_location_history.get(i);
            editor.putString("save_location_history_" + i, string);
        }
        editor.apply();
    }

    private void longClickedGallery() {
        if (MyDebug.LOG) {
            Log.d(TAG, "longClickedGallery");
            System.out.println("---------------longClickedGallery, save_location_history.size(): " + save_location_history.size());
        }
        if (save_location_history.size() <= 0) {
            if (MyDebug.LOG) {
                Log.d(TAG, "save_location_history.size() <= 1,,,," + save_location_history.size());
                System.out.println("save_location_history.size() <= 1,,,," + save_location_history.size());
            }
            return;
        }
        final int theme = android.R.style.Theme_Black_NoTitleBar_Fullscreen;
        //final int theme = android.R.style.Theme_Black;
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this, theme);
        alertDialog.setTitle(R.string.choose_save_location);
        CharSequence[] items = new CharSequence[save_location_history.size() + 1];
        int index = 0;
        // save_location_history is stored in order most-recent-last
        for (int i = 0; i < save_location_history.size(); i++) {
            items[index++] = save_location_history.get(save_location_history.size() - 1 - i);
        }
        final int clear_index = index;
        items[index++] = getResources().getString(R.string.clear_folder_history);
        /*final int new_index = index;
        items[index++] = getResources().getString(R.string.new_save_location);*/
        alertDialog.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == clear_index) {
                    if (MyDebug.LOG)
                        Log.d(TAG, "selected clear save history");
                    new AlertDialog.Builder(MainActivity.this)
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setTitle(R.string.clear_folder_history)
                            .setMessage(R.string.clear_folder_history_question)
                            .setPositiveButton(R.string.answer_yes, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if (MyDebug.LOG)
                                        Log.d(TAG, "confirmed clear save history");
                                    clearFolderHistory();
                                }
                            })
                            .setNegativeButton(R.string.answer_no, null)
                            .show();
                    setWindowFlagsForCamera();
                } else {
                    if (MyDebug.LOG)
                        Log.d(TAG, "selected: " + which);
                    if (which >= 0 && which < save_location_history.size()) {
                        String save_folder = save_location_history.get(save_location_history.size() - 1 - which);
                        if (MyDebug.LOG)
                            Log.d(TAG, "changed save_folder from history to: " + save_folder);
                        preview.showToast(null, getResources().getString(R.string.changed_save_location) + "\n" + save_folder);
                        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putString("preference_save_location", save_folder);
                        editor.apply();
                        updateFolderHistory(); // to move new selection to most recent
                    }
                    setWindowFlagsForCamera();
                }
            }
        });
        alertDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface arg0) {
                setWindowFlagsForCamera();
            }
        });
        alertDialog.show();
        //getWindow().setLayout(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
        setWindowFlagsForSettings();
    }

    static private void putBundleExtra(Bundle bundle, String key, List<String> values) {
        if (values != null) {
            String[] values_arr = new String[values.size()];
            int i = 0;
            for (String value : values) {
                values_arr[i] = value;
                i++;
            }
            bundle.putStringArray(key, values_arr);
        }
    }

    //deprecation
    public void clickedShare(View view) {

    }

    //deprecation
    public void clickedTrash(View view) {
    }

    private void takePicture() {
        if (MyDebug.LOG)
            Log.d(TAG, "takePicture");
        closePopup();

        // camera id == 0
        Log.d(TAG, "===================");
        Log.d(TAG, "Before Taking A Picture, which camera is this? " + String.valueOf(this.preview.getCameraId()));
        this.preview.takePicturePressed();
    }


    void lockScreen() {
        ((ViewGroup) findViewById(R.id.locker)).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View arg0, MotionEvent event) {
                return gestureDetector.onTouchEvent(event);
                //return true;
            }
        });
        screen_is_locked = true;
    }

    void unlockScreen() {
        ((ViewGroup) findViewById(R.id.locker)).setOnTouchListener(null);
        screen_is_locked = false;
    }

    boolean isScreenLocked() {
        return screen_is_locked;
    }

    class MyGestureDetector extends SimpleOnGestureListener {
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            try {
                if (MyDebug.LOG)
                    Log.d(TAG, "from " + e1.getX() + " , " + e1.getY() + " to " + e2.getX() + " , " + e2.getY());
                final ViewConfiguration vc = ViewConfiguration.get(MainActivity.this);
                //final int swipeMinDistance = 4*vc.getScaledPagingTouchSlop();
                final float scale = getResources().getDisplayMetrics().density;
                final int swipeMinDistance = (int) (160 * scale + 0.5f); // convert dps to pixels
                final int swipeThresholdVelocity = vc.getScaledMinimumFlingVelocity();
                if (MyDebug.LOG) {
                    Log.d(TAG, "from " + e1.getX() + " , " + e1.getY() + " to " + e2.getX() + " , " + e2.getY());
                    Log.d(TAG, "swipeMinDistance: " + swipeMinDistance);
                }
                float xdist = e1.getX() - e2.getX();
                float ydist = e1.getY() - e2.getY();
                float dist2 = xdist * xdist + ydist * ydist;
                float vel2 = velocityX * velocityX + velocityY * velocityY;
                if (dist2 > swipeMinDistance * swipeMinDistance && vel2 > swipeThresholdVelocity * swipeThresholdVelocity) {
                    preview.showToast(screen_locked_toast, R.string.unlocked);
                    unlockScreen();
                }
            } catch (Exception e) {
            }
            return false;
        }

        @Override
        public boolean onDown(MotionEvent e) {
            preview.showToast(screen_locked_toast, R.string.screen_is_locked);
            return true;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        if (MyDebug.LOG)
            Log.d(TAG, "onSaveInstanceState");
        super.onSaveInstanceState(state);
        if (this.preview != null) {
            preview.onSaveInstanceState(state);
        }
    }

    public void broadcastFile(File file, boolean is_new_picture, boolean is_new_video) {
        // note that the new method means that the new folder shows up as a file when connected to a PC via MTP (at least tested on Windows 8)
        if (file.isDirectory()) {
            //this.sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.fromFile(file)));
            // ACTION_MEDIA_MOUNTED no longer allowed on Android 4.4! Gives: SecurityException: Permission Denial: not allowed to send broadcast android.intent.action.MEDIA_MOUNTED
            // note that we don't actually need to broadcast anything, the folder and contents appear straight away (both in Gallery on device, and on a PC when connecting via MTP)
            // also note that we definitely don't want to broadcast ACTION_MEDIA_SCANNER_SCAN_FILE or use scanFile() for folders, as this means the folder shows up as a file on a PC via MTP (and isn't fixed by rebooting!)
        } else {
            // both of these work fine, but using MediaScannerConnection.scanFile() seems to be preferred over sending an intent
            //this.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)));
            MediaScannerConnection.scanFile(this, new String[]{file.getAbsolutePath()}, null,
                    new MediaScannerConnection.OnScanCompletedListener() {
                        public void onScanCompleted(String path, Uri uri) {
                            if (MyDebug.LOG) {
                                Log.d("ExternalStorage", "Scanned " + path + ":");
                                Log.d("ExternalStorage", "-> uri=" + uri);
                            }
                        }
                    }
            );
            if (is_new_picture) {
                this.sendBroadcast(new Intent(Camera.ACTION_NEW_PICTURE, Uri.fromFile(file)));
                // for compatibility with some apps - apparently this is what used to be broadcast on Android?
                this.sendBroadcast(new Intent("com.android.camera.NEW_PICTURE", Uri.fromFile(file)));
            } else if (is_new_video) {
                this.sendBroadcast(new Intent(Camera.ACTION_NEW_VIDEO, Uri.fromFile(file)));
            }
        }
    }

    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;

    public String getSaveLocation() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String folder_name = sharedPreferences.getString("preference_save_location", "OpenCamera");
//		System.out.println("getSaveLocation(): "+folder_name);
        return folder_name;
    }

    static File getBaseFolder() {
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
    }

    static File getImageFolder(String folder_name) {
        File file = null;
        if (folder_name.length() > 0 && folder_name.lastIndexOf('/') == folder_name.length() - 1) {
            // ignore final '/' character
            folder_name = folder_name.substring(0, folder_name.length() - 1);
        }
        //if( folder_name.contains("/") ) {
        if (folder_name.startsWith("/")) {
            file = new File(folder_name);
        } else {
            file = new File(getBaseFolder(), folder_name);
        }
        /*if( MyDebug.LOG ) {
            Log.d(TAG, "folder_name: " + folder_name);
			Log.d(TAG, "full path: " + file);
		}*/
        return file;
    }

    public File getImageFolder() {
        String folder_name = getSaveLocation();
        return getImageFolder(folder_name);
    }

    /**
     * Create a File for saving an image or video
     */
    @SuppressLint("SimpleDateFormat")
    public File getOutputMediaFile(Preview p, int type) {
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = getImageFolder();
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                if (MyDebug.LOG)
                    Log.e(TAG, "failed to create directory");
                return null;
            }
            broadcastFile(mediaStorageDir, false, false);
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmssSSS").format(new Date());
        String index = "";
        File mediaFile = null;
        String suffix = null;
        if (p == preview.get(0))
            suffix = "_0";
        else
            suffix = "_1";
        for (int count = 1; count <= 100; count++) {
            if (type == MEDIA_TYPE_IMAGE) {
                mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                        "IMG_" + timeStamp + index + suffix + ".png");
            } else if (type == MEDIA_TYPE_VIDEO) {
                mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                        "VID_" + timeStamp + index + ".mp4");
            } else {
                return null;
            }
            if (!mediaFile.exists()) {
                break;
            }
            index = "_" + count; // try to find a unique filename
        }


        if (MyDebug.LOG) {
            Log.d(TAG, "getOutputMediaFile returns: " + mediaFile);
        }
        return mediaFile;
    }

    public boolean supportsAutoStabilise() {
        return this.supports_auto_stabilise;
    }

    public boolean supportsForceVideo4K() {
        return this.supports_force_video_4k;
    }

    @SuppressWarnings("deprecation")
    public long freeMemory() { // return free memory in MB
        try {
            File image_folder = this.getImageFolder();
            StatFs statFs = new StatFs(image_folder.getAbsolutePath());
            // cast to long to avoid overflow!
            long blocks = statFs.getAvailableBlocks();
            long size = statFs.getBlockSize();
            long free = (blocks * size) / 1048576;

            return free;
        } catch (IllegalArgumentException e) {
            // can fail on emulator, at least!
            return -1;
        }
    }

    public Preview getPreview() {
        //TODO: better design to control each camera.
        return this.preview.get(0);
    }

    // for testing:
    public ArrayList<String> getSaveLocationHistory() {
        return this.save_location_history;
    }

    public LocationListener getLocationListener() {
        return this.locationListener;
    }
}