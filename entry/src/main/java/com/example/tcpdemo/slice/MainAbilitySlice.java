package com.example.tcpdemo.slice;

import com.example.tcpdemo.ResourceTable;
import com.example.tcpdemo.TCPClient;
import ohos.aafwk.ability.AbilitySlice;
import ohos.aafwk.content.Intent;
import ohos.agp.components.Button;
import ohos.agp.components.Component;
import ohos.agp.components.Text;
import ohos.agp.components.TextField;
import ohos.agp.window.dialog.ToastDialog;
import ohos.hiviewdfx.HiLog;
import ohos.hiviewdfx.HiLogLabel;

import java.net.Socket;

public class MainAbilitySlice extends AbilitySlice implements Component.ClickedListener {

    HiLogLabel LABEL = new HiLogLabel(HiLog.LOG_APP, 0x00202, MainAbilitySlice.class.getSimpleName());

    private final int STATE_DISCONNECTED = 1;
    private final int STATE_CONNECTING = 2;
    private final int STATE_CONNECTED = 3;

    private int mSocketConnectState = STATE_DISCONNECTED;

    private final String TAG = "TcpClientActivity";
    private Button mBtnConnect, mBtnSend;
    private TextField mFieldIp, mFieldPort, mFieldMsg;
    private Text mClientState, mTvReceive;
    public Socket mSocket;

    private final int DEFAULT_PORT = 8086;
    private String mIpAddress;  //服务端ip地址
    private int mClientPort; //端口,默认为8086，可以进行设置
    private static final String IP_ADDRESS = "ip_address";
    private static final String CLIENT_PORT = "client_port";
    private static final String CLIENT_MESSAGETXT = "client_msgtxt";

    TCPClient tcpClient = null;

    @Override
    public void onStart(Intent intent) {
        super.onStart(intent);
        super.setUIContent(ResourceTable.Layout_ability_main);

        mBtnConnect = (Button) findComponentById(ResourceTable.Id_bt_client_connect);
        mBtnSend = (Button) findComponentById(ResourceTable.Id_bt_client_send);
        mFieldIp = (TextField) findComponentById(ResourceTable.Id_text_field_ip);
        mFieldPort = (TextField) findComponentById(ResourceTable.Id_text_field_port);
        mFieldMsg = (TextField) findComponentById(ResourceTable.Id_client_sendMsg);
        mClientState = (Text) findComponentById(ResourceTable.Id_client_state);
        mTvReceive = (Text) findComponentById(ResourceTable.Id_client_receive);

        mFieldPort.setText(DEFAULT_PORT);

        mBtnConnect.setClickedListener(this);
        mBtnSend.setClickedListener(this);
    }

    @Override
    public void onActive() {
        super.onActive();

        if (mSocketConnectState == STATE_CONNECTED) {
            mBtnConnect.setText("断开");
            mClientState.setText("已连接");
        } else if (mSocketConnectState == STATE_DISCONNECTED) {
            mBtnConnect.setText("连接");
            mClientState.setText("已断开连接");
        } else if (mSocketConnectState == STATE_CONNECTING) {
            mClientState.setText("正在连接");
            mClientState.setText("已连接");
        }
    }

    @Override
    public void onForeground(Intent intent) {
        super.onForeground(intent);
    }

    @Override
    protected void onStop() {
        super.onStop();
        tcpClient.disconnect();
    }

    @Override
    public void onClick(Component component) {
        switch (component.getId()) {
            case ResourceTable.Id_bt_client_connect:
                if (mSocketConnectState == STATE_CONNECTED) {
                    tcpClient.disconnect();
                } else {
                    startConnect();
                }
                break;
            case ResourceTable.Id_bt_client_send:
                sendTxt();
                break;
            default:
                break;
        }
    }

    private void startConnect() {
        HiLog.info(LABEL, "startConnect");
        mIpAddress = mFieldIp.getText();
        String port = mFieldPort.getText();
        if (port != null && !port.isEmpty()) {
            mClientPort = Integer.parseInt(port);
        }

        if (mIpAddress == null || mIpAddress.length() == 0) {
            showToast("请设置ip地址");
            return;
        }

        initData();
        tcpClient.connect();
    }

    private void sendTxt() {
        String str = mFieldMsg.getText().toString();
        if (str.length() == 0)
            return;

        tcpClient.sendData(str.getBytes());
    }

    private void initData() {
        tcpClient = TCPClient.getInstance().init(mIpAddress, mClientPort, 2000)
                .setConnectCallback(new TCPClient.OnConnectCallback() {
                    @Override
                    public void onConnectSuccess() {
                        getUITaskDispatcher().asyncDispatch(() -> {
                            mClientState.setText("已连接");
                            mBtnConnect.setText("断开");
                        });
                    }

                    @Override
                    public void onConnectFailure() {
                        HiLog.info(LABEL, "onnected failed!");
                        getUITaskDispatcher().asyncDispatch(() -> {
                            mSocketConnectState = STATE_DISCONNECTED;
                            mBtnConnect.setText("连接");
                            mClientState.setText("连接失败");
                        });
                    }
                })
                .setReceiveDataCallback(new TCPClient.OnReceiveCallback() {
                    @Override
                    public void onReceiveSuccess(String recData) {
                        getUITaskDispatcher().asyncDispatch(() -> {
                            mClientState.setText("data coming!!!");
                            String text = mTvReceive.getText().toString() + "\r\n" + recData;
                            mTvReceive.setText(text);
                        });
                    }

                    @Override
                    public void onReceiveFailure() {
                        getUITaskDispatcher().asyncDispatch(() -> mClientState.setText("read data failed"));
                    }

                    @Override
                    public void onDisconnected() {
                        getUITaskDispatcher().asyncDispatch(() -> {
                            mClientState.setText("已断开连接");
                            mBtnConnect.setText("连接");
                            mSocketConnectState = STATE_DISCONNECTED;
                            tcpClient.disconnect();
                        });
                    }
                })
                .setSendDataCallback(new TCPClient.OnSendCallback() {
                    @Override
                    public void onSendSuccess() {
                        getUITaskDispatcher().asyncDispatch(() -> mClientState.setText("write data success"));
                    }

                    @Override
                    public void onSendFailure() {
                        getUITaskDispatcher().asyncDispatch(() -> mClientState.setText("write data failure"));
                    }
                });
    }

    private void showToast(String message) {
        if (message == null || message.isEmpty()) {
            return;
        }
        new ToastDialog(getContext())
                .setText(message)
                .show();
    }
}
