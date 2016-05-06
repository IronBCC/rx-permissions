package com.ironbcc.rxpermitions;

import android.Manifest;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import rx.functions.Action1;

public class RxPermissionDemoActivity
    extends AppCompatActivity {

    private static final int MY_PERMISSIONS_REQUEST_READ_CONTACTS = 1234;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rx_permission_demo);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        assert fab != null;
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onFabClick();
            }
        });
    }

    private void onFabClick() {
        showContactList();
    }

    private void showContactList() {
        final AlertDialog dialog = new AlertDialog.Builder(this)
            .setView(R.layout.dialog_contact_permition_explanation)
            .create();

        RxPermissions.requestWithRationale(
            dialog,
            this,
            Manifest.permission.READ_CONTACTS
        )
            .doOnNext(new Action1<Boolean>() {
                @Override
                public void call(Boolean granted) {
                    if (granted) {

                        ContentResolver cr = getContentResolver();
                        Cursor cur = cr.query(ContactsContract.Contacts.CONTENT_URI,
                                              null, null, null, null
                        );
                        new AlertDialog.Builder(RxPermissionDemoActivity.this)
                            .setAdapter(
                                new SimpleCursorAdapter(
                                    RxPermissionDemoActivity.this,
                                    android.R.layout.simple_list_item_1,
                                    cur,
                                    new String[]{ContactsContract.Contacts.DISPLAY_NAME}, new int[]{android.R.id.text1}, 0
                                ),
                                null
                            )
                            .show();
                    } else {
                        Snackbar.make(findViewById(R.id.fab), "Permission not granted! You son of the bitch!!!", Snackbar.LENGTH_LONG)
                            .setAction("Grant", new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    showContactList();
                                }
                            }).show();
                    }
                }
            })
            .subscribe();
    }

    private boolean tryToRequestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_CONTACTS)) {

                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                final AlertDialog dialog = new AlertDialog.Builder(this)
                    .setView(R.layout.dialog_contact_permition_explanation)
                    .create();
                dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        requestPermissions();
                    }
                });
                dialog.show();
            } else {
                // No explanation needed, we can request the permission.
                requestPermissions();

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
            return false;
        }
        return true;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            new String[]{Manifest.permission.READ_CONTACTS}, MY_PERMISSIONS_REQUEST_READ_CONTACTS
        );
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        RxPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_rx_permission_demo, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
