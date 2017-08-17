package com.senssun.bluetooth.tools.entity;

public class MessObject {
    private String name; //名字
    private String address; //地址
    private String mess;    //内容
    private String status; //状态
    private int rssi;   //信号强弱

    private boolean sendProduct;//发送产品信息
    private boolean sendProductSuc;//接收产品信息成功
    private boolean sendUser;//发送个人信息
    private boolean sendUserSuc;//发送个人信息成功
    private boolean sendTimeDate;//发送时间信息
    private boolean sendTimeDateSuc;//发送时间信息成功


    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getMess() {
        return mess;
    }

    public void setMess(String mess) {
        this.mess = mess;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getRssi() {
        return rssi;
    }

    public void setRssi(int rssi) {
        this.rssi = rssi;
    }

    public boolean isSendProduct() {
        return sendProduct;
    }

    public void setSendProduct(boolean sendProduct) {
        this.sendProduct = sendProduct;
    }

    public boolean isSendProductSuc() {
        return sendProductSuc;
    }

    public void setSendProductSuc(boolean sendProductSuc) {
        this.sendProductSuc = sendProductSuc;
    }

    public boolean isSendUser() {
        return sendUser;
    }

    public void setSendUser(boolean sendUser) {
        this.sendUser = sendUser;
    }

    public boolean isSendUserSuc() {
        return sendUserSuc;
    }

    public void setSendUserSuc(boolean sendUserSuc) {
        this.sendUserSuc = sendUserSuc;
    }

    public boolean isSendTimeDate() {
        return sendTimeDate;
    }

    public void setSendTimeDate(boolean sendTimeDate) {
        this.sendTimeDate = sendTimeDate;
    }

    public boolean isSendTimeDateSuc() {
        return sendTimeDateSuc;
    }

    public void setSendTimeDateSuc(boolean sendTimeDateSuc) {
        this.sendTimeDateSuc = sendTimeDateSuc;
    }

}
