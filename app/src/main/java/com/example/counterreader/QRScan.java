package com.example.counterreader;


import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.counterreader.Adapters.CountersLeftAdapter;
import com.example.counterreader.Helpers.DatabaseHelper;
import com.google.zxing.Result;

import me.dm7.barcodescanner.zxing.ZXingScannerView;
import android.database.Cursor;

public class QRScan extends AppCompatActivity implements ZXingScannerView.ResultHandler{
    private static final int CAMERA_PERMISSION_REQUEST = 123;
    private final String adminPassword = "1234";

    SharedPreferences sharedPreferences;

    private ZXingScannerView scannerView;

    ImageView flashButton,adminButton,countersLeft;
    Button backToExport;

    Boolean firstTimeDB, updateNeeded;

    DatabaseHelper databaseHelper;
    Cursor cursor;

    String oldVersion = "1";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.qr_scan_activity);

        sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        firstTimeDB = sharedPreferences.getBoolean("firstTimeDB", false);
        updateNeeded = sharedPreferences.getBoolean("updateNeeded", false);
        oldVersion = sharedPreferences.getString("oldVersion", oldVersion);


        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST);
        } else {
            initScannerView();
        }

        flashButton = findViewById(R.id.flashLightButton);
        flashButton.setOnClickListener(v->{
            scannerView.toggleFlash();
        });
        databaseHelper = new DatabaseHelper(this);

        if(!oldVersion.equals(BuildConfig.VERSION_NAME)){
            updateNeeded = true;
            oldVersion = BuildConfig.VERSION_NAME;

            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("updateNeeded", updateNeeded);
            editor.putString("oldVersion", oldVersion);
            editor.apply();

            updateDatabase();
        }else{
            createInitialDatabase();
        }

        backToExport = findViewById(R.id.backToExportButt);
        int counters = databaseHelper.getIndexesHigherThanZero();
        int maxCounters= databaseHelper.getRowCount();
        if (counters == maxCounters){
            backToExport.setVisibility(View.VISIBLE);
            backToExport.setOnClickListener(v -> {
                Intent intent = new Intent(this, PreviewExportData.class);
                startActivity(intent);
            });
        }

        adminButton = findViewById(R.id.adminButton);
        adminButton.setOnClickListener(v -> {
            openAdminDialog();
        });
        countersLeft = findViewById(R.id.countersLeft);
        countersLeft.setOnClickListener(v -> {
            View popupView = getLayoutInflater().inflate(R.layout.item_counters, null);

            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
            alertDialogBuilder.setView(popupView);

            RecyclerView recyclerView = popupView.findViewById(R.id.recyclerViewCounters);
            recyclerView.setLayoutManager(new LinearLayoutManager(this));

            databaseHelper = new DatabaseHelper(this);
            Cursor cursor = databaseHelper.getCountersLeft();

            CountersLeftAdapter itemAdapter = new CountersLeftAdapter(cursor);
            recyclerView.setAdapter(itemAdapter);

            AlertDialog alertDialog = new AlertDialog.Builder(this)
                    .setView(popupView)
                    .setTitle("Contoare de scanat:")
                    .setPositiveButton("OK", null)
                    .create();

            alertDialog.show();
        });
    }

    private void openAdminDialog(){
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.admin_dialog_activity, null);
        dialogBuilder.setView(dialogView);

        final EditText editTextPassword = dialogView.findViewById(R.id.editTextPassword);
        editTextPassword.requestFocus();

        dialogBuilder.setTitle("Introdu parola");
        dialogBuilder.setPositiveButton("Verifica", (dialog, whichButton) -> {
            // Check the entered password here
            String enteredPassword = editTextPassword.getText().toString();

            if (enteredPassword.equals(adminPassword)) {
                startActivity(new Intent(QRScan.this, PreviewExportData.class));
                finish();
            } else {
                Toast.makeText(QRScan.this, R.string.wrongPass, Toast.LENGTH_SHORT).show();
            }
        });

        AlertDialog alertDialog = dialogBuilder.create();
        alertDialog.show();
    }

    private void initScannerView() {
        scannerView = findViewById(R.id.zxscan);
        scannerView.setResultHandler(this);
        scannerView.startCamera();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (scannerView != null) {
            scannerView.setResultHandler(this);
            scannerView.startCamera();
        }
    }
    @Override
    public void onPause() {
        super.onPause();
        if (scannerView != null) {
            scannerView.stopCamera();
        }
    }

    @Override
    public void onBackPressed() {
        startActivity(new Intent(this, MainActivity.class));
    }

    @Override
    public void handleResult(Result result) {
        cursor = databaseHelper.getDataByQR(String.valueOf(result));
        if (cursor != null && cursor.moveToFirst()) {
            double newIndex = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_INDEX_NOU));
            cursor.close();
            if(newIndex == 0){
                Intent intent = new Intent(QRScan.this, CameraActivity.class);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("scannedQRCode", result.toString());
                editor.apply();
                startActivity(intent);
            }else if (newIndex > 0) {
                showConfirmationDialog(getString(R.string.dejaCitit1) + newIndex
                                + "\n" + getString(R.string.dejaCitit2)
                        ,result);
            }
        }else {
            Toast.makeText(this, R.string.qrInvalid, Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, QRScan.class));
        }
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {
        super.onPointerCaptureChanged(hasCapture);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initScannerView();
            } else {
                Toast.makeText(this, R.string.cameraPermissionNeeded, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showConfirmationDialog(String message, Object result) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.contorScannedAlready)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .setNegativeButton("Cancel", null);

        final AlertDialog alertDialog = builder.create();

        alertDialog.setOnShowListener(dialog -> {
            Button positiveButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);

            positiveButton.setOnClickListener(v -> {
                Intent intent = new Intent(QRScan.this, CameraActivity.class);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("scannedQRCode", result.toString());
                editor.apply();
                alertDialog.dismiss();
                startActivity(intent);
            });

            Button negativeButton = alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            negativeButton.setOnClickListener(v -> startActivity(new Intent(this, QRScan.class)));
        });

        alertDialog.show();
    }

    public void updateDatabase(){
        if (updateNeeded) {
            databaseHelper.updateData(1,     "CEC",                      "agentie",          "termic rece",      "72089956",            39.641,      40.757,         "100001");
            databaseHelper.updateData(2,     "CEC",                      "agentie",          "termic cald",      "72083110",            10.101,      10.101,         "100002");
            databaseHelper.updateData(3,     "CEC",                      "2",                "server termic",    "717661501 ET 2",      32.864,      34.403,         "100003");
            databaseHelper.updateData(4,     "CEC",                      "3",                "server termic",    "71706294 ET 3",       21.828,      22.723,         "100004");
            databaseHelper.updateData(5,     "CEC",                      "4",                "server termic",    "71706293 ET 4",       22.594,      23.286,         "100005");
            databaseHelper.updateData(6,     "CEC",                      "ET 4",             "apa",              "46004593",            751.789,     777.772,        "100006");
            databaseHelper.updateData(7,     "CEC",                      "ET 4",             "apa",              "46004614",            217.312,     223.921,        "100007");
            databaseHelper.updateData(8,     "Tabac",                    "parter",           "termic rece",      "71761498",            23.651,      25.153,         "100008");
            databaseHelper.updateData(9,     "Tabac",                    "parter",           "termic cald",      "71761497",            5.334,       5.337,          "100009");
            databaseHelper.updateData(10,    "Ted's",                    "parter",           "termic rece",      "71761500",            36.898,      37.482,         "100010");
            databaseHelper.updateData(11,    "Ted's",                    "parter",           "termic cald",      "71761499",            24.197,      24.198,         "100011");
            databaseHelper.updateData(12,    "Ted's",                    "parter",           "apa",              "",                    515.878,     537.34,         "100012");
            databaseHelper.updateData(13,    "Roche",                    "15",               "server",           "71925161",            61.511,      63573,          "100013");
            databaseHelper.updateData(14,    "Roche",                    "16",               "server",           "71925152",            59.604,      61.612,         "100014");
            databaseHelper.updateData(15,    "BAI SEVICIU",              "25",               "apa",              "",                    76.693,      77,             "100015");
            databaseHelper.updateData(16,    "Parcare",                  "S1",               "electric",         "1019304131",          4430.5,      4501.7,         "100016");
            databaseHelper.updateData(17,    "Parcare",                  "S1",               "electric",         "1019304097",          145736.3,    148601.4,       "100017");
            databaseHelper.updateData(18,    "Parcare",                  "S1",               "electric",         "1020115119",          3731.3,      3731.3,         "100018");
            databaseHelper.updateData(19,    "Parcare pwc",              "",                 "electric",         "121177079",           10621.1,     11141,          "100019");
            databaseHelper.updateData(20,    "Parcare",                  "S1",               "electric",         "1020115112",          105907.3,    114504.00,      "100020");
            databaseHelper.updateData(21,    "Parcare",                  "S2",               "electric",         "1019464163",          83920.6,     89215.5,        "100021");
            databaseHelper.updateData(22,    "Spalatorie ATO",           "S3",               "electric",         "26-33010",            14992.47,    15460.17,       "100022");
            databaseHelper.updateData(23,    "Irigatii parcare 1",       "S1",               "apa",              "8ZRI1914765533",      9517,        9871,           "100023");
            databaseHelper.updateData(24,    "Irigatii parcare 2",       "S1",               "apa",              "8ZRI1915032202",      1837,        1942,           "100024");
            databaseHelper.updateData(25,    "Irigatii curte lumina",    "S2",               "apa",              "8ZRI1915032208",      48,          53,             "100025");
            databaseHelper.updateData(26,    "Vodafone",                 "S1",               "electric",         "190100000662/P34S02", 32193,       33577,          "100026");
            databaseHelper.updateData(27,    "Orange",                   "S3",               "electric",         "0120/SGS0217",        30933.74,    31745.4,        "100027");
            databaseHelper.updateData(28,    "MSD server",               "5",                "termic rece",      "71706295",            41.878,      43.24,          "100028");
            databaseHelper.updateData(29,    "Contor parter baie",       "parter",           "apa",              "50007705",            186.395,     191.065,        "100029");
            databaseHelper.updateData(30,    "AV8 Restaurant",           "Et 25",            "gaz",              "5551009/2020",        2900.251,    2969.776,       "100030");
            databaseHelper.updateData(31,    "Roche",                    "parter",           "apa",              "39800961",            17.521,      18.282,         "100031");
            databaseHelper.updateData(32,    "Roche",                    "parter",           "termic rece",      "71925154",            2.794,       2.93,           "100032");
            databaseHelper.updateData(33,    "Roche",                    "parter",           "termic rece",      "71925157",            36.85,       37.943,         "100033");
            databaseHelper.updateData(34,    "Roche",                    "parter",           "termic rece",      "71925158",            6.44,        6.762,          "100034");
            databaseHelper.updateData(35,    "Roche",                    "parter",           "termic cald",      "71925153",            48.679,      48.7,           "100035");
            databaseHelper.updateData(36,    "Roche",                    "parter",           "termic cald",      "71925160",            48.125,      48.127,         "100036");
            databaseHelper.updateData(37,    "Roche",                    "parter",           "termic cald",      "71925159",            25.418,      25.418,         "100037");
            databaseHelper.updateData(38,    "P29 ato",                  "parter",           "apa",              "49008663",            20.421,      21.236,         "100038");
            databaseHelper.updateData(39,    "P29 ato",                  "parter",           "termic cald",      "71925156",            2.551,       2.551,          "100039");
            databaseHelper.updateData(40,    "P29 ato",                  "parter",           "termic rece",      "71925155",            6.842,       7.253,          "100040");
            databaseHelper.updateData(41,    "vestiare mentenanta",      "",                 "apa",              "EZRI0250007721",      242.917,     264.278,        "100041");
            databaseHelper.updateData(42,    "vestiar PWC",              "S2",               "electric",         "",                    3311.37,     154.2,          "100042");
            databaseHelper.updateData(43,    "Contor -2 MSD",            "S2",               "electric",         "",                    0.01,        0.01,           "100043");
            databaseHelper.updateData(44,    "Contor -3 Roche",          "S3",               "electric",         "521236105",           1741.4,      2046.5,         "100044");
            databaseHelper.updateData(45,    "Contor 26 MSD",            "26",               "electric",         "DDS6788",             789.6,       789.6,          "100045");
            databaseHelper.updateData(46,    "Contor E-infra",           "S2",               "electric",         "EN50470-3",           1383.82,     1496.04,        "100046");
            databaseHelper.updateData(47,    "Contor general de gaz",    "mecanic [m3]",     "gaz",              "3403401178/2017",     380933.05,   381004.75,      "100047");
            databaseHelper.updateData(48,    "Contor general de gaz",    "electronic [m3]",  "gaz",              "Corus / Itron",       380984.00,   381056.00,      "100048");
            databaseHelper.updateData(49,    "Contor general de gaz",    "convertit [Nm3]",  "gaz",              "Corus / Itron",       455796.3,    455876.954,     "100049");


            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("updateNeeded", false);
            editor.apply();
        }
    }

    public void createInitialDatabase() {
        if (!firstTimeDB) {
            databaseHelper.insertData("CEC",                      "agentie",          "termic rece",      "72089956",             39.641,           "100001");
            databaseHelper.insertData("CEC",                      "agentie",          "termic cald",      "72083110",             10.101,           "100002");
            databaseHelper.insertData("CEC",                      "2",                "server termic",    "717661501 ET 2",       32.864,           "100003");
            databaseHelper.insertData("CEC",                      "3",                "server termic",    "71706294 ET 3",        21.828,           "100004");
            databaseHelper.insertData("CEC",                      "4",                "server termic",    "71706293 ET 4",        22.594,           "100005");
            databaseHelper.insertData("CEC",                      "ET 4",             "apa",              "46004593",             751.789,          "100006");
            databaseHelper.insertData("CEC",                      "ET 4",             "apa",              "46004614",             217.312,          "100007");
            databaseHelper.insertData("Tabac",                    "parter",           "termic rece",      "71761498",             23.651,           "100008");
            databaseHelper.insertData("Tabac",                    "parter",           "termic cald",      "71761497",             5.334,            "100009");
            databaseHelper.insertData("Ted's",                    "parter",           "termic rece",      "71761500",             36.898,           "100010");
            databaseHelper.insertData("Ted's",                    "parter",           "termic cald",      "71761499",             24.197,           "100011");
            databaseHelper.insertData("Ted's",                    "parter",           "apa",              "",                     515.878,          "100012");
            databaseHelper.insertData("Roche",                    "15",               "server",           "71925161",             61.511,           "100013");
            databaseHelper.insertData("Roche",                    "16",               "server",           "71925152",             59.604,           "100014");
            databaseHelper.insertData("BAI SEVICIU",              "25",               "apa",              "",                     76.693,           "100015");
            databaseHelper.insertData("Parcare",                  "S1",               "electric",         "1019304131",           4430.5,           "100016");
            databaseHelper.insertData("Parcare",                  "S1",               "electric",         "1019304097",           145736.3,         "100017");
            databaseHelper.insertData("Parcare",                  "S1",               "electric",         "1020115119",           3731.3,           "100018");
            databaseHelper.insertData("Parcare pwc",              "",                 "electric",         "121177079",            10621.1,          "100019");
            databaseHelper.insertData("Parcare",                  "S1",               "electric",         "1020115112",           105907.3,         "100020");
            databaseHelper.insertData("Parcare",                  "S2",               "electric",         "1019464163",           83920.6,          "100021");
            databaseHelper.insertData("Spalatorie ATO",           "S3",               "electric",         "26-33010",             14992.47,         "100022");
            databaseHelper.insertData("Irigatii parcare 1",       "S1",               "apa",              "8ZRI1914765533",       9517,             "100023");
            databaseHelper.insertData("Irigatii parcare 2",       "S1",               "apa",              "8ZRI1915032202",       1837,             "100024");
            databaseHelper.insertData("Irigatii curte lumina",    "S2",               "apa",              "8ZRI1915032208",       48,               "100025");
            databaseHelper.insertData("Vodafone",                 "S1",               "electric",         "190100000662/P34S02",  32193,            "100026");
            databaseHelper.insertData("Orange",                   "S3",               "electric",         "0120/SGS0217",         30933.74,         "100027");
            databaseHelper.insertData("MSD server",               "5",                "termic rece",      "71706295",             41.878,           "100028");
            databaseHelper.insertData("Contor parter baie",       "parter",           "apa",              "50007705",             186.395,          "100029");
            databaseHelper.insertData("AV8 Restaurant",           "Et 25",            "gaz",              "5551009/2020",         2900.251,         "100030");
            databaseHelper.insertData("Roche",                    "parter",           "apa",              "39800961",             17.521,           "100031");
            databaseHelper.insertData("Roche",                    "parter",           "termic rece",      "71925154",             2.794,            "100032");
            databaseHelper.insertData("Roche",                    "parter",           "termic rece",      "71925157",             36.85,            "100033");
            databaseHelper.insertData("Roche",                    "parter",           "termic rece",      "71925158",             6.44,             "100034");
            databaseHelper.insertData("Roche",                    "parter",           "termic cald",      "71925153",             48.679,           "100035");
            databaseHelper.insertData("Roche",                    "parter",           "termic cald",      "71925160",             48.125,           "100036");
            databaseHelper.insertData("Roche",                    "parter",           "termic cald",      "71925159",             25.418,           "100037");
            databaseHelper.insertData("P29 ato",                  "parter",           "apa",              "49008663",             20.421,           "100038");
            databaseHelper.insertData("P29 ato",                  "parter",           "termic cald",      "71925156",             2.551,            "100039");
            databaseHelper.insertData("P29 ato",                  "parter",           "termic rece",      "71925155",             6.842,            "100040");
            databaseHelper.insertData("vestiare mentenanta",      "",                 "apa",              "EZRI0250007721",       242.917,          "100041");
            databaseHelper.insertData("vestiar PWC",              "S2",               "electric",         "",                     3311.37,          "100042");
            databaseHelper.insertData("Contor -2 MSD",            "S2",               "electric",         "",                     0.01,             "100043");
            databaseHelper.insertData("Contor -3 Roche",          "S3",               "electric",         "521236105",            1741.4,           "100044");
            databaseHelper.insertData("Contor 26 MSD",            "26",               "electric",         "DDS6788",              789.6,            "100045");
            databaseHelper.insertData("Contor E-infra",           "S2",               "electric",         "EN50470-3",            1383.82,          "100046");
            databaseHelper.insertData("Contor general de gaz",    "mecanic [m3]",     "gaz",              "3403401178/2017",      380933.05,        "100047");
            databaseHelper.insertData("Contor general de gaz",    "electronic [m3]",  "gaz",              "Corus / Itron",        380984.00,        "100048");
            databaseHelper.insertData("Contor general de gaz",    "convertit [Nm3]",  "gaz",              "Corus / Itron",        455796.3,         "100049");

            cursor = databaseHelper.getAllData();
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    ContentValues values = new ContentValues();
                    values.put(DatabaseHelper.COLUMN_INDEX_NOU, 0);
                    values.put(DatabaseHelper.COLUMN_IMAGE_URI," ");

                    String qrCode = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_COD_QR));
                    String whereClause = DatabaseHelper.COLUMN_COD_QR + "=?";
                    String[] whereArgs = {qrCode};
                    databaseHelper.getWritableDatabase().update(DatabaseHelper.TABLE_NAME, values, whereClause, whereArgs);

                } while (cursor.moveToNext());

                cursor.close();
            }

            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("firstTimeDB", true);
            editor.apply();
        }
    }


}