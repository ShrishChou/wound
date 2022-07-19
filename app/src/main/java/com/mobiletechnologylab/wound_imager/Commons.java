package com.mobiletechnologylab.wound_imager;

import android.app.Activity;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by xsoriano on 5/20/17.
 */

public class Commons {

    private Commons() {
    }

    public final static String ID_RESERVED_FOR_ANONYMOUS = "00000";

    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy.MMM.dd");

    public static final String EMPTY_STRING = "";

    public final static int CLINICIAN_ID_LENGTH = 2;
    public final static int PATIENT_ID_LENGTH = 5;
    public final static int PATIENT_ID_SHORT_LENGTH = 5;

    /* Here are the strings that are used as extras when passing values in between Activities via
    intents. The style convention is to use the the prefix EXTRA_ before the start of the variable
    name, which seems to be the same as the value of the string. Here is a link to the reference in
    the Android Development Documentation
    https://developer.android.com/training/basics/firstapp/starting-activity.html#java
     */
    public final static String EXTRA_CLINICIAN_ID_KEY = "CLINICIAN_ID";
    public final static String EXTRA_PATIENT_ID_KEY = "PATIENT_ID";
    public final static String EXTRA_TIMESTAMP_KEY = "TIMESTAMP";
    public final static String EXTRA_DURATION_KEY = "DURATION";
    public final static String EXTRA_PATIENT_NAME_KEY = "PATIENT_NAME";
    public final static String EXTRA_PATIENT_DOB_KEY = "PATIENT_DOB";
    public final static String EXTRA_PATIENT_AGE_KEY = "PATIENT_AGE";
    public final static String EXTRA_PATIENT_HEIGHT_KEY = "PATIENT_HEIGHT";
    public final static String EXTRA_PATIENT_GENDER_KEY = "PATIENT_GENDER";
    public final static String EXTRA_MOTHER_NAME_KEY = "MOTHER_NAME";
    public final static String EXTRA_MOTHER_DOB_KEY = "MOTHER_DOB";
    public final static String EXTRA_ADDRESS_KEY = "ADDRESS";
    public final static String EXTRA_PHONE_KEY = "PHONE";
    public final static String EXTRA_RATION_KEY = "RATION";
    public final static String EXTRA_ANEMIA_INFO_KEY = "ANEMIA_INFO";
    public final static String EXTRA_FOOD_TYPE_KEY = "FOOD_TYPE";
    public final static String EXTRA_COOKING_MATERIALS_KEY = "COOKING_MATERIALS";
    public final static String EXTRA_SHOW_INSTRUCTIONS_KEY = "SHOW_INSTRUCTIONS";
    public final static String EXTRA_CLINICIAN_ID_DIGIT_KEY_PREFIX = "CLINICIAN_ID_DIGIT_";
    public final static String EXTRA_PATIENT_ID_DIGIT_KEY_PREFIX = "PATIENT_ID_DIGIT_";
    public final static String EXTRA_APP_MEASUREMENT_KEY = "APP_MEASUREMENT";
    public final static String EXTRA_MANUAL_MEASUREMENT_KEY = "MANUAL_MEASUREMENT";
    public final static String EXTRA_TIME_ELAPSED_KEY = "TIME_ELAPSED";
    public final static String EXTRA_GPS_KEY = "GPS";
    public final static String EXTRA_APP_VERSION_KEY = "APP_VERSION";
    public static final String EXTRA_EDIT_MODE_KEY = "EDIT_MODE";
    public final static String EXTRA_OUTCOME_KEY = "OUTCOME";
    public final static String EXTRA_NUMBER_OF_TRIALS_KEY = "NUMBER_OF_TRIALS";
    public final static String EXTRA_CURRENT_TRIAL_COUNT_KEY = "CURRENT_TRIAL_COUNT";
    public final static int RESULT_OUTCOME_RETEST = 1;
    public final static int RESULT_OUTCOME_RESTART = 2;
    public final static int RESULT_OUTCOME_EXIT = 3;


    public final static String HOME_FOLDER = "MobileTechLab";
    public final static String PROFILES_FOLDER = HOME_FOLDER + File.separator + "Profiles";
    public final static String APP_FOLDER = HOME_FOLDER + File.separator + "PeakFlowMeter";
    public final static String MEASUREMENTS_FOLDER = APP_FOLDER + File.separator + "Measurements";


    private static File getDir(String folder) {
        String filepath = Environment.getExternalStorageDirectory().getPath();
        File file = new File(filepath, folder);
        return file;
    }

    private static File checkIfExistsOrMkDir(File dirs) {
        boolean wasSuccessful;
        if (!dirs.exists()) {
            wasSuccessful = dirs.mkdirs();
            Log.d("SAVED", Boolean.toString(wasSuccessful));
        }
        return dirs;
    }

    public static File getHomeDir() {
        String filepath = Environment.getExternalStorageDirectory().getPath();
        File dirs = new File(filepath, HOME_FOLDER);
        return checkIfExistsOrMkDir(dirs);
    }


