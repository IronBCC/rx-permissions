package com.ironbcc.rxpermissions;

/**
 * TODO: Add destription
 *
 * @author ironbcc on 30.05.16.
 */
public enum PermissionGroup {
    CALENDAR("android.permission.READ_CALENDAR"),
    CAMERA("android.permission.CAMERA"),
    CONTACTS("android.permission.READ_CONTACTS"),
    LOCATION("android.permission.ACCESS_COARSE_LOCATION"),
    MICROPHONE("android.permission.RECORD_AUDIO"),
    PHONE("android.permission.READ_PHONE_STATE"),
    SENSORS("android.permission.BODY_SENSORS"),
    SMS("android.permission.RECEIVE_SMS"),
    STORAGE("android.permission.WRITE_EXTERNAL_STORAGE");

    private final String value;

    PermissionGroup(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
