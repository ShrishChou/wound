package com.mobiletechnologylab.wound_imager.profile;


import android.content.Context;
import android.media.MediaScannerConnection;
import android.util.Log;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by xsoriano on 11/10/17.
 */

public class Profile {

    public final static String PROFILE_PATIENT_ID_KEY = "patientId";
    public final static String PROFILE_CLINICIAN_ID_KEY = "clinicianId";
    public final static String PROFILE_TIMESTAMP_KEY = "timestamp";
    public final static String PROFILE_GPS_KEY = "gps";
    public final static String PROFILE_APP_VERSION_KEY = "appVersion";
    public final static String PROFILE_DURATION_KEY = "duration";
    public final static String PROFILE_PATIENT_NAME_KEY = "patientName";
    public final static String PROFILE_PATIENT_DOB_KEY = "patientDoB";
    public final static String PROFILE_PATIENT_GENDER_KEY = "patientGender";
    public final static String PROFILE_PATIENT_HEIGHT_KEY = "patientHeight";
    public final static String PROFILE_MOTHER_NAME_KEY = "motherName";
    public final static String PROFILE_ADDRESS_KEY = "address";
    public final static String PROFILE_PHONE_NUMBER_KEY = "phoneNumber";
    public final static String PROFILE_MOTHER_DOB_KEY = "motherDoB";
    public final static String PROFILE_RATION_KEY = "ration";
    public final static String PROFILE_ANEMIA_INFO_KEY = "anemiaInfo";
    public final static String PROFILE_FOOD_TYPE_KEY = "foodType";
    public final static String PROFILE_COOKING_MATERIALS_KEY = "cookingMaterial";

    public final static String PATIENT_ID_FORMAT = "%05d";
    public final static String CLINICIAN_ID_FORMAT = "%02d";


    public static final Set<String> PROFILE_JSON_KEYS;

    static {
        PROFILE_JSON_KEYS = new HashSet<String>();
        PROFILE_JSON_KEYS.add(PROFILE_PATIENT_ID_KEY);
        PROFILE_JSON_KEYS.add(PROFILE_CLINICIAN_ID_KEY);
        PROFILE_JSON_KEYS.add(PROFILE_TIMESTAMP_KEY);
        PROFILE_JSON_KEYS.add(PROFILE_GPS_KEY);
        PROFILE_JSON_KEYS.add(PROFILE_APP_VERSION_KEY);
        PROFILE_JSON_KEYS.add(PROFILE_DURATION_KEY);
        PROFILE_JSON_KEYS.add(PROFILE_PATIENT_NAME_KEY);
        PROFILE_JSON_KEYS.add(PROFILE_PATIENT_DOB_KEY);
        PROFILE_JSON_KEYS.add(PROFILE_PATIENT_GENDER_KEY);
        PROFILE_JSON_KEYS.add(PROFILE_PATIENT_HEIGHT_KEY);
        PROFILE_JSON_KEYS.add(PROFILE_MOTHER_NAME_KEY);
        PROFILE_JSON_KEYS.add(PROFILE_ADDRESS_KEY);
        PROFILE_JSON_KEYS.add(PROFILE_PHONE_NUMBER_KEY);
        PROFILE_JSON_KEYS.add(PROFILE_MOTHER_DOB_KEY);
        PROFILE_JSON_KEYS.add(PROFILE_RATION_KEY);
        PROFILE_JSON_KEYS.add(PROFILE_ANEMIA_INFO_KEY);
        PROFILE_JSON_KEYS.add(PROFILE_FOOD_TYPE_KEY);
        PROFILE_JSON_KEYS.add(PROFILE_COOKING_MATERIALS_KEY);
    }

    private HashMap<String, Object> mFields;
    private File mProfilesPath;
    private boolean mEditMode = false;
    private boolean mExists;

    public Profile(String id, File profilesPath) {
        mFields = new HashMap<String, Object>();
        mFields.put(PROFILE_PATIENT_ID_KEY, formatUncheckedPatientId(id));
        mProfilesPath = profilesPath;
        mExists = loadProfile();

    }

    private String getId() {
        String uncheckedId = (String) mFields.get(PROFILE_PATIENT_ID_KEY);
        return formatUncheckedPatientId(uncheckedId);
    }

    public static String formatUncheckedPatientId(String uncheckedId) {
        return formatUncheckedId(PATIENT_ID_FORMAT, uncheckedId);
    }

    public static String formatUncheckedClinicianId(String uncheckedId) {
        return formatUncheckedId(CLINICIAN_ID_FORMAT, uncheckedId);
    }

    private static String formatUncheckedId(String format, String uncheckedId) {
        String formattedId = uncheckedId;
        if (isInteger(uncheckedId)) {
            int id = Integer.parseInt(uncheckedId);
            formattedId = String.format(format, id);
        }
        return formattedId;
    }


    public static boolean isInteger(String s) {
        return isInteger(s, 10);
    }

    private static boolean isInteger(String s, int radix) {
        if (s == null) {
            return false;
        }
        if (s.isEmpty()) {
            return false;
        }
        for (int i = 0; i < s.length(); i++) {
            if (i == 0 && s.charAt(i) == '-') {
                if (s.length() == 1) {
                    return false;
                } else {
                    continue;
                }
            }
            if (Character.digit(s.charAt(i), radix) < 0) {
                return false;
            }
        }
        return true;
    }


    public void addField(String key, Object value) {
        if (key.equals(PROFILE_PATIENT_ID_KEY)) {
            return;
        }
        if (key.equals(PROFILE_CLINICIAN_ID_KEY) && value != null) {
            value = formatUncheckedClinicianId((String) value);
        }
        mFields.put(key, value);
        if (!PROFILE_JSON_KEYS.contains(key)) {
            PROFILE_JSON_KEYS.add(key);
        }

    }

    public String getField(String key) {
        if (mFields.containsKey(key)) {
            if (mFields.get(key) != null) {
                String field = String.valueOf(mFields.get(key));
                return field;
            }
        }
        return "";
    }

    public boolean saveProfile(Context context) {
        boolean success = false;
        JSONObject profileJSON = new JSONObject();
        File profileFile = new File(mProfilesPath, getId() + ".json");
        if (profileFile.exists() && !mEditMode) {
            return success;
        } else {
            try {
                for (String key : mFields.keySet()) {

                    profileJSON.put(key, getField(key));
                }

                FileOutputStream profileOutputStream = new FileOutputStream(profileFile);
                profileOutputStream.write(profileJSON.toString().getBytes());
                profileOutputStream.flush();
                profileOutputStream.close();
                MediaScannerConnection
                        .scanFile(context, new String[]{profileFile.getAbsolutePath()}, null, null);
                success = false;
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return success;
    }


    private boolean loadProfile() {
        File profileFile = new File(mProfilesPath, getId() + ".json");
        HashMap<String, Object> storedFields = new HashMap<String, Object>();
        boolean exists = profileFile.exists();
        boolean success = false;
        if (exists) {
            JSONObject profileJSON = null;

            try {
                profileJSON = getJSONObjectFromFile(profileFile);
                storedFields = (HashMap<String, Object>) jsonToMap(profileJSON);

            } catch (Exception e) {
                e.printStackTrace();
                Log.v("Profile Loading Error",
                        "Error occurred loading the profile: " + e.getMessage(), e);
                return success;
            }
            for (String fieldKey : storedFields.keySet()) {
                if (!mFields.containsKey(fieldKey)) {
                    mFields.put(fieldKey, storedFields.get(fieldKey));
                }
            }
            success = true;
        }
        return success;

    }

    public boolean profileExists() {
        return new Boolean(mExists);
    }


    public boolean checkIfAllNeededFieldsArePresent() {
        return true;
    }

    public static Map<String, Object> jsonToMap(JSONObject json) throws JSONException {
        Map<String, Object> retMap = new HashMap<String, Object>();

        if (json != JSONObject.NULL) {
            retMap = toMap(json);
        }
        return retMap;
    }

    public static Map<String, Object> toMap(JSONObject object) throws JSONException {
        Map<String, Object> map = new HashMap<String, Object>();

        Iterator<String> keysItr = object.keys();
        while (keysItr.hasNext()) {
            String key = keysItr.next();
            Object value = object.get(key);

            if (value instanceof JSONArray) {
                value = toList((JSONArray) value);
            } else if (value instanceof JSONObject) {
                value = toMap((JSONObject) value);
            }
            map.put(key, value);
        }
        return map;
    }

    public static List<Object> toList(JSONArray array) throws JSONException {
        List<Object> list = new ArrayList<Object>();
        for (int i = 0; i < array.length(); i++) {
            Object value = array.get(i);
            if (value instanceof JSONArray) {
                value = toList((JSONArray) value);
            } else if (value instanceof JSONObject) {
                value = toMap((JSONObject) value);
            }
            list.add(value);
        }
        return list;
    }

    public boolean setEditMode(boolean newVal) {
        mEditMode = newVal;
        return mEditMode;
    }

    private JSONObject getJSONObjectFromFile(File f) throws Exception {
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


}
