package com.mobiletechnologylab.wound_imager.profile;

import static com.mobiletechnologylab.wound_imager.Commons.getProfilesDir;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import java.io.File;


/**
 * Created by xsoriano on 5/21/17.
 */

public class ProfileSQLiteHelper extends SQLiteOpenHelper {

    public static final String TABLE_PATIENT_PROFILE = "patient_profile";
    public static final String COLUMN_ID = "patient_id";
    public static final String COLUMN_NAME = "patient_name";

    private static final String DATABASE_NAME = "existing_profiles.db";
    private static final int DATABASE_VERSION = 1;

    // Database creation sql statement
    private static final String DATABASE_CREATE = "create table "
            + TABLE_PATIENT_PROFILE + "( " + COLUMN_ID
            + " integer primary key, " + COLUMN_NAME
            + " text not null);";

    public ProfileSQLiteHelper(Context context) {
        super(context, getProfilesDir().getAbsolutePath() + File.separator
                + DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL(DATABASE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(ProfileSQLiteHelper.class.getName(),
                "Upgrading database from version " + oldVersion + " to "
                        + newVersion + ", which will destroy all old data");
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PATIENT_PROFILE);
        onCreate(db);
    }

}
