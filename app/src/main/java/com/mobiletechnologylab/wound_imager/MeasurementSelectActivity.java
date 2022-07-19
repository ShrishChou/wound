package com.mobiletechnologylab.wound_imager;

import static com.mobiletechnologylab.apilib.apis.common.ToastUtils.toast;
import static com.mobiletechnologylab.storagelib.wound.activities.ViewMeasurementsActivity.getMeasurements;
import static com.mobiletechnologylab.storagelib.utils.PermissionsHandler.STORAGE;

import android.content.Intent;
import androidx.databinding.DataBindingUtil;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import com.mobiletechnologylab.wound_imager.databinding.ActivityMeasurementSelectBinding;
import com.mobiletechnologylab.wound_imager.utils.MeasurementSelectAdapter;
import com.mobiletechnologylab.wound_imager.utils.MeasurementSelectAdapter.CheckBoxClickedListener;
import com.mobiletechnologylab.storagelib.wound.WoundDb;
import com.mobiletechnologylab.storagelib.interfaces.MeasurementDialog;
import com.mobiletechnologylab.storagelib.wound.tables.measurements.LocalMetadata.MeasurementType;
import com.mobiletechnologylab.storagelib.wound.tables.measurements.MeasurementDbRow;
import com.mobiletechnologylab.storagelib.wound.tables.measurements.MeasurementDbRowInfo;
import com.mobiletechnologylab.storagelib.diabetes.tables.patient_profiles.PatientProfileDbRowInfo;
import com.mobiletechnologylab.storagelib.interfaces.SortableMeasurementInfoIface;
import com.mobiletechnologylab.storagelib.utils.PermissionsHandler;
import com.mobiletechnologylab.storagelib.utils.StorageSettings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class MeasurementSelectActivity extends AppCompatActivity implements
        CheckBoxClickedListener {
    protected enum SortByKey {
        MEASUREMENT_DATE,
        MEASUREMENT_TYPE,
    }

    protected SortByKey currentSortKey = SortByKey.MEASUREMENT_DATE;

    public static final String RESULT_SELECTED_MEASUREMENTS = "Res:SELECTED_MEASUREMENTS";

    private HashMap<Integer, Long> selectedMeasurements;


    ActivityMeasurementSelectBinding B;

    StorageSettings storageSettings;

    WoundDb db;
    PatientProfileDbRowInfo pInfo;
    PermissionsHandler permissionsHandler;

    ArrayList<MeasurementDbRow> measurements = new ArrayList<>();
    ArrayList<MeasurementDbRowInfo> measurementsDataset;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        storageSettings = new StorageSettings(this);
        B = DataBindingUtil.setContentView(this, R.layout.activity_measurement_select);
        selectedMeasurements = new HashMap<>();
        B.doneBtn.setOnClickListener(v -> {
            if (selectedMeasurements.isEmpty()) {
                toast(this, getString(R.string.select_measurements));
                return;
            }
            Intent out = new Intent();
            out.putExtra(RESULT_SELECTED_MEASUREMENTS, selectedMeasurements);
            setResult(RESULT_OK, out);
            finish();
        });
    }

    public static Map<Integer, Long> extractSelectedMeasurements(Intent data) {
        return (Map<Integer, Long>) data.getSerializableExtra(RESULT_SELECTED_MEASUREMENTS);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        permissionsHandler.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onStart() {
        super.onStart();
        permissionsHandler = new PermissionsHandler(this, STORAGE, () -> {
            db = WoundDb.getInstance(this);
            AsyncTask.execute(() -> {
                pInfo = new PatientProfileDbRowInfo(db.patients()
                        .getRowWithLocalId(storageSettings.getSelectedPatientLocalId()));
                runOnUiThread(this::setupUi);
            });
        });
    }

    private void setupUi() {
        if (pInfo == null) {
            Log.w(TAG, "Patient Info");
            finish();
            return;
        }
        getSupportActionBar().setTitle(pInfo.getName());
        loadMeasurements();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            onBackPressed();
        }

        if (id == com.mobiletechnologylab.storagelib.R.id.action_sort) {
            if (currentSortKey == SortByKey.MEASUREMENT_DATE) {
                currentSortKey = SortByKey.MEASUREMENT_TYPE;
            } else if (currentSortKey == SortByKey.MEASUREMENT_TYPE) {
                currentSortKey = SortByKey.MEASUREMENT_DATE;
            }
            configureListView();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void loadMeasurements() {
        runOnUiThread(() -> {
            B.loading.setVisibility(View.VISIBLE);
            B.measurementsLv.setVisibility(View.GONE);
            AsyncTask.execute(() -> {
                measurements.clear();
                measurements.addAll(getMeasurements(db, MeasurementType.ALL, pInfo));
                runOnUiThread(() -> {
                    configureListView();
                    B.loading.setVisibility(View.GONE);
                    B.measurementsLv.setVisibility(View.VISIBLE);
                });
            });
        });
    }

    private static final String TAG = MeasurementSelectActivity.class.getSimpleName();

    private void configureListView() {
        measurementsDataset = new ArrayList<>(measurements.size());
        for (MeasurementDbRow e : measurements) {
            Log.v(TAG, "Measurements: " + e);
            measurementsDataset.add(new MeasurementDbRowInfo(e));
        }

        Comparator<SortableMeasurementInfoIface> cmp = null;

        if (currentSortKey == SortByKey.MEASUREMENT_DATE) {
            cmp = this::sortByDate;
            Collections.sort(measurementsDataset, cmp);

        } else if (currentSortKey == SortByKey.MEASUREMENT_TYPE) {
            cmp = this::sortByType;
            Collections.sort(measurementsDataset, cmp);

        } else {
            Collections.sort(measurementsDataset,
                    (t1, t) -> {
                        if (t.getDateRecordedOn() != null && t1.getDateRecordedOn() != null) {
                            return t.getDateRecordedOn().compareTo(t1.getDateRecordedOn());
                        }
                        if (t.getDateRecordedOn() == null) {
                            Log.v(TAG, "Null: " + t);
                            return -1;
                        }
                        Log.v(TAG, "Null: " + t1);
                        return 1;
                    });

        }

        MeasurementSelectAdapter adapter = new MeasurementSelectAdapter(measurementsDataset,
                selectedMeasurements, this);
        B.measurementsLv.setAdapter(adapter);
        B.measurementsLv.setOnItemClickListener((adapterView, view, i, l) -> {
            Log.d(TAG, "configureListView: Showing dialog");
            MeasurementDialog measurementDialog = measurementsDataset.get(i)
                    .getDialog(this, db, pInfo);
            if (measurementDialog != null) {
                measurementDialog.show(this::loadMeasurements);
            }
        });
    }

    @Override
    public void onMeasurementCheckBoxChecked(int i, boolean checked) {
        Integer key = measurementsDataset.get(i).getRow().localId;
        if (checked) {
            selectedMeasurements
                    .put(key, measurementsDataset.get(i).getRow().serverId);
        } else {
            selectedMeasurements.remove(key);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(com.mobiletechnologylab.storagelib.R.menu.view_measurements_menu, menu);
        return true;
    }

    /**
     * Sort by decreasing date.
     */
    protected int sortByDate(SortableMeasurementInfoIface a, SortableMeasurementInfoIface b) {
        if (b.getDateRecordedOn() != null && a.getDateRecordedOn() != null) {
            return b.getDateRecordedOn().compareTo(a.getDateRecordedOn());
        }
        if (b.getDateRecordedOn() == null) {
            return -1;
        }
        return 1;
    }

    protected int sortByName(SortableMeasurementInfoIface a, SortableMeasurementInfoIface b) {
        if (b.getDescription() != null && a.getDescription() != null) {
            if (b.getDescription().equals(a.getDescription())) {
                return sortByDate(a, b);
            }
            return b.getDescription().compareTo(a.getDescription());
        }
        if (b.getDescription() == null) {
            return -1;
        }
        return 1;
    }

    /**
     * Sort by measurement type.
     */
    protected int sortByType(SortableMeasurementInfoIface a, SortableMeasurementInfoIface b) {
        if (b.getTypeName() != null && a.getTypeName() != null) {
            if (b.getTypeName().equals(a.getTypeName())) {
                return sortByName(a, b);
            }
            return b.getTypeName().compareTo(a.getTypeName());
        }
        if (b.getTypeName() == null) {
            return -1;
        }
        return 1;
    }
}
