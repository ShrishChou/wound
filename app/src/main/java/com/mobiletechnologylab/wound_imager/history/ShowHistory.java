package com.mobiletechnologylab.wound_imager.history;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import au.com.bytecode.opencsv.CSVReader;
import com.mobiletechnologylab.wound_imager.R;
import java.io.File;
import java.io.FileReader;

/**
 * Displays LED reading results and the will eventually display the final hg-level result after the
 * regression is implemented. These results are displayed when a given patient history file is
 * selected in the ViewHistory activity. The results displayed are those of the selected file.
 */
public class ShowHistory extends Activity {

    private TextView results;
    private int patientId;
    private String patientName;
    private TextView show_patient;
    private TextView show_id;
    private TextView show_result;
    private double hemoResult;
    int[] i;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.history_show);

        show_patient = (TextView) findViewById(R.id.patient_name);
        show_id = (TextView) findViewById(R.id.patient_id);
        show_result = (TextView) findViewById(R.id.result_value);
        show_result.setMovementMethod(new ScrollingMovementMethod());

        loadData(getIntent());
    }

    @Override
    protected void onStop() {
        super.onStop();
        finish();
    }

    void loadData(Intent it) {
        try {
            CSVReader reader = new CSVReader(
                    new FileReader(new File(it.getStringExtra(Intent.EXTRA_SUBJECT))));

            StringBuilder measurements = new StringBuilder(1024);
            String[] row;
            while ((row = reader.readNext()) != null) {
                if ("Measurement".equals(row[0])) {
                    measurements.append(row[2]).append(": ").append(row[1]).append(" mg/dL")
                            .append("\n");
                } else if ("Patient Name".equals(row[0])) {
                    show_patient.setText("Name: " + row[1]);
                } else if ("Patient ID".equals(row[0])) {
                    show_id.setText("ID: " + row[1]);
                }
            }
            reader.close();
            show_result.setText(measurements.toString());
        } catch (Exception e1) {
            android.util.Log
                    .v("AR Flow Meter", "An error occurred viewing the history: " + e1.getMessage(),
                            e1);
            Toast.makeText(this, "An error occurred viewing the history: " + e1.getMessage(),
                    Toast.LENGTH_LONG).show();
            return;
        }
    }

    public void onExport(View v) {
        Intent it = getIntent();
        try {
            Intent emailIntent = new Intent(Intent.ACTION_SEND);
            emailIntent.setType("text/plain");
            emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "AR Flow Meter Data Export");
            emailIntent.putExtra(android.content.Intent.EXTRA_STREAM,
                    Uri.fromFile(new File(it.getStringExtra(Intent.EXTRA_SUBJECT))));
            startActivity(Intent.createChooser(emailIntent, "Send AR Flow Meter Data:"));
        } catch (Exception e) {
            android.util.Log.v("AR Flow Meter",
                    "An error occurred exporting the results: " + e.getMessage(), e);
            Toast.makeText(this, "An error occurred exporting the results: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }
}