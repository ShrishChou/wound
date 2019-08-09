package com.mobiletechnologylab.wound_screener;

import static com.mobiletechnologylab.apilib.apis.common.ApiDispatcherUtils.GSON;
import static com.mobiletechnologylab.apilib.apis.common.ToastUtils.toast;
import static com.mobiletechnologylab.storagelib.utils.FolderStructure.getWoundThermalMeasurementsDir;
import static com.mobiletechnologylab.storagelib.utils.FolderStructure.getWoundVisibleMeasurementsDir;
import static com.mobiletechnologylab.storagelib.utils.PermissionsHandler.AllOf;
import static com.mobiletechnologylab.storagelib.utils.PermissionsHandler.GPS;
import static com.mobiletechnologylab.storagelib.utils.PermissionsHandler.STORAGE;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.mobiletechnologylab.apilib.apis.common.ApiDispatcherUtils;
import com.mobiletechnologylab.apilib.apis.common.ApiSettings;
import com.mobiletechnologylab.apilib.apis.wound.diagnostics.add_measurement.common.Metadata;
import com.mobiletechnologylab.apilib.apis.wound.diagnostics.add_measurement.wound_questionnaire.PostRequest;
import com.mobiletechnologylab.apilib.apis.wound.diagnostics.add_measurement.wound_questionnaire.PostRequest.DiagnosticMeasurement;
import com.mobiletechnologylab.apilib.apis.wound.diagnostics.add_measurement.wound_questionnaire.PostRequest.WoundQuestionnaire;
import com.mobiletechnologylab.apilib.apis.wound.diagnostics.add_measurement.wound_thermal_image.PostRequest.WoundThermalImage;
import com.mobiletechnologylab.storagelib.diabetes.tables.measurements.LocalMetadata.MeasurementType;
import com.mobiletechnologylab.storagelib.diabetes.tables.patient_profiles.PatientProfileDbRow;
import com.mobiletechnologylab.storagelib.diabetes.tables.patient_profiles.PatientProfileDbRowInfo;
import com.mobiletechnologylab.storagelib.utils.ContainerAppUtils;
import com.mobiletechnologylab.storagelib.utils.DbUtils;
import com.mobiletechnologylab.storagelib.utils.PermissionsHandler;
import com.mobiletechnologylab.storagelib.utils.StorageSettings;
import com.mobiletechnologylab.storagelib.wound.WoundDb;
import com.mobiletechnologylab.storagelib.wound.tables.measurements.LocalMeasurement;
import com.mobiletechnologylab.storagelib.wound.tables.measurements.LocalMetadata;
import com.mobiletechnologylab.storagelib.wound.tables.measurements.LocalMetadata.WoundThermalImageMeasurementMeta;
import com.mobiletechnologylab.storagelib.wound.tables.measurements.MeasurementDbRow;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class LauncherActivity extends AppCompatActivity {

    private static final String TAG = LauncherActivity.class.getSimpleName();
    private static final int WOUND_VISIBLE_REQ_CODE = 424;
    private static final int WOUND_QUESTIONNAIRE_REQ_CODE = 425;
    private static final int WOUND_THERMAL_REQ_CODE = 426;


    private static final String QCONTAINER_APP_ENTRY = ".ContainerAppEntryActivity";
    private static final String WOUND_VISIBLE_IMAGE_PKG = ApiSettings.WOUND_ASSESSMENT_PACKAGE;
    private static final String WOUND_THERMAL_IMAGE_PKG = "com.mobiletechnologylab.thermalscreener";

    private static final String WOUND_VISIBLE_ACTIVITY =
            WOUND_VISIBLE_IMAGE_PKG + QCONTAINER_APP_ENTRY;
    private static final String WOUND_THERMAL_ACTIVITY =
            WOUND_THERMAL_IMAGE_PKG + QCONTAINER_APP_ENTRY;

    PermissionsHandler requiredPermissions;
    WoundDb db;
    PatientProfileDbRow patientDbRow;
    PatientProfileDbRowInfo pInfo = new PatientProfileDbRowInfo();
    StorageSettings storageSettings;
    String pod = "";
    Boolean kinyarwanda = true;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_measurements_summary);
        storageSettings = new StorageSettings(this);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (kinyarwanda) {
            TextView questionnaireLabel = findViewById(R.id.woundQuestionnaireAppNameTextView);
            questionnaireLabel.setText("Ibibazo bisanzwe");
            Button questionnaireStartButton = findViewById(R.id.woundQuestionnaireBtn);
            questionnaireStartButton.setText("Tangira");

            TextView visibleLabel = findViewById(R.id.woundAppNameTextView);
            visibleLabel.setText("Ifoto igaragara");
            Button visibleStartButton = findViewById(R.id.woundVisibleImageBtn);
            visibleStartButton.setText("Tangira");

            TextView thermalLabel = findViewById(R.id.woundThermalAppNameTextView);
            thermalLabel.setText("Ifoto y' ubushyuhe");
            Button thermalStartButton = findViewById(R.id.woundThermalBtn);
            thermalStartButton.setText("Tangira");

            Button doneButton = findViewById(R.id.doneBtn);
            doneButton.setText("Turasoje");
        }
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
        requiredPermissions = new PermissionsHandler(this, AllOf(GPS, STORAGE), () -> {
            if (DbUtils.isDataDbAvailable(this)) {
                db = WoundDb.getInstance(this);
            }

            AsyncTask.execute(() -> {
                Log.d(TAG, "selected patient: " + storageSettings.getSelectedPatientLocalId());
                if (db != null) {
                    patientDbRow = db.patients()
                            .getRowWithLocalId(storageSettings.getSelectedPatientLocalId());
                    pInfo = new PatientProfileDbRowInfo(patientDbRow);
                }
                Log.v(TAG, "pInfo: " + pInfo);
                runOnUiThread(() -> {
                    ((EditText) findViewById(R.id.patientIdEt)).setText(pInfo.getUsername());
                    setOnClickListeners();
                });
            });
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        requiredPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.v(TAG, "Got result back. RequestCode=" + requestCode + ". resultcode: " + resultCode);
        if (resultCode != RESULT_OK) {
            return;
        }

        if (requestCode == WOUND_VISIBLE_REQ_CODE) {
            setUIResult(R.id.woundVisibleImageBtn, R.id.statusIndicatorWoundImageView);
            return;
        }

        if (requestCode == WOUND_QUESTIONNAIRE_REQ_CODE) {
            setUIResult(R.id.woundQuestionnaireBtn,
                    R.id.statusIndicatorWoundQuestionnaireImageView);
            WoundQuestionnaire answers = GSON
                    .fromJson(data.getStringExtra(ScreeningActivity.RESULT_ANSWERS),
                            WoundQuestionnaire.class);
            saveQuestionnaireAnswersToDb(answers);
            pod = answers.getPod();
            return;
        }

        if (requestCode == WOUND_THERMAL_REQ_CODE) {
            setUIResult(R.id.woundThermalBtn, R.id.statusIndicatorWoundThermalImageView);
            saveThermalDataToDb(data);
            return;
        }

    }

    private static final String ARG_VISIBLE_SHARE_IMAGE = "Arg:VisibleShareImage";
    private static final String ARG_VISIBLE_IMAGE_PATH = "Arg:VisibleImage";
    private static final String ARG_VISIBLE_COLOR_PATH = "Arg:VisibleColor";

    private static final String ARG_THERMAL_IMAGE_PATH = "Arg:ThermalImage";
    private static final String ARG_THERMAL_CSV_PATH = "Arg:ThermalCsv";
    private static final String RESULT_THERMAL_IMAGE_PATH = "Res:ThermalImage";
    private static final String RESULT_THERMAL_CSV_PATH = "Res:ThermalCsv";

    public static final SimpleDateFormat THERMAL_DATE_FMT = new SimpleDateFormat(
            "yyyy.MM.dd.HH.mm.ss", Locale.US);

    private void saveThermalDataToDb(Intent data) {
        LocalMetadata metadata = new LocalMetadata.Builder()
                .setMeasurementType(LocalMetadata.MeasurementType.THERMAL_IMAGE.name())
                .setLocalClinicianIdAtCreation(storageSettings.getLoggedInClinicianLocalId())
                .setLocalPatientIdAtCreation(pInfo.getLocalId())
                .setServerPatientId(pInfo.getServerId())
                .setWoundThermalImageMeasurementMeta(
                        new WoundThermalImageMeasurementMeta(
                                data.getStringExtra(RESULT_THERMAL_IMAGE_PATH)))
                .createLocalMetadata();

        LocalMeasurement local = new LocalMeasurement.Builder()
                .setWoundThermalImage(
                        new com.mobiletechnologylab.apilib.apis.wound.diagnostics.add_measurement.wound_thermal_image.PostRequest.Builder()
                                .setPatientId(pInfo.getServerId())
                                .setUsergroupId(
                                        storageSettings.getApiSettings().getActiveUserGroupId())
                                .setMeasurement(
                                        new com.mobiletechnologylab.apilib.apis.wound.diagnostics.add_measurement.wound_thermal_image.PostRequest.DiagnosticMeasurement.Builder()
                                                .setWoundThermalImage(
                                                        new WoundThermalImage(
                                                                data.getStringExtra(
                                                                        RESULT_THERMAL_IMAGE_PATH),
                                                                data.getStringExtra(
                                                                        RESULT_THERMAL_CSV_PATH),
                                                                null))
                                                .setMetadata(new Metadata.Builder()
                                                        .setRecordedOn(
                                                                ApiDispatcherUtils
                                                                        .dateTimeInServerFormat())
                                                        .setClientType(
                                                                ApiDispatcherUtils.getClientType())
                                                        .setClientId(
                                                                ApiDispatcherUtils
                                                                        .getClientId(this))
                                                        .setClientVersion(
                                                                ApiDispatcherUtils
                                                                        .getClientVersion(this))
                                                        .setGpsLatitude(0d)
                                                        .setGpsLongitude(0d)
                                                        .createMetadata())
                                                .createDiagnosticMeasurement())
                                .createPostRequest())
                .createLocalMeasurement();
        AsyncTask.execute(() -> {
            db.measurements().insert(new MeasurementDbRow(local, metadata));
            runOnUiThread(() -> toast(this, "Thermal data saved"));
        });
    }


    private void saveQuestionnaireAnswersToDb(WoundQuestionnaire answers) {
        LocalMeasurement measurement = new LocalMeasurement.Builder()
                .setWoundQuestionnaire(new PostRequest.Builder()
                        .setPatientId(pInfo.getServerId())
                        .setUsergroupId(storageSettings.getApiSettings().getActiveUserGroupId())
                        .setMeasurement(new DiagnosticMeasurement.Builder()
                                .setMetadata(new Metadata.Builder()
                                        .setRecordedOn(
                                                ApiDispatcherUtils.dateTimeInServerFormat())
                                        .setClientType(ApiDispatcherUtils.getClientType())
                                        .setClientId(
                                                ApiDispatcherUtils.getClientId(this))
                                        .setClientVersion(
                                                ApiDispatcherUtils
                                                        .getClientVersion(this))
                                        .setGpsLatitude(0d)
                                        .setGpsLongitude(0d)
                                        .createMetadata())
                                .setWoundQuestionnaire(answers)
                                .createDiagnosticMeasurement())
                        .createPostRequest())
                .createLocalMeasurement();

        LocalMetadata metadata = new LocalMetadata.Builder()
                .setMeasurementType(MeasurementType.QUESTIONNAIRE.name())
                .setLocalClinicianIdAtCreation(storageSettings.getLoggedInClinicianLocalId())
                .setLocalPatientIdAtCreation(pInfo.getLocalId())
                .setServerPatientId(pInfo.getServerId())
                .createLocalMetadata();

        AsyncTask.execute(() -> {
            db.measurements().insert(new MeasurementDbRow(measurement, metadata));
            runOnUiThread(() -> {
                toast(this, "Questionnaire Save successful");
                setResult(RESULT_OK);
            });
        });
    }

    private void setUIResult(int measurementWidgetId, int presenceId) {
        Button measurementBtn = findViewById(measurementWidgetId);
        Drawable weightDrawable = measurementBtn.getBackground();
        weightDrawable = DrawableCompat.wrap(weightDrawable);
        DrawableCompat
                .setTint(weightDrawable, getResources().getColor(android.R.color.holo_green_light));
        weightDrawable = DrawableCompat.unwrap(weightDrawable);
        measurementBtn.setBackground(weightDrawable);
        measurementBtn.setText("Completed");

        ImageView status = findViewById(presenceId);
        status.setImageResource(android.R.drawable.presence_online);
    }

    private void setOnClickListeners() {
        String podString = "";
        if (pod != null && !pod.isEmpty()) {
            podString = pod + '-';
        }
        String timestamp = THERMAL_DATE_FMT.format(new Date());
        String fileNameWithoutExtOrPrefix = pInfo.getUsername() + "-" +
                podString + timestamp;

        Bundle visibleParams = new Bundle();
        markWithContainerParams(visibleParams);
        File visiblePatientFolder = new File(getWoundVisibleMeasurementsDir(), "" + pInfo.getUsername());
        visiblePatientFolder.mkdirs();
        String visibleImageFileName = "visible-" + fileNameWithoutExtOrPrefix + ".jpg";
        String colorChartImageFileName = "color-" + fileNameWithoutExtOrPrefix + ".jpg";
        visibleParams.putString(ARG_VISIBLE_IMAGE_PATH,
                new File(visiblePatientFolder, visibleImageFileName).getAbsolutePath());
        visibleParams.putString(ARG_VISIBLE_COLOR_PATH,
                new File(visiblePatientFolder, colorChartImageFileName).getAbsolutePath());
        if (pod != null && !pod.isEmpty() && pod.equals("p10")) {
            visibleParams.putBoolean(ARG_VISIBLE_SHARE_IMAGE, true);
        }
        else {
            visibleParams.putBoolean(ARG_VISIBLE_SHARE_IMAGE, false);
        }

        setOnClickListenerForMeasurements(R.id.woundVisibleImageBtn,
                WOUND_VISIBLE_IMAGE_PKG, WOUND_VISIBLE_ACTIVITY,
                WOUND_VISIBLE_REQ_CODE, visibleParams);

        findViewById(R.id.woundQuestionnaireBtn).setOnClickListener(v -> {
            startActivityForResult(new Intent(this, ScreeningActivity.class),
                    WOUND_QUESTIONNAIRE_REQ_CODE);
        });

        Bundle thermalParams = new Bundle();
        markWithContainerParams(thermalParams);
        File thermalPatientFolder = new File(getWoundThermalMeasurementsDir(), "" + pInfo.getUsername());
        thermalPatientFolder.mkdirs();
        String thermalImageFileName = "thermal-" + fileNameWithoutExtOrPrefix + ".jpg";
        String thermalCsvFileName = "thermal-" + fileNameWithoutExtOrPrefix + ".csv";
        thermalParams.putString(ARG_THERMAL_IMAGE_PATH,
                new File(thermalPatientFolder, thermalImageFileName).getAbsolutePath());
        thermalParams.putString(ARG_THERMAL_CSV_PATH,
                new File(thermalPatientFolder, thermalCsvFileName).getAbsolutePath());
        setOnClickListenerForMeasurements(R.id.woundThermalBtn,
                WOUND_THERMAL_IMAGE_PKG, WOUND_THERMAL_ACTIVITY,
                WOUND_THERMAL_REQ_CODE, thermalParams);

        Button doneBtn = findViewById(R.id.doneBtn);
        doneBtn.setOnClickListener(v -> finish());
    }


    public void markWithContainerParams(Bundle externalAppExtras) {
        ContainerAppUtils.exportCredentialsForSubAppBundle(this, externalAppExtras);
    }

    private void setOnClickListenerForMeasurements(int widgetId, final String packageName,
            final String targetActivity,
            final int requestCode,
            final Bundle extras) {
        Button measurementBtn = findViewById(widgetId);
        measurementBtn.setOnClickListener(v -> {
            Intent it = new Intent();
            it.setComponent(new ComponentName(packageName, targetActivity));
            it.putExtras(extras);
            try {
                startActivityForResult(it, requestCode);
            } catch (ActivityNotFoundException e) {
                Log.e(TAG, "Activity not found?", e);
                handleMissingApp(packageName);
            }
        });
    }

    private void handleMissingApp(final String packageNameForIfMissing) {
        // 1. Instantiate an AlertDialog.Builder with its constructor
        AlertDialog.Builder builder = new AlertDialog.Builder(LauncherActivity.this);
        String dialogMessage = getString(R.string.missing_app_dialog_message);
        String dialogTitle = getString(R.string.missing_app_dialog_title);
        // 2. Chain together various setter methods to set the dialog characteristics
        builder.setMessage(dialogMessage)
                .setTitle(dialogTitle);
        // Add the buttons
        builder.setPositiveButton(R.string.missing_app_dialog_install_action_text,
                (dialog, id) -> {
                    try {
                        if (packageNameForIfMissing == null) {
                            startActivity(new Intent(Intent.ACTION_VIEW,
                                    Uri.parse(
                                            "market://dev?id=Mobile+Technology+Lab")));
                        } else {
                            startActivity(
                                    new Intent(Intent.ACTION_VIEW, Uri.parse(
                                            "market://details?id="
                                                    + packageNameForIfMissing)));
                        }
                    } catch (ActivityNotFoundException anfe) {
                        if (packageNameForIfMissing == null) {
                            startActivity(
                                    new Intent(Intent.ACTION_VIEW, Uri.parse(
                                            "https://play.google.com/store/apps/developer?id=Mobile+Technology+Lab")));
                        } else {
                            startActivity(
                                    new Intent(Intent.ACTION_VIEW, Uri.parse(
                                            "https://play.google.com/store/apps/developer?id="
                                                    + packageNameForIfMissing)));
                        }
                    }
                });
        builder.setNegativeButton(R.string.missing_app_dialog_cancel_action_text, null);

        // 3. Get the AlertDialog from create()
        AlertDialog dialog = builder.create();

        dialog.show();
    }

}
