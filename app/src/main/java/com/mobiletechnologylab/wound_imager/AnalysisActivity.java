package com.mobiletechnologylab.wound_imager;

import static com.mobiletechnologylab.apilib.apis.common.ToastUtils.toast;
import static com.mobiletechnologylab.wound_imager.Commons.EXTRA_CLINICIAN_ID_KEY;
import static com.mobiletechnologylab.wound_imager.Commons.EXTRA_PATIENT_ID_KEY;
import static com.mobiletechnologylab.storagelib.core.BaseTable.LOCAL_DATA_COL;
import static com.mobiletechnologylab.storagelib.core.BaseTable.METADATA_COL;
import static com.mobiletechnologylab.storagelib.core.BaseTable.SERVER_DATA_COL;
import static com.mobiletechnologylab.storagelib.utils.PermissionsHandler.STORAGE;
import static com.mobiletechnologylab.storagelib.utils.dbclause.ClauseJoiner.Or;
import static com.mobiletechnologylab.storagelib.utils.dbclause.JsonFieldLikeClause.JsonLike;
import static com.mobiletechnologylab.storagelib.utils.dbclause.WhereClause.Where;
import static com.mobiletechnologylab.wound_imager.MeasurementSelectActivity.extractSelectedMeasurements;
import static com.mobiletechnologylab.wound_imager.MeasurementSelectActivity.extractSelectedImage;
import static com.mobiletechnologylab.wound_imager.MeasurementSelectActivity.extractSelectedColor;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;

import androidx.databinding.DataBindingUtil;

import com.mobiletechnologylab.apilib.apis.common.ApiDispatcherUtils;
import com.mobiletechnologylab.apilib.apis.common.ResponseCallback;
import com.mobiletechnologylab.apilib.apis.wound.diagnostics.add_measurement.common.ServerDiagnosticMeasurement;
import com.mobiletechnologylab.apilib.apis.wound.diagnostics.run_analysis.clinician.ClinicianRunAnalysisApi;
import com.mobiletechnologylab.apilib.apis.wound.diagnostics.run_analysis.clinician.ServerDiagnosticPrediction;
import com.mobiletechnologylab.apilib.apis.wound.diagnostics.view_analyses.questionnaire.clinician.ClinicianViewAnalysesApi;
import com.mobiletechnologylab.apilib.apis.wound.diagnostics.view_analyses.questionnaire.clinician.PostRequest;
import com.mobiletechnologylab.apilib.apis.wound.diagnostics.view_analyses.questionnaire.clinician.PostResponse;
import com.mobiletechnologylab.storagelib.diabetes.tables.clinician_profiles.ClinicianProfileDbRow;
import com.mobiletechnologylab.storagelib.diabetes.tables.clinician_profiles.ClinicianProfileDbRowInfo;
import com.mobiletechnologylab.storagelib.interfaces.ClinicianProfileIface;
import com.mobiletechnologylab.storagelib.wound.WoundDb;
import com.mobiletechnologylab.wound_imager.databinding.ActivityAnalysisBinding;
import com.mobiletechnologylab.storagelib.diabetes.tables.patient_profiles.PatientProfileDbRow;
import com.mobiletechnologylab.storagelib.pulmonary.activities.ClinicianLoginActivity;
import com.mobiletechnologylab.storagelib.wound.adapters.DiagnosticAdapter;
import com.mobiletechnologylab.storagelib.wound.tables.diagnostics.DiagnosticDbRow;
import com.mobiletechnologylab.storagelib.wound.tables.diagnostics.DiagnosticDbRowInfo;
import com.mobiletechnologylab.storagelib.wound.tables.diagnostics.LocalDiagnosticPrediction;
import com.mobiletechnologylab.storagelib.wound.tables.diagnostics.LocalDiagnosticPrediction.PredictionType;
import com.mobiletechnologylab.storagelib.wound.tables.diagnostics.LocalDiagnosticPrediction.WoundPrediction;
import com.mobiletechnologylab.storagelib.wound.tables.diagnostics.LocalMetadata;
import com.mobiletechnologylab.storagelib.wound.tables.measurements.LocalMeasurement;
import com.mobiletechnologylab.storagelib.wound.tables.measurements.LocalMetadata.MeasurementType;
import com.mobiletechnologylab.storagelib.wound.tables.measurements.MeasurementDbRow;
import com.mobiletechnologylab.storagelib.wound.tables.measurements.MeasurementDbRowInfo;
import com.mobiletechnologylab.storagelib.diabetes.tables.patient_profiles.PatientProfileDbRowInfo;
import com.mobiletechnologylab.storagelib.utils.ContainerAppUtils;
import com.mobiletechnologylab.storagelib.utils.PermissionsHandler;
import com.mobiletechnologylab.storagelib.utils.StorageSettings;
import com.mobiletechnologylab.storagelib.utils.dbclause.Clause;
import com.mobiletechnologylab.storagelib.utils.dbclause.WhereClause;
import com.mobiletechnologylab.wound_imager.history.OrchestratorActivity;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.PriorityQueue;

