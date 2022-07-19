package com.mobiletechnologylab.wound_imager.profile;

/**
 * Created by xsoriano on 5/21/17.
 */

public class BasicProfile {

    private long mId;
    private String mName;

    public long getId() {
        return mId;
    }

    public void setId(long Id) {
        this.mId = Id;
    }

    public String getName() {
        return mName;
    }

    public void setName(String Name) {
        this.mName = Name;
    }

    // Will be used by the ArrayAdapter in the ListView
    @Override
    public String toString() {
        if (mId >= 0) {
            return String.format("%05d", mId) + " - " + mName;
        } else {
            return mName;
        }
    }

}
