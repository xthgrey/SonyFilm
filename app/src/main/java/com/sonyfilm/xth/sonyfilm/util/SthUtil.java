package com.sonyfilm.xth.sonyfilm.util;

import java.util.Random;

public class SthUtil {
    /**
     * 字节数组转换为十六进制字符串
     *
     * @param b byte[] 需要转换的字节数组
     * @return String 十六进制字符串
     */
    public static String byte2hex(byte b[]) {
        if (b == null) {
            throw new IllegalArgumentException(
                    "Argument b ( byte array ) is null! ");
        }
        String hs = "";
        String stmp = "";
        for (int n = 0; n < b.length; n++) {
            stmp = Integer.toHexString(b[n] & 0xff);
            if (stmp.length() == 1) {
                hs = hs + "0" + stmp;
            } else {
                hs = hs + stmp;
            }
            hs = hs + " ";
        }
        return hs.toUpperCase();
    }

    public static short getCRC(byte[] data, int Len) {
        int Reg_CRC=0xffff;
        int temp;
        int i,j;

        for( i = 0; i<Len; i ++)
        {
            temp = data[i];
            if(temp < 0) temp += 256;
            temp &= 0xff;
            Reg_CRC^= temp;

            for (j = 0; j<8; j++)
            {
                if ((Reg_CRC & 0x0001) == 0x0001)
                    Reg_CRC=(Reg_CRC>>1)^0xA001;
                else
                    Reg_CRC >>=1;
            }
        }
        return (short)(Reg_CRC&0xffff);
    }

    public static short getRandomData(){
        Random random = new Random();
        return (short) random.nextInt(65536);
    }

    public static short mergeByteToShort(byte high,byte low){
        return (short)(((high &0x00FF) << 8) | (0x00ff & low));
    }
}
