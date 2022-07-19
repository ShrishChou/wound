package com.mobiletechnologylab.wound_imager.history;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.mobiletechnologylab.wound_imager.R;

/**
 * An example full-screen activity that shows and hides the system UI (i.e. status bar and
 * navigation/system bar) with user interaction.
 */
public class InstructionsActivity extends AppCompatActivity {
    Boolean kinyarwanda = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.history_activity_instructions);

        if (kinyarwanda) {
            TextView step1Label = findViewById(R.id.step1);
            step1Label.setText(R.string.instruction_label_kr_1);

            TextView step2Label = findViewById(R.id.step2);
            step2Label.setText(R.string.instruction_label_kr_2);

            Button startButton = findViewById(R.id.start_button);
            startButton.setText("Tangira");
        }


        Button closeButton = (Button) findViewById(R.id.start_button);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent output = new Intent();
                setResult(RESULT_OK, output);
                finish();
            }
        });

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

    @Override
    protected void onResume() {
        super.onResume();
    }

//    public void goForth(View v)
//    {
//        Intent output = new Intent();
//        setResult(RESULT_OK, output);
//        finish();
//    }
}
