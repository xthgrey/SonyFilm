package com.sonyfilm.xth.sonyfilm.ble;

public class BleData {
    private byte header;
    private byte func;
    private byte length;
    private short random;
    private short content;
    private short crc16;

    public byte getHeader() {
        return header;
    }

    public void setHeader(byte header) {
        this.header = header;
    }

    public byte getFunc() {
        return func;
    }

    public void setFunc(byte func) {
        this.func = func;
    }

    public byte getLength() {
        return length;
    }

    public void setLength(byte length) {
        this.length = length;
    }

    public short getRandom() {
        return random;
    }

    public void setRandom(short random) {
        this.random = random;
    }

    public short getContent() {
        return content;
    }

    public void setContent(short content) {
        this.content = content;
    }

    public short getCrc16() {
        return crc16;
    }

    public void setCrc16(short crc16) {
        this.crc16 = crc16;
    }
}
