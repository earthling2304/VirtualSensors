package bach.jianxu.watchsense;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends Activity {

    private static String TAG = "WatchSense";
    public static final int PERMISSIONS_REQUEST_CODE = 1;
    private SensingService mSensingService = new SensingService();
    private Messenger mService;
    private boolean mBound;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final EditText gestureTxt = (EditText) findViewById(R.id.gesture_txt);
        Button recordBtn = (Button) findViewById(R.id.record_btn);
        recordBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String gestureName = gestureTxt.getText() != null ? gestureTxt.getText().toString() : "default";
                if (Shell.isSuAvailable()) {
                    String fname = "/sdcard/temp/recorded_gesture_"
                            + gestureName.trim().replaceAll(" ", "_").toLowerCase() + ".txt";
                    String command = "/data/local/tmp/getevent -t /dev/input/event1 > " + fname;
                    Log.d(TAG, "Command is: " + command);
                    Shell.runCommand(command);
                }
            }
        });

        Button btn = (Button) findViewById(R.id.calibration_btn);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "onClick() happens");
                Message msg = Message.obtain(null, 1, 0, 0);
                try {
                    mService.send(msg);
                } catch (Exception e) {e.printStackTrace();}
            }
        });
        //Intent aint = new Intent(this, SensingService.class);
        bindService(new Intent(this, SensingService.class), mConnection,
                Context.BIND_AUTO_CREATE);
        //startService(aint);
        checkPermissions();
    }

    /**
     * Class for interacting with the main interface of the service.
     */
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the object we can use to
            // interact with the service.  We are communicating with the
            // service using a Messenger, so here we get a client-side
            // representation of that from the raw IBinder object.
            mService = new Messenger(service);
            mBound = true;
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            mService = null;
            mBound = false;
        }
    };

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

    @Override
    protected void onStop() {
        super.onStop();
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
    }
}
