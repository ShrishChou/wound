package com.mobiletechnologylab.wound_imager.history;

import static com.mobiletechnologylab.wound_imager.Commons.EXTRA_PATIENT_ID_KEY;
import static com.mobiletechnologylab.wound_imager.Commons.EXTRA_PATIENT_NAME_KEY;
import static com.mobiletechnologylab.wound_imager.Commons.getAppDir;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import au.com.bytecode.opencsv.CSVReader;
import com.mobiletechnologylab.wound_imager.R;
import com.mobiletechnologylab.wound_imager.image.ImageTargets;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Date;

/**
 * This activity shows the available files in a patient's history using a list view. Upon clicking
 * on the name for a certain file, the user is taken to the ShowHistory activity, where they can
 * view the LED readings from the file.
 */
public class ViewHistory extends Activity {

    private String patientName;
    private int patientId;
    int[] i;
    Date lastModified;

    private File patientDir;

    private ArrayAdapter<String> adapter;

    public static final String EXTRA_FILE = "com.mobiletechnologylab.peakflowmeter.history.EXTRA_FILE";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.history_view);

        patientName = getIntent().getStringExtra(EXTRA_PATIENT_NAME_KEY);
        patientId = getIntent().getIntExtra(EXTRA_PATIENT_ID_KEY, 0);
        i = new int[6];

        this.setTitle(patientName);

        ListView list = (ListView) findViewById(R.id.saved_scan_list);

        // get the list of available files
        File results = new File(getAppDir(), "results");
        patientDir = new File(results, Integer.toString(patientId));
        String[] files;
        if (patientDir.exists()) {
            files = patientDir.list();

        } else {
            files = new String[]{"No Patient Information Available"};
        }

        // make a list adapter to handle clicks on the list
        adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, files);
        list.setAdapter(adapter);

        AdapterView.OnItemClickListener listener = new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapter, View view,
                    int position, long id) {

                String filename = (String) adapter.getItemAtPosition(position);
                String scanFile = new File(patientDir, filename).getPath();

                CSVReader csvReader = null;
                String[] row2 = new String[10];

                // Reads the data for the patient from the CSV file
                try {
                    csvReader = new CSVReader(new FileReader(scanFile));
                    csvReader.readNext();
                    row2 = csvReader.readNext();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                // Turns the CSV data into integers so that they can stored in
                // an array and sent
                // to the ShowHistory activity using intent extras
                for (int j = 0; j < 6; j++) {
                    i[j] = Integer.parseInt(row2[j + 3]);
                }

                Intent intent = new Intent(ViewHistory.this, ShowHistory.class);
                // intent.putExtra(NewScanActivity.EXTRA_RESULTS, i);
                intent.putExtra(ImageTargets.EXTRA_PATIENT_ID, patientId);
                intent.putExtra(ImageTargets.EXTRA_PATIENT_NAME, patientName);
                startActivity(intent);
            }
        };
        list.setOnItemClickListener(listener);
    }


    @Override
    protected void onStop() {
        super.onStop();
        finish();
    }
}
