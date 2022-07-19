/*===============================================================================

Copyright (c) 2012-2014 Qualcomm Connected Experiences, Inc. All Rights Reserved.

Vuforia is a trademark of QUALCOMM Incorporated, registered in the United States 
and other countries. Trademarks of QUALCOMM Incorporated are used with permission.
===============================================================================*/

package com.mobiletechnologylab.wound_imager.image;

import static com.mobiletechnologylab.apilib.apis.common.ToastUtils.toast;
import static com.mobiletechnologylab.storagelib.utils.FolderStructure.getWoundVisibleMeasurementsDir;
import static com.mobiletechnologylab.wound_imager.Commons.EXTRA_CURRENT_TRIAL_COUNT_KEY;
import static com.mobiletechnologylab.wound_imager.Commons.EXTRA_NUMBER_OF_TRIALS_KEY;
import static com.mobiletechnologylab.wound_imager.Commons.EXTRA_PATIENT_AGE_KEY;
import static com.mobiletechnologylab.wound_imager.Commons.EXTRA_PATIENT_GENDER_KEY;
import static com.mobiletechnologylab.wound_imager.Commons.EXTRA_PATIENT_HEIGHT_KEY;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.media.MediaScannerConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import androidx.annotation.NonNull;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.mobiletechnologylab.storagelib.utils.FolderStructure;
import com.mobiletechnologylab.wound_imager.ApplicationControl;
import com.mobiletechnologylab.wound_imager.ApplicationException;
import com.mobiletechnologylab.wound_imager.ApplicationSession;
import com.mobiletechnologylab.wound_imager.R;
import com.mobiletechnologylab.wound_imager.history.OrchestratorActivity;
import com.mobiletechnologylab.wound_imager.ui.AppMenu;
import com.mobiletechnologylab.wound_imager.ui.AppMenuGroup;
import com.mobiletechnologylab.wound_imager.ui.AppMenuInterface;
import com.mobiletechnologylab.wound_imager.ui.ReviewImageActivity;
import com.mobiletechnologylab.wound_imager.utils.ApplicationGLView;
import com.mobiletechnologylab.wound_imager.utils.LoadingDialogHandler;
import com.mobiletechnologylab.wound_imager.utils.Texture;
import com.mobiletechnologylab.storagelib.utils.PermissionsHandler;
import com.qualcomm.vuforia.CameraDevice;
import com.qualcomm.vuforia.DataSet;
import com.qualcomm.vuforia.ObjectTracker;
import com.qualcomm.vuforia.STORAGE_TYPE;
import com.qualcomm.vuforia.State;
import com.qualcomm.vuforia.Trackable;
import com.qualcomm.vuforia.Tracker;
import com.qualcomm.vuforia.TrackerManager;
import com.qualcomm.vuforia.Vuforia;
import org.opencv.android.OpenCVLoader;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Vector;


