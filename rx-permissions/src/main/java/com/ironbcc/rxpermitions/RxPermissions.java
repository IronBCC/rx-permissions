package com.ironbcc.rxpermitions;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Action0;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.functions.FuncN;
import rx.subjects.PublishSubject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class RxPermissions {
    private static int lastRequestCode = 1010;
    private static final HashMap<Integer, PublishSubject<Boolean>> requestMap = new HashMap<>();

    @NonNull
    public static Observable<Boolean> observe(final Activity activity, String... permissions) {
        List<Observable<Boolean>> observables = new ArrayList<>(permissions.length);
        for (String permission : permissions) {
            PublishSubject<Boolean> subj = PublishSubject.create();
            observables.add(subj.startWith(isGranted(activity, permission)));
        }
        return Observable.combineLatest(observables, RESULT_CHECKER);
    }

    @NonNull
    public static Observable<Boolean> request(final Activity activity, final String... permissions) {
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
    public static Observable<Boolean> requestWithRationale(final Dialog rationaleDialog, final Activity activity, final String... permissions) {
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
    public static Observable<Boolean> requestWithRationale(final Observable<Void> rationale, final Activity activity, final String... permissions) {
        return Observable.from(permissions)
            .map(new Func1<String, Boolean>() {
                @Override
                public Boolean call(String permission) {
                    return ActivityCompat.shouldShowRequestPermissionRationale(activity, permission);
                }
            })
            .reduce(new Func2<Boolean, Boolean, Boolean>() {
                @Override
                public Boolean call(Boolean value, Boolean accumulatedValue) {
                    accumulatedValue = accumulatedValue ? value : false;
                    return accumulatedValue;
                }
            })
            .flatMap(new Func1<Boolean, Observable<Boolean>>() {
                @Override
                public Observable<Boolean> call(Boolean shouldShowRequestPermissionRationale) {
                    if(shouldShowRequestPermissionRationale) {
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
    private static Observable<Boolean> schedulePermissionsRequest(final Activity activity, final String[] permissions) {
        final int requestCode = getRequestCode();
        final PublishSubject<Boolean> subj = PublishSubject.create();
        requestMap.put(requestCode, subj);

        return subj.doOnSubscribe(new Action0() {
            @Override
            public void call() {
                ActivityCompat.requestPermissions(activity, permissions, requestCode);
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
        requestMap.remove(requestCode);
        return true;
    }

    private static int getRequestCode() {
        return lastRequestCode++;
    }

    private static boolean isGranted(final Activity activity, String permission) {
        return Build.VERSION.SDK_INT < 23 /*M*/ || isGranted60(activity, permission);
    }

    @TargetApi(Build.VERSION_CODES.M) private static boolean isGranted60(final Activity activity, String permission) {
        return activity.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
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
