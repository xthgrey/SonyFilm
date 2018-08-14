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

    /**
     * Gets bluetooth adapter.
     *
     * @param
     * @return bluetooth adapter
     * @decs
     * @author H
     * @time 2018 /8/14 15:11
     */
    public BluetoothAdapter getBluetoothAdapter() {
        return bluetoothAdapter;
    }

    /**
     * Instantiates a new Blue tooth util.
     *
     * @param
     * @return
     * @decs
     * @author H
     * @time 2018 /8/14 15:11
     */
    public BlueToothUtil() {
        bondBleDeviceList = new ArrayList<>();
        bleDeviceList = new ArrayList<>();
    }

    /**
     * Gets bond devices.
     *
     * @param
     * @return bond devices
     * @decs
     * @author H
     * @time 2018 /8/14 15:12
     */
    public Set<BluetoothDevice> getBondDevices() {
        return bondDevices;
    }

    /**
     * Gets bond ble device list.
     *
     * @param
     * @return bond ble device list
     * @decs
     * @author H
     * @time 2018 /8/14 15:12
     */
    public List<BleDevice> getBondBleDeviceList() {
        return bondBleDeviceList;
    }

    /**
     * Gets ble device list list.
     *
     * @param
     * @return ble device list list
     * @decs
     * @author H
     * @time 2018 /8/14 15:12
     */
    public List<BleDevice> getBleDeviceListList() {
        return bleDeviceList;
    }

    /**
     * Sets bond ble device list.
     *
     * @param bondBleDeviceList the bond ble device list
     * @return
     * @decs
     * @author H
     * @time 2018 /8/14 15:12
     */
    public void setBondBleDeviceList(List<BleDevice> bondBleDeviceList) {
        this.bondBleDeviceList = bondBleDeviceList;
    }

    /**
     * Sets ble device list list.
     *
     * @param noBondBleDeviceList the no bond ble device list
     */
    public void setBleDeviceListList(List<BleDevice> noBondBleDeviceList) {
        this.bleDeviceList = noBondBleDeviceList;
    }

    /**
     * Is have blue tooth boolean.
     *
     * @param
     * @return boolean
     * @decs
     * @author H
     * @time 2018 /8/14 15:12
     */
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

    /**
     * Gets bond device.
     *
     * @param
     * @return
     * @decs
     * @author H
     * @time 2018 /8/14 15:12
     */
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