public class ImageTargets extends Activity implements ApplicationControl,
                                                      AppMenuInterface {

    private static final int NEXT_TRIAL_REQUEST_CODE = 101;
    private static final int REVIEW_IMG = 201;
    public static String KEY_PFM_MEASUREMENT = "pfmMeasurement";
    public static String KEY_PFM_PERCENT_NORMAL = "pfmPercentNormal";
    public static int NUMBER_OF_TRIALS = 3;
    public final static String EXTRA_PATIENT_NAME = "com.mobiletechnologylab.peakflowmeter.ImageTargets.PATIENT_NAME";
    public final static String EXTRA_PATIENT_ID = "com.mobiletechnologylab.peakflowmeter.ImageTargets.PATIENT_ID";

    private static final String LOGTAG = "ImageTargets";
    private static final String AR_FILE_EXT = ".txt";

    public static final SimpleDateFormat THERMAL_DATE_FMT = new SimpleDateFormat(
            "yyyy.MM.dd.HH.mm.ss", Locale.US);

    private DecimalFormat format;

    private int patientHeight;
    private String patientGender;
    private int patientAge;
    private int mCurrentTrialCount;
    private int mNumberOfTrials;

    ApplicationSession vuforiaAppSession;

    private DataSet mCurrentDataset;
    private int mCurrentDatasetSelectionIndex = 0;
    private int mStartDatasetsIndex = 0;
    private int mDatasetsNumber = 0;
    private int onScreen = -1;
    private ArrayList<String> mDatasetStrings = new ArrayList<String>();

    // Our OpenGL view:
    private ApplicationGLView mGlView;
    private ImageTargetRenderer mRenderer;
    private GestureDetector mGestureDetector;
    private Vector<Texture> mTextures;

    private boolean mSwitchDatasetAsap = false;
    private boolean mFlash = false;
    private boolean mContAutofocus = false;
    private boolean mExtendedTracking = false;

    private View mFlashOptionView;
    private RelativeLayout mUILayout;
    private RelativeLayout mOverlayView;
    private AppMenu mSampleAppMenu;
    LoadingDialogHandler loadingDialogHandler = new LoadingDialogHandler(this);

    boolean mIsDroidDevice = false;
    private String measuredValue = "none";

    private float lastMeasurement = -1;
    private float mCurrentMaxReading;
    private boolean mARPaused = false;

    private static final String TAG = "ImageTargets";

    private PermissionsHandler permissionsHandler;

    private static final boolean FAKE_IT_ALL = false;

    public static final String STORAGE_FOLDER = "MobileTechLab/WoundAssessment";
    private Bitmap imageCapture;
    private Bitmap colorChartCapture;

    private ImageButton mSaveButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(LOGTAG, "onCreate");
        super.onCreate(savedInstanceState);

        if (FAKE_IT_ALL) {
            Intent output = new Intent();
            output.putExtra(KEY_PFM_MEASUREMENT, 300.0f);
            output.putExtra(KEY_PFM_PERCENT_NORMAL, 1.1f);
            setResult(RESULT_OK, output);
            finish();
            return;
        }

        Bundle bundle = getIntent().getExtras();

        if (bundle != null) {
            patientHeight = bundle.getInt(EXTRA_PATIENT_HEIGHT_KEY);
            patientGender = bundle.getString(EXTRA_PATIENT_GENDER_KEY);
            String ageStr = bundle.getString(EXTRA_PATIENT_AGE_KEY);

            try {
                patientAge = Integer.parseInt(ageStr);
            } catch (NumberFormatException e) {
                Log.e(TAG, "Age Error for " + ageStr, e);
                patientAge = 21;
            }

            mNumberOfTrials = bundle.getInt(EXTRA_NUMBER_OF_TRIALS_KEY, NUMBER_OF_TRIALS);
            mCurrentTrialCount = bundle.getInt(EXTRA_CURRENT_TRIAL_COUNT_KEY, 1);
            mCurrentMaxReading = bundle.getInt(KEY_PFM_MEASUREMENT, -1);
        }

        vuforiaAppSession = new ApplicationSession(this);

        format = new DecimalFormat("0.5f");

        if (!OpenCVLoader.initDebug()) {
            // Handle initialization error
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        permissionsHandler = new PermissionsHandler(this, PermissionsHandler.CAMERA, () -> {
            startLoadingAnimation();
            mDatasetStrings.add("woundTargetV8.xml");

            vuforiaAppSession.initAR(this, ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

            mGestureDetector = new GestureDetector(this, new GestureListener());
            mTextures = new Vector<Texture>();
            loadTextures();

            mIsDroidDevice = android.os.Build.MODEL.toLowerCase().startsWith("droid");
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        permissionsHandler.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    // Process Single Tap event to trigger autofocus
    private class GestureListener extends
                                  GestureDetector.SimpleOnGestureListener {

        private final Handler autofocusHandler = new Handler();

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            autofocusHandler.postDelayed(new Runnable() {
                public void run() {
                    boolean result = CameraDevice.getInstance().setFocusMode(
                            CameraDevice.FOCUS_MODE.FOCUS_MODE_TRIGGERAUTO);
                    if (!result) {
                        Log.e("SingleTapUp", "Unable to trigger focus");
                    }
                }
            }, 1000L);
            return true;
        }
    }

    private void loadTextures() {
        mTextures.add(Texture.loadTextureFromApk("heart-tex.png", getAssets()));
    }

    @Override
    protected void onResume() {
        Log.d(LOGTAG, "onResume");
        super.onResume();

        if (mSaveButton != null)
        {
            mSaveButton.setBackgroundResource(R.drawable.button_not_ready);
            mSaveButton.setEnabled(false);
        }

        // This is needed for some Droid devices to force portrait
        if (mIsDroidDevice) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        try {
            vuforiaAppSession.resumeAR();
        } catch (ApplicationException e) {
            Log.e(LOGTAG, e.getString());
        }

        // Resume the GL view:
        if (mGlView != null) {
            mGlView.setVisibility(View.VISIBLE);
            mGlView.onResume();
        }
    }

    // Callback for configuration changes the activity handles itself
    @Override
    public void onConfigurationChanged(Configuration config) {
        Log.d(LOGTAG, "onConfigurationChanged");
        super.onConfigurationChanged(config);

        vuforiaAppSession.onConfigurationChanged();
    }

    // Called when the system is about to start resuming a previous activity.
    @Override
    protected void onPause() {
        Log.d(LOGTAG, "onPause");
        super.onPause();

        if (mGlView != null) {
            mGlView.setVisibility(View.INVISIBLE);
            mGlView.onPause();
        }

//        if (mFlashOptionView != null && mFlash) {
        if (mFlashOptionView != null && !mFlash) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                ((Switch) mFlashOptionView).setChecked(false);
            } else {
                ((CheckBox) mFlashOptionView).setChecked(false);
            }
        }

        try {
            vuforiaAppSession.pauseAR();
        } catch (ApplicationException e) {
            Log.e(LOGTAG, e.getString());
        }
    }

    // The final call you receive before your activity is destroyed.
    @Override
    protected void onDestroy() {
        Log.d(LOGTAG, "onDestroy");
        if (FAKE_IT_ALL) {
            super.onDestroy();
            return;
        }

        try {
            vuforiaAppSession.stopAR();
        } catch (ApplicationException e) {
            Log.e(LOGTAG, e.getString());
        }

        mTextures.clear();
        mTextures = null;
        System.gc();
        super.onDestroy();
    }

    // Initializes AR application components.
    private void initApplicationAR() {
        int depthSize = 16;
        int stencilSize = 0;
        boolean translucent = Vuforia.requiresAlpha();

        mGlView = new ApplicationGLView(this);
        mGlView.init(translucent, depthSize, stencilSize);
        mRenderer = new ImageTargetRenderer(this, vuforiaAppSession);
        mRenderer.setTextures(mTextures);
        mGlView.setRenderer(mRenderer);
    }


    private void startLoadingAnimation() {
        LayoutInflater inflater = LayoutInflater.from(this);
        mUILayout = (RelativeLayout) inflater.inflate(R.layout.camera_overlay,
                null, false);
        mOverlayView = (RelativeLayout) inflater.inflate(R.layout.info_overlay,
                null);

        mUILayout.setVisibility(View.VISIBLE);
        mUILayout.setBackgroundColor(Color.BLACK);

        loadingDialogHandler.mLoadingDialogContainer = mUILayout
                .findViewById(R.id.loading_indicator);
        loadingDialogHandler
                .sendEmptyMessage(LoadingDialogHandler.SHOW_LOADING_DIALOG);

        addContentView(mUILayout, new LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT));

        mSaveButton = (ImageButton) mOverlayView.findViewById(R.id.bcontinue);
        mSaveButton.setEnabled(false);
    }


    @Override
    public boolean doLoadTrackersData() {
        TrackerManager tManager = TrackerManager.getInstance();
        ObjectTracker imageTracker = (ObjectTracker) tManager
                .getTracker(ObjectTracker.getClassType());
        if (imageTracker == null) {
            return false;
        }

        if (mCurrentDataset == null) {
            mCurrentDataset = imageTracker.createDataSet();
        }

        if (mCurrentDataset == null) {
            return false;
        }

        if (!mCurrentDataset.load(
                mDatasetStrings.get(mCurrentDatasetSelectionIndex),
                STORAGE_TYPE.STORAGE_APPRESOURCE)) {
            return false;
        }

        if (!imageTracker.activateDataSet(mCurrentDataset)) {
            return false;
        }

        int numTrackables = mCurrentDataset.getNumTrackables();
        for (int count = 0; count < numTrackables; count++) {
            Trackable trackable = mCurrentDataset.getTrackable(count);
            if (isExtendedTrackingActive()) {
                trackable.startExtendedTracking();
            }

            String name = "Current Dataset : " + trackable.getName();
            trackable.setUserData(name);
            Log.d(LOGTAG, "UserData:Set the following user data "
                    + (String) trackable.getUserData());
        }

        return true;
    }


    @Override
    public boolean doUnloadTrackersData() {
        // Indicate if the trackers were unloaded correctly
        boolean result = true;

        TrackerManager tManager = TrackerManager.getInstance();
        ObjectTracker imageTracker = (ObjectTracker) tManager
                .getTracker(ObjectTracker.getClassType());
        if (imageTracker == null) {
            return false;
        }

        if (mCurrentDataset != null && mCurrentDataset.isActive()) {
            if (imageTracker.getActiveDataSet().equals(mCurrentDataset)
                    && !imageTracker.deactivateDataSet(mCurrentDataset)) {
                result = false;
            } else if (!imageTracker.destroyDataSet(mCurrentDataset)) {
                result = false;
            }
            mCurrentDataset = null;
        }

        return result;
    }


    @SuppressWarnings("deprecation")
    @Override
    public void onInitARDone(ApplicationException exception) {

        if (exception == null) {
            initApplicationAR();

            mRenderer.mIsActive = true;
            addContentView(mGlView, new LayoutParams(LayoutParams.MATCH_PARENT,
                    LayoutParams.MATCH_PARENT));

            addContentView(mOverlayView, new LayoutParams(
                    LayoutParams.FILL_PARENT,
                    LayoutParams.FILL_PARENT));

            mUILayout.bringToFront();
            mUILayout.setBackgroundColor(Color.TRANSPARENT);

            try {
                vuforiaAppSession.startAR(CameraDevice.CAMERA.CAMERA_DEFAULT);
            } catch (ApplicationException e) {
                Log.e(LOGTAG, e.getString());
            }

            boolean result = CameraDevice.getInstance().setFocusMode(
                    CameraDevice.FOCUS_MODE.FOCUS_MODE_CONTINUOUSAUTO);

            if (result) {
                mContAutofocus = true;
            } else {
                Log.e(LOGTAG, "Unable to enable continuous autofocus");
            }

            //Macro Focus Capability Added
            boolean resultMacro = CameraDevice.getInstance().setFocusMode(
                    CameraDevice.FOCUS_MODE.FOCUS_MODE_MACRO);

            if (resultMacro) {
                mContAutofocus = true;
            } else {
                Log.e(LOGTAG, "Unable to enable macro focus");
            }

            mSampleAppMenu = new AppMenu(this, this, "Image Targets",
                    mGlView, mUILayout, null);
            setSampleAppMenuSettings();
//            CameraDevice.getInstance().setFlashTorchMode(!mFlash);
            CameraDevice.getInstance().setFlashTorchMode(mFlash);

        } else {
            Log.e(LOGTAG, exception.getString());
            finish();
        }
    }


    @Override
    public void onQCARUpdate(State state) {
        if (mSwitchDatasetAsap) {
            mSwitchDatasetAsap = false;
            TrackerManager tm = TrackerManager.getInstance();
            ObjectTracker it = (ObjectTracker) tm.getTracker(ObjectTracker
                    .getClassType());
            if (it == null || mCurrentDataset == null
                    || it.getActiveDataSet() == null) {
                Log.d(LOGTAG, "Failed to swap datasets");
                return;
            }

            doUnloadTrackersData();
            doLoadTrackersData();
        }
    }


    @Override
    public boolean doInitTrackers() {
        boolean result = true;

        TrackerManager tManager = TrackerManager.getInstance();
        Tracker tracker;
        tracker = tManager.initTracker(ObjectTracker.getClassType());
        if (tracker == null) {
            Log.e(
                    LOGTAG,
                    "Tracker not initialized. Tracker already initialized or the camera is already started");
            result = false;
        } else {
            Log.i(LOGTAG, "Tracker successfully initialized");
        }
        return result;
    }


    @Override
    public boolean doStartTrackers() {
        // Indicate if the trackers were started correctly
        boolean result = true;

        Tracker imageTracker = TrackerManager.getInstance().getTracker(
                ObjectTracker.getClassType());
        if (imageTracker != null) {
            imageTracker.start();
        }

        return result;
    }


    @Override
    public boolean doStopTrackers() {
        // Indicate if the trackers were stopped correctly
        boolean result = true;

        Tracker imageTracker = TrackerManager.getInstance().getTracker(
                ObjectTracker.getClassType());
        if (imageTracker != null) {
            imageTracker.stop();
        }

        return result;
    }


    @Override
    public boolean doDeinitTrackers() {
        // Indicate if the trackers were deinitialized correctly
        boolean result = true;

        TrackerManager tManager = TrackerManager.getInstance();
        tManager.deinitTracker(ObjectTracker.getClassType());

        return result;
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Process the Gestures
        if (mSampleAppMenu != null && mSampleAppMenu.processEvent(event)) {
            return true;
        }

        return mGestureDetector.onTouchEvent(event);
    }


    boolean isExtendedTrackingActive() {
        return mExtendedTracking;
    }

    final public static int CMD_BACK = -1;
    final public static int CMD_EXTENDED_TRACKING = 1;
    final public static int CMD_AUTOFOCUS = 2;
    final public static int CMD_FLASH = 3;
    final public static int CMD_CAMERA_FRONT = 4;
    final public static int CMD_CAMERA_REAR = 5;
    final public static int CMD_DATASET_START_INDEX = 6;


    // This method sets the menu's settings
    private void setSampleAppMenuSettings() {
        AppMenuGroup group;
        group = mSampleAppMenu.addGroup("", false);
        group.addTextItem(getString(R.string.menu_back), -1);

        group = mSampleAppMenu.addGroup("", true);
        group.addSelectionItem(getString(R.string.menu_extended_tracking),
                CMD_EXTENDED_TRACKING, false);
        group.addSelectionItem(getString(R.string.menu_contAutofocus),
                CMD_AUTOFOCUS, mContAutofocus);
        mFlashOptionView = group.addSelectionItem(
                getString(R.string.menu_flash), CMD_FLASH, false);

        CameraInfo ci = new CameraInfo();
        boolean deviceHasFrontCamera = false;
        boolean deviceHasBackCamera = false;
        for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
            Camera.getCameraInfo(i, ci);
            if (ci.facing == CameraInfo.CAMERA_FACING_FRONT) {
                deviceHasFrontCamera = true;
            } else if (ci.facing == CameraInfo.CAMERA_FACING_BACK) {
                deviceHasBackCamera = true;
            }
        }

        if (deviceHasBackCamera && deviceHasFrontCamera) {
            group = mSampleAppMenu.addGroup(getString(R.string.menu_camera),
                    true);
            group.addRadioItem(getString(R.string.menu_camera_front),
                    CMD_CAMERA_FRONT, false);
            group.addRadioItem(getString(R.string.menu_camera_back),
                    CMD_CAMERA_REAR, true);
        }

        group = mSampleAppMenu
                .addGroup(getString(R.string.menu_datasets), true);
        mStartDatasetsIndex = CMD_DATASET_START_INDEX;
        mDatasetsNumber = mDatasetStrings.size();

        group.addRadioItem("DLab_marker", mStartDatasetsIndex, true);

        mSampleAppMenu.attachMenu();
    }


    //Update data onscreen
    public void updateByteCount(int count, int pixel, float x, float y, boolean bol) {
        TextView text = (TextView) findViewById(R.id.feedback_textView);

        text.setText("Searching...");
        text.setTextColor(pixel);
        measuredValue = "none";

        if (bol) {
            String message = "String.valueOf(count) + L/M";
            measuredValue = String.valueOf(count);
            text.setText(message);
        }

        setCurrentColor(text.getCurrentTextColor());
    }

    public int getCurrentColor() {
        return onScreen;
    }

    private void setCurrentColor(int arg) {
        onScreen = arg;
    }

    @Override
    public boolean menuProcess(int command) {

        boolean result = true;
        switch (command) {
            case CMD_BACK:
                finish();
                break;

            case CMD_FLASH:
                result = CameraDevice.getInstance().setFlashTorchMode(!mFlash);

                if (result) {
                    mFlash = !mFlash;
                } else {
                    showToast(getString(mFlash ? R.string.menu_flash_error_off
                            : R.string.menu_flash_error_on));
                    Log.e(LOGTAG,
                            getString(mFlash ? R.string.menu_flash_error_off
                                    : R.string.menu_flash_error_on));
                }
                break;

            case CMD_AUTOFOCUS:
                if (mContAutofocus) {
                    result = CameraDevice.getInstance().setFocusMode(
                            CameraDevice.FOCUS_MODE.FOCUS_MODE_NORMAL);

                    if (result) {
                        mContAutofocus = false;
                    } else {
                        showToast(getString(R.string.menu_contAutofocus_error_off));
                        Log.e(LOGTAG,
                                getString(R.string.menu_contAutofocus_error_off));
                    }
                } else {
                    result = CameraDevice.getInstance().setFocusMode(
                            CameraDevice.FOCUS_MODE.FOCUS_MODE_CONTINUOUSAUTO);

                    if (result) {
                        mContAutofocus = true;
                    } else {
                        showToast(getString(R.string.menu_contAutofocus_error_on));
                        Log.e(LOGTAG,
                                getString(R.string.menu_contAutofocus_error_on));
                    }
                }

                break;

            case CMD_CAMERA_FRONT:
            case CMD_CAMERA_REAR:

                // Turn off the flash
//                if (mFlashOptionView != null && mFlash) {
                if (mFlashOptionView != null && !mFlash) {
                    // OnCheckedChangeListener is called upon changing the checked state
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                        ((Switch) mFlashOptionView).setChecked(false);
                    } else {
                        ((CheckBox) mFlashOptionView).setChecked(false);
                    }
                }

                vuforiaAppSession.stopCamera();

                try {
                    vuforiaAppSession
                            .startAR(command == CMD_CAMERA_FRONT ? CameraDevice.CAMERA.CAMERA_FRONT
                                    : CameraDevice.CAMERA.CAMERA_BACK);
                } catch (ApplicationException e) {
                    showToast(e.getString());
                    Log.e(LOGTAG, e.getString());
                    result = false;
                }
                doStartTrackers();
                break;

            case CMD_EXTENDED_TRACKING:
                for (int tIdx = 0; tIdx < mCurrentDataset.getNumTrackables(); tIdx++) {
                    Trackable trackable = mCurrentDataset.getTrackable(tIdx);

                    if (!mExtendedTracking) {
                        if (!trackable.startExtendedTracking()) {
                            Log.e(LOGTAG,
                                    "Failed to start extended tracking target");
                            result = false;
                        } else {
                            Log.d(LOGTAG,
                                    "Successfully started extended tracking target");
                        }
                    } else {
                        if (!trackable.stopExtendedTracking()) {
                            Log.e(LOGTAG,
                                    "Failed to stop extended tracking target");
                            result = false;
                        } else {
                            Log.d(LOGTAG,
                                    "Successfully started extended tracking target");
                        }
                    }
                }

                if (result) {
                    mExtendedTracking = !mExtendedTracking;
                }
                break;

            default:
                if (command >= mStartDatasetsIndex
                        && command < mStartDatasetsIndex + mDatasetsNumber) {
                    mSwitchDatasetAsap = true;
                    mCurrentDatasetSelectionIndex = command
                            - mStartDatasetsIndex;
                }
                break;
        }
        return result;
    }

    private void showToast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    private String saveImageToSdCard(Bitmap bmp, String filepath) {
        try {
            File jpg = new File(filepath);
            FileOutputStream fos = new FileOutputStream(jpg);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 1;
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            fos.close();
            MediaScannerConnection
                    .scanFile(getApplicationContext(), new String[]{jpg.getAbsolutePath()}, null,
                            null); // needed so that file shows up on phone without having to reboot phone

            return jpg.getAbsolutePath();
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
        }

        return null;
    }

    public void goForth(View v) {
        File woundVisibleHomeFolder = new File(FolderStructure.ROOT, FolderStructure.WOUND_VISIBLE_APP_FOLDER);
        File tempImageFolder = new File(woundVisibleHomeFolder, "temp");

        if (!tempImageFolder.exists()) {
            tempImageFolder.mkdirs();
        }
        String woundImagePath = new File(tempImageFolder, "visible.jpg").getAbsolutePath();
        String colorChartPath = new File(tempImageFolder, "colorChart.jpg").getAbsolutePath();

        saveImageToSdCard(imageCapture, woundImagePath);
        saveImageToSdCard(colorChartCapture, colorChartPath);

        Intent output = new Intent();
        output.putExtra(OrchestratorActivity.ARG_TEMP_WOUND_IMAGE_PATH, woundImagePath);
        output.putExtra(OrchestratorActivity.ARG_TEMP_COLOR_CHART_PATH, colorChartPath);
        setResult(RESULT_OK, output);

        finish();

//        Intent output = new Intent();

//        ByteArrayOutputStream woundStream = new ByteArrayOutputStream();
//        imageCapture.compress(Bitmap.CompressFormat.PNG, 100, woundStream);
//        byte[] woundImageByteArray = woundStream.toByteArray();
//        output.putExtra(ReviewImageActivity.ARG_WOUND_IMAGE, woundImageByteArray);
//
//        ByteArrayOutputStream colorChartStream = new ByteArrayOutputStream();
//        colorChartCapture.compress(Bitmap.CompressFormat.PNG, 100, colorChartStream);
//        byte[] colorChartImageByteArray = colorChartStream.toByteArray();
//        output.putExtra(ReviewImageActivity.ARG_COLOR_CHART_IMAGE, colorChartImageByteArray);

//        setResult(RESULT_OK, output);
//        finish();
    }

    public void updateCapture(Bitmap woundImage, Bitmap colorChartImage) {
        ImageView view = ((ImageView) findViewById(R.id.capture));
        view.setImageBitmap(woundImage);
        view.setVisibility(View.VISIBLE);
        imageCapture = woundImage;
        colorChartCapture = colorChartImage;
    }

    public void hideCapture() {
        ImageView view = ((ImageView) findViewById(R.id.capture));
        view.setVisibility(View.GONE);
    }

    public void setPhi(double phi) {
        if (!mARPaused) {
            TextView text = (TextView) findViewById(R.id.feedback_textView);
            text.setText("Phi: " + String.format("%.4f", phi));

            if (phi > 0.5) {
                mSaveButton.setBackgroundResource(R.drawable.button_not_ready);
                mSaveButton.setEnabled(false);
            }
//            else if (phi > 0.2) {
//                mSaveButton.setBackgroundResource(R.drawable.button_almost_ready);
//                mSaveButton.setEnabled(false);
//            }
            else {
                mSaveButton.setBackgroundResource(R.drawable.button_ready);
                mSaveButton.setEnabled(true);
            }
        }
    }

    public void displayTargetFound() {
        TextView text = (TextView) findViewById(R.id.feedback_textView);
        text.setText("Target found");
    }

    private void resetARWork() {
        // Turn off the flash

//        if (mFlashOptionView != null && mFlash) {
        if (mFlashOptionView != null && !mFlash) {
            // OnCheckedChangeListener is called upon changing the checked state
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                ((Switch) mFlashOptionView).setChecked(false);
            } else {
                ((CheckBox) mFlashOptionView).setChecked(false);
            }
        }

        vuforiaAppSession.stopCamera();
        mARPaused = true;
//        lastMeasurement = -1;
//        TextView feedbackTextView = (TextView) findViewById(R.id.feedback_textView);
//        feedbackTextView.setText("Searching...");
//        feedbackTextView.setVisibility(View.GONE);
        mSaveButton.setVisibility(View.GONE);
    }

    private void restartARWork() {
        try {
            vuforiaAppSession.startAR(CameraDevice.CAMERA.CAMERA_BACK);
            mARPaused = false;
        } catch (ApplicationException e) {
            showToast(e.getString());
            Log.e(LOGTAG, e.getString());
        }
        doStartTrackers();
        if (mFlashOptionView != null && mFlash) {
            // OnCheckedChangeListener is called upon changing the checked state
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                ((Switch) mFlashOptionView).setChecked(true);
            } else {
                ((CheckBox) mFlashOptionView).setChecked(true);
            }
        }
//        CameraDevice.getInstance().setFlashTorchMode(!mFlash);
        CameraDevice.getInstance().setFlashTorchMode(mFlash);
//        findViewById(R.id.feedback_textView).setVisibility(View.VISIBLE);
        mSaveButton.setVisibility(View.VISIBLE);
    }

}
