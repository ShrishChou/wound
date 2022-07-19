package com.mobiletechnologylab.utils;

import android.app.Activity;

public class ContainerAppUtils {

    public static final String FROM_CONTAINER_BOOL_KEY = "FROM_CONTAINER";

    public static boolean wasStartedFromContainerApp(Activity activity) {
        return activity.getIntent().getBooleanExtra(FROM_CONTAINER_BOOL_KEY, false);
    }
}