import org.opencv.imgproc.Imgproc;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.common.TensorProcessor;
import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.label.TensorLabel;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import au.com.bytecode.opencsv.CSVWriter;

public class AnalysisActivity extends AppCompatActivity {

    PermissionsHandler permissionsHandler;
    WoundDb db2;
    WoundDb cliniciansDb;
    ActivityAnalysisBinding B;
    StorageSettings storageSettings;
    PatientProfileDbRowInfo pInfo;
    ClinicianProfileDbRowInfo cInfo;

    ArrayList<DiagnosticDbRowInfo> diagnostics;

    private Bitmap woundImage;
    private Bitmap colorChartImage;

    private boolean doCloudAnalysis = false;

    private static final String TAG = "AnalysisActivity";

    private static final String MODEL_PATH = "bestRgbNeuralNetTFLiteModel.tflite";

    static final int DIM_IMG_SIZE_X = 224;
    static final int DIM_IMG_SIZE_Y = 224;
    private static final int IMAGE_MEAN = 128;
    private static final float IMAGE_STD = 128.0f;

    private int[] intValues = new int[DIM_IMG_SIZE_X * DIM_IMG_SIZE_Y];
    private Interpreter tflite;
    private ByteBuffer imgData = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        B = DataBindingUtil.setContentView(this, R.layout.activity_analysis);
        storageSettings = new StorageSettings(this);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            onBackPressed();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStart() {
        super.onStart();
        permissionsHandler = new PermissionsHandler(this, STORAGE, () -> {
            db2 = WoundDb.getInstance(this);
            cliniciansDb = WoundDb.getCliniciansDb(this);
            AsyncTask.execute(() -> {
                PatientProfileDbRow row = db2.patients()
                        .getRowWithLocalId(storageSettings.getSelectedPatientLocalId());
                pInfo = new PatientProfileDbRowInfo(row);
                ClinicianProfileDbRow crow = cliniciansDb.clinicians().getRowWithLocalId(storageSettings.getLoggedInClinicianLocalId());
                cInfo = new ClinicianProfileDbRowInfo(crow);
                runOnUiThread(this::setUpUi);
            });
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        permissionsHandler.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!permissionsHandler.granted()) {
            return;
        }
        loadDiagnosticsFromDb();
    }

    private void loadDiagnosticsFromDb() {
        if (db2 == null) {
            return;
        }
        B.loading.setVisibility(View.VISIBLE);
        B.analysesLv.setVisibility(View.GONE);
        AsyncTask.execute(() -> {
            List<DiagnosticDbRow> rows = db2.diagnostics().getRows(getDiagnosticsWhereClause());
            if (rows != null) {
                diagnostics = new ArrayList<>();
                for (DiagnosticDbRow r : rows) {
                    diagnostics.add(new DiagnosticDbRowInfo(r));
                }
            }
            runOnUiThread(() -> {
                configureListView();
                B.loading.setVisibility(View.GONE);
                B.analysesLv.setVisibility(View.VISIBLE);
            });
        });
    }

    @NonNull
    private WhereClause getDiagnosticsWhereClause() {
        ArrayList<Clause> clauses = new ArrayList<>();
        clauses.add(JsonLike(METADATA_COL, "patient_id", pInfo.getServerId()));
        clauses.add(JsonLike(LOCAL_DATA_COL, "local_patient_id", pInfo.getLocalId()));
        if (pInfo.getServerId() != null) {
            JsonLike(LOCAL_DATA_COL, "server_patient_id", pInfo.getServerId());
        }
        return Where(Or(clauses.toArray(new Clause[clauses.size()])));
    }

    private void configureListView() {
        Collections.sort(diagnostics, (t, t1) -> t1.getCreatedOn().compareTo(t.getCreatedOn()));
        DiagnosticAdapter adapter = new DiagnosticAdapter(diagnostics, this);
        B.analysesLv.setAdapter(adapter);
        B.analysesLv.setOnItemClickListener((adapterView, view, i, l) -> {

            if (diagnostics.get(i).isAvailableOnServer()) {
                showCloudAnalysisDialog(diagnostics.get(i).getServer());
            } else {
                showLocalAnalysisDialog(diagnostics.get(i).getLocal());
            }
        });
    }

    private void showLocalAnalysisDialog(LocalDiagnosticPrediction localDiagnosticPrediction) {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle("Generated On " + localDiagnosticPrediction.getCreatedOn());

        b.setPositiveButton("Close", (d, i) -> {
            d.dismiss();
        });

        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater
                .inflate(com.mobiletechnologylab.storagelib.R.layout.dialog_pulmonary_questionnaire,
                        null);
        b.setView(dialogView);

        LinearLayout layout = dialogView
                .findViewById(com.mobiletechnologylab.storagelib.R.id.measurementsLinLayout);

        addTextView(layout, "Wound Prediction: " + localDiagnosticPrediction.getWoundPrediction().getInfected());
        addTextView(layout, "Clinician: " + cInfo.getName());
        addTextView(layout, "Algorithm Version: " + localDiagnosticPrediction.getAlgoVersion());
        b.show();
    }

    private void showCloudAnalysisDialog(ServerDiagnosticPrediction serverDiagnosticPrediction) {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle("Generated On " + serverDiagnosticPrediction.getCreatedOn());

        b.setPositiveButton("Close", (d, i) -> {
            d.dismiss();
        });

        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater
                .inflate(com.mobiletechnologylab.storagelib.R.layout.dialog_pulmonary_questionnaire,
                        null);
        b.setView(dialogView);

        LinearLayout layout = dialogView
                .findViewById(com.mobiletechnologylab.storagelib.R.id.measurementsLinLayout);

        addTextView(layout, "Has Wound Infection: " + serverDiagnosticPrediction.getHasWoundInfection());
        addTextView(layout, "Clinician: " + cInfo.getName());
        addTextView(layout, "Error: " + serverDiagnosticPrediction.getError());
        addTextView(layout, "Algorithm Version: " + serverDiagnosticPrediction.getServerVersion());
        b.show();
    }

    private void addTextView(LinearLayout layout, String s) {
        TextView valueTV = new TextView(this);
        valueTV.setText(s);
        valueTV.setTextSize(18);
        valueTV.setLayoutParams(new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT));
        layout.addView(valueTV);
    }

