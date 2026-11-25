package com.myco;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.locuslabs.sdk.llpublic.LLOnGetPOIDetailsCallback;
import com.locuslabs.sdk.llpublic.LLPOI;
import com.locuslabs.sdk.llpublic.LLPOIDatabase;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HeadlessModeActivity extends AppCompatActivity {

    // Data APIs can run on a background thread (recommended)
    private final ExecutorService backgroundExecutor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_headless_mode);

        startHeadlessMode();
    }

    @Override
    protected void onDestroy() {

        backgroundExecutor.shutdown();
        super.onDestroy();
    }

    private void startHeadlessMode() {

        // Once LLConfiguration's applicationContext and accountID are set (see MyApplication), the SDK can perform all
        // headless operations without a map: venue data (LLVenueDatabase), POIs and search results (LLPOIDatabase),
        // and directions (LLNavigationDatabase). For example, get the details for a POI:
        LLPOIDatabase llpoiDatabase = new LLPOIDatabase();

        // Run the data API on a background thread, then post UI work back to the main thread
        backgroundExecutor.execute(() -> llpoiDatabase.getPOIDetails("lax", "870", new LLOnGetPOIDetailsCallback() {
            @Override
            public void successCallback(LLPOI llpoi) {

                String message = "Name: " + llpoi.getName() + "\nID: " + llpoi.getId() + "\nDesc: " + llpoi.getDescription();
                runOnUiThread(() -> {
                    AlertDialog.Builder dialog = new AlertDialog.Builder(HeadlessModeActivity.this);
                    dialog.setMessage(message);
                    dialog.setTitle("Got POI details in headless mode");
                    dialog.setPositiveButton("OK", null);
                    dialog.create().show();
                });
            }

            @Override
            public void failureCallback(Throwable throwable) {

                Log.e("LOG", "Failed to get POI details", throwable);
            }
        }));
    }
}
