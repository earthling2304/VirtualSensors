package bach.jianxu.watchsense;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

public class SensorFragment extends Fragment implements
        SensorEventListener,
        DataApi.DataListener,
        MessageApi.MessageListener,
        GoogleApiClient.ConnectionCallbacks {

    private static final float SHAKE_THRESHOLD = 1.1f;
    private static final int SHAKE_WAIT_TIME_MS = 250;
    private static final float ROTATION_THRESHOLD = 2.0f;
    private static final int ROTATION_WAIT_TIME_MS = 100;

    private GoogleApiClient mGoogleApiClient;
    private static final String TAG = "WATCH";
    private static final String WEAR_MESSAGE_PATH = "/message";

    private View mView;
    private TextView mAccelero;
    private TextView mGyroscope;
    private SensorManager mSensorManager;
    private Sensor mSensor;
    private int mSensorType;
    private long mShakeTime = 0;
    private long mRotationTime = 0;

    private static Activity mAct;
    private long cnt = 1;
    public static SensorFragment newInstance(int sensorType, Activity ap) {
        SensorFragment f = new SensorFragment();

        // Supply sensorType as an argument
        Bundle args = new Bundle();
        args.putInt("sensorType", sensorType);
        f.setArguments(args);
        mAct = ap;
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mGoogleApiClient = new GoogleApiClient.Builder(mAct)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .build();
        mGoogleApiClient.connect();

        Bundle args = getArguments();
        if(args != null) {
            mSensorType = args.getInt("sensorType");
        }

        mSensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(mSensorType);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        mView = inflater.inflate(R.layout.sensor, container, false);

        mAccelero = (TextView) mView.findViewById(R.id.txt_accelero);
        //mTextTitle.setText(mSensor.getStringType());
        mGyroscope = (TextView) mView.findViewById(R.id.txt_gyroscope);

        return mView;
    }

    @Override
    public void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float gX = event.values[0] / SensorManager.GRAVITY_EARTH;
        float gY = event.values[1] / SensorManager.GRAVITY_EARTH;
        float gZ = event.values[2] / SensorManager.GRAVITY_EARTH;

        //Log.i(TAG, "Getting data: x:" + gX + ", y:" + gY + ", z:" + gZ);

        // If sensor is unreliable, then just return
//        if (event.accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE)
//        {
//            Log.w(TAG, "Skipping because of the accuracy");
//            return;
//        }
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            // assign directions
            float ax = event.values[0];
            float ay = event.values[1];
            float az = event.values[2];
            float af = (float) Math.sqrt(Math.pow(ax, 2)+ Math.pow(ay, 2)+ Math.pow(az, 2));
            mAccelero.setText("\nAccelerometer :"+"\n"+
                    "\u00E2x: "+ String.valueOf(ax)+"\n"+
                    "\u00E2y: "+ String.valueOf(ay)+"\n"+
                    "\u00E2z: "+ String.valueOf(az)+"\n"+
                    "\u00E2Net: "+ String.valueOf(af)
            );

        } else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            // assign directions
            float gx = event.values[0];
            float gy = event.values[1];
            float gz = event.values[2];
            float gf = (float) Math.sqrt(Math.pow(gx, 2)+ Math.pow(gy, 2)+ Math.pow(gz, 2));
            mGyroscope.setText("\nGyroscope :"+"\n"+
                    "\u03A9x: "+ String.valueOf(gx/gf)+"\n"+
                    "\u03A9y: "+ String.valueOf(gy/gf)+"\n"+
                    "\u03A9z: "+ String.valueOf(gz/gf)+"\n"
            );
        }

        if (cnt++ % 100 == 0) {
            sendMessage(WEAR_MESSAGE_PATH, mAccelero.getText().toString());
        }

//        if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
//            detectShake(event);
//        }
//        else if(event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
//            detectRotation(event);
//        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }


    // References:
    //  - http://jasonmcreynolds.com/?p=388
    //  - http://code.tutsplus.com/tutorials/using-the-accelerometer-on-android--mobile-22125
    private void detectShake(SensorEvent event) {
        long now = System.currentTimeMillis();

        double gX = event.values[0] / SensorManager.GRAVITY_EARTH;
        double gY = event.values[1] / SensorManager.GRAVITY_EARTH;
        double gZ = event.values[2] / SensorManager.GRAVITY_EARTH;
        Log.i("Sensor", "Getting data: x-" + gX + ",y-" + gY + ",z-" + gZ);

        if((now - mShakeTime) > SHAKE_WAIT_TIME_MS) {
            mShakeTime = now;


            // gForce will be close to 1 when there is no movement
            double gForce = Math.sqrt(gX*gX + gY*gY + gZ*gZ);

            // Change background color if gForce exceeds threshold;
            // otherwise, reset the color
            if(gForce > SHAKE_THRESHOLD) {
                mView.setBackgroundColor(Color.rgb(0, 100, 0));
            }
            else {
                mView.setBackgroundColor(Color.BLACK);
            }
        }
    }

    private void detectRotation(SensorEvent event) {
        long now = System.currentTimeMillis();

        if((now - mRotationTime) > ROTATION_WAIT_TIME_MS) {
            mRotationTime = now;

            // Change background color if rate of rotation around any
            // axis and in any direction exceeds threshold;
            // otherwise, reset the color
            if(Math.abs(event.values[0]) > ROTATION_THRESHOLD ||
               Math.abs(event.values[1]) > ROTATION_THRESHOLD ||
               Math.abs(event.values[2]) > ROTATION_THRESHOLD) {
                mView.setBackgroundColor(Color.rgb(0, 100, 0));
            }
            else {
                mView.setBackgroundColor(Color.BLACK);
            }
        }
    }

    private void sendMessage(final String path, final String text) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();

                for (Node node: nodes.getNodes()) {
                    MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(
                            mGoogleApiClient, node.getId(), path, text.getBytes() ).await();
                }
            }
        }).start();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Wearable.MessageApi.addListener(mGoogleApiClient, this);

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onDataChanged(DataEventBuffer dataEventBuffer) {

    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        Log.i(TAG,"Received message.~~~~~~~~~~~~~~~~~~~~~~~" + new String(messageEvent.getData()));

    }
}
