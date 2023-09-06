package com.example.counterreader;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Button;
import android.widget.Toast;

import com.example.counterreader.Adapters.SquareAdapter;
import com.example.counterreader.Helpers.FileShareHelper;
import com.example.counterreader.Models.SquareItem;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private List<SquareItem> squareItems;
    private SquareAdapter squareAdapter;

    private final String directoryPathOfFiles = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) + "/CounterReader";

    private long backPressedTime = 0;
    private static final int TIME_INTERVAL = 2000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        RecyclerView recyclerView = findViewById(R.id.mainRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        squareItems = new ArrayList<>();
        squareAdapter = new SquareAdapter(squareItems);
        recyclerView.setAdapter(squareAdapter);

        loadFilesFromFolder();

        Button scanQRCodes = findViewById(R.id.scanQRCodes);
        scanQRCodes.setOnClickListener(v -> {
            startActivity(new Intent(this, QRScan.class));
        });
    }


    @Override
    public void onBackPressed() {
        if (System.currentTimeMillis() - backPressedTime < TIME_INTERVAL) {
            super.onBackPressed();
        } else {
            backPressedTime = System.currentTimeMillis();
            Toast.makeText(this, "Apasati din nou pentru a iesi", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        backPressedTime = 0;
        super.onDestroy();
    }

    @SuppressLint("NotifyDataSetChanged")
    private void loadFilesFromFolder() {
        File folder = new File(directoryPathOfFiles);

        if (folder.exists() && folder.isDirectory()) {
            File[] files = folder.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.toLowerCase().endsWith(".pdf") || name.toLowerCase().endsWith(".xlsx");
                }
            });

            if (files != null) {
                for (int i = 0; i + 1 < files.length; i += 2) {
                    File file1 = files[i];
                    File file2 = files[i + 1];

                    if (file1.isFile() && file2.isFile()) {
                        String fileName1 = file1.getName();
                        String fileName2 = file2.getName();
                        String[] parts = fileName1.split("_");
                        String title = " ";

                        if (parts[parts.length - 1].contains("xlsx")) {
                            if (parts.length >= 2) {
                                title = parts[1] + " " + parts[2].replace(".xlsx", "");
                            }
                        } else {
                            title = parts[1] + " " + parts[2].replace(".pdf", "");
                        }

                        SquareItem squareItem = new SquareItem(title, fileName1, fileName2);
                        squareItems.add(squareItem);

                    }
                }
                squareAdapter.notifyDataSetChanged();
            }
        }
    }
}