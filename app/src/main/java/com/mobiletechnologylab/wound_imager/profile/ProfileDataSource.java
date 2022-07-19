package com.mobiletechnologylab.wound_imager.profile;

/**
 * Created by xsoriano on 5/21/17.
 */

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import java.util.ArrayList;
import java.util.List;

public class ProfileDataSource {

    // Database fields
    private SQLiteDatabase database;
    private ProfileSQLiteHelper dbHelper;
    private String[] allColumns = {ProfileSQLiteHelper.COLUMN_ID,
            ProfileSQLiteHelper.COLUMN_NAME};

    public ProfileDataSource(Context context) {
        dbHelper = new ProfileSQLiteHelper(context);
    }

    public void open() throws SQLException {
        database = dbHelper.getWritableDatabase();
    }

    public void close() {
        dbHelper.close();
    }

    public BasicProfile createProfile(int id, String name) {
        ContentValues values = new ContentValues();
        values.put(ProfileSQLiteHelper.COLUMN_ID, id);
        values.put(ProfileSQLiteHelper.COLUMN_NAME, name);
        long insertId = database.insert(ProfileSQLiteHelper.TABLE_PATIENT_PROFILE, null,
                values);
        Cursor cursor = database.query(ProfileSQLiteHelper.TABLE_PATIENT_PROFILE,
                allColumns, ProfileSQLiteHelper.COLUMN_ID + " = " + insertId, null,
                null, null, null);
        cursor.moveToFirst();
        BasicProfile newBasicProfile = cursorToProfile(cursor);
        cursor.close();
        return newBasicProfile;
    }

    public void deleteProfile(BasicProfile basicProfile) {
        Long id = basicProfile.getId();
        System.out.println("BasicProfile deleted with id: " + id);
        database.delete(ProfileSQLiteHelper.TABLE_PATIENT_PROFILE, ProfileSQLiteHelper.COLUMN_ID
                + " = " + id, null);
    }

    public void deleteProfile(int id) {
        if (profileExists(id)) {
            System.out.println("BasicProfile deleted with id: " + id);
            database.delete(ProfileSQLiteHelper.TABLE_PATIENT_PROFILE, ProfileSQLiteHelper.COLUMN_ID
                    + " = " + id, null);
        }
    }

    public List<BasicProfile> getAllProfiles() {
        List<BasicProfile> basicProfiles = new ArrayList<BasicProfile>();

        Cursor cursor = database.query(ProfileSQLiteHelper.TABLE_PATIENT_PROFILE,
                allColumns, null, null, null, null, null);

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            BasicProfile basicProfile = cursorToProfile(cursor);
            basicProfiles.add(basicProfile);
            cursor.moveToNext();
        }
        // make sure to close the cursor
        cursor.close();
        return basicProfiles;
    }

    public boolean profileExists(int id) {
        String Query = "Select * from " + ProfileSQLiteHelper.TABLE_PATIENT_PROFILE +
                " where " + ProfileSQLiteHelper.COLUMN_ID + " = " + id;
        Cursor cursor = database.rawQuery(Query, null);
        if (cursor.getCount() <= 0) {
            cursor.close();
            return false;
        }
        cursor.close();
        return true;
    }

    private BasicProfile cursorToProfile(Cursor cursor) {
        BasicProfile basicProfile = new BasicProfile();
        basicProfile.setId(cursor.getInt(0));
        basicProfile.setName(cursor.getString(1));
        return basicProfile;
    }
}

