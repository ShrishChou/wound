package com.mobiletechnologylab.wound_imager.history;

import static com.mobiletechnologylab.apilib.apis.common.ToastUtils.toast;
import static com.mobiletechnologylab.wound_imager.Commons.EXTRA_OUTCOME_KEY;
import static com.mobiletechnologylab.wound_imager.Commons.RESULT_OUTCOME_EXIT;
import static com.mobiletechnologylab.wound_imager.Commons.RESULT_OUTCOME_RESTART;
import static com.mobiletechnologylab.wound_imager.Commons.RESULT_OUTCOME_RETEST;
import static com.mobiletechnologylab.storagelib.utils.PermissionsHandler.AllOf;
import static com.mobiletechnologylab.storagelib.utils.PermissionsHandler.GPS;
import static com.mobiletechnologylab.storagelib.utils.PermissionsHandler.STORAGE;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.text.InputType;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import com.mobiletechnologylab.wound_imager.R;
import com.mobiletechnologylab.storagelib.wound.WoundDb;
import com.mobiletechnologylab.storagelib.wound.tables.measurements.MeasurementDbRow;
import com.mobiletechnologylab.storagelib.diabetes.tables.patient_profiles.PatientProfileDbRow;
import com.mobiletechnologylab.storagelib.diabetes.tables.patient_profiles.PatientProfileDbRowInfo;
import com.mobiletechnologylab.storagelib.utils.GpsUtils;
import com.mobiletechnologylab.storagelib.utils.PermissionsHandler;
import com.mobiletechnologylab.storagelib.utils.StorageSettings;

/**
 * An example full-screen activity that shows and hides the system UI (i.e. status bar and
 * navigation/system bar) with user interaction.
 */
public class ResultsActivity extends AppCompatActivity {

    public final static String KEY_PFM_MEASUREMENT = "pfmMeasurement";
    public final static String KEY_PFM_MEASUREMENT_PERCENT_NORMAL = "pfmMeasurementPercentNormal";


    private PermissionsHandler permissionsHandler;
    private WoundDb db;
    private MeasurementDbRow dbRow;
    private PatientProfileDbRowInfo pInfo;
    private StorageSettings storageSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.history_activity_results);
        storageSettings = new StorageSettings(this);

        float measurement = getIntent().getFloatExtra(KEY_PFM_MEASUREMENT, 0.0f);
        int m = (int) measurement;
        ((TextView) findViewById(R.id.measurement)).setText("Peak Flow = " + m + " L/min");
        this.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            onBackPressed();
        }

        return super.onOptionsItemSelected(item);
    }

    private GpsUtils gpsUtils;

    @Override
    protected void onStart() {
        super.onStart();
        permissionsHandler = new PermissionsHandler(this, AllOf(STORAGE, GPS), () -> {
            if (GpsUtils.locationServicesRequired(this)) {
                return;
            }
            db = WoundDb.getInstance(this);
            AsyncTask.execute(() -> {
                PatientProfileDbRow patientRow = db.patients()
                        .getRowWithLocalId(storageSettings.getSelectedPatientLocalId());
                pInfo = new PatientProfileDbRowInfo(patientRow);
                gpsUtils = new GpsUtils(this);
            });
        });
    }

    public void onExit(View v) {
        Intent outputIntent = new Intent();
        outputIntent.putExtra(EXTRA_OUTCOME_KEY, RESULT_OUTCOME_EXIT);
        setResult(RESULT_OK, outputIntent);
        finish();

//        // return to the home screen
//        Intent intent = new Intent(Intent.ACTION_MAIN);
//        intent.addCategory(Intent.CATEGORY_HOME);
//        startActivity(intent);
    }

    public void onRestart(View v) {
        Intent outputIntent = new Intent();
        outputIntent.putExtra(EXTRA_OUTCOME_KEY, RESULT_OUTCOME_RESTART);
        setResult(RESULT_OK, outputIntent);
        finish();
    }

    public void onRetest(View v) {
        Intent outputIntent = new Intent();
        outputIntent.putExtra(EXTRA_OUTCOME_KEY, RESULT_OUTCOME_RETEST);
        setResult(RESULT_OK, outputIntent);
        finish();
    }

    private Float height;
    EditText heightEt;

    public void onSave(View v) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT);
        params.weight = 1.0f;
        params.gravity = Gravity.CENTER_HORIZONTAL;
        heightEt = new EditText(this);
        if (height != null) {
            heightEt.setText(height.toString());
        }
        heightEt.setInputType(InputType.TYPE_CLASS_NUMBER);
        heightEt.setLayoutParams(params);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter Patient Height (cm)");
        builder.setView(heightEt);
        builder.setCancelable(false);
        builder.setPositiveButton("Save", (d, i) -> {
            if (heightEt.getText().toString().isEmpty() || !heightEt.getText().toString()
                    .matches("\\d+(?:\\.\\d+)?")) {
                toast(this, "Valid height required");
                d.dismiss();
                onSave(v);
                return;
            }
            height = Float.parseFloat(heightEt.getText().toString());
//            saveMeasurementToDb(v);
        });
        builder.show();
    }

//    private void saveMeasurementToDb(View v) {
//        float reading = getIntent().getFloatExtra(KEY_PFM_MEASUREMENT, 0.0f);
//        LocalMetadata metadata = new
//                LocalMetadata.Builder()
//                .setLocalPatientIdAtCreation(pInfo.getLocalId())
//                .setLocalClinicianIdAtCreation(storageSettings.getLoggedInClinicianLocalId())
//                .setServerPatientId(pInfo.getServerId())
//                .setMeasurementType(MeasurementType.PEAK_FLOW_METER)
//                .createLocalMetadata();
//
//        LocalMeasurement measurement = new LocalMeasurement.Builder()
//                .setPfm(new PostRequest.Builder()
//                        .setPatientId(pInfo.getServerId())
//                        .setUsergroupId(storageSettings.getApiSettings().getActiveUserGroupId())
//                        .setMeasurement(new DiagnosticMeasurement.Builder()
//                                .setMetadata(new Metadata.Builder()
//                                        .setClientId(getPackageName())
//                                        .setClientType(ApiDispatcherUtils.CLIENT_TYPE)
//                                        .setClientVersion(
//                                                ApiDispatcherUtils.getPackageVersion(this))
//                                        .setGpsLatitude(gpsUtils.getLatitude())
//                                        .setGpsLongitude(gpsUtils.getLongitude())
//                                        .setRecordedOn(ApiDispatcherUtils.dateTimeInServerFormat())
//                                        .createMetadata())
//                                .setPeakFlowMetaRequest(new PeakFlowMeterRequest.Builder()
//                                        .setHeightCm(height)
//                                        .setMaxPfmReading(reading)
//                                        .createPeakFlowMeterRequest())
//                                .createDiagnosticMeasurement())
//                        .createPostRequest())
//                .createLocalMeasurement();
//        if (dbRow == null) {
//            dbRow = new MeasurementDbRow(measurement, metadata);
//        } else {
//            dbRow.setLocalData(measurement);
//            dbRow.setMetadata(metadata);
//        }
//
//        AsyncTask.execute(() -> {
//            dbRow.localId = (int) db.measurements().insert(dbRow);
//            runOnUiThread(() -> {
//                v.setVisibility(View.GONE);
//                findViewById(R.id.retest_button).setVisibility(View.GONE);
//                findViewById(R.id.restart_button).setVisibility(View.VISIBLE);
//                promptToUpload();
//            });
//        });
//    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        permissionsHandler.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void promptToUpload() {
        if (!storageSettings.isLoggedInWithCloudCredentials()) {
            new AlertDialog.Builder(this)
                    .setTitle("Upload to cloud")
                    .setMessage(
                            "The measurements were successfully saved on your device. In order to upload these measurements to the cloud, you will need to log in with your cloud credentials. After logging in, you can upload all local measurements from the patient's history page.")
                    .setPositiveButton("Ok", null)
                    .show();
            return;
        }
        if (pInfo.getServerId() == null) {
            new AlertDialog.Builder(this)
                    .setTitle("Upload to cloud")
                    .setMessage(
                            "The measurements were successfully saved on your device. This patient profile has not yet been uploaded to cloud, as a result, features such as Cloud Analysis and Cloud backup are disabled for this patient. Upload the patient profile from the patient selection selection page.")
                    .setPositiveButton("Ok", null)
                    .show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Upload to cloud")
                .setMessage(
                        "The measurements were saved on your device successfully. To enable Cloud Backup and Cloud Analysis however, you would need to upload these measurements to the cloud. Would you like to upload these measurements to the cloud now?")
                .setPositiveButton("Upload", (d, i) -> {
//                    uploadToCloud();
                    d.dismiss();
                })
                .setNegativeButton("No", (d, i) -> d.dismiss())
                .show();
    }

//    private void uploadToCloud() {
//        ProgressDialog loading = ProgressDialog.show(this, "Uploading to cloud", "Please wait...");
//        new ClinicianAddPfmMeasurementApi(this).callApi(dbRow.getLocalData().getPfm(),
//                new ResponseCallback<ServerDiagnosticMeasurement>(this) {
//                    @Override
//                    public void onDone() {
//                        loading.dismiss();
//                    }
//
//                    @Override
//                    public void onSuccess(ServerDiagnosticMeasurement response) {
//                        dbRow.setServerData(response);
//                        db.measurements().update(dbRow);
//                        runOnUiThread(() -> {
//                            toast(ResultsActivity.this, "Upload successful");
//                        });
//                    }
//                });
//    }
}