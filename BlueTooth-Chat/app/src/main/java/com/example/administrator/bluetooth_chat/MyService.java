package com.example.administrator.bluetooth_chat;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.UUID;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;


/**
 * Created by Administrator on 2017/2/15.
 */
public class MyService {

    private static final UUID MY_UUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c6666");
    // 成员变量
    private  BluetoothAdapter btAdapter;

    SelectDevice activity;
    private AcceptThread myAcceptThread;
    private ConnectThread myConnectThread;
    private ConnectedThread myConnectedThread;
    private int myState;
    // 表示当前连接状态的常量
    public static final int STATE_NONE = 0;       // 什么也没做
    public static final int STATE_LISTEN = 1;     // 正在监听连接
    public static final int STATE_CONNECTING = 2; // 正在连接
    public static final int STATE_CONNECTED = 3;  // 已连接到设备
    // 构造器
    public MyService(SelectDevice activity) {
        this.activity = activity;

        btAdapter = BluetoothAdapter.getDefaultAdapter();
        myState = STATE_NONE;


    }
    //设置当前连接状态的方法
    private synchronized void setState(int state) {
        myState = state;
    }
    //获取当前连接状态的方法
    public synchronized int getState() {
        return myState;
    }
    //开启service的方法
    public synchronized void start() {
        Log.e("text","start");
        // 关闭不必要的线程
        if (myConnectThread != null) {myConnectThread.cancel(); myConnectThread = null;}
        if (myConnectedThread != null) {myConnectedThread.cancel(); myConnectedThread = null;}
        if (myAcceptThread == null) {// 开启线程监听连接
            myAcceptThread = new AcceptThread();
            myAcceptThread.start();
        }
        setState(STATE_LISTEN);
    }
    //连接设备的方法
    public synchronized void connect(BluetoothDevice device) {
        Log.e("text","connect");
        // 关闭不必要的线程
        if (myState == STATE_CONNECTING) {
            if (myConnectThread != null) {myConnectThread.cancel(); myConnectThread = null;}
        }
        if (myConnectedThread != null) {myConnectedThread.cancel(); myConnectedThread = null;}

        Toast.makeText(activity,"正在连接...", Toast.LENGTH_SHORT).show();

        // 开启线程连接设备
        myConnectThread = new ConnectThread(device);
        myConnectThread.start();

        setState(STATE_CONNECTING);
    }

    /******************************************************************************************
     ******************************************************************************************/
    //开启管理和已连接的设备间通话的线程的方法
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
        Log.e("text","connected");
        // 关闭不必要的线程
        if (myConnectThread != null) {myConnectThread.cancel(); myConnectThread = null;}
        if (myConnectedThread != null) {myConnectedThread.cancel(); myConnectedThread = null;}
        if (myAcceptThread != null) {myAcceptThread.cancel(); myAcceptThread = null;}

        // 创建并启动ConnectedThread

        myConnectedThread = new ConnectedThread(socket);
        myConnectedThread.start();

