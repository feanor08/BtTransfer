package com.adarsh.bttrasfer;

import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    //Create Objects-------------------------------------------------------
    private Button buttonUp, send;
    private View rootView;
    private TextView textFolder;
    private TextView tvFileName;
    private ImageView ivThumb;
    private LinearLayout fileInfoLayout;
    static final int CUSTOM_DIALOG_ID = 0;
    private ListView dialog_ListView;
    private File root, fileroot, curFolder;
    private List<String> fileList = new ArrayList<String>();
    private List<String> fileNameList = new ArrayList<String>();
    private static final int DISCOVER_DURATION = 300;
    private static final int REQUEST_BLU = 1;
    private BluetoothAdapter btAdatper = BluetoothAdapter.getDefaultAdapter();

    private String selectedFilePath = "";

    //---------------------------------------------------------------
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        rootView = findViewById(R.id.root_view);

        tvFileName = (TextView) findViewById(R.id.tvFileName);
        ivThumb = (ImageView) findViewById(R.id.ivThumbNail);
        fileInfoLayout = (LinearLayout) findViewById(R.id.fileInfoLayout);

        send = (Button) findViewById(R.id.sendBtooth);

        root = new File(Environment.getExternalStorageDirectory().getAbsolutePath());
        curFolder = root;

        fileInfoLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialog(CUSTOM_DIALOG_ID);
            }
        });

        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!selectedFilePath.isEmpty())
                    sendViaBluetooth();
                else
                    showSnackBar("Please choose file to share.");
            }
        });
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        Dialog dialog = null;
        switch (id) {
            case CUSTOM_DIALOG_ID:
                dialog = new Dialog(MainActivity.this);
                dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
                dialog.setContentView(R.layout.dailoglayout);
                dialog.setTitle("File Selector");
                dialog.setCancelable(true);
                dialog.setCanceledOnTouchOutside(true);
                textFolder = (TextView) dialog.findViewById(R.id.folder);
                buttonUp = (Button) dialog.findViewById(R.id.up);
                buttonUp.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ListDir(curFolder.getParentFile());
                    }
                });
                dialog_ListView = (ListView) dialog.findViewById(R.id.dialoglist);
                dialog_ListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        File selected = new File(fileList.get(position));
                        if (selected.isDirectory()) {
                            ListDir(selected);
                        } else if (selected.isFile()) {
                            getselectedFile(selected);
                        } else {
                            dismissDialog(CUSTOM_DIALOG_ID);
                        }
                    }
                });
                break;
        }
        return dialog;
    }

    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        super.onPrepareDialog(id, dialog);
        switch (id) {
            case CUSTOM_DIALOG_ID:
                ListDir(curFolder);
                break;
        }
    }

    public void getselectedFile(File f) {
        selectedFilePath = f.getAbsolutePath();
        fileList.clear();
        fileNameList.clear();
        dismissDialog(CUSTOM_DIALOG_ID);

        Bitmap thumbImage = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(selectedFilePath), 128, 128);
        if (thumbImage != null) {
            ivThumb.setImageBitmap(thumbImage);
        } else {
            ivThumb.setImageResource(R.drawable.placeholder);
        }

        tvFileName.setText(selectedFilePath);
    }

    public void ListDir(File f) {
        if (f.equals(root)) {
            buttonUp.setEnabled(false);
        } else {
            buttonUp.setEnabled(true);
        }
        curFolder = f;
        textFolder.setText(f.getAbsolutePath());
//        dataPath.setText(f.getAbsolutePath());
        File[] files = f.listFiles();
        fileList.clear();
        fileNameList.clear();

        for (File file : files) {
            fileList.add(file.getPath());
            fileNameList.add(file.getName());
        }
        ArrayAdapter<String> directoryList = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, fileNameList);
        dialog_ListView.setAdapter(directoryList);
    }

    //exit to application---------------------------------------------------------------------------
    public void exit(View V) {
        btAdatper.disable();
//        Toast.makeText(this,"Now Bluetooth is off... Thanks. ***", Toast.LENGTH_LONG).show();
        finish();
    }

    //Method for send file via bluetooth------------------------------------------------------------
    public void sendViaBluetooth() {
        /*if (dataPath.getText().toString().trim().isEmpty() || dataPath.getText().toString().contains("Choose file")) {
            //            Toast.makeText(this, "Please select a file.", Toast.LENGTH_LONG).show();
            showSnackBar("Please select a file.");
        } else {*/
        if (btAdatper == null) {
//                Toast.makeText(this, "Device not support bluetooth", Toast.LENGTH_LONG).show();
            showSnackBar("Device not support bluetooth");
        } else {
            enableBluetooth();
        }
//        }
    }

    public void enableBluetooth() {
        Intent discoveryIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoveryIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, DISCOVER_DURATION);
        startActivityForResult(discoveryIntent, REQUEST_BLU);
    }

    //Override method for sending data via bluetooth availability--------------------------
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == DISCOVER_DURATION && requestCode == REQUEST_BLU) {
            Intent i = new Intent();
            i.setAction(Intent.ACTION_SEND);
            i.setType("*/*");
            File file = new File(selectedFilePath);

            i.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));

            PackageManager pm = getPackageManager();
            List<ResolveInfo> list = pm.queryIntentActivities(i, 0);
            if (list.size() > 0) {
                String packageName = null;
                String className = null;
                boolean found = false;

                for (ResolveInfo info : list) {
                    packageName = info.activityInfo.packageName;
                    if (packageName.equals("com.android.bluetooth")) {
                        className = info.activityInfo.name;
                        found = true;
                        break;
                    }
                }
                //CHECK BLUETOOTH available or not------------------------------------------------
                if (!found) {
//                    Toast.makeText(this, "Bluetooth han't been found", Toast.LENGTH_LONG).show();
                    showSnackBar("Bluetooth han't been found");
                } else {
                    i.setClassName(packageName, className);
                    startActivity(i);
                }
            }
        } else {
//            Toast.makeText(this, "Bluetooth is cancelled", Toast.LENGTH_LONG).show();
            showSnackBar("Bluetooth is cancelled");
        }
    }

    private void showSnackBar(String message) {
        Snackbar snackbar = Snackbar.make(rootView, message, Snackbar.LENGTH_SHORT);
        snackbar.show();
    }

}