    private void setUpUi() {
        getSupportActionBar().setTitle(pInfo.getName());
        B.createNewBtn.setOnClickListener(v -> onCreateNewClicked());
        B.importFromServerBtn.setOnClickListener(v -> onImportClicked());
    }

    private void onImportClicked() {
//        if (cloudLoginRequired()) {
//            return;
//        }
//        ProgressDialog loading = ProgressDialog.show(this, "Importing analyses", "Please wait...");
//        new ClinicianViewAnalysesApi(this).callApi(new PostRequest.Builder()
//                .setPatientIds(Collections.singletonList(pInfo.getServerId()))
//                .createPostRequest(), new ResponseCallback<PostResponse>(AnalysisActivity.this) {
//            @Override
//            public void onDone() {
//                loading.dismiss();
//            }
//
//            @Override
//            public void onSuccess(PostResponse response) {
//                for (ServerDiagnosticPrediction p : response.getResults()) {
//                    addNewServerPrediction(p);
//                }
//                runOnUiThread(() -> {
//                    toast(AnalysisActivity.this, "Cloud sync successful");
//                    loadDiagnosticsFromDb();
//                });
//            }
//        });
    }

    private void addNewServerPrediction(ServerDiagnosticPrediction p) {
        DiagnosticDbRow row = db2.diagnostics().getRowWithServerId(p.getIdentity());
        if (row == null) {
            row = new DiagnosticDbRow(p);
        } else {
            row.setServerData(p);
        }

        LocalMetadata metadata = row.getMetadata();
        if (metadata == null) {
            metadata = new LocalMetadata();
        }
        metadata.setPatientId(pInfo.getServerId());
        row.setMetadata(metadata);
        db2.diagnostics().insert(row);
    }

