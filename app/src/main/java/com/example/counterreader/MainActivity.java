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
import java.util.Map;
import java.util.HashMap;

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

        Button scanQRCodes = findViewById(R.id.scanQRCodes);
        scanQRCodes.setOnClickListener(v -> {
            startActivity(new Intent(this, QRScan.class));
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (squareItems != null) {
            squareItems.clear();
        }
        loadFilesFromFolder();
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

        if (!folder.exists() || !folder.isDirectory()) {
            squareAdapter.notifyDataSetChanged();
            return;
        }

        File[] files = folder.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                String lower = name.toLowerCase();
                return lower.endsWith(".pdf") || lower.endsWith(".xlsx");
            }
        });

        if (files == null || files.length == 0) {
            squareAdapter.notifyDataSetChanged();
            return;
        }

        class Session {
            String pdf;
            String xlsx;
            long lastModified;
        }

        Map<String, Session> sessions = new HashMap<>();

        for (File f : files) {
            if (!f.isFile()) continue;
            String name = f.getName();
            int dot = name.lastIndexOf('.');
            String base = dot > 0 ? name.substring(0, dot) : name; // CounterData_Month_Year
            Session s = sessions.get(base);
            if (s == null) {
                s = new Session();
            }
            if (name.toLowerCase().endsWith(".pdf")) {
                s.pdf = name;
            } else if (name.toLowerCase().endsWith(".xlsx")) {
                s.xlsx = name;
            }
            s.lastModified = Math.max(s.lastModified, f.lastModified());
            sessions.put(base, s);
        }

        List<Map.Entry<String, Session>> entries = new ArrayList<>(sessions.entrySet());
        entries.sort((e1, e2) -> Long.compare(e2.getValue().lastModified, e1.getValue().lastModified));

        for (Map.Entry<String, Session> entry : entries) {
            String base = entry.getKey();
            Session s = entry.getValue();
            if (s.pdf == null || s.xlsx == null) {
                // skip incomplete pairs
                continue;
            }
            String[] parts = base.split("_");
            String title = base;
            if (parts.length >= 3) {
                title = parts[1] + " " + parts[2];
            }
            // Keep consistent: excel first, pdf second
            SquareItem squareItem = new SquareItem(title, s.xlsx, s.pdf);
            squareItems.add(squareItem);
        }

        squareAdapter.notifyDataSetChanged();
    }
}
