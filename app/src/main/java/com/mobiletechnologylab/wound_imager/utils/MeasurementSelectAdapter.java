package com.mobiletechnologylab.wound_imager.utils;


import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;
import com.mobiletechnologylab.storagelib.R;
import com.mobiletechnologylab.storagelib.wound.tables.measurements.MeasurementDbRowInfo;
import java.util.ArrayList;
import java.util.HashMap;

public class MeasurementSelectAdapter extends ArrayAdapter<MeasurementDbRowInfo> {

    public interface CheckBoxClickedListener {

        void onMeasurementCheckBoxChecked(int i, boolean checked);
    }

    private static class ViewHolder {

        CheckBox checkBox;
        TextView descriptionTv;
        TextView recordedOnTv;
        TextView recorderUserNameTv;
        AppCompatImageView isUploadedToCloudIv;
        AppCompatImageView uploadToCloudIv;
        AppCompatImageView measurementTypeIv;
    }

    private HashMap<Integer, Long> selectedMeasurements;

    public MeasurementSelectAdapter(ArrayList<MeasurementDbRowInfo> measurements,
                                    HashMap<Integer, Long> selectedMeasurements, Context context) {
        super(context, R.layout.row_measurement_with_checkbox, measurements);
        this.selectedMeasurements = selectedMeasurements;
    }

    private static final String TAG = MeasurementSelectAdapter.class.getSimpleName();

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        ViewHolder vh;
        if (convertView == null) {
            vh = new ViewHolder();
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(R.layout.row_measurement_with_checkbox, parent, false);
            vh.recordedOnTv = convertView.findViewById(R.id.recordedOnTv);
            vh.recorderUserNameTv = convertView.findViewById(R.id.recorderUserNameTv);
            vh.descriptionTv = convertView.findViewById(R.id.descriptionTv);
            vh.isUploadedToCloudIv = convertView.findViewById(R.id.isUploadedToCloudIv);
            vh.uploadToCloudIv = convertView.findViewById(R.id.uploadToCloudIv);
            vh.measurementTypeIv = convertView.findViewById(R.id.measurementTypeIv);
            vh.checkBox = convertView.findViewById(R.id.selectedCheckBox);
            convertView.setTag(vh);
        } else {
            vh = (ViewHolder) convertView.getTag();
        }

        MeasurementDbRowInfo mInfo = getItem(position);
        vh.descriptionTv.setText(mInfo.getDescription());
        vh.recordedOnTv.setText(mInfo.getRecordedOn());
        vh.recorderUserNameTv.setText(mInfo.getRecorderUserName());
        vh.isUploadedToCloudIv
                .setVisibility(mInfo.isAvailableOnServer() ? View.VISIBLE : View.GONE);
        vh.uploadToCloudIv.setVisibility(mInfo.isAvailableOnServer() ? View.GONE : View.VISIBLE);
        vh.measurementTypeIv.setImageResource(mInfo.getDrawable());
        vh.checkBox.setOnCheckedChangeListener((compoundButton, checked) -> {
            ((CheckBoxClickedListener) getContext()).onMeasurementCheckBoxChecked(position, checked);
        });
        vh.checkBox.setChecked(selectedMeasurements.containsKey(mInfo.getRow().localId));
        return convertView;
    }

}

