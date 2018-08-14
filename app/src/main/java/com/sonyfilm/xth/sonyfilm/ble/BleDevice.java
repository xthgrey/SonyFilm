package com.sonyfilm.xth.sonyfilm.ble;

/**
 * The type Ble device.
 *
 * @param
 * @author H
 * @decs
 * @time 2018 /8/14 15:11
 * @return
 */
public class BleDevice {
    private String name;
    private String address;
    private String state;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }
}
