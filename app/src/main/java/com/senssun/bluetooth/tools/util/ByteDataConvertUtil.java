package com.senssun.bluetooth.tools.util;

import java.io.UnsupportedEncodingException;

public class ByteDataConvertUtil
{
  public static byte Int2Byte(int num)
  {
    return (byte)(num & 0xFF);
  }

  public static int Byte2Int(byte byteNum)
  {
    return byteNum >= 0 ? byteNum : 128 + (128 + byteNum);
  }

  public static void BinnCat(byte[] from, byte to, int offset, int len) {
    int max = offset + len;
    int min = offset;
    int i = min; for (int j = 0; i < max; j++) {
      to = from[j];

      i++;
    }
  }

  public static void BinnCat(byte[] from, byte[] to, int index, int len)
  {
    int min = index;
    int i = 0; for (int j = min; i < len; j++) {
      to[i] = from[j];

      i++;
    }
  }

  public static byte[] LongToBin(long from, int len)
  {
    byte[] to = new byte[len];
    int max = len;

    int i_move = max - 1; for (int i_to = 0; i_move >= 0; i_to++) {
      to[i_to] = ((byte)(int)(from >> 8 * i_move));

      i_move--;
    }

    return to;
  }

  public static void LongToBin(long from, byte[] to, int offset, int len)
  {
    int max = len;
    int min = offset;

    int i_move = max - 1; for (int i_to = min; i_move >= 0; i_to++) {
      to[i_to] = ((byte)(int)(from >> 8 * i_move));

      i_move--;
    }
  }

  public static byte[] IntToBin(int from, int len)
  {
    byte[] to = new byte[len];
    int max = len;

    int i_move = max - 1; for (int i_to = 0; i_move >= 0; i_to++) {
      to[i_to] = ((byte)(from >> 8 * i_move));

      i_move--;
    }

    return to;
  }

  public static byte[] IntToBin(int from, byte[] to, int offset, int len)
  {
    int max = len;
    int min = offset;

    int i_move = max - 1; for (int i_to = min; i_move >= 0; i_to++) {
      to[i_to] = ((byte)(from >> 8 * i_move));

      i_move--;
    }

    return to;
  }

  public static long BinToLong(byte[] from, int offset, int len)
  {
    int min = offset;
    long to = 0L;
    int i_move = len - 1; for (int i_from = min; i_move >= 0; i_from++) {
      to = to << 8 | from[i_from] & 0xFF;

      i_move--;
    }

    return to;
  }

  public static int BinToInt(byte[] from, int offset, int len)
  {
    int to = 0;
    int min = offset;
    to = 0;

    int i_move = len - 1; for (int i_from = min; i_move >= 0; i_from++) {
      to = to << 8 | from[i_from] & 0xFF;

      i_move--;
    }

    return to;
  }

  public static byte[] getMacBytes(String mac)
  {
    byte[] macBytes = new byte[6];
    String[] strArr = mac.split(":");

    for (int i = 0; i < strArr.length; i++) {
      int value = Integer.parseInt(strArr[i], 16);
      macBytes[i] = ((byte)value);
    }
    return macBytes;
  }

  public static String getStrBytes(byte[] data, int offset, int len) {
    if (data.length < offset + len)
      return null;
    String str = "";
    for (int i = 0; i < len; i++) {
      str = str + String.format("%02X", new Object[] { Byte.valueOf(data[(offset + i)]) });
    }
    return str;
  }

  public static String bytesToHexString(byte[] src)
  {
    StringBuilder stringBuilder = new StringBuilder();
    if ((src == null) || (src.length <= 0)) {
      return null;
    }
    for (int i = 0; i < src.length; i++) {
      int v = src[i] & 0xFF;
      String hv = Integer.toHexString(v);

      if (hv.length() < 2) {
        stringBuilder.append(0);
      }
      stringBuilder.append(hv);
      stringBuilder.append(" ");
    }
    return stringBuilder.toString();
  }

  public static String[] bytesToHexStrings(byte[] src)
  {
    if ((src == null) || (src.length <= 0)) {
      return null;
    }
    String[] str = new String[src.length];

    for (int i = 0; i < src.length; i++) {
      int v = src[i] & 0xFF;
      String hv = Integer.toHexString(v);
      if (hv.length() == 1) {
        hv = 0 + hv;
      }
      str[i] = hv;
    }

    return str;
  }


  public static int toRevInt(byte[] from, int index, int len)
  {
    int to = 0;
    int min = index + len - 1;
    int i = 0; for (int i_from = min; i < len; i_from--) {
      to = to << 8 | from[i_from] & 0xFF;

      i++;
    }

    return to;
  }

  public static byte[] Int2Bit8(int num) {
    byte b = (byte)num;
    byte[] array = new byte[8];
    for (int i = 0; i <= 7; i++) {
      array[i] = ((byte)(b & 0x1));
      b = (byte)(b >> 1);
    }

    return array;
  }

  public static int Bit8Array2Int(byte[] from) {
    int len = from.length;

    int i = 0;
    for (int j = len - 1; j >= 0; j--) {
      i += (from[j] << len - 1 - j);
    }

    return i;
  }

  public static byte i2b(int in) {
    return (byte)Integer.toHexString(in).charAt(0);
  }

  public static byte[] byteMerger(byte[] byte_1, byte[] byte_2)
  {
    byte[] byte_3 = new byte[byte_1.length + byte_2.length];
    System.arraycopy(byte_1, 0, byte_3, 0, byte_1.length);
    System.arraycopy(byte_2, 0, byte_3, byte_1.length, byte_2.length);
    return byte_3;
  }

  public static byte[] StringToByte(String str, String charEncode) {
    byte[] destObj = null;
    try {
      if ((str == null) || (str.trim().equals(""))) {
        return new byte[0];
      }

      destObj = str.getBytes(charEncode);
    }
    catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    }
    return destObj;
  }
}