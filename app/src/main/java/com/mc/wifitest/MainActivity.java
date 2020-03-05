package com.mc.wifitest;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.nfc.Tag;
import android.os.Bundle;

import java.util.List;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.mc.wifitest.utils.WifiUtils;

/**
 * Search WIFI and show in ListView
 *
 */
public class MainActivity extends Activity implements OnClickListener,
        OnItemClickListener {


    private final String TAG = "WifiTest";
    private Button search_btn;
    private ListView wifi_lv;
    private WifiUtils mUtils;
    private List<String> result;
    private ProgressDialog progressdlg = null;
    private ProgressBar progressBar = null;
    private int progressStatus = 0;
    private IntentFilter mWifiStateFilter;
    private WifiManager mWifiManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        mUtils = new WifiUtils(this);
        findViews();
        setLiteners();

        mWifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
        mWifiStateFilter = new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION);
        mWifiStateFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        mWifiStateFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        mWifiStateFilter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
        mWifiStateFilter.addAction(WifiManager.RSSI_CHANGED_ACTION);
        mWifiStateFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        registerReceiver(mWifiBroadcastReceiver, mWifiStateFilter);
    }

    private void findViews() {
        this.search_btn = (Button) findViewById(R.id.search_btn);
        this.wifi_lv = (ListView) findViewById(R.id.wifi_lv);
    }

    private void setLiteners() {
        search_btn.setOnClickListener(this);
        wifi_lv.setOnItemClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.search_btn) {
            getWifi();
            showDialog();
        }
    }
    // call this method only if you are on 6.0 and up, otherwise call doGetWifi()
    private void getWifi() {
        if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, 0x12345);
            Log.d(TAG, "send request permission.");
        } else {
            Log.d(TAG, "start scan cause permission granted");

            boolean ret = mWifiManager.startScan();
            Log.d(TAG, "11startScan ret = " + ret);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == 0x12345) {
            for (int grantResult : grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
            }
            boolean ret = mWifiManager.startScan();
            Log.d(TAG, "22startScan ret = " + ret);
        }
    }

    /**
     * init dialog and show
     */
    private void showDialog() {
        Log.d(TAG, "showDialog: ");
/*        progressdlg = new ProgressDialog(MainActivity.this);
        //progressdlg.setCanceledOnTouchOutside(false);
        progressdlg.setCancelable(false);
        progressdlg.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressdlg.setMessage(getString(R.string.wait_moment));

        progressdlg.show();*/

        new Thread(new Runnable() {
            @Override
            public void run() {
                while(progressStatus < 100) {
                    progressStatus ++;

                    progressBar.setProgress(progressStatus);
                    Log.d(TAG, "run, progressStatus is " + progressStatus);
                    try{
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();

    }

    /**
     * dismiss dialog
     */
    //private void progressDismiss() {
    //    if (progressdlg != null) {
    //        progressdlg.dismiss();
    //    }
    //}

    private BroadcastReceiver mWifiBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "onReceive: " + intent);
            if (intent.getAction().equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                new MyAsyncTask().execute();
            }
        }
    };
    class MyAsyncTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... arg0) {
            //扫描附近WIFI信息
            Log.d(TAG, "doInBackground: ");
            result = mUtils.getScanWifiResult();
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            Log.d(TAG, "onPostExecute: ");
            //progressDismiss();
            initListViewData();
        }
    }

    private void initListViewData() {
        Log.d(TAG, "initListViewData, result: " + result);
        if (null != result && result.size() > 0) {
            wifi_lv.setAdapter(new ArrayAdapter<String>(
                    getApplicationContext(), R.layout.wifi_list_item,
                    R.id.ssid, result));
        } else {
            wifi_lv.setEmptyView(findViewById(R.layout.list_empty));
        }
    }

    @Override
    public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
        TextView tv = (TextView) arg1.findViewById(R.id.ssid);
        Log.d(TAG, "onItemClick: ");
        if (!TextUtils.isEmpty(tv.getText().toString())) {
            Intent in = new Intent(MainActivity.this, WifiConnectActivity.class);
            in.putExtra("ssid", tv.getText().toString());
            startActivity(in);
        }
    }
}
