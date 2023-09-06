package com.example.counterreader.Helpers;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;

import androidx.core.content.FileProvider;

import com.example.counterreader.BuildConfig;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FileShareHelper {
    private final Context context;
    private final String directoryPathOfFiles;
    private final String excelFileName;
    private final String pdfFileName;
    Uri excelFileUri, pdfFileUri;

    public FileShareHelper(Context context, String directoryPathOfFiles, String excelFileName, String pdfFileName) {
        this.context = context;
        this.directoryPathOfFiles = directoryPathOfFiles;
        this.excelFileName = excelFileName;
        this.pdfFileName = pdfFileName;
    }

    public void shareFiles() {
        File excelFile = new File(directoryPathOfFiles, excelFileName);
        File pdfFile = new File(directoryPathOfFiles, pdfFileName);

        excelFileUri = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".provider", excelFile);
        pdfFileUri = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".provider", pdfFile);

        ArrayList<Uri> fileUris = new ArrayList<>();
        fileUris.add(excelFileUri);
        fileUris.add(pdfFileUri);

        Intent intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_SUBJECT, "Raport contoare");
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"cosmin.geanta@anatower.ro"});
        intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, fileUris);

        Intent chooser = Intent.createChooser(intent, "Share File");

        List<ResolveInfo> resInfoList = context.getPackageManager().queryIntentActivities(chooser, PackageManager.MATCH_DEFAULT_ONLY);
        for (ResolveInfo resolveInfo : resInfoList) {
            String packageName = resolveInfo.activityInfo.packageName;
            context.grantUriPermission(packageName, excelFileUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            context.grantUriPermission(packageName, pdfFileUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
        context.startActivity(chooser);
    }
}
