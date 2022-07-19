package com.mobiletechnologylab.wound_imager.history;

import static com.mobiletechnologylab.apilib.apis.common.ToastUtils.toast;
import static com.mobiletechnologylab.storagelib.utils.PermissionsHandler.AllOf;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.util.Log;

import com.mobiletechnologylab.wound_imager.AnalysisActivity;
import com.mobiletechnologylab.storagelib.wound.activities.BaseWoundLandingActivity;
import com.mobiletechnologylab.storagelib.wound.tables.measurements.LocalMetadata;
import com.mobiletechnologylab.wound_imager.GeneralSettingsActivity;
import com.mobiletechnologylab.wound_imager.R;


public class LandingActivity extends BaseWoundLandingActivity {
    @Override
    protected Drawable getLogo() {
        return getResources().getDrawable(R.drawable.wi_logo);
    }

    @Override
    protected void goToScreeningAnonymously(int requestCode) {
        Intent outgoingIntent = new Intent(this, OrchestratorActivity.class);
        startActivityForResult(outgoingIntent, requestCode);
    }

    @Override
    protected void goToScreeningActivity(int request) {
        Intent outgoingIntent = new Intent(this, OrchestratorActivity.class);
        startActivityForResult(outgoingIntent, request);
    }

    @Override
    protected LocalMetadata.MeasurementType getMeasurementType() {
        return LocalMetadata.MeasurementType.IMAGE;
    }

    @Override
    protected boolean skipResultsPage() {
        return true;
    }


    @Override
    protected void goToResultsPageForAnonymous(Intent data, int requestCode) {
        // Not needed.
    }

    @Override
    protected void goToResultsPage(Intent data, int requestCode) {
        // Not needed.
    }

    @Override
    protected void goToSettings() {
        startActivity(new Intent(this, GeneralSettingsActivity.class));
    }
}
