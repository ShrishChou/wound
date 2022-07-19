package com.mobiletechnologylab.wound_imager.history;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;

import com.mobiletechnologylab.wound_imager.image.ImageTargets;
import com.mobiletechnologylab.wound_imager.ui.ReviewImageActivity;
import com.mobiletechnologylab.utils.ContainerAppUtils;

public class OrchestratorActivity extends AppCompatActivity {

    private static final String TAG = OrchestratorActivity.class.getSimpleName();
    private static final int INSTRUCTIONS_ACTIVITY_REQ_CODE = 1003;
    private static final int IMAGE_TARGETS_REQ_CODE = 1004;
    private static final int REVIEW_IMAGE_REQ_CODE = 1005;

    public static final String ARG_VISIBLE_SHARE_IMAGE = "Arg:VisibleShareImage";
    public static final String ARG_VISIBLE_IMAGE_PATH = "Arg:VisibleImage";
    public static final String ARG_VISIBLE_COLOR_PATH = "Arg:VisibleColor";

    public static final String ARG_TEMP_WOUND_IMAGE_PATH = "Arg:TempWoundImagePath";
    public static final String ARG_TEMP_COLOR_CHART_PATH = "Arg:TempColorChartPath";


    private Boolean shareImage = false;
    private String imagePath = "";
    private String colorChartPath = "";
    private boolean launchedFromContainerApp = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        launchedFromContainerApp = ContainerAppUtils.wasStartedFromContainerApp(this);

        if (launchedFromContainerApp) {
            if (getIntent().getStringExtra(ARG_VISIBLE_IMAGE_PATH) == null ||
                    getIntent().getStringExtra(ARG_VISIBLE_COLOR_PATH) == null) {
                Log.v(TAG, "UHOH: " );
                setResult(RESULT_CANCELED);
                finish();
                return;
            }
            shareImage = getIntent().getBooleanExtra(ARG_VISIBLE_SHARE_IMAGE, false);
            imagePath = getIntent().getStringExtra(ARG_VISIBLE_IMAGE_PATH);
            colorChartPath = getIntent().getStringExtra(ARG_VISIBLE_COLOR_PATH);
        }

        startActivityForResult(new Intent(this, InstructionsActivity.class),
                INSTRUCTIONS_ACTIVITY_REQ_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == INSTRUCTIONS_ACTIVITY_REQ_CODE) {
            if (resultCode != RESULT_OK) {
                setResult(RESULT_CANCELED);
                finish();
                return;
            }
            Intent imageIntent = new Intent(this, ImageTargets.class);
            startActivityForResult(imageIntent, IMAGE_TARGETS_REQ_CODE);
            return;
        }

        if (requestCode == IMAGE_TARGETS_REQ_CODE) {
            if (resultCode != RESULT_OK) {
                startActivityForResult(new Intent(this, InstructionsActivity.class),
                        INSTRUCTIONS_ACTIVITY_REQ_CODE);
                return;
            }
            Intent reviewIntent = new Intent(this, ReviewImageActivity.class);
            reviewIntent.putExtras(getIntent());
            reviewIntent.putExtras(data);
            startActivityForResult(reviewIntent, REVIEW_IMAGE_REQ_CODE);
            return;
        }

        if (requestCode == REVIEW_IMAGE_REQ_CODE) {
            if (resultCode != RESULT_OK) {
                startActivityForResult(new Intent(this, ImageTargets.class),
                        IMAGE_TARGETS_REQ_CODE);
                return;
            }
            setResult(RESULT_OK);
            finish();
        }
    }
}