    private boolean cloudLoginRequired() {
        if (!storageSettings.isLoggedInWithCloudCredentials()) {
            new AlertDialog.Builder(this)
                    .setTitle("Cloud Login Required")
                    .setMessage(
                            "In order to import diagnostics for this patient from the cloud, "
                                    + "you will need to login with your cloud credentials.")
                    .setPositiveButton("Login", (d, i) -> {
                        d.dismiss();
                        ClinicianLoginActivity.startCloudLoginActivity(this);
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            return true;
        }
        if (pInfo.getServerId() == null) {
            new AlertDialog.Builder(this)
                    .setTitle("Patient Profile Not Uploaded")
                    .setMessage(
                            "This feature is only available for patient profiles present on the "
                                    + "cloud. Upload this patient profile, and retry.")
                    .setPositiveButton("Ok", null)
                    .show();
            return true;
        }
        return false;
    }

    private void onCreateNewClicked() {
        new AlertDialog.Builder(this)
                .setTitle("Select Analysis Type")
                .setSingleChoiceItems(new String[]{"Local Analysis", "Cloud Analysis"}, -1,
                        (dialogInterface, i) -> {
                            dialogInterface.dismiss();
                            switch (i) {
                                case 0:
                                    doLocalAnalysisHelper();
                                    break;
                                case 1:
                                    doCloudAnalysis();
                                    break;
                            }
                        })
                .show();
    }

    private WhereClause getWhereClause(MeasurementType measurementType) {
        ArrayList<Clause> clauses = new ArrayList<>();
        // Filter patient.
        if (pInfo.getServerId() != null) {
            clauses.add(Or(
                    JsonLike(METADATA_COL, "local_patient_id_at_creation", pInfo.getLocalId()),
                    JsonLike(METADATA_COL, "server_patient_id", pInfo.getServerId()),
                    JsonLike(SERVER_DATA_COL, "patient", pInfo.getServerId()),
                    JsonLike(LOCAL_DATA_COL, "patient_id", pInfo.getServerId())
            ));
        } else {
            clauses.add(JsonLike(METADATA_COL, "local_patient_id_at_creation", pInfo.getLocalId()));
        }

        clauses.add(JsonLike(METADATA_COL, "measurement_type", measurementType.name()));
        return Where(clauses.toArray(new Clause[clauses.size()]));
    }

    public void close() {
        tflite.close();
        tflite = null;
    }

    private MappedByteBuffer loadModelFile(Activity activity) throws IOException {
//        AssetFileDescriptor fileDescriptor = activity.getAssets().openFd(MODEL_PATH);
//        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
//        FileChannel fileChannel = inputStream.getChannel();
//        long startOffset = fileDescriptor.getStartOffset();
//        long declaredLength = fileDescriptor.getDeclaredLength();
//        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
        MappedByteBuffer tfliteModel = FileUtil.loadMappedFile(activity,MODEL_PATH);
        return tfliteModel;
    }

    private void convertBitmapToByteBuffer(Bitmap bitmap) {
        if (imgData == null) {
            return;
        }
        imgData.rewind();
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        // Convert the image to floating point.
        int pixel = 0;
        long startTime = SystemClock.uptimeMillis();
        for (int i = 0; i < DIM_IMG_SIZE_X; ++i) {
            for (int j = 0; j < DIM_IMG_SIZE_Y; ++j) {
                final int val = intValues[pixel++];
                imgData.putFloat((((val >> 16) & 0xFF)-IMAGE_MEAN)/IMAGE_STD);
                imgData.putFloat((((val >> 8) & 0xFF)-IMAGE_MEAN)/IMAGE_STD);
                imgData.putFloat((((val) & 0xFF)-IMAGE_MEAN)/IMAGE_STD);
            }
        }
        long endTime = SystemClock.uptimeMillis();
        Log.d(TAG, "Timecost to put values into ByteBuffer: " + Long.toString(endTime - startTime));
    }

    private Mat preprocessImage(Mat img, Mat colorChart){
        int[] redBGR = {16, 29, 81};
        int[] redLAB = {19, 24, 21};

        int[] blueBGR = {50, 36, 17};
        int[] blueLAB = {13, -4, -12};

        int[] yellowBGR = {23, 255, 255};
        int[] yellowLAB = {98, -16, 90};

        int[] tanBGR = {135, 189, 220};
        int[] tanLAB = {78, 6, 32};

        int[] brownBGR = {65, 89, 131};
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
        ArrayList<String> usedColors = new ArrayList<String>();

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

        for(String color: colorCharts) {

            double height = colorChart.size().height;
            double width = colorChart.size().width;

            ArrayList<ArrayList<Double>> LABcolorValues = allColorLabValues.get(color);
            Mat colorChartModified = new Mat();

            if (color == "red") {
                colorChartModified = new Mat(colorChart, new Rect(0, 0, (int) (width / 2), (int) (height / 2)));
            } else if (color == "blue") {
                colorChartModified = new Mat(colorChart, new Rect(0, (int) (height / 2), (int) (width / 2), (int) (height / 2)));
            } else if (color == "yellow" || color == "tan") {
                colorChartModified = new Mat(colorChart, new Rect((int) (width / 2), 0, (int) (width / 2), (int) (height / 2)));
            } else if (color == "brown") {
                colorChartModified = new Mat(colorChart, new Rect((int) (width / 2), (int) (height / 2), (int) (width / 2), (int) (height / 2)));
            }

            height = colorChartModified.size().height;
            width = colorChartModified.size().width;

            Mat gray = new Mat();
            Mat thresh = new Mat();
            Imgproc.cvtColor(colorChartModified, gray, Imgproc.COLOR_RGB2GRAY);
            Imgproc.threshold(gray, thresh, 35, 255, Imgproc.THRESH_BINARY);

            List<MatOfPoint> contoursList = new ArrayList<>();
            Mat hierarchy = new Mat();

            Imgproc.findContours(thresh, contoursList, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
            Log.d(TAG, "contoursList: " + contoursList.size());

            for (MatOfPoint contour: contoursList) {

                MatOfPoint2f c = new MatOfPoint2f(contour.toArray());
                // Get statistics of the shape in the image
                double peri = Imgproc.arcLength(c, true);
                MatOfPoint2f approx = new MatOfPoint2f();
                Imgproc.approxPolyDP(c, approx, 0.1 * peri, true);

                double cont_area = Imgproc.contourArea(approx);

                Rect bound = Imgproc.boundingRect(contour);
                double x = bound.x;
                double y = bound.y;
                double w = bound.width;
                double h = bound.height;
                float aspect_ratio = (float)(w/h);

//                Log.d(TAG, "approx: " + Arrays.toString(approx.toArray()));

                if(approx.toArray().length == 4 && w > 100 && h > 90 && cont_area > 9000) {

//                    Imgproc.drawContours(image, [approx], -1, new Scalar(0, 255, 0), 1);
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

                    Log.d(TAG, "xvalues: " + xvalues);
                    Log.d(TAG, "yvalues: " + yvalues);

                    double[] pixelSample;
                    try{
                        pixelSample = colorChartModified.get(xvalues, yvalues);
                    }
                    catch(Exception e){
                        continue;
                    }

                    Log.d(TAG, "pixelSample: " + Arrays.toString(pixelSample));

                    if(pixelSample.length > 0 && (pixelSample[0] > 240 && pixelSample[1] > 240 && pixelSample[2] > 240 || pixelSample[0] < 30 && pixelSample[1] < 30 && pixelSample[2] < 30))
                        continue;

                    Mat colorChartLAB = new Mat();
                    Imgproc.cvtColor(colorChartModified, colorChartLAB, Imgproc.COLOR_BGR2Lab);
                    double[] pixelSampleLAB = colorChartLAB.get(xvalues, yvalues);

                    LABcolorValues.get(0).add((pixelSampleLAB[0]/255*100));
                    LABcolorValues.get(1).add((pixelSampleLAB[1]-128));
                    LABcolorValues.get(2).add((pixelSampleLAB[2]-128));

                    int[] labColors = colorsToLabs.get(color);

                    colorDeltas.get(color)[0] = (labColors[0] - (pixelSampleLAB[0]/255*100));
                    colorDeltas.get(color)[1] = (labColors[1] - (pixelSampleLAB[1]-128));
                    colorDeltas.get(color)[2] = (labColors[2] - (pixelSampleLAB[2]-128));

                    Log.d(TAG, "colorDeltas: " + Arrays.toString(colorDeltas.get(color)));
                }
            }
            usedColors.add(color);
        }

        double[] adjustment = {0,0,0};
        for(String key: colorDeltas.keySet()) {
            adjustment[0] += colorDeltas.get(key)[0];
            adjustment[1] += colorDeltas.get(key)[1];
            adjustment[2] += colorDeltas.get(key)[2];
        }

        double[] finalAdjustments = {0,0,0};

        Log.d(TAG, "usedColors: " + usedColors.toString());

        finalAdjustments[0] = adjustment[0]/usedColors.size();
        finalAdjustments[1] = adjustment[1]/usedColors.size();
        finalAdjustments[2] = adjustment[2]/usedColors.size();
        Log.d(TAG, "final adjustments: [" + finalAdjustments[0] + ", " + finalAdjustments[1] + ", " + finalAdjustments[2] + "]");

        Mat finalImage = img;

        if(!(finalAdjustments[0] == 0. && finalAdjustments[1] == 0. && finalAdjustments[2] == 0.)) {
            Mat imageLAB = new Mat();
            Imgproc.cvtColor(img, imageLAB, Imgproc.COLOR_BGR2Lab);

            int rows = imageLAB.rows();
            int cols = imageLAB.cols();

            for(int i = 0; i < rows; i++){
                for(int j = 0; j < cols; j++){
                    double[] prev = imageLAB.get(i,j);
                    double[] updated = {prev[0], Math.min(Math.max(prev[1]+finalAdjustments[1]-6, 0),252), Math.min(Math.max(prev[2]+finalAdjustments[2]+11, 0),252)};
                    imageLAB.put(i, j, updated);

                    if(i == 10 && j == 10 || i == 25 && j == 25 || i == 115 && j == 115){
                        Log.d(TAG, "prev: " + Arrays.toString(prev));
                        Log.d(TAG, "updated: " + Arrays.toString(updated));
                    }
                }
            }

            Imgproc.cvtColor(imageLAB, finalImage, Imgproc.COLOR_Lab2BGR);
        }

        Mat finalResized = new Mat();
        Imgproc.resize(finalImage, finalResized, new Size(224, 224), Imgproc.INTER_NEAREST);

        return finalResized;
    }

    private void doLocalAnalysisHelper() {
        doCloudAnalysis = false;
        if (cloudLoginRequired()) {
            return;
        }
        startActivityForResult(new Intent(this, MeasurementSelectActivity.class),
                MEASUREMENT_SELECT_REQ_CODE);
    }

    private void doLocalAnalysis(Map<Integer, String> images, Map<Integer, String> colors) {
        OpenCVLoader.initDebug();
        ProgressDialog loading = ProgressDialog.show(this, "Running analysis", "Please wait...");
        Log.d(TAG, "local analysis!");
        try {
            tflite = new Interpreter(loadModelFile(this));
            tflite.allocateTensors();
//            int inputCount = tflite.getInputTensorCount();
//            int outputCount = tflite.getOutputTensorCount();
//            Log.d(TAG, "input: " + inputCount + ", output: " + outputCount);
//            labelList = loadLabelList(this);
//            imgData = ByteBuffer.allocateDirect(4 * DIM_BATCH_SIZE * DIM_IMG_SIZE_X * DIM_IMG_SIZE_Y * DIM_PIXEL_SIZE);
//            imgData.order(ByteOrder.nativeOrder());
//            Log.d(TAG, "labelList.size: " + labelList.size());
//            labelProbArray = new float[1][labelList.size()];
//            filterLabelProbArray = new float[FILTER_STAGES][labelList.size()];
            Log.d(TAG, "Created a Tensorflow Lite Image Classifier.");

            // COMMENT OUT for whether to test on directory of uninfected/infected images
//            AsyncTask.execute(() -> {
//                        testTFLiteDirectory();
//                    });

//            String filename = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath() + "/resnet 2/resnet/infected/visible-63-p11-2019.10.15.12.25.23.jpg";
//            Mat imageMat = imageCodecs.imread(filename);
//            String colorFilename = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath() + "/color-patient1-2022.06.08.14.11.42.jpg";
//            Mat colorMat = imageCodecs.imread(colorFilename);
            List<String> imagePaths = new ArrayList<>(images.values());
            List<String> colorPaths = new ArrayList<>(colors.values());

            List<String> results = new ArrayList<>();

            for(int i = 0; i < imagePaths.size(); i++) {
                Log.d(TAG, imagePaths.get(i));
                Log.d(TAG, colorPaths.get(i));
                runTFLiteAnalysis(imagePaths.get(i), colorPaths.get(i), loading, results);
            }

            close();
            loading.dismiss();

            String appendedResults = "";

            for (String s: results){
                appendedResults += s + "\n";
            }
            CharSequence resultCharSeq = "" + appendedResults;

            new AlertDialog.Builder(this)
                    .setTitle("Analysis Results")
                    .setMessage(resultCharSeq)
                    .show();

        }
        catch(IOException e){}

        loadDiagnosticsFromDb();
    }

    private void runTFLiteAnalysis(String imagePath, String colorPath, ProgressDialog loading, List<String> results) {
        Imgcodecs imageCodecs = new Imgcodecs();

        Mat imageMat = imageCodecs.imread(imagePath);
        Mat colorMat = imageCodecs.imread(colorPath);

        // preprocessing pipeline
        loading.setMessage("Preprocessing image..");
        long startProcess = SystemClock.uptimeMillis();
        Mat preprocessedMat = preprocessImage(imageMat, colorMat);
        long endProcess = SystemClock.uptimeMillis();
        Log.d(TAG, "Time to process image: " + Long.toString(endProcess - startProcess));
//            Log.d(TAG, "processed image height: " + preprocessedMat.height());
//            Log.d(TAG, "processed image width: " + preprocessedMat.width());
        // write processed image to documents
        imageCodecs.imwrite(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).getPath() + "/processedImg.jpg", preprocessedMat);
        loading.setMessage("Finished preprocessing..");
//            Bitmap bmp = BitmapFactory.decodeFile(filename);
        Bitmap bmp = Bitmap.createBitmap(preprocessedMat.cols(), preprocessedMat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(preprocessedMat, bmp);
        // method 1
//            bmp = Bitmap.createScaledBitmap(bmp, 224, 224, false);
//            convertBitmapToByteBuffer(bmp);
//
//            TensorBuffer probabilityBuffer = TensorBuffer.createFixedSize(new int[]{1, 10}, DataType.UINT8);
//
//            if(tflite != null){
////                tflite.run(imgData, labelProbArray);
//                tflite.run(imgData, probabilityBuffer.getBuffer());
//            }
//
//            probabilityBuffer.getFloatArray();
//            Log.d(TAG, Arrays.toString(probabilityBuffer.getFloatArray()));

//            TensorProcessor probabilityProcessor = new TensorProcessor.Builder().add(new NormalizeOp(0, 255)).build();
//            TensorLabel labels = new TensorLabel(labelList, probabilityProcessor.process(probabilityBuffer));
//            Map<String, Float> floatMap = labels.getMapWithFloatValue();
//
//            Log.d(TAG, "infected: " + floatMap.get("infected"));
//            Log.d(TAG, "uninfected: " + floatMap.get("uninfected"));

        //method 2

//            ImageProcessor imageProcessor =
//                    new ImageProcessor.Builder()
//                            .add(new ResizeOp(224, 224, ResizeOp.ResizeMethod.BILINEAR))
//                            .build();

        // tensorflow lite implementation
        TensorImage tensorImage = new TensorImage(DataType.FLOAT32);
        tensorImage.load(bmp);
//            tensorImage = imageProcessor.process(tensorImage);

        TensorBuffer probabilityBuffer =
                TensorBuffer.createFixedSize(new int[]{1, 6}, DataType.FLOAT32);

        if(null != tflite) {
            loading.setMessage("Running model..");
            long startRun = SystemClock.uptimeMillis();
            tflite.run(tensorImage.getBuffer(), probabilityBuffer.getBuffer());
            long endRun = SystemClock.uptimeMillis();
            Log.d(TAG, "Time to run inference: " + Long.toString(endRun - startRun));
        }

        float[] resultArr = probabilityBuffer.getFloatArray();
        Log.d(TAG, Arrays.toString(resultArr));

        boolean infected = resultArr[0] > 5*Math.pow(10, -7) ? true : false;
        String infectedResult = infected ? "INFECTED" : "NOT-INFECTED";

        // create alert
        String resultChar = Arrays.toString(resultArr);
        results.add(resultChar);

        AsyncTask.execute(() -> {
            db2.diagnostics().insert(new DiagnosticDbRow(
                    new LocalDiagnosticPrediction.Builder()
                            .setLocalPatientId(storageSettings.getSelectedPatientLocalId())
                            .setServerPatientId(pInfo.getServerId())
                            .setCreatedTime(System.currentTimeMillis())
                            .setType(PredictionType.WOUND_PREDICTION)
                            .setAlgoVersion("1.0")
                            .setClinician("temp")
                            .setWoundPrediction(new WoundPrediction(infectedResult, resultArr[0]))
                            .createLocalDiagnosticPrediction()));
        });

    }

    private void testTFLiteDirectory() {
//        File infectedDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString() + "/resnet 2/resnet/infected/");
        File infectedDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString() + "/infected_v3/infected_new/");
        File[] infectedImgs = infectedDir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return (name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png"));
            }
        });

//        File uninfectedDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString() + "/resnet 2/resnet/uninfected/");
        File uninfectedDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString() + "/uninfected_v3/uninfected_new");
        File[] uninfectedImgs = uninfectedDir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return (name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png"));
            }
        });

        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString() + "/tfliteTest.csv");

        try {
            // create FileWriter object with file as parameter
            FileWriter outputfile = new FileWriter(file);

            // create CSVWriter object filewriter object as parameter
            CSVWriter writer = new CSVWriter(outputfile);

            // adding header to csv
            String[] header = { "Filename", "Result1", "Result2", "Result3", "Result4", "Expected" };
            writer.writeNext(header);

            // test images in directory
            for(int i = 0; i < infectedImgs.length; i++){
                String filename = infectedImgs[i].getPath();
                Log.d(TAG, filename);
                Bitmap bmp = BitmapFactory.decodeFile(filename);

//                ImageProcessor imageProcessor =
//                        new ImageProcessor.Builder()
//                                .add(new ResizeOp(224, 224, ResizeOp.ResizeMethod.BILINEAR))
//                                .build();

                TensorImage tensorImage = new TensorImage(DataType.FLOAT32);
                tensorImage.load(bmp);
//                tensorImage = imageProcessor.process(tensorImage);

                TensorBuffer probabilityBuffer =
                        TensorBuffer.createFixedSize(new int[]{1, 6}, DataType.FLOAT32);

                if(null != tflite) {
                    tflite.run(tensorImage.getBuffer(), probabilityBuffer.getBuffer());
                }

                float[] result = probabilityBuffer.getFloatArray();
                Log.d(TAG, Arrays.toString(probabilityBuffer.getFloatArray()));

//                    String result = Arrays.toString(probabilityBuffer.getFloatArray());

                // add data to csv
                String[] data = { filename, ""+result[0], ""+result[1], ""+result[2], ""+result[3], "infected" };
                writer.writeNext(data);
            }

            for(int i = 0; i < uninfectedImgs.length; i++){
                String filename = uninfectedImgs[i].getPath();
                Log.d(TAG, filename);
                Bitmap bmp = BitmapFactory.decodeFile(filename);

//                ImageProcessor imageProcessor =
//                        new ImageProcessor.Builder()
//                                .add(new ResizeOp(224, 224, ResizeOp.ResizeMethod.BILINEAR))
//                                .build();

                TensorImage tensorImage = new TensorImage(DataType.FLOAT32);
                tensorImage.load(bmp);
//                tensorImage = imageProcessor.process(tensorImage);

                TensorBuffer probabilityBuffer =
                        TensorBuffer.createFixedSize(new int[]{1, 6}, DataType.FLOAT32);

                if(null != tflite) {
                    tflite.run(tensorImage.getBuffer(), probabilityBuffer.getBuffer());
                }

                float[] result = probabilityBuffer.getFloatArray();
                Log.d(TAG, Arrays.toString(probabilityBuffer.getFloatArray()));
//                    String result = Arrays.toString(probabilityBuffer.getFloatArray());

                // add data to csv
                String[] data = { filename, ""+result[0], ""+result[1], ""+result[2], ""+result[3], "uninfected" };
                writer.writeNext(data);
            }

            // closing writer connection
            writer.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static final int MEASUREMENT_SELECT_REQ_CODE = 1001;

    private void doCloudAnalysis() {
        doCloudAnalysis = true;
        if (cloudLoginRequired()) {
            return;
        }
        Intent measurementSelect = new Intent(this, MeasurementSelectActivity.class);
        measurementSelect.putExtra("cloud", true);
        startActivityForResult(measurementSelect,
                MEASUREMENT_SELECT_REQ_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == MEASUREMENT_SELECT_REQ_CODE && resultCode == RESULT_OK) {
            if(doCloudAnalysis) {
                sendCloudAnalysisRequest(extractSelectedMeasurements(data));
            }
            else {
                doLocalAnalysis(extractSelectedImage(data), extractSelectedColor(data));
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void sendCloudAnalysisRequest(Map<Integer, Long> measurements) {
        if (cloudLoginRequired()) {
            return;
        }
        ProgressDialog loading = ProgressDialog.show(this, "Starting Analysis", "Please wait...");
        new ClinicianRunAnalysisApi(this).callApi(
                new com.mobiletechnologylab.apilib.apis.wound.diagnostics.run_analysis.clinician.PostRequest.Builder()
                        .setUsergroupId(storageSettings.getApiSettings().getActiveUserGroupId())
                        .setMeasurementIds(new ArrayList<>(measurements.values()))
                        .createPostRequest(),
                new ResponseCallback<ServerDiagnosticPrediction>(this) {
                    @Override
                    public void onDone() {
                        loading.dismiss();
                    }

                    @Override
                    public void onSuccess(ServerDiagnosticPrediction response) {
                        runOnUiThread(() -> {
                            loading.dismiss();
                            toast(AnalysisActivity.this,
                                    "Analysis request sent to the cloud successfully.");
                            ProgressDialog waiting = ProgressDialog
                                    .show(AnalysisActivity.this, "Analyzing", "Please wait...");
                            // timeout before it stops running in seconds
                            int timeout = 30;
                            long start = System.currentTimeMillis();
                            waitForAnalysisToComplete(response, waiting, start, timeout);
                        });
                    }
                });
    }

    private boolean isDiagReady(ServerDiagnosticPrediction response) {
        return response.getHasWoundInfection() != null;
    }

    private void waitForAnalysisToComplete(ServerDiagnosticPrediction serverPred,
            ProgressDialog waiting, long start, int timeout) {
        Log.d(TAG, serverPred.toString());
        Handler handler = new Handler();
        // interval for checking view_analyses in seconds
        int checking_interval = 2;
        handler.postDelayed(() -> {
            new ClinicianViewAnalysesApi(this).callApi(new PostRequest.Builder()
                    .setUsergroupIds(Collections.singletonList(storageSettings.getApiSettings().getActiveUserGroupId()))
                    .setMeasurementIds(serverPred.getMeasurements())
                    .createPostRequest(), new ResponseCallback<PostResponse>(this) {

                @Override
                public void onError(int statusCode, String errorMessage) {
                    super.onError(statusCode, errorMessage);
                    runOnUiThread(waiting::dismiss);
                }

                @Override
                public void onIOException(IOException e) {
                    super.onIOException(e);
                    runOnUiThread(waiting::dismiss);
                }

                @Override
                public void onSuccess(PostResponse response) {
                    Log.d(TAG, response.toString());
                    if (isDiagReady(response.getResults().iterator().next())) {
                        addNewServerPrediction(response.getResults().iterator().next());
                        runOnUiThread(() -> {
                            toast(AnalysisActivity.this, "Analysis complete");
                            waiting.dismiss();
                            loadDiagnosticsFromDb();
                        });
                    } else {
                        if(System.currentTimeMillis() < start + timeout * 1000) {
                            runOnUiThread(() -> {
                                waitForAnalysisToComplete(serverPred, waiting, start, timeout);
                            });
                        }
                        else {
                            Toast.makeText(getApplicationContext(), "No Result Received.", Toast.LENGTH_LONG).show();
                        }
                    }
                }
            });
        }, checking_interval * 1000);
    }


}
