package com.mobiletechnologylab.wound_imager.ui;

import static com.mobiletechnologylab.apilib.apis.common.ToastUtils.toast;
import static com.mobiletechnologylab.storagelib.utils.FolderStructure.getWoundVisibleMeasurementsDir;
import static com.mobiletechnologylab.storagelib.utils.PermissionsHandler.STORAGE;

import android.content.Context;
import androidx.databinding.DataBindingUtil;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import com.mobiletechnologylab.apilib.apis.common.ApiDispatcherUtils;
import com.mobiletechnologylab.apilib.apis.wound.diagnostics.add_measurement.common.Metadata;
import com.mobiletechnologylab.apilib.apis.wound.diagnostics.add_measurement.wound_image.PostRequest;
import com.mobiletechnologylab.storagelib.diabetes.tables.patient_profiles.PatientProfileDbRow;
import com.mobiletechnologylab.storagelib.diabetes.tables.patient_profiles.PatientProfileDbRowInfo;
import com.mobiletechnologylab.storagelib.utils.DbUtils;
import com.mobiletechnologylab.storagelib.utils.FolderStructure;
import com.mobiletechnologylab.storagelib.utils.PermissionsHandler;
import com.mobiletechnologylab.storagelib.utils.ShareUtils;
import com.mobiletechnologylab.storagelib.utils.StorageSettings;
import com.mobiletechnologylab.storagelib.wound.WoundDb;
import com.mobiletechnologylab.storagelib.wound.tables.measurements.LocalMeasurement;
import com.mobiletechnologylab.storagelib.wound.tables.measurements.LocalMetadata;
import com.mobiletechnologylab.storagelib.wound.tables.measurements.MeasurementDbRow;
import com.mobiletechnologylab.wound_imager.AnalysisActivity;
import com.mobiletechnologylab.wound_imager.R;
import com.mobiletechnologylab.wound_imager.databinding.ActivityReviewImageBinding;
import com.mobiletechnologylab.utils.ContainerAppUtils;
import com.mobiletechnologylab.wound_imager.history.OrchestratorActivity;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import android.app.AlertDialog;
import android.content.DialogInterface;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

public class ReviewImageActivity extends AppCompatActivity {

    public static final String WOUND_FILENAME_PREFIX = "visible";
    public static final String COLOR_FILENAME_PREFIX = "color";

    public static final SimpleDateFormat THERMAL_DATE_FMT = new SimpleDateFormat(
            "yyyy.MM.dd.HH.mm.ss", Locale.US);

    private PermissionsHandler permissionsHandler;
    private WoundDb db;
    private ActivityReviewImageBinding B;
    private PatientProfileDbRowInfo pInfo = new PatientProfileDbRowInfo();


    private boolean saveEnabled;

    private Bitmap woundImage;
    private Bitmap colorChartImage;
    private Boolean shareImage = false;
    private String imagePath = "";
    private String colorChartPath = "";
    private Mat woundImageMat = new Mat();
    private Mat colorChartMat = new Mat();

    private boolean launchedFromContainerApp = false;

    Boolean kinyarwanda = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        B = DataBindingUtil.setContentView(this, R.layout.activity_review_image);
        saveEnabled = new StorageSettings(this).getSelectedPatientLocalId() > 0;

        if (kinyarwanda) {
            B.continueBtn.setText("Ohereza ifoto");
            B.discardBtn.setText("Siba wongere ufotore");
        }

        File tempWoundImageFile = new File(getIntent().getStringExtra(OrchestratorActivity.ARG_TEMP_WOUND_IMAGE_PATH));
        File tempColorChartFile = new File(getIntent().getStringExtra(OrchestratorActivity.ARG_TEMP_COLOR_CHART_PATH));
        File tempFolder = tempColorChartFile.getParentFile();

        woundImage = BitmapFactory.decodeFile(tempWoundImageFile.getAbsolutePath());
        colorChartImage = BitmapFactory.decodeFile(tempColorChartFile.getAbsolutePath());
        tempWoundImageFile.delete();
        tempColorChartFile.delete();
        tempFolder.delete();

