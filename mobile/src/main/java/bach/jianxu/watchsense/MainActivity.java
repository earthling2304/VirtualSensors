package bach.jianxu.watchsense;

import android.Manifest;
import android.content.Intent;

import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {

    private static String TAG = "WatchSense";
    public static final int PERMISSIONS_REQUEST_CODE = 1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent aint = new Intent(this, SensingService.class);
        startService(aint);
        checkPermissions();
    }

    private void checkPermissions() {
        boolean writeExternalStoragePermissionGranted =
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                        == PackageManager.PERMISSION_GRANTED;

        if (!writeExternalStoragePermissionGranted) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    PERMISSIONS_REQUEST_CODE);
        }

    }

}
