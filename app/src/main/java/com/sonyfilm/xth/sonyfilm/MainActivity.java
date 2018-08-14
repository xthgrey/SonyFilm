
package com.sonyfilm.xth.sonyfilm;

import android.Manifest;
import android.annotation.SuppressLint;
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
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.sonyfilm.xth.sonyfilm.ble.BleAdapter;
import com.sonyfilm.xth.sonyfilm.ble.BleData;
import com.sonyfilm.xth.sonyfilm.ble.BleDevice;
import com.sonyfilm.xth.sonyfilm.ble.BlueToothUtil;
import com.sonyfilm.xth.sonyfilm.util.Constants;
import com.sonyfilm.xth.sonyfilm.util.LogUtil;
import com.sonyfilm.xth.sonyfilm.util.SthUtil;
import com.uuzuche.lib_zxing.activity.CodeUtils;
import com.uuzuche.lib_zxing.activity.ZXingLibrary;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import static com.sonyfilm.xth.sonyfilm.util.Constants.OPEN_BLUE_TOOTH;
import static com.sonyfilm.xth.sonyfilm.util.Constants.REC_SIZE;


/**
 * The type Main activity.
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private BlueToothUtil blueToothUtil;
    private ListView bleDeviceListView;
    private BleAdapter bleAdapter;
    private BleReceiver bleReceiver;
    private BluetoothSocket btSocket;

    private OutputStream out;
    private InputStream in;
    private byte[] sendBytes;
    private byte[] recBytes;
    private BleData bleSendData;
    private BleData bleRecData;
    private Timer timer;

    private BleDevice itemClickBleDevice;

    //查询相关控件
    private Toolbar toolBar;
    private FloatingActionButton filmInquireButton;
    private TextView filmAmountView;
    private TextView choiseBleDeviceAddressView;

    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        @SuppressLint("SetTextI18n")
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.BOND_LIST://显示蓝牙配对列表，和配对状态
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
                                    LogUtil.e(Constants.BLE_START_PAIR);
                                    returnValue = (Boolean) createBondMethod.invoke(btDev);
                                    itemClickBleDevice.setState(Constants.BLE_START_PAIR);

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
                case Constants.SEARCH_FILM://查询胶片
                    timer.cancel();
                    filmAmountView.setText(Constants.RESIDUE_FILM_STRING + (short) (bleRecData.getContent() + bleSendData.getRandom() - bleRecData.getRandom()));
                    filmInquireButton.setClickable(true);//恢复按键功能
                    break;
                case Constants.REPEAT_SEND://重发
                    if (bleRecData.getContent() < 3) {
                        bleSendData(bleSendData.getHeader(), bleSendData.getFunc(), bleSendData.getLength());
                    } else {
                        Toast.makeText(MainActivity.this, Constants.RESTART_APP, Toast.LENGTH_LONG).show();
                    }
                    break;
                case Constants.RESET_FILM://重置胶片
                    timer.cancel();
                    LogUtil.e("胶片已经重置:" + (short) (bleRecData.getContent() + bleSendData.getRandom() - bleRecData.getRandom()));
                    break;
                case Constants.REPEAT_TIME_OUT://重发超时
                    Toast.makeText(MainActivity.this, Constants.RESTART_APP_SCAN, Toast.LENGTH_LONG).show();
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    /**
     *@decs
     *@author H
     *@time 2018/8/14 15:14
     * @param [menu]
     * @return boolean
     */
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.tool_bar_menu, menu);//动态创建Toolbar中的菜单
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.scan_qr:
//                LogUtil.d("you click scan qr!");
//                sendData((byte) 0xA0, (byte) 0xFF, (byte) 0x07);
//
//                timer = new Timer();
//                TimerTask task = new TimerTask() {
//                    @Override
//                    public void run() {
//                        // 需要做的事:发送消息
//                        sendData((byte) 0xA0, (byte) 0x55, (byte) 0x07);
//
//                        timer = new Timer();
//                        TimerTask task = new TimerTask() {
//                            @Override
//                            public void run() {
//                                Message message = new Message();
//                                message.what = Constants.REPEAT_TIME_OUT;
//                                handler.sendMessage(message);
//                            }
//                        };
//                        timer.schedule(task, 500);
//                    }
//                };
//                timer.schedule(task, 500);
                Intent scanIntent = new Intent(this, QrcodeActivity.class);
                startActivityForResult(scanIntent, Constants.QR_RESULT);

                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ZXingLibrary.initDisplayOpinion(this);

        blueToothUtil = new BlueToothUtil();
        filmWidget();
        if (blueToothUtil.isHaveBlueTooth()) {
            Intent turnOnBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(turnOnBtIntent, OPEN_BLUE_TOOTH);
        }
    }


    /**
     *@decs  查询胶片相关控件初始化
     *@author H
     *@time 2018/8/14 15:14
     * @param
     * @return
     */
    private void filmWidget() {
        filmInquireButton = (FloatingActionButton) findViewById(R.id.film_inquire);
        filmAmountView = (TextView) findViewById(R.id.film_amount);
        choiseBleDeviceAddressView = (TextView) findViewById(R.id.choise_ble_device_address);
        filmInquireButton.setVisibility(View.GONE);
        filmAmountView.setVisibility(View.GONE);
        filmInquireButton.setOnClickListener(this);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    /**
     *@decs
     *@author H
     *@time 2018/8/14 15:14
     * @param []
     * @return void
     */
    private void checkPermissions() {
        //判断是否有访问位置的权限，没有权限，直接申请位置权限
        if ((checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                || (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                || (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.CAMERA}, Constants.PERMISSION_RESULT);
        } else {
            blueToothUtil.getBondDevice();
            registerBleReceiver();
            sendMessage(Constants.BOND_LIST);
        }
    }
    /**
     *@decs
     *@author H
     *@time 2018/8/14 15:15
     * @param
     * @return
     */
    private void sendMessage(int what) {
        Message message = new Message();
        message.what = what;
        handler.sendMessage(message);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        LogUtil.d(getClass().getName() + "---" + new Throwable().getStackTrace()[0].getMethodName() + " : " + "--requestCode:" + requestCode);
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
            case Constants.QR_RESULT:
                //处理扫描结果（在界面上显示）
                if (null != data) {
                    Bundle bundle = data.getExtras();
                    if (bundle == null) {
                        return;
                    }
                    if (bundle.getInt(CodeUtils.RESULT_TYPE) == CodeUtils.RESULT_SUCCESS) {
                        String result = bundle.getString(CodeUtils.RESULT_STRING);
                        Toast.makeText(this, "解析结果:" + result, Toast.LENGTH_LONG).show();
                    } else if (bundle.getInt(CodeUtils.RESULT_TYPE) == CodeUtils.RESULT_FAILED) {
                        Toast.makeText(MainActivity.this, Constants.QR_ANALYSIS_FAIL, Toast.LENGTH_LONG).show();
                    }
                }
                break;
            default:
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
    /**
     *@decs 蓝牙广播注册
     *@author H
     *@time 2018/8/14 15:15
     * @param
     * @return
     */
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
        Toast.makeText(this, Constants.BLE_CLOSED, Toast.LENGTH_SHORT).show();
        super.onDestroy();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.film_inquire:
                bleSendData((byte) 0xA0, (byte) 0x01, (byte) 0x07);
                filmInquireButton.setClickable(false);//取消按键功能
                timer = new Timer();
                TimerTask task = new TimerTask() {
                    @Override
                    public void run() {
                        filmInquireButton.setClickable(true);//取消按键功能
                    }
                };
                timer.schedule(task, 200);
                break;
            default:
                break;
        }
    }

    /**
     * The type Ble receiver.
     *
     * @param
     * @author H
     * @decs
     * @time 2018 /8/14 15:15
     * @return
     */
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
                        bleDevice.setState(Constants.BLE_BOUND);
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
                            LogUtil.e(Constants.BLE_PAIRING);
                            itemClickBleDevice.setState(Constants.BLE_PAIRING);
                            break;
                        case BluetoothDevice.BOND_BONDED:
                            itemClickBleDevice.setState(Constants.BLE_PAIRED);
                            connect(device, itemClickBleDevice);//连接设备
                            LogUtil.e(Constants.BLE_PAIRED);
                            break;
                        case BluetoothDevice.BOND_NONE:
                            itemClickBleDevice.setState(Constants.BLE_DEPAIR);
                            LogUtil.e(Constants.BLE_DEPAIR);
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
    /**
     *@decs 蓝牙连接上了
     *@author H
     *@time 2018/8/14 15:16
     * @param
     * @return
     */
    private void connect(BluetoothDevice btDev, BleDevice bleDevice) {
        UUID uuid = UUID.fromString(Constants.SPP_UUID);
        try {
            LogUtil.e(Constants.BLE_CONNECTED);
            btSocket = btDev.createRfcommSocketToServiceRecord(uuid);
            bleDevice.setState(Constants.BLE_CONNECTED);
            bleAdapter.notifyDataSetChanged();

            bleDeviceListView.setVisibility(View.GONE);
            filmInquireButton.setVisibility(View.VISIBLE);
            filmAmountView.setVisibility(View.VISIBLE);

            choiseBleDeviceAddressView.setText(bleDevice.getAddress());

            toolBar = (Toolbar) findViewById(R.id.main_layout_toolbar);
            setSupportActionBar(toolBar);//将toolBar作为ActionBar
            //添加toolbar导航栏
            ActionBar actionBar = getSupportActionBar();
            actionBar.setTitle("");

            btSocket.connect();
            createBuffer();


        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    /**
     *@decs 判断蓝牙设备是否绑定
     *@author H
     *@time 2018/8/14 15:16
     * @param
     * @return
     */
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
    /**
     *@decs 蓝牙发送和接收操作
     *@author H
     *@time 2018/8/14 15:16
     * @param
     * @return
     */
    private void createBuffer() {
        try {
            out = btSocket.getOutputStream();
            in = btSocket.getInputStream();
            sendBytes = new byte[Constants.SEND_SIZE];
            recBytes = new byte[Constants.REC_SIZE];
            bleSendData = new BleData();
            bleRecData = new BleData();
            //蓝牙接收线程
            new Thread(new Runnable() {
                @Override
                public void run() {
                    int recCounter = 0;
                    try {
                        while (true) {
                            recBytes[recCounter] = (byte) in.read();
                            switch (recCounter) {
                                case 0:
                                    if (recBytes[recCounter] == (byte) 0xA8) {
                                        recCounter++;
                                    } else {
                                        recCounter = 0;
                                    }
                                    break;
                                case 1:
                                    if (recBytes[recCounter] == (byte) 0x01 || recBytes[recCounter] == (byte) 0x55 || recBytes[recCounter] == (byte) 0xFF) {
                                        recCounter++;
                                    } else {
                                        recCounter = 0;
                                    }
                                    break;
                                case 2:
                                    if (recBytes[recCounter] == (byte) 0x09) {
                                        recCounter++;
                                    } else {
                                        recCounter = 0;
                                    }
                                    break;
                                case 3:
                                case 4:
                                case 5:
                                case 6:
                                case 7:
                                    recCounter++;
                                    break;
                                case 8:
                                    LogUtil.e("REC------：" + SthUtil.byte2hex(recBytes));
                                    LogUtil.e("Native CRC------：" + SthUtil.getCRC(recBytes, REC_SIZE - 2));
                                    LogUtil.e("Recv   CRC------：" + SthUtil.mergeByteToShort(recBytes[Constants.REC_SIZE - 1], recBytes[Constants.REC_SIZE - 2]));
                                    if (SthUtil.mergeByteToShort(recBytes[Constants.REC_SIZE - 1], recBytes[Constants.REC_SIZE - 2]) == SthUtil.getCRC(recBytes, REC_SIZE - 2)) {
                                        LogUtil.e("CRC success!!!!");
                                        bleRecData.setHeader(recBytes[0]);
                                        bleRecData.setFunc(recBytes[1]);
                                        bleRecData.setLength(recBytes[2]);
                                        bleRecData.setRandom(SthUtil.mergeByteToShort(recBytes[4], recBytes[3]));
                                        bleRecData.setContent(SthUtil.mergeByteToShort(recBytes[6], recBytes[5]));
                                        bleRecData.setCrc16(SthUtil.mergeByteToShort(recBytes[8], recBytes[7]));
                                        switch (recBytes[1]) {
                                            case (byte) 0x01:
                                                sendMessage(Constants.SEARCH_FILM);
                                                break;
                                            case (byte) 0x55:
                                                sendMessage(Constants.REPEAT_SEND);
                                                break;
                                            case (byte) 0xFF:
                                                sendMessage(Constants.RESET_FILM);
                                                break;
                                            default:
                                                break;
                                        }
                                    }
                                    recCounter = 0;
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


    /**
     *@decs 蓝牙协议发送
     *@author H
     *@time 2018/8/14 15:17
     * @param
     * @return
     */
    private void bleSendData(byte header, byte func, byte length) {
        short crc16;
        bleSendData.setHeader(header);
        bleSendData.setFunc(func);
        bleSendData.setLength(length);
        sendBytes[0] = bleSendData.getHeader();
        sendBytes[1] = bleSendData.getFunc();
        sendBytes[2] = bleSendData.getLength();

        sendBytes[4] = (byte) (SthUtil.getRandomData() >> 8);
        sendBytes[3] = (byte) SthUtil.getRandomData();
        bleSendData.setRandom(SthUtil.mergeByteToShort(sendBytes[4], sendBytes[3]));

        crc16 = SthUtil.getCRC(sendBytes, Constants.SEND_SIZE - 2);
        sendBytes[6] = (byte) (crc16 >> 8);
        sendBytes[5] = (byte) crc16;
        bleSendData.setCrc16(SthUtil.mergeByteToShort(sendBytes[6], sendBytes[5]));

        try {
            if (out != null) {
                out.write(sendBytes);
                LogUtil.e("SEND------：" + SthUtil.byte2hex(sendBytes));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