        // convert Bitmap to Mat
        Bitmap bmp32 = woundImage.copy(Bitmap.Config.ARGB_8888, true);
        Utils.bitmapToMat(bmp32, woundImageMat);
        Bitmap ccBmp = colorChartImage.copy(Bitmap.Config.ARGB_8888, true);
        Utils.bitmapToMat(ccBmp, colorChartMat);

        // add image quality checks here
        boolean passed = true;

        // blur check
        boolean tooBlurry = checkBlur();
        // lighting check
        boolean badLighting = checkLighting();
        // color check
        boolean badColor = checkColor();

        if(tooBlurry || badLighting || badColor) passed = false;

        String blurCheck = tooBlurry ? "failed" : "passed";
        String lightCheck = badLighting ? "failed" : "passed";
        String colorCheck = badColor ? "failed" : "passed";

        B.imageQualityResults.setText("Blur Check: " + blurCheck + "\nLighting Check: " + lightCheck + "\nColor Check: " + colorCheck);

        if(!passed){
            B.continueBtn.setVisibility(View.GONE);
            B.discardBtn.setVisibility(View.VISIBLE);
        }
        else{
            B.discardBtn.setVisibility(View.GONE);
            B.continueBtn.setVisibility(View.VISIBLE);
        }

        B.previewIv.setImageBitmap(woundImage);

        getSupportActionBar().setTitle("Preview");
        B.discardBtn.setOnClickListener(v -> onBackPressed());
        if (!saveEnabled) {
            B.continueBtn.setText("Continue");
        }
        B.continueBtn.setOnClickListener(v -> {
            if (saveEnabled) {
                saveImage();
            }
            else {
                setResult(RESULT_OK);
                finish();
            }
            Intent analysisPage = new Intent(this, AnalysisActivity.class);
            analysisPage.putExtra("colorChart", colorChartPath);
            analysisPage.putExtra("woundImage", imagePath);
            startActivity(analysisPage);
        });
    }

    private static final String TAG = ReviewImageActivity.class.getSimpleName();

    private boolean checkBlur() {
        Mat blurGray = new Mat();
        Imgproc.cvtColor(woundImageMat, blurGray, Imgproc.COLOR_BGR2GRAY);
        Mat lap = new Mat();
        Imgproc.Laplacian(blurGray, lap, CvType.CV_64F);
        MatOfDouble median = new MatOfDouble();
        MatOfDouble std= new MatOfDouble();
        Core.meanStdDev(lap, median, std);
        Double fm = Math.pow(std.get(0,0)[0],2);
        if(fm < 10) {
            return true;
        }
        return false;
    }

    private boolean checkLighting() {
        Mat light = new Mat();
        Imgproc.cvtColor(woundImageMat, light, Imgproc.COLOR_BGR2Lab);
        // calculate median
        ArrayList<Double> lVals = new ArrayList<>();
        for(int i = 0; i < light.cols(); i++){
            for(int j = 0; j < light.rows(); j++){
                lVals.add(light.get(j,i)[0]);
            }
        }
        Collections.sort(lVals);
        double lightMedian = lVals.get(lVals.size()/2);
        if(lightMedian < 20 || lightMedian > 80){
            return true;
        }
        return false;
    }

    private boolean checkColor() {
        int[] redLAB = {19, 24, 21};
        int[] blueLAB = {13, -4, -12};
        int[] yellowLAB = {98, -16, 90};
        int[] tanLAB = {78, 6, 32};
        int[] brownLAB = {42, 16, 21};

        Map<String, int[]> colorsToLabs = new HashMap<>();
        colorsToLabs.put("red",redLAB);
        colorsToLabs.put("blue",blueLAB);
        colorsToLabs.put("yellow",yellowLAB);
        colorsToLabs.put("tan",tanLAB);
        colorsToLabs.put("brown",brownLAB);

        ArrayList<ArrayList<Double>> redList = new ArrayList<>(3);
        redList.add(new ArrayList<Double>());
        redList.add(new ArrayList<Double>());
        redList.add(new ArrayList<Double>());
        ArrayList<ArrayList<Double>> blueList = new ArrayList<>(3);
        blueList.add(new ArrayList<Double>());
        blueList.add(new ArrayList<Double>());
        blueList.add(new ArrayList<Double>());
        ArrayList<ArrayList<Double>> yellowList = new ArrayList<>(3);
        yellowList.add(new ArrayList<Double>());
        yellowList.add(new ArrayList<Double>());
        yellowList.add(new ArrayList<Double>());
        ArrayList<ArrayList<Double>> tanList = new ArrayList<>(3);
        tanList.add(new ArrayList<Double>());
        tanList.add(new ArrayList<Double>());
        tanList.add(new ArrayList<Double>());
        ArrayList<ArrayList<Double>> brownList = new ArrayList<>(3);
        brownList.add(new ArrayList<Double>());
        brownList.add(new ArrayList<Double>());
        brownList.add(new ArrayList<Double>());

        Map<String, ArrayList<ArrayList<Double>>> allColorLabValues = new HashMap<>();
        allColorLabValues.put("red", redList);
        allColorLabValues.put("blue", blueList);
        allColorLabValues.put("yellow", yellowList);
        allColorLabValues.put("tan", tanList);
        allColorLabValues.put("brown", brownList);

        String[] colorCharts = {"red", "blue", "yellow", "tan", "brown"};

        double[] redDeltas = {0,0,0};
        double[] blueDeltas = {0,0,0};
        double[] yellowDeltas = {0,0,0};
        double[] tanDeltas = {0,0,0};
        double[] brownDeltas = {0,0,0};
        Map<String, double[]> colorDeltas = new HashMap<>();
        colorDeltas.put("red", redDeltas);
        colorDeltas.put("blue", blueDeltas);
        colorDeltas.put("yellow", yellowDeltas);
        colorDeltas.put("tan", tanDeltas);
        colorDeltas.put("brown", brownDeltas);

        // find each of 5 chosen pixels and find LAB values
        for(String color: colorCharts) {
            double height = colorChartMat.size().height;
            double width = colorChartMat.size().width;

            ArrayList<ArrayList<Double>> LABcolorValues = allColorLabValues.get(color);
            Mat colorChartModified = new Mat();

            if (color == "red") {
                colorChartModified = new Mat(colorChartMat, new Rect(0, 0, (int) (width / 2), (int) (height / 2)));
            } else if (color == "blue") {
                colorChartModified = new Mat(colorChartMat, new Rect(0, (int) (height / 2), (int) (width / 2), (int) (height / 2)));
            } else if (color == "yellow" || color == "tan") {
                colorChartModified = new Mat(colorChartMat, new Rect((int) (width / 2), 0, (int) (width / 2), (int) (height / 2)));
            } else if (color == "brown") {
                colorChartModified = new Mat(colorChartMat, new Rect((int) (width / 2), (int) (height / 2), (int) (width / 2), (int) (height / 2)));
            }

            Mat gray = new Mat();
            Mat thresh = new Mat();
            Imgproc.cvtColor(colorChartModified, gray, Imgproc.COLOR_RGB2GRAY);
            Imgproc.threshold(gray, thresh, 35, 255, Imgproc.THRESH_BINARY);

            List<MatOfPoint> contoursList = new ArrayList<>();
            Mat hierarchy = new Mat();

            Imgproc.findContours(thresh, contoursList, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
//            Log.d(TAG, "contoursList: " + contoursList.size());

            for (MatOfPoint contour: contoursList) {

                MatOfPoint2f c = new MatOfPoint2f(contour.toArray());
                // Get statistics of the shape in the image
                double peri = Imgproc.arcLength(c, true);
                MatOfPoint2f approx = new MatOfPoint2f();
                Imgproc.approxPolyDP(c, approx, 0.1 * peri, true);
                double cont_area = Imgproc.contourArea(approx);
                Rect bound = Imgproc.boundingRect(contour);
                double w = bound.width;
                double h = bound.height;

//                Log.d(TAG, "approx: " + Arrays.toString(approx.toArray()));

                if(approx.toArray().length == 4 && w > 100 && h > 90 && cont_area > 9000) {
                    int xvalues = 0;
                    int yvalues = 0;

                    if (color == "red" || color == "blue") {
                        xvalues = (int)approx.toArray()[2].y - (int)(h/5);
                        yvalues = (int)approx.toArray()[2].x - (int)(w/8);
                    } else if (color == "yellow") {
                        xvalues = (int)approx.toArray()[1].y - (int)(4*h/5);
                        yvalues = (int)approx.toArray()[1].x + (int)(3*w/8);
                    } else if (color == "tan") {
                        xvalues = (int)approx.toArray()[1].y - (int)(h/5);
                        yvalues = (int)approx.toArray()[1].x + (int)(w/8);
                    } else if (color == "brown") {
                        xvalues = (int)approx.toArray()[1].y - (int)(h/5);
                        yvalues = (int)approx.toArray()[1].x + (int)(3*w/8);
                    }

//                    Log.d(TAG, "xvalues: " + xvalues);
//                    Log.d(TAG, "yvalues: " + yvalues);

                    double[] pixelSample;
                    try{
                        pixelSample = colorChartModified.get(xvalues, yvalues);
                    }
                    catch(Exception e){
                        continue;
                    }


                    if(pixelSample.length > 0 && (pixelSample[0] > 240 && pixelSample[1] > 240 && pixelSample[2] > 240 || pixelSample[0] < 30 && pixelSample[1] < 30 && pixelSample[2] < 30))
                        continue;

                    Mat colorChartLAB = new Mat();
                    Imgproc.cvtColor(colorChartModified, colorChartLAB, Imgproc.COLOR_BGR2Lab);
                    double[] pixelSampleLAB = colorChartLAB.get(xvalues, yvalues);

                    LABcolorValues.get(0).add((pixelSampleLAB[0]/255*100));
                    LABcolorValues.get(1).add((pixelSampleLAB[1]-128));
                    LABcolorValues.get(2).add((pixelSampleLAB[2]-128));

                    Log.d(TAG, "pixelSampleLAB: " + Arrays.toString(pixelSampleLAB));
                    pixelSampleLAB[0] = (pixelSampleLAB[0]/255*100);
                    pixelSampleLAB[1] -= 128;
                    pixelSampleLAB[2] -= 128;

                    if (color == "red") {
                        // A range: 0 to 40, B range: 0 to 40
                        if(pixelSampleLAB[1] < 0 || pixelSampleLAB[1] > 40 || pixelSampleLAB[2] < -50 || pixelSampleLAB[2] > 40) return true;
                    } else if (color == "blue") {
                        // A range: -10 to 10, B range: -30 to 20
                        if(pixelSampleLAB[1] < -10 || pixelSampleLAB[1] > 20 || pixelSampleLAB[2] < -30 || pixelSampleLAB[2] > 30) return true;
                    } else if (color == "yellow") {
                        // A range: -30 to 10, B range: 0 to 100
                        if(pixelSampleLAB[1] < -40 || pixelSampleLAB[1] > 10 || pixelSampleLAB[2] < -30 || pixelSampleLAB[2] > 100) return true;
                    } else if (color == "tan") {
                        // A range: -10 to 20, B range: 0 to 50
                        if(pixelSampleLAB[1] < -20 || pixelSampleLAB[1] > 30 || pixelSampleLAB[2] < -40 || pixelSampleLAB[2] > 50) return true;
                    } else if (color == "brown") {
                        // A range: 0 to 20, B range: -10 to 20
                        if(pixelSampleLAB[1] < -10 || pixelSampleLAB[1] > 30 || pixelSampleLAB[2] < -30 || pixelSampleLAB[2] > 30) return true;
                    }
                }
            }
        }
        return false;
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

    private void saveToDb(String woundImagePath, String colorChartImagePath) {
        StorageSettings storageSettings = new StorageSettings(this);
        if (storageSettings.getSelectedPatientLocalId() <= 0) {
            return;
        }
        AsyncTask.execute(() -> {
            WoundDb db = WoundDb.getInstance(getApplicationContext());
            PatientProfileDbRow patient = db.patients()
                    .getRowWithLocalId(storageSettings.getSelectedPatientLocalId());
            if (patient == null) {
                Log.e(TAG, "patient not found in db.");
                return;
            }
            LocalMetadata metadata = new LocalMetadata.Builder()
                    .setMeasurementType(LocalMetadata.MeasurementType.IMAGE.name())
                    .setLocalClinicianIdAtCreation(storageSettings.getLoggedInClinicianLocalId())
                    .setLocalPatientIdAtCreation(patient.localId)
                    .setServerPatientId(patient.serverId)
                    .setWoundImageMeasurementMeta(
                            new LocalMetadata.WoundImageMeasurementMeta(woundImagePath))
                    .createLocalMetadata();

            LocalMeasurement local = new LocalMeasurement.Builder()
                    .setWoundImage(new PostRequest.Builder()
                            .setPatientId(patient.serverId)
                            .setUsergroupId(storageSettings.getApiSettings().getActiveUserGroupId())
                            .setMeasurement(new PostRequest.DiagnosticMeasurement.Builder()
                                    .setWoundImage(new PostRequest.WoundImage(woundImagePath, colorChartImagePath))
                                    .setMetadata(new Metadata.Builder()
                                            .setRecordedOn(
                                                    ApiDispatcherUtils.dateTimeInServerFormat())
                                            .setClientType(ApiDispatcherUtils.getClientType())
                                            .setClientId(
                                                    ApiDispatcherUtils.getClientId(this))
                                            .setClientVersion(
                                                    ApiDispatcherUtils
                                                            .getClientVersion(this))
//                                            .setGpsLatitude(gpsUtils.getLatitude())
//                                            .setGpsLongitude(gpsUtils.getLongitude())
                                            .setGpsLatitude(0d)
                                            .setGpsLongitude(0d)
                                            .createMetadata())
                                    .createDiagnosticMeasurement())
                            .createPostRequest())
                    .createLocalMeasurement();
            db.measurements().insert(new MeasurementDbRow(local, metadata));

            this.runOnUiThread(() -> {
                toast(this, "Image saved");
            });
        });
    }

    private void shareDialog(Context context, String path) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        if (kinyarwanda) {
            builder.setMessage(R.string.dialog_message_kr);
            builder.setPositiveButton(R.string.dialog_ok_kr, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    ShareUtils.ShareImage(context, path);
                    setResult(RESULT_OK);
                    finish();
                }
            });
            builder.setNegativeButton(R.string.dialog_cancel_kr, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    setResult(RESULT_OK);
                    finish();
                }
            });
        }
        else {
            builder.setMessage(R.string.dialog_message);
            builder.setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    ShareUtils.ShareImage(context, path);
                    setResult(RESULT_OK);
                    finish();
                }
            });
            builder.setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    setResult(RESULT_OK);
                    finish();
                }
            });
        }
        builder.setCancelable(false);
        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);

        dialog.show();
    }

    private void saveImage() {
        launchedFromContainerApp = ContainerAppUtils.wasStartedFromContainerApp(this);

        if (launchedFromContainerApp) {
            shareImage = getIntent().getBooleanExtra(OrchestratorActivity.ARG_VISIBLE_SHARE_IMAGE, false);
            imagePath = getIntent().getStringExtra(OrchestratorActivity.ARG_VISIBLE_IMAGE_PATH);
            colorChartPath = getIntent().getStringExtra(OrchestratorActivity.ARG_VISIBLE_COLOR_PATH);
        }
        else
        {
            shareImage = false;
            File patientFolder = new File(getWoundVisibleMeasurementsDir(), "" + pInfo.getUsername());
            // Create the storage directory if it does not exist
            if (!patientFolder.exists()) {
                patientFolder.mkdirs();
            }

            String timestamp = THERMAL_DATE_FMT.format(new Date());
            String filenameWithoutPrefix = pInfo.getUsername() + "-" + timestamp + ".jpg";
            imagePath = new File(patientFolder, WOUND_FILENAME_PREFIX + "-" +
                    filenameWithoutPrefix).getAbsolutePath();
            colorChartPath = new File(patientFolder, COLOR_FILENAME_PREFIX + "-" +
                    filenameWithoutPrefix).getAbsolutePath();
        }

        saveImageToSdCard(woundImage, imagePath);
        saveImageToSdCard(colorChartImage, colorChartPath);

        saveToDb(imagePath, colorChartPath);
        if (shareImage) {
            shareDialog(this, imagePath);
        }
        else {
            setResult(RESULT_OK);
            finish();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        permissionsHandler = new PermissionsHandler(this, STORAGE, () -> {
            if (DbUtils.isDataDbAvailable(this)) {
                db = WoundDb.getInstance(this);
                AsyncTask.execute(() -> {
                    pInfo = new PatientProfileDbRowInfo(
                            db.patients().getRowWithLocalId(new StorageSettings(this)
                                    .getSelectedPatientLocalId()));
                });
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        permissionsHandler.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

}
