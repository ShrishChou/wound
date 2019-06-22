package com.mobiletechnologylab.ncscreenerv2;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import com.mobiletechnologylab.storagelib.malnutrition.activities.BaseMalnutritionLandingActivity;
import com.mobiletechnologylab.storagelib.malnutrition.activities.PatientProfileActivity;
import com.mobiletechnologylab.storagelib.malnutrition.tables.measurements.LocalMetadata.MeasurementType;
import com.mobiletechnologylab.storagelib.pulmonary.activities.NoOpActivity;

public class LandingActivity extends BaseMalnutritionLandingActivity {

    @Override
    protected Drawable getLogo() {
        return getResources().getDrawable(R.drawable.logo);
    }

    @Override
    protected void goToScreeningAnonymously(int requestCode) {
        startActivityForResult(new Intent(this, MeasurementSummaryActivity.class),
                requestCode);
    }

    @Override
    protected void goToScreeningActivity(int requestCode) {
        startActivityForResult(new Intent(this, MeasurementSummaryActivity.class),
                requestCode);
    }

    @Override
    protected boolean skipResultsPage() {
        return true;
    }

    @Override
    protected void goToResultsPageForAnonymous(Intent data, int requestCode) {
    }

    @Override
    protected void goToResultsPage(Intent data, int requestCode) {
    }

    @Override
    protected void goToPatientProfile(int requestCode) {
        Intent goToPatientProfileActivity = new Intent(this, PatientProfileActivity.class);
        goToPatientProfileActivity
                .putExtra(PatientProfileActivity.ARG_SHOW_DIAGNOSTICS_AND_ANALYSIS_BOOL, true);
        startActivityForResult(goToPatientProfileActivity, requestCode);
    }

    @Override
    protected MeasurementType getMeasurementType() {
        return MeasurementType.ALL;
    }

    @Override
    protected void goToPatientDiagnosis(int requestCode) {
        startActivityForResult(new Intent(this, NoOpActivity.class), requestCode);
    }

    @Override
    protected void goToComputerAnalysis(int requestCode) {
        startActivityForResult(new Intent(this, NoOpActivity.class), requestCode);
    }
}
