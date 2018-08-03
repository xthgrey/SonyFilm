package com.sonyfilm.xth.sonyfilm.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

import com.sonyfilm.xth.sonyfilm.util.LogUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class BlueToothUtil {
    private BluetoothAdapter bluetoothAdapter;
    private List<BleDevice> bondBleDeviceList;
    private List<BleDevice> bleDeviceList;
    private Set<BluetoothDevice> bondDevices;

    public BluetoothAdapter getBluetoothAdapter() {
        return bluetoothAdapter;
    }

    public BlueToothUtil() {
        bondBleDeviceList = new ArrayList<>();
        bleDeviceList = new ArrayList<>();
    }

    public Set<BluetoothDevice> getBondDevices() {
        return bondDevices;
    }

    public List<BleDevice> getBondBleDeviceList() {
        return bondBleDeviceList;
    }
    public List<BleDevice> getBleDeviceListList() {
        return bleDeviceList;
    }

    public void setBondBleDeviceList(List<BleDevice> bondBleDeviceList) {
        this.bondBleDeviceList = bondBleDeviceList;
    }

    public void setBleDeviceListList(List<BleDevice> noBondBleDeviceList) {
        this.bleDeviceList = noBondBleDeviceList;
    }

    public Boolean isHaveBlueTooth() {
        LogUtil.d(getClass().getName() + "---" + new Throwable().getStackTrace()[0].getMethodName() + " : ");
        Boolean result = false;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter != null) {
            result = true;
        } else {
            result = false;
        }
        return result;
    }

    public void getBondDevice() {
        BleDevice bleDevice = new BleDevice();
        bondDevices = bluetoothAdapter.getBondedDevices();
        if (bondDevices.size() > 0) {
            for (BluetoothDevice bluetoothDevice : bondDevices) {
                LogUtil.e(bluetoothDevice.getName() + ":" + bluetoothDevice.getAddress() + "\n");
                bleDevice.setName(bluetoothDevice.getName());
                bleDevice.setAddress(bluetoothDevice.getAddress());
                bondBleDeviceList.add(bleDevice);
            }
        } else {
            LogUtil.e(getClass().getName() + "---" + new Throwable().getStackTrace()[0].getMethodName() + " : " + "no bond device!");
        }
    }
}
