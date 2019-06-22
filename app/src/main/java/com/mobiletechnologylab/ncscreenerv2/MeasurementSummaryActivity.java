package com.mobiletechnologylab.ncscreenerv2;

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
import com.mobiletechnologylab.apilib.apis.common.ApiSettings;
import com.mobiletechnologylab.storagelib.diabetes.tables.patient_profiles.PatientProfileDbRow;
import com.mobiletechnologylab.storagelib.diabetes.tables.patient_profiles.PatientProfileDbRowInfo;
import com.mobiletechnologylab.storagelib.malnutrition.MalnutritionDb;
import com.mobiletechnologylab.storagelib.utils.ContainerAppUtils;
import com.mobiletechnologylab.storagelib.utils.DbUtils;
import com.mobiletechnologylab.storagelib.utils.PermissionsHandler;
import com.mobiletechnologylab.storagelib.utils.StorageSettings;

public class MeasurementSummaryActivity extends AppCompatActivity {

    private static final String TAG = MeasurementSummaryActivity.class.getSimpleName();
    private static final int MUAC_REQUEST_CODE = 424;


    private static final String QCONTAINER_APP_ENTRY = ".ContainerAppEntryActivity";
    private static final String MUAC_PACKAGE = ApiSettings.MUAC_PACKAGE;

    private static final String MUAC_ACTIVITY =
            MUAC_PACKAGE + QCONTAINER_APP_ENTRY;

    private static final String PLAY_STORE_URL = "https://play.google.com/store/apps/details?id=";
    private static final String PLAY_STORE_APP = "https://play.google.com/store/apps/details?id=";

    PermissionsHandler requiredPermissions;
    MalnutritionDb db;
    PatientProfileDbRow patientDbRow;
    PatientProfileDbRowInfo pInfo = new PatientProfileDbRowInfo();
    StorageSettings storageSettings;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_measurements_summary);
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
        requiredPermissions = new PermissionsHandler(this, AllOf(GPS, STORAGE), () -> {
            if (DbUtils.isDataDbAvailable(this)) {
                db = MalnutritionDb.getInstance(this);
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
                    ((EditText) findViewById(R.id.patientIdEt)).setText("" + pInfo.getLocalId());
                    ((EditText) findViewById(R.id.patientNameEt)).setText(pInfo.getName());
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
        if (resultCode == RESULT_OK) {
            if (requestCode == MUAC_REQUEST_CODE) {
                setUIResult(R.id.muacBtn);
                ImageView questionnaireStatus = (ImageView) findViewById(
                        R.id.statusIndicatorMuacImageView);
                questionnaireStatus.setImageResource(android.R.drawable.presence_online);
            }
        }
    }

    private void setUIResult(int measurementWidgetId) {
        Button measurementBtn = (Button) findViewById(measurementWidgetId);
        Drawable weightDrawable = measurementBtn.getBackground();
        weightDrawable = DrawableCompat.wrap(weightDrawable);
        DrawableCompat
                .setTint(weightDrawable, getResources().getColor(android.R.color.holo_green_light));
        weightDrawable = DrawableCompat.unwrap(weightDrawable);
        measurementBtn.setBackground(weightDrawable);
        measurementBtn.setText("Completed");
    }

    private void setOnClickListeners() {
        Bundle muacParams = new Bundle();
        markWithContainerParams(muacParams);

        setOnClickListenerForMeasurements(R.id.muacBtn,
                MUAC_PACKAGE, MUAC_ACTIVITY,
                MUAC_REQUEST_CODE, muacParams);

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
        AlertDialog.Builder builder = new AlertDialog.Builder(MeasurementSummaryActivity.this);
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