    public static File getAppDir() {
        String filepath = Environment.getExternalStorageDirectory().getPath();
        File profilesFolder = new File(filepath, APP_FOLDER);
        return checkIfExistsOrMkDir(profilesFolder);
    }

    public static File getProfilesDir() {
        String filepath = Environment.getExternalStorageDirectory().getPath();
        File profilesFolder = new File(filepath, PROFILES_FOLDER);
        return checkIfExistsOrMkDir(profilesFolder);
    }

    public static File getMeasurementsDir() {
        String filepath = Environment.getExternalStorageDirectory().getPath();
        File measurementsFolder = new File(filepath, MEASUREMENTS_FOLDER);
        return checkIfExistsOrMkDir(measurementsFolder);
    }

//    public static final String[] JSON_KEYS = new String[] {"patientId",
//            "clinicianId",
//            "timestamp",
//            "gps",
//            "duration",
//            "appVersion",
//            "patientName",
//            "patientDoB",
//            "patientGender",
//            "motherName",
//            "address",
//            "phoneNumber",
//            "motherDoB",
//            "ration",
//            "anemiaInfo",
//            "foodType",
//            "cookingMaterial"};

    public static final Map<String, String> PROFILE_JSON_KEYS;

    static {
        Map<String, String> aMap = new HashMap<String, String>();
        aMap.put(EXTRA_PATIENT_ID_KEY, "patientId");
        aMap.put(EXTRA_CLINICIAN_ID_KEY, "clinicianId");
        aMap.put(EXTRA_TIMESTAMP_KEY, "timestamp");
        aMap.put(EXTRA_GPS_KEY, "gps");
        aMap.put(EXTRA_DURATION_KEY, "duration");
        aMap.put(EXTRA_APP_VERSION_KEY, "appVersion");
        aMap.put(EXTRA_PATIENT_NAME_KEY, "patientName");
        aMap.put(EXTRA_PATIENT_DOB_KEY, "patientDoB");
        aMap.put(EXTRA_PATIENT_GENDER_KEY, "patientGender");
        aMap.put(EXTRA_PATIENT_HEIGHT_KEY, "patientHeight");
        aMap.put(EXTRA_MOTHER_NAME_KEY, "motherName");
        aMap.put(EXTRA_ADDRESS_KEY, "address");
        aMap.put(EXTRA_PHONE_KEY, "phoneNumber");
        aMap.put(EXTRA_MOTHER_DOB_KEY, "motherDoB");
        aMap.put(EXTRA_RATION_KEY, "ration");
        aMap.put(EXTRA_ANEMIA_INFO_KEY, "anemiaInfo");
        aMap.put(EXTRA_FOOD_TYPE_KEY, "foodType");
        aMap.put(EXTRA_COOKING_MATERIALS_KEY, "cookingMaterial");
        PROFILE_JSON_KEYS = Collections.unmodifiableMap(aMap);
    }

    public static String profileSafeGet(JSONObject profileJSON, String key) {
        String val = "";
        if (profileJSON.has(PROFILE_JSON_KEYS.get(key))) {
            try {
                val = (String) profileJSON.get(PROFILE_JSON_KEYS.get(key));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return val;
    }


    public static void safeToast(final Activity activity, String validationMessage) {
        if (activity != null && validationMessage != null) {
            if (!validationMessage.isEmpty()) {
                final String finalValidationMessage = validationMessage;
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(activity, finalValidationMessage, Toast.LENGTH_LONG).show();
                    }
                });
            }
        }
    }

    public static Long[] updateElapsedTime(Long elapsedTime, Long startTime) {
        Long endTime = System.currentTimeMillis();
        Long delta = endTime - startTime;
        elapsedTime += delta;
        startTime = endTime;
        return new Long[]{elapsedTime, startTime};
    }


    public static JSONObject getJSONObjectFromFile(File f) throws Exception {
        String jsonString = null;
        JSONObject json = new JSONObject();
        if (f.exists()) {
            InputStream is = new FileInputStream(f.getAbsolutePath());
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            jsonString = new String(buffer, "UTF-8");
        }
        if (jsonString != null) {
            json = new JSONObject(jsonString);
        }
        return json;
    }

    public static int getPatientAgeInYears(String dob) {
        String[] dobParts = dob.split("\\.");
        if (dobParts.length != 3) {
            return -1;
        }
        int year = Integer.parseInt(dobParts[2]);
        int month = Integer.parseInt(dobParts[1]) + 1;
        int day = Integer.parseInt(dobParts[0]);
        return getPatientAgeInYears(day, month, year);
    }


    public static int getPatientAgeInYears(int day, int month, int year) {
        Calendar dob = Calendar.getInstance();
        dob.set(year, month, day);
        Calendar now = Calendar.getInstance();

        long diff = now.getTimeInMillis() - dob.getTimeInMillis(); //result in millis

        int years = (int) (diff / (long) (24 * 60 * 60 * 365.25f * 1000));
        return years;
    }


}
