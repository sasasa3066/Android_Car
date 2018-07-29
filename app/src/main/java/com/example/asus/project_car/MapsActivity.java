package com.example.asus.project_car;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.UUID;


import static com.example.asus.project_car.BTActivity.isLocationEnable;


public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static GoogleMap mMap;// Google API用戶端物件
    private static GoogleApiClient googleApiClient;
    private static Location location;
    private static double latitude=0;//latitude=23.0020353;
    private static double longitude=0;//longitude=120.264362

    //Bluetooth
    private static BluetoothAdapter bluetoothAdapter;
    private static BluetoothDevice device;
    private static BluetoothSocket socket;
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // 一定要是這組
    private static OutputStream outputStream = null;
    private static InputStream inputStream = null;
    private final int REQUEST_ENABLE_BT=1;
    private static final int REQUEST_CODE_ACCESS_COARSE_LOCATION = 1;
    private static AlertDialog.Builder dialog;
    private static byte[] readBuffer;
    private static int readBufferPosition;
    private static Thread workerThread;
    private static Thread workerThreadForLocation;
    private final static int MESSAGE_READ = 2;
    private static int distance_byte=0;
    private static Handler handler;
    private static Handler handlerForLocation=new Handler();

    private Button btn_send;
    private Button btn_btStart;
    private TextView tex_distance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        dialog=new AlertDialog.Builder(MapsActivity.this);
        buildGoogleApiClient();//--------------------20180729---------------
        tex_distance=(TextView)findViewById(R.id.tex_distance);
        handler=new Handler(){
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if(msg.what == MESSAGE_READ){
                    String readMessage = null;
                    distance_byte =0;
                    try {
                        readMessage = new String((byte[]) msg.obj, "ASCII");
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    //textView.setText(readMessage+i++);
                    distance_byte = readBuffer [0] & 0xFF;//直接使用別人的有時間再研究......................................
                    tex_distance.setText("前方超音波距離:"+distance_byte);//這邊一定要有String不能單純只有數字
                }
            }
        };
        btn_send=(Button)findViewById(R.id.btn_BT);
        btn_send.setOnClickListener(send);
        btn_btStart=(Button)findViewById(R.id.btn_btStart);
        btn_btStart.setOnClickListener(btStart);

    }



    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in Sydney and move the camera
        LatLng sydney = new LatLng(latitude, longitude);//LatLng(latitude ,longitude);
        Log.e("Location address is :","---------------------------------------------------Latitude:"+latitude+",Longitude:"+longitude);
        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
    }

    View.OnClickListener send=new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            try{
                // 送出訊息
                String message ="5";
                outputStream.write(message.getBytes());

            }catch(IOException e){

            }
        }
    };
    void connectBT(){//有一個嚴重的BUG還沒開始接收前，arduino就開始傳資料APP會停止回應
        bluetoothAdapter=BluetoothAdapter.getDefaultAdapter();
        if(bluetoothAdapter==null){
            dialog.setMessage("Your device doesn't support Bluetooth");
            dialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    finish();
                }
            });
            dialog.show();
            return;
        }
        if(!bluetoothAdapter.isEnabled()){
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, REQUEST_ENABLE_BT);
        }
        //-------------------------------------------------------------------------------------------copy past follw-------------------------------------------------------------------------------------------------
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {//如果 API level 是大于等于 23(Android 6.0) 时
            //判断是否具有权限
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                //判断是否需要向用户解释为什么需要申请该权限
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_COARSE_LOCATION)) {
                    dialog.setMessage("自Android 6.0开始需要打开位置权限才可以搜索到Ble设备");
                    dialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {

                        }
                    });
                    dialog.show();
                }
                //请求权限
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        REQUEST_CODE_ACCESS_COARSE_LOCATION);
            }
        }
        boolean a=isLocationEnable(MapsActivity.this);
        String str="LocationEnable is "+String.valueOf(a);
        Toast.makeText(MapsActivity.this,str,Toast.LENGTH_SHORT).show();
        //-------------------------------------------------------------------------------------------copy past above------------------------------------------

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        final BroadcastReceiver broadcastReceiver=new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(BluetoothDevice.ACTION_FOUND.equals(intent.getAction())){
                    device=intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    dialog.setMessage(device.getAddress()+"  "+device.getName());
                    dialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {

                        }
                    });
                    dialog.show();
                    try{
                        bluetoothAdapter.cancelDiscovery();
                        socket=device.createInsecureRfcommSocketToServiceRecord(MY_UUID);
                        socket.connect();//連接上去
                        outputStream = socket.getOutputStream();
                        inputStream=socket.getInputStream();
                        readBufferPosition=0;
                        workerThread =new Thread(new Runnable() {
                            @Override
                            public void run() {
                                while(true){
                                    try{
                                        readBufferPosition=inputStream.available();
                                        if(readBufferPosition!=0){
                                            readBuffer = new byte[1024];
                                            SystemClock.sleep(100); //pause and wait for rest of data. Adjust this depending on your sending speed.
                                            readBufferPosition = inputStream.available(); // how many bytes are ready to be read?
                                            readBufferPosition = inputStream.read(readBuffer, 0, readBufferPosition); // record how many bytes we actually read
                                            handler.obtainMessage(MESSAGE_READ, readBufferPosition, -1, readBuffer)
                                                    .sendToTarget(); // Send the obtained bytes to the UI activity
                                        }

                                    }catch(IOException e){
                                        Log.e("while break ",":"+e);
                                        break;
                                    }
                                }
                            }
                        });
                        workerThread.start();
                    }catch(IOException e){
                        Log.e("Connect is error",":"+e);
                    }
                }
            }
        };
        registerReceiver(broadcastReceiver, filter);
        bluetoothAdapter.startDiscovery(); //開始搜尋裝置
    }
    View.OnClickListener btStart=new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            connectBT();
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        googleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(googleApiClient.isConnected()){
            googleApiClient.disconnect();
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        // 當 GoogleApiClient 連上 Google Play Service 後要執行的動作
        location= LocationServices.FusedLocationApi.getLastLocation(googleApiClient);// 這行指令在 IDE 會出現紅線，不過仍可正常執行，可不予理會
        if(location!=null){
            longitude=location.getLongitude();
            latitude=location.getLatitude();
            workerThreadForLocation=new Thread(new Runnable() {
                @Override
                public void run() {
                    while(true){
                        try{
                            longitude=location.getLongitude();
                            latitude=location.getLatitude();
                            String msg="Latitude:"+latitude+" Longitude:"+longitude;
                            handlerForLocation.post(new Runnable() {
                                @Override
                                public void run() {
                                    MapsActivity.this.tex_distance.setText("經度"+longitude+'\n'+"緯度"+latitude);
                                }
                            });
                            Thread.sleep(300);


                        }catch (InterruptedException e){
                            Log.e("Erroe--------*-*-*-*: ","is"+e);
                        }
                    }
                }
            });
            workerThreadForLocation.start();

        }else{
            Toast.makeText(this, "偵測不到定位，請確認定位功能已開啟。", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.e("***Error Message***", "Connection suspended");
        googleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.i("***Error Message***", "Connection failed: ConnectionResult.getErrorCode() = " + connectionResult.getErrorCode());
    }
    protected synchronized void buildGoogleApiClient()//直接複製貼上的funciton
    {
        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }
}