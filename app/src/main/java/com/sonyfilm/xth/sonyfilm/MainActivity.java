
package com.sonyfilm.xth.sonyfilm;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.sonyfilm.xth.sonyfilm.ble.BleAdapter;
import com.sonyfilm.xth.sonyfilm.ble.BleDevice;
import com.sonyfilm.xth.sonyfilm.ble.BlueToothUtil;
import com.sonyfilm.xth.sonyfilm.util.Constants;
import com.sonyfilm.xth.sonyfilm.util.LogUtil;
import com.sonyfilm.xth.sonyfilm.util.SthUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

import static com.sonyfilm.xth.sonyfilm.util.Constants.OPEN_BLUE_TOOTH;
import static com.sonyfilm.xth.sonyfilm.util.Constants.REC_SIZE;


public class MainActivity extends AppCompatActivity {

    private BlueToothUtil blueToothUtil;
    private ListView bleDeviceListView;
    private BleAdapter bleAdapter;
    private BleReceiver bleReceiver;
    private BluetoothSocket btSocket;

    private OutputStream out;
    private InputStream in;
    private byte[] sendBytes;
    private byte[] recBytes;

    private BleDevice itemClickBleDevice;

    //查询相关控件
    private Button filmInquireButton;
    private TextView filmAmountView;

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.BOND_LIST:
                    bleDeviceListView = (ListView) findViewById(R.id.ble_list);
                    bleAdapter = new BleAdapter(MainActivity.this, R.layout.ble_device_item, blueToothUtil.getBleDeviceListList());
                    bleDeviceListView.setAdapter(bleAdapter);
                    bleDeviceListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                            if (blueToothUtil.getBluetoothAdapter().isDiscovering()) {
                                blueToothUtil.getBluetoothAdapter().cancelDiscovery();
                            }
                            itemClickBleDevice = blueToothUtil.getBleDeviceListList().get(i);
                            BluetoothDevice btDev = blueToothUtil.getBluetoothAdapter().getRemoteDevice(itemClickBleDevice.getAddress());
                            try {
                                Boolean returnValue = false;
                                if (btDev.getBondState() == BluetoothDevice.BOND_NONE) {
                                    //利用反射方法调用BluetoothDevice.createBond(BluetoothDevice remoteDevice);
                                    Method createBondMethod = BluetoothDevice.class
                                            .getMethod("createBond");
                                    LogUtil.e("开始配对");
                                    returnValue = (Boolean) createBondMethod.invoke(btDev);
                                    itemClickBleDevice.setState("开始配对");

                                } else if (btDev.getBondState() == BluetoothDevice.BOND_BONDED) {
                                    connect(btDev, itemClickBleDevice);
                                }
                                bleAdapter.notifyDataSetChanged();
                                LogUtil.d("onItemClick:" + itemClickBleDevice.getName() + ":" + itemClickBleDevice.getAddress() + ":" + itemClickBleDevice.getState() + ":" + btDev.getBondState());
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });
                    break;
                default:
                    break;
            }
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        blueToothUtil = new BlueToothUtil();
        filmWidget();
        if (blueToothUtil.isHaveBlueTooth()) {
            Intent turnOnBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(turnOnBtIntent, OPEN_BLUE_TOOTH);
        }
    }

    //查询胶片相关操作
    private void filmWidget() {
        filmInquireButton = (Button) findViewById(R.id.film_inquire);
        filmAmountView = (TextView) findViewById(R.id.film_amount);
        filmInquireButton.setVisibility(View.GONE);
        filmAmountView.setVisibility(View.GONE);
        filmInquireButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendData((byte)0xA0,(byte)0x01,(byte)0x07);
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void checkPermissions() {
        //判断是否有访问位置的权限，没有权限，直接申请位置权限
        if ((checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                || (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, Constants.PERMISSION_RESULT);
        } else {
            blueToothUtil.getBondDevice();
            registerBleReceiver();
            sendMessage(Constants.BOND_LIST);
        }
    }

    private void sendMessage(int what) {
        Message message = new Message();
        message.what = what;
        handler.sendMessage(message);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        LogUtil.d(getClass().getName() + "---" + new Throwable().getStackTrace()[0].getMethodName() + " : " + "--requestCode:" + requestCode + "--permissions:" + permissions);
        boolean permission = true;
        switch (requestCode) {
            case Constants.PERMISSION_RESULT:
                for (int i : grantResults) {
                    LogUtil.d("grantResults[" + i + "]=" + grantResults[i]);
                    if (i != PackageManager.PERMISSION_GRANTED) {
                        permission = false;
                    }
                }
                if (!permission) {
                    Toast.makeText(this, Constants.BLE_LOCATION_PERMISSION, Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    blueToothUtil.getBondDevice();
                    registerBleReceiver();
                    sendMessage(Constants.BOND_LIST);
                }
                break;
            default:
                break;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        LogUtil.d(getClass().getName() + "---" + new Throwable().getStackTrace()[0].getMethodName() + " : " + "--requestCode:" + requestCode + "--resultCode:" + resultCode);
        switch (requestCode) {
            case Constants.OPEN_BLUE_TOOTH:
                if (resultCode == RESULT_OK) {
                    Toast.makeText(this, Constants.BLE_OPEN_SUCCESS, Toast.LENGTH_SHORT).show();
                    checkPermissions();
                } else if (resultCode == RESULT_CANCELED) {
                    Toast.makeText(this, Constants.BLE_NO_OPEN, Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            default:
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void registerBleReceiver() {
        blueToothUtil.getBluetoothAdapter().startDiscovery();
        IntentFilter intentFilter = new IntentFilter();
        bleReceiver = new BleReceiver();
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(bleReceiver, intentFilter);
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(bleReceiver);
        blueToothUtil.getBluetoothAdapter().disable();
        Toast.makeText(this, Constants.CLOSE_BLE, Toast.LENGTH_SHORT).show();
        super.onDestroy();
    }

    public class BleReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            // TODO: This method is called when the BroadcastReceiver is receiving
            // an Intent broadcast.
            String action = intent.getAction();
            BluetoothDevice device;
            switch (action) {
                case BluetoothAdapter.ACTION_DISCOVERY_STARTED:
                    LogUtil.e("开始搜索 ...");
                    break;
                case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                    LogUtil.e("搜索结束");
                    break;
                case BluetoothDevice.ACTION_FOUND:
                    device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    BleDevice bleDevice = new BleDevice();
                    bleDevice.setName(device.getName());
                    bleDevice.setAddress(device.getAddress());

                    if (isBondDevice(device)) {
                        bleDevice.setState("已绑定");
                    }
                    List<BleDevice> bleDeviceListTemp = blueToothUtil.getBleDeviceListList();
                    boolean flag = true;
                    if (bleDeviceListTemp.size() > 0) {
                        for (BleDevice device1 : bleDeviceListTemp) {
                            if (device1.getAddress().equals(bleDevice.getAddress())) {
                                flag = false;
                                break;
                            }
                        }
                        if (flag) {
                            blueToothUtil.getBleDeviceListList().add(bleDevice);
                            LogUtil.d(bleDevice.getName() + ":" + bleDevice.getAddress());
                        }
                    } else {
                        blueToothUtil.getBleDeviceListList().add(bleDevice);
                    }
//                    Message message = new Message();
//                    message.what = Constants.UPDATE_BLE_LIST;
//                    handler.sendMessage(message);
                    bleAdapter.notifyDataSetChanged();
                    LogUtil.e(bleDevice.getName() + ":" + bleDevice.getAddress());
                    break;
                case BluetoothDevice.ACTION_BOND_STATE_CHANGED:
                    device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    switch (device.getBondState()) {
                        case BluetoothDevice.BOND_BONDING:
                            LogUtil.e("正在配对......");
                            itemClickBleDevice.setState("正在配对");
                            break;
                        case BluetoothDevice.BOND_BONDED:
                            itemClickBleDevice.setState("配对完成");
                            connect(device, itemClickBleDevice);//连接设备
                            LogUtil.e("配对完成......");
                            break;
                        case BluetoothDevice.BOND_NONE:
                            itemClickBleDevice.setState("取消配对");
                            LogUtil.e("取消配对......");
                        default:
                            break;
                    }
                    bleAdapter.notifyDataSetChanged();
                    break;
                default:
                    break;
            }
        }
    }

    private void connect(BluetoothDevice btDev, BleDevice bleDevice) {
        UUID uuid = UUID.fromString(Constants.SPP_UUID);
        try {
            LogUtil.e("已连接......");
            btSocket = btDev.createRfcommSocketToServiceRecord(uuid);
            bleDevice.setState("已连接");
            bleAdapter.notifyDataSetChanged();
            btSocket.connect();
            createBuffer();

            bleDeviceListView.setVisibility(View.GONE);
            filmInquireButton.setVisibility(View.VISIBLE);
            filmAmountView.setVisibility(View.VISIBLE);

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private boolean isBondDevice(BluetoothDevice device) {
        boolean repeatFlag = false;
        for (BluetoothDevice bluetoothDevice :
                blueToothUtil.getBondDevices()) {
            if (bluetoothDevice.getAddress().equals(device.getAddress())) {
                repeatFlag = true;
            }
        }
        return repeatFlag;
    }

    private void createBuffer() {
        try {
            out = btSocket.getOutputStream();
            in = btSocket.getInputStream();
            sendBytes = new byte[Constants.SEND_SIZE];
            recBytes = new byte[Constants.REC_SIZE];

            new Thread(new Runnable() {
                @Override
                public void run() {
                    int recCounter = 0;
                    try {
                        while(true){
                            recBytes[recCounter] = (byte) in.read();
                            switch (recCounter){
                                case 0:
                                    if(recBytes[recCounter] == (byte)0xA8){
                                        recCounter ++;
                                    }else{
                                        recCounter = 0;
                                    }
                                    break;
                                case 1:
                                    if(recBytes[recCounter] == (byte)0x01 || recBytes[recCounter] == (byte)0x55 || recBytes[recCounter] == (byte)0xFF){
                                        recCounter ++;
                                    }else{
                                        recCounter = 0;
                                    }
                                    break;
                                case 2:
                                    if(recBytes[recCounter] == (byte)0x09){
                                        recCounter ++;
                                    }else{
                                        recCounter = 0;
                                    }
                                    break;
                                case 3:
                                case 4:
                                case 5:
                                case 6:
                                case 7:
                                    recCounter ++;
                                    break;
                                case 8:
                                    if((recBytes[Constants.REC_SIZE - 2] << 8 | recBytes[Constants.REC_SIZE - 1]) == SthUtil.getCRC(recBytes,REC_SIZE - 2)){
                                        LogUtil.e("CRC success!!!!");
                                    }
                                    recCounter =0;
                                    LogUtil.e("REC------："+SthUtil.byte2hex(recBytes));
                                    break;
                            }
                        }


                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
//            if (out != null) {
            // 需要发送的信息
//                byte[] b = {(byte) 0xA8, (byte) 0xFF,(byte)0x06,(byte)0x11,(byte)0x01,(byte)0xCC};
            // 以utf-8的格式发送出去
//                out.write(b);
//                LogUtil.e("write:"+ SthUtil.byte2hex(b));
//            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendData(byte header, byte func, byte length) {
        short crc16;
        short randomData;
        sendBytes[0] = header;
        sendBytes[1] = func;
        sendBytes[2] = length;
        randomData = SthUtil.getRandomData();
        sendBytes[4] = (byte) (randomData >> 8);
        sendBytes[3] = (byte) randomData;
        crc16 = SthUtil.getCRC(sendBytes,Constants.SEND_SIZE - 2);
        sendBytes[6] = (byte) (crc16 >> 8);
        sendBytes[5] = (byte) crc16;
        try {
            if(out != null){
                out.write(sendBytes);
                LogUtil.e("SEND------："+SthUtil.byte2hex(sendBytes));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
