package com.gec.bluetoothcarlink;

import com.gec.bluetoothcarlink.adapter.DeviceListAdapter;
import com.gec.bluetoothcarlink.util.Logger;
import com.gec.bluetoothcarlink.widget.WhorlView;

import android.app.Activity;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Message;
import android.os.Process;
import android.view.View;
import android.widget.ListView;

public class DeviceScanActivity extends ListActivity {
	private static final String TAG = DeviceScanActivity.class.getCanonicalName();

	private static final int REQUEST_ENABLE = 0x00;

	private static final int WHAT_CANCEL_DISCOVERY = 0x01;
	private static final int WHAT_DEVICE_UPDATE = 0x02;

	private static final int SCAN_PERIOD = 30 * 1000;

	private DeviceListAdapter mLeDeviceListAdapter = null;
	private BluetoothAdapter mBluetoothAdapter = null;
	private WhorlView mWhorlView = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_device_scan);

		init();
	}

	@Override
	protected void onStart() {
		super.onStart();

		mLeDeviceListAdapter = new DeviceListAdapter(this);
		setListAdapter(mLeDeviceListAdapter);

		scanDevice(true);
		mWhorlView.setVisibility(View.VISIBLE);
	}

	@Override
	protected void onPause() {
		scanDevice(false);

		super.onPause();
	}

	@Override
	protected void onDestroy() {
		unregReceiver();

		super.onDestroy();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_ENABLE && resultCode == Activity.RESULT_CANCELED) {
			finish();
			Process.killProcess(Process.myPid());
		}
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);

		BluetoothDevice device = mLeDeviceListAdapter.getDevice(position);
		if (device == null) {
			return;
		}
		Intent intent = new Intent(this, MainActivity.class);
		Bundle bundle = new Bundle();
		bundle.putParcelable("device", device);
		intent.putExtras(bundle);

		scanDevice(false);
		startActivity(intent);
		finish();
	}

	/**
	 * 消息处理者
	 */
	private Handler mHandler = new Handler(new Callback() {

		@Override
		public boolean handleMessage(Message msg) {
			switch (msg.what) {

			case WHAT_DEVICE_UPDATE:
				mLeDeviceListAdapter.addDevice((BluetoothDevice) msg.obj);
				mLeDeviceListAdapter.notifyDataSetChanged();
				break;

			case WHAT_CANCEL_DISCOVERY:
				mWhorlView.setVisibility(View.GONE);
				break;

			default:
				break;
			}
			return false;
		}
	});

	/**
	 * 初始化
	 */
	private void init() {
		mWhorlView = (WhorlView) findViewById(R.id.whorl_view);
		mWhorlView.start();

		// 初始化本地蓝牙设备
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

		// 检测蓝牙设备是否开启，如果未开启，发起Intent并回调
		if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
			Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableIntent, REQUEST_ENABLE);
		}

		registerReceiver();
	}

	/**
	 * 是否扫描蓝牙设备
	 * 
	 * @param enable
	 */
	private void scanDevice(boolean enable) {
		if (enable) {
			Logger.d(TAG, "[1]--> startDiscovery()");
			mBluetoothAdapter.startDiscovery();

			mHandler.postDelayed(new Runnable() {

				@Override
				public void run() {
					mBluetoothAdapter.cancelDiscovery();
					Logger.d(TAG, "[2]--> cancelDiscovery()");
					mHandler.sendEmptyMessage(WHAT_CANCEL_DISCOVERY);
				}
			}, SCAN_PERIOD);
		} else {
			Logger.d(TAG, "[3]--> cancelDiscovery()");
			mBluetoothAdapter.cancelDiscovery();
		}
	}

	/**
	 * 注册广播接收器
	 */
	private void registerReceiver() {
		registerReceiver(mReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
	}

	/**
	 * 注销广播接收器
	 */
	private void unregReceiver() {
		if (mReceiver != null) {
			unregisterReceiver(mReceiver);
		}
	}

	/**
	 * 广播接收器
	 */
	private BroadcastReceiver mReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (BluetoothDevice.ACTION_FOUND == action) {
				BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				Logger.d(TAG, "[4] --> " + device.getName() + "------" + device.getAddress());
				if (device != null) {
					mHandler.sendMessage(mHandler.obtainMessage(WHAT_DEVICE_UPDATE, device));
				}
			}
		}
	};
}