        setState(STATE_CONNECTED);

    }

    public synchronized void stop() {//停止所有线程的方法
        Log.e("text","stop");
        if (myConnectThread != null) {myConnectThread.cancel(); myConnectThread = null;}
        if (myConnectedThread != null) {myConnectedThread.cancel(); myConnectedThread = null;}
        if (myAcceptThread != null) {myAcceptThread.cancel(); myAcceptThread = null;}

        setState(STATE_NONE);
    }
    /******************************************************************************************
     ******************************************************************************************/


    private class AcceptThread extends Thread {//用于监听连接的线程
        // 本地服务器端ServerSocket
        private final BluetoothServerSocket mmServerSocket;
        public AcceptThread() {
            BluetoothServerSocket tmpSS = null;
            try {// 创建用于监听的服务器端ServerSocket
                tmpSS = btAdapter.listenUsingRfcommWithServiceRecord ("BluetoothChat",MY_UUID);
            } catch (IOException e) {
                e.printStackTrace();
            }
            mmServerSocket = tmpSS;
        }
        public void run() {
            setName("AcceptThread");
            BluetoothSocket socket = null;
            while (myState != STATE_CONNECTED) {//如果没有连接到设备
                try {
                    socket = mmServerSocket.accept();//获取连接的Sock
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
                if (socket != null) {// 如果连接成功
                    synchronized (MyService.this) {
                        switch (myState) {
                            case STATE_LISTEN:
                            case STATE_CONNECTING:
                                // 开启管理连接后数据交流的线程
                                Message msg = activity.myHandler.obtainMessage(3);
                                Bundle bundle = new Bundle();
                                bundle.putString("client_address",socket.getRemoteDevice().getAddress());
                                bundle.putString("client_name", socket.getRemoteDevice().getName());
                                msg.setData(bundle);
                                activity.myHandler.sendMessage(msg);

                                connected(socket, socket.getRemoteDevice());
                                break;
                            case STATE_NONE:
                            case STATE_CONNECTED:
                                try {// 关闭新Socket
                                    socket.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                break;
                        }
                    }
                }
            }
        }
        public void cancel() {
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    //用于尝试连接其他设备的线程
    private class ConnectThread extends Thread {
        private final BluetoothSocket myBtSocket;
        private final BluetoothDevice mmDevice;
        public ConnectThread(BluetoothDevice device) {
            mmDevice = device;
            BluetoothSocket tmp = null;
            // 通过正在连接的设备获取BluetoothSocket
            try {
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                e.printStackTrace();
            }
            myBtSocket = tmp;
        }
        public void run() {
            setName("ConnectThread");
            btAdapter.cancelDiscovery();// 取消搜索设备
            try {// 连接到BluetoothSocket
                myBtSocket.connect();//尝试连接

            } catch (IOException e) {
                setState(STATE_LISTEN);//连接断开后设置状态为正在监听
                try {// 关闭socket
                    myBtSocket.close();
                } catch (IOException e2) {
                    e.printStackTrace();
                }
                activity.myHandler.sendEmptyMessage(2);
                MyService.this.start();//如果连接不成功，重新开启service

                return;
            }

            Message msg = activity.myHandler.obtainMessage(1);
            Bundle bundle = new Bundle();
            bundle.putString("server_address",mmDevice.getAddress());
            bundle.putString("server_name", mmDevice.getName());
            msg.setData(bundle);
            activity.myHandler.sendMessage(msg);


            synchronized (MyService.this) {// 将ConnectThread线程置空
                myConnectThread = null;
            }


            connected(myBtSocket, mmDevice);// 开启管理连接后数据交流的线程

        }
        public void cancel() {
            try {
                myBtSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

/******************************************************************************************
 ******************************************************************************************/
    public void write(String msg) {//向ConnectedThread写入数据的方法
        ConnectedThread tmpCt;// 创建临时对象引用
        synchronized (this) {// 锁定ConnectedThread
            if (myState != STATE_CONNECTED) return;
            tmpCt = myConnectedThread;
        }
        tmpCt.write(msg);// 写入数据
    }


    //用于管理连接后数据交流的线程
    private class ConnectedThread extends Thread {
        private final BluetoothSocket myBtSocket;

        DataInputStream din = null;
        DataOutputStream dout = null;

        public ConnectedThread(BluetoothSocket socket) {
            this.myBtSocket = socket;

            // 获取BluetoothSocket的输入输出流
            try {
                din = new DataInputStream(socket.getInputStream());
                dout = new DataOutputStream(socket.getOutputStream());


            } catch (IOException e) {

                activity.myHandler.sendEmptyMessage(2);
                e.printStackTrace();
            }
        }
        public void run() {


            while (true) {// 一直监听输入流
                try {
                    String msgRev = din.readUTF();// 从输入流中读入数据
                    //将读入的数据发送到Activity

                     //Message msg =   activity.myHandler.obtainMessage(3, msgRev);
                   // msg.sendToTarget();


                } catch (IOException e) {
                    e.printStackTrace();
                    setState(STATE_LISTEN);//连接断开后设置状态为正在监听
                    break;
                }
            }
        }
        //向输出流中写入数据的方法
        public void write(String msg) {
            try {
                dout.writeUTF(msg);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        //向输出流中写入数据的方法

        /******************************************************************************************
         ******************************************************************************************/

        public void cancel() {
            try {
                myBtSocket.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
