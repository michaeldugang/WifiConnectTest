package com.mc.wifitest;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.mc.wifitest.utils.WifiUtils;
/**
 * 连接指定的WIFI
 *
 */
public class WifiConnectActivity extends Activity implements OnClickListener {
    private Button connect_btn;
    private TextView wifi_ssid_tv;
    private EditText wifi_pwd_tv;
    private static WifiUtils mUtils;
    // wifi之ssid
    private String ssid;
    private String pwd;
    private ProgressDialog progressdlg = null;
    private static final String TAG = "WifiConnectActivity";
    private BroadcastReceiver mReceiver;

    static final int MSG_TIMEOUT = 1;
    static final int CONNECTION_TIMEOUT = 15000;




    private final Handler mConHandler = new Handler() {
        public void handleMessage(Message msg) {
            Log.d(TAG, "handleMessage: msg " + msg);
            if (mUtils.isNetworkConnected()) {
                mHandler.sendEmptyMessage(0);
            } else {
                mHandler.sendEmptyMessage(1);
            }
        }
    };

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    showToast("WIFI连接成功");
                    finish();
                    break;
                case 1:
                    showToast("WIFI连接失败");
                    break;

            }
            progressDismiss();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: ");
        setContentView(R.layout.activity_connect);
        mUtils = new WifiUtils(this);
        findViews();
        setLiteners();
        initDatas();


        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
        mReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                if (WifiManager.SUPPLICANT_STATE_CHANGED_ACTION.equals(intent.getAction())) {
                    SupplicantState state = intent.getParcelableExtra(
                            WifiManager.EXTRA_NEW_STATE);

                    Log.d(TAG, "Got supplicant state: " + state.name());

                    switch (state) {
                        case ASSOCIATING:
                            //mWasAssociating = true;
                            break;
                        case ASSOCIATED:
                            //mWasAssociated = true;
                            break;
                        case COMPLETED:
                            // this just means the supplicant has connected, now
                            // we wait for the rest of the framework to catch up
                            break;
                        case DISCONNECTED:
                        case DORMANT:
                            /*if (mWasAssociated || mWasHandshaking) {
                                notifyListener(mWasHandshaking ? StateMachine.RESULT_BAD_AUTH
                                        : StateMachine.RESULT_UNKNOWN_ERROR);
                            }*/
                            break;
                        case INTERFACE_DISABLED:
                        case UNINITIALIZED:
                            //notifyListener(StateMachine.RESULT_UNKNOWN_ERROR);
                            break;
                        case FOUR_WAY_HANDSHAKE:
                        case GROUP_HANDSHAKE:
                            //mWasHandshaking = true;
                            break;
                        case INACTIVE:
                            /*if (mWasAssociating && !mWasAssociated) {
                                // If we go inactive after 'associating' without ever having
                                // been 'associated', the AP(s) must have rejected us.
                                notifyListener(StateMachine.RESULT_REJECTED_BY_AP);
                                break;
                            }*/
                            break;
                        case INVALID:
                            break;
                        case SCANNING:
                            break;
                        default:
                            return;
                    }
                    mHandler.removeMessages(MSG_TIMEOUT);
                    mHandler.sendEmptyMessageDelayed(MSG_TIMEOUT, CONNECTION_TIMEOUT);
                }
            }

        };
    }

    /**
     * init dialog
     */
    private void progressDialog() {
        progressdlg = new ProgressDialog(this);
        progressdlg.setCanceledOnTouchOutside(false);
        progressdlg.setCancelable(false);
        progressdlg.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressdlg.setMessage(getString(R.string.wait_moment));
        progressdlg.show();
    }

    /**
     * dissmiss dialog
     */
    private void progressDismiss() {
        if (progressdlg != null) {
            progressdlg.dismiss();
        }
    }

    private void initDatas() {
        ssid = getIntent().getStringExtra("ssid");
        if (!TextUtils.isEmpty(ssid)) {
            ssid = ssid.replace("\"", "");
        }
        this.wifi_ssid_tv.setText(ssid);
    }

    private void findViews() {
        this.connect_btn = (Button) findViewById(R.id.connect_btn);
        this.wifi_ssid_tv = (TextView) findViewById(R.id.wifi_ssid_tv);
        this.wifi_pwd_tv = (EditText) findViewById(R.id.wifi_pwd_tv);
    }

    private void setLiteners() {
        connect_btn.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.connect_btn) {// 下一步操作
            pwd = wifi_pwd_tv.getText().toString();
            // 判断密码输入情况
            if (TextUtils.isEmpty(pwd)) {
                Toast.makeText(this, "请输入wifi密码", Toast.LENGTH_SHORT).show();
                return;
            }
            progressDialog();
            // 在子线程中处理各种业务
            dealWithConnect(ssid, pwd);
        }
    }

    private void dealWithConnect(final String ssid, final String pwd) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();
                // 检验密码输入是否正确
                boolean pwdSucess = mUtils.connectWifiTest(ssid, pwd);
                mConHandler.sendEmptyMessageDelayed(MSG_TIMEOUT, CONNECTION_TIMEOUT);
/*                try {
                    Thread.sleep(4000);
                } catch (Exception e) {
                    e.printStackTrace();
                }*/
            }
        }).start();
    }

    private void showToast(String str) {
        Toast.makeText(WifiConnectActivity.this, str, Toast.LENGTH_SHORT).show();
    }
}
