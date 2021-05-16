package com.example.tcpdemo;

import ohos.hiviewdfx.HiLog;
import ohos.hiviewdfx.HiLogLabel;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.logging.Logger;

public class TCPClient {

    HiLogLabel LABEL = new HiLogLabel(HiLog.LOG_APP, 0x00201, TCPClient.class.getSimpleName());

    public interface OnConnectCallback {
        void onConnectSuccess();

        void onConnectFailure();
    }

    public interface OnReceiveCallback {
        void onReceiveSuccess(String str);

        void onReceiveFailure();

        void onDisconnected();
    }

    public interface OnSendCallback {
        void onSendSuccess();

        void onSendFailure();
    }

    private static final String TAG = TCPClient.class.getSimpleName();
    private static TCPClient instance;

    /**
     * 服务端的 IP 地址
     */
    private String ip;
    /**
     * 服务端接口
     */
    private int port;

    private int timeout = 2000;

    private OnConnectCallback onConnectCallback = null;
    private OnReceiveCallback onReceiveCallback = null;
    private OnSendCallback onSendCallback = null;

    private Socket socket;
    private InputStream inputStream;
    private OutputStream outputStream;
    SocketReceiveThread receiveThread;

    public static TCPClient getInstance() {
        if (instance == null) {
            synchronized (TCPClient.class) {
                if (instance == null) {
                    instance = new TCPClient();
                }
            }
        }

        return instance;
    }

    public TCPClient init(String ip, int port, int timeout) {
        this.ip = ip;
        this.port = port;
        this.timeout = timeout;
        return this;
    }

    public TCPClient setConnectCallback(OnConnectCallback onConnectCallback) {
        this.onConnectCallback = onConnectCallback;
        return this;
    }

    public TCPClient setReceiveDataCallback(OnReceiveCallback onReceiveCallback) {
        this.onReceiveCallback = onReceiveCallback;
        return this;
    }

    public TCPClient setSendDataCallback(OnSendCallback onSendCallback) {
        this.onSendCallback = onSendCallback;
        return this;
    }

    public void connect() {
        connectToServer();
    }

    public void disconnect() {
        disconnectFromServer();
    }

    public void sendData(byte[] data) {
        sendDataToServer(data);
    }

    private void connectToServer() {
        if (!isParameterCorrect()) {
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    //连接服务端，指定服务端ip地址和端口号。
                    socket = new Socket();
                    socket.connect(new InetSocketAddress(ip, port), timeout);
                    //获取输出流、输入流
                    outputStream = socket.getOutputStream();
                    inputStream = socket.getInputStream();
                    onConnectCallback.onConnectSuccess();

                    receiveThread = new SocketReceiveThread();
                    receiveThread.start();

                    onConnectCallback.onConnectSuccess();
                } catch (IOException e) {
                    onConnectCallback.onConnectFailure();
                }
            }
        }).start();
    }

    private void disconnectFromServer() {
        try {
            if (receiveThread != null) {
                receiveThread.threadExit();
                receiveThread = null;
            }
            if (outputStream != null) {
                outputStream.close(); //关闭输出流
                outputStream = null;
            }
            if (inputStream != null) {
                inputStream.close(); //关闭输入流
                inputStream = null;
            }
            if (socket != null) {
                socket.close();  //关闭socket
                socket = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendDataToServer(final byte[] data) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (data == null || outputStream == null || socket == null) {
                    onSendCallback.onSendFailure();
                    return;
                }
                try {
                    outputStream.write(data);
                    outputStream.flush();
                    HiLog.info(LABEL, "Snd Data Successfully. :)");
                    onSendCallback.onSendSuccess();
                } catch (IOException e) {
                    e.printStackTrace();
                    HiLog.info(LABEL, "Send Data Failed. :(");
                    onSendCallback.onSendFailure();
                }
            }
        }).start();
    }

    class SocketReceiveThread extends Thread {
        private boolean threadExit;

        public SocketReceiveThread() {
            HiLog.info(LABEL, "Create a new socket receive thread");
            threadExit = false;
        }

        public void run() {
            byte[] buffer = new byte[1024];
            while (!threadExit) {
                HiLog.info(LABEL, "SocketReceiveThread is running.");
                if (inputStream == null) {
                    threadExit = true;
                    return;
                }
                try {
                    // 读取数据，返回值表示读到的数据长度。-1表示结束
                    // 阻塞直到接收
                    int length = inputStream.read(buffer);
                    if (length == -1) {
                        HiLog.info(LABEL, "read read -1");
                        onReceiveCallback.onDisconnected();
                        break;
                    } else {
                        String receiveData = new String(buffer, 0, length);
                        HiLog.info(LABEL, "read buffer: %{public}s, count: %{public}s", receiveData, length);
                        onReceiveCallback.onReceiveSuccess(receiveData);
                    }
                } catch (SocketException e) {
                    e.printStackTrace();
                    onReceiveCallback.onReceiveFailure();
                } catch (IOException e) {
                    HiLog.info(LABEL, "read buffer:error");
                    e.printStackTrace();
                    onReceiveCallback.onReceiveFailure();
                }
            }
        }

        void threadExit() {
            threadExit = true;
        }
    }

    private boolean isParameterCorrect() {
        return (ip != null)
                && (port > 0)
                && (onConnectCallback != null)
                && (onReceiveCallback != null)
                && (onSendCallback != null);
    }
}
