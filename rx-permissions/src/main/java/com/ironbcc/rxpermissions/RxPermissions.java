package com.ironbcc.rxpermissions;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import rx.Observable;
import rx.functions.*;
import rx.subjects.PublishSubject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class RxPermissions {
    private static final int BASE_REQUEST_CODE = 1000;
    private static volatile int lastRequestCode = BASE_REQUEST_CODE;
    private static final HashMap<Integer, PublishSubject<Boolean>> requestMap = new HashMap<>();

    @NonNull
    public static Observable<Boolean> observe(final Activity activity, final PermissionGroup permissions) {
        return Observable.defer(new Func0<Observable<Boolean>>() {
            @Override
            public Observable<Boolean> call() {
                return Observable.just(isGranted(activity, permissions.getValue()));
            }
        });
    }

    @NonNull
    public static Observable<Boolean> request(final Activity activity, final PermissionGroup permissions) {
        return observe(activity, permissions)
            .flatMap(new Func1<Boolean, Observable<Boolean>>() {
                    @Override
                    public Observable<Boolean> call(Boolean granted) {
                        if (!granted) {
                            return schedulePermissionsRequest(activity, permissions);
                        }
                        return Observable.just(true);
                    }
                });
    }

    @NonNull
    public static Observable<Boolean> requestWithRationale(final Dialog rationaleDialog, final Activity activity, final PermissionGroup permissions) {
        final PublishSubject<Void> rationaleSubject = PublishSubject.create();
        rationaleDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                rationaleSubject.onNext(null);
            }
        });
        return requestWithRationale(
            rationaleSubject.doOnSubscribe(new Action0() {
                @Override
                public void call() {
                    rationaleDialog.show();
                }
            }),
            activity,
            permissions);
    }

    @NonNull
    public static Observable<Boolean> requestWithRationale(final Observable<Void> rationale, final Activity activity, final PermissionGroup permissions) {
        return Observable.defer(new Func0<Observable<Boolean>>() {
            @Override
            public Observable<Boolean> call() {
                return Observable.just(ActivityCompat.shouldShowRequestPermissionRationale(activity, permissions.getValue()));
            }
        })
            .flatMap(new Func1<Boolean, Observable<Boolean>>() {
                @Override
                public Observable<Boolean> call(Boolean shouldShowRequestPermissionRationale) {
                    if (shouldShowRequestPermissionRationale) {
                        return rationale
                            .flatMap(new Func1<Void, Observable<Boolean>>() {
                                @Override
                                public Observable<Boolean> call(Void aVoid) {
                                    return request(activity, permissions);
                                }
                            });
                    }
                    return request(activity, permissions);
                }
            })
            ;
    }

    @NonNull
    private static Observable<Boolean> schedulePermissionsRequest(final Activity activity, final PermissionGroup permissions) {
        final int requestCode = getRequestCode();
        final PublishSubject<Boolean> subj = PublishSubject.create();
        requestMap.put(requestCode, subj);

        return subj.doOnSubscribe(new Action0() {
            @Override
            public void call() {
                ActivityCompat.requestPermissions(activity, new String[] {permissions.getValue()}, requestCode);
            }
        });
    }


    public static boolean onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        final PublishSubject<Boolean> publishSubject = requestMap.get(requestCode);
        if(publishSubject == null) return false;
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            publishSubject.onNext(true);
        } else {
            publishSubject.onNext(false);
        }
        releaseRequestCode(requestCode);
        return true;
    }

    private synchronized static int getRequestCode() {
        return lastRequestCode++;
    }

    private synchronized static void releaseRequestCode(int requestCode) {
        requestMap.remove(requestCode);

        final int maxKeyValue = getMaxKeyValue(requestMap);
        if(maxKeyValue == Integer.MIN_VALUE) {
            lastRequestCode = BASE_REQUEST_CODE;
        } else {
            lastRequestCode = maxKeyValue;
        }
    }

    private synchronized static int getMaxKeyValue(HashMap<Integer, ?> map) {
        Integer max = Integer.MIN_VALUE;
        for (Integer key : map.keySet()) {
            max = key.compareTo(max) > 0 ? key : max;
        }
        return max;
    }

    private static boolean isGranted(final Activity activity, String permission) {
        return ActivityCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED;
    }

    private static FuncN<Boolean> RESULT_CHECKER = new FuncN<Boolean>() {
        @Override
        public Boolean call(Object... results) {
            for (Object result : results) {
                if (!((Boolean) result)) {
                    return false;
                }
            }
            return true;
        }
    };

}
