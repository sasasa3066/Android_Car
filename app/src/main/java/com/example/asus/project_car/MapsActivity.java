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
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.provider.Settings;
import android.support.annotation.NonNull;
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


public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,LocationListener {

    private static GoogleMap mMap;// Google API用戶端物件
    private static Location locationForWifi;//------------test-----------------
    private static Location locationForGps;
    private static LocationManager locationManager;
    private static double latitude=0;
    private static double longitude=0;

    //Bluetooth
    private static BluetoothAdapter bluetoothAdapter;
    private static BluetoothDevice device;
    private static BluetoothSocket socket;
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // 一定要是這組
    private static OutputStream outputStream = null;
    private static InputStream inputStream = null;
    private final int REQUEST_ENABLE_BT=1;
    private static final int REQUEST_CODE_ACCESS_COARSE_LOCATION = 1;
    private static final int REQUEST_PERMISSION_PHONE_STATE = 1;
    private static AlertDialog.Builder dialog;
    private static byte[] readBuffer;
    private static int readBufferPosition;
    private static Thread workerThread;
    private final static int MESSAGE_READ = 2;
    private static int distance_byte=0;
    private static Handler handler;

    private Button btn_send;
    private Button btn_btStart;
    private Button btn_set;
    private TextView tex_distance;
    private TextView tex_location;

    int x=0;//測試用..................................
    int Num=0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        dialog=new AlertDialog.Builder(MapsActivity.this);
        tex_distance=(TextView)findViewById(R.id.tex_distance);
        tex_location=(TextView)findViewById(R.id.tex_location);
        //----------------------------------------------copy past follow -----------------------------------------------------------------------
        //複製網址:https://github.com/matsurigoto/AndroidGPSExample/blob/master/app/src/main/java/com/example/duranhsieh/gpsexample/MainActivity.java
        LocationManager status = (LocationManager) (this.getSystemService(Context.LOCATION_SERVICE));
        if (status.isProviderEnabled(LocationManager.GPS_PROVIDER) || status.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {

                } else {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.INTERNET, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_PERMISSION_PHONE_STATE);
                }
                return;
            }
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
            Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);//使用GPS

            getLocation(location);
        } else {
            Toast.makeText(this, "請開啟定位服務", Toast.LENGTH_LONG).show();
            startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));	//開啟設定
        }
        //--------------------------------------------------------copy pasy above by-------------------------------------------------------------------------------------
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
                    tex_distance.setText("前方超音波距離:"+distance_byte+'\n'+"");//這邊一定要有String不能單純只有數字
                }
            }
        };
        btn_send=(Button)findViewById(R.id.btn_BT);
        btn_send.setOnClickListener(send);
        btn_btStart=(Button)findViewById(R.id.btn_btStart);
        btn_btStart.setOnClickListener(btStart);
        btn_set=(Button)findViewById(R.id.btn_set);
        btn_set.setOnClickListener(setNum);
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
            if(device!=null){//僅適用這隻程式因為這隻程式只有連接上時才有device
                try{
                    // 送出訊息
                    String message ="5";
                    outputStream.write(message.getBytes());

                }catch(IOException e){

                }
            }else{
                Toast.makeText(MapsActivity.this,"Not connected BT",Toast.LENGTH_SHORT).show();
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

    private void getLocation(Location location) {
        if(location != null) {
            this.locationForGps=location;
            longitude = locationForGps.getLongitude();
            latitude = locationForGps.getLatitude();
            tex_location.setText("X:"+x+'\n'+"緯度:"+latitude+'\n'+"經度:"+longitude);
            if(x<100){
                x++;
            }else{
                x=0;
            }
        }
        else {
            Toast.makeText(this, "無法定位座標", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_PERMISSION_PHONE_STATE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
                    Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    if(location!=null){
                        getLocation(location);
                    }
                }
                return;
            }
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        getLocation(location);
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }
    View.OnClickListener setNum=new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if(Num==0){
                Num=1;
            }else{
                Num=0;
            }
        }
    };
}