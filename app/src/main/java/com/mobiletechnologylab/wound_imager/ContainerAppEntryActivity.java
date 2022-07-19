package com.mobiletechnologylab.wound_imager;

import static com.mobiletechnologylab.storagelib.utils.PermissionsHandler.AllOf;
import static com.mobiletechnologylab.storagelib.utils.PermissionsHandler.CAMERA;
import static com.mobiletechnologylab.storagelib.utils.PermissionsHandler.GPS;
import static com.mobiletechnologylab.storagelib.utils.PermissionsHandler.STORAGE;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import com.mobiletechnologylab.storagelib.diabetes.tables.patient_profiles.PatientProfileDbRow;
import com.mobiletechnologylab.storagelib.diabetes.tables.patient_profiles.PatientProfileDbRowInfo;
import com.mobiletechnologylab.storagelib.utils.ContainerAppUtils;
import com.mobiletechnologylab.storagelib.utils.PermissionsHandler;
import com.mobiletechnologylab.storagelib.utils.StorageSettings;
import com.mobiletechnologylab.storagelib.wound.WoundDb;
import com.mobiletechnologylab.wound_imager.history.OrchestratorActivity;
import com.mobiletechnologylab.wound_imager.ui.ReviewImageActivity;

public class ContainerAppEntryActivity extends Activity {

    private static final String TAG = ContainerAppEntryActivity.class.getSimpleName();
    private static final int START_ORCHESTRATOR_REQ_CODE = 300;
//    private static final String ARG_VISIBLE_POD = "Arg:VisiblePod";
//    private static final String ARG_VISIBLE_IMAGE_PATH = "Arg:VisibleImage";
//    private static final String ARG_VISIBLE_COLOR_PATH = "Arg:VisibleColor";

    StorageSettings storageSettings;
    PermissionsHandler permissionsHandler;
    WoundDb db;
    PatientProfileDbRowInfo pInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        storageSettings = new StorageSettings(this);

        ContainerAppUtils.maybeImportCredentialsFromContainerApp(this);
    }

    boolean screenerLaunched = false;

    @Override
    protected void onStart() {
        super.onStart();
        permissionsHandler = new PermissionsHandler(this, AllOf(GPS, STORAGE, CAMERA), () -> {
            Log.i(TAG, "All permissions were granted.");
            WoundDb db = WoundDb.getInstance(this);
            AsyncTask.execute(() -> {
                PatientProfileDbRow row = db.patients()
                        .getRowWithLocalId(storageSettings.getSelectedPatientLocalId());
                pInfo = row == null ? null : new PatientProfileDbRowInfo(row);

                runOnUiThread(() -> {
                    if (!screenerLaunched) {
                        screenerLaunched = true;
                        launchScreener();
                    }
                });
            });
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
            String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        permissionsHandler.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void launchScreener() {
        Intent intent = new Intent(this, OrchestratorActivity.class);
        intent.putExtras(getIntent());  // for args.
        startActivityForResult(intent, START_ORCHESTRATOR_REQ_CODE);
   }

    private void done(boolean success) {
        Intent out = new Intent();
        if (success) {
            setResult(RESULT_OK, out);
            finish();
            return;
        }
        setResult(RESULT_CANCELED, out);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.v(TAG, "OnActivity: " + requestCode + " code:" + resultCode);
        if (resultCode != RESULT_OK) {
            done(false);
            return;
        }

        switch (requestCode) {
            case START_ORCHESTRATOR_REQ_CODE: {
                Log.v(TAG, "Calling done:");
                done(true);
                return;
            }
        }
        done(true);
    }
}
