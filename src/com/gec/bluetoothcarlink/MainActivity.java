package com.gec.bluetoothcarlink;

import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

import com.gec.bluetoothcarlink.util.Logger;
import com.gec.bluetoothcarlink.util.Toaster;
import com.gec.bluetoothcarlink.widget.Direction;
import com.gec.bluetoothcarlink.widget.Rudder;
import com.gec.bluetoothcarlink.widget.Rudder.RudderListener;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

public class MainActivity extends Activity implements RudderListener {
	private static final String TAG = MainActivity.class.getSimpleName();
	private static final String SPP_UUID = "00001101-0000-1000-8000-00805F9B34FB";

	// 协议常量
	private static byte[] data = new byte[] { 0x00, 0x0D, 0x0A };

	// 提示内容
	private TextView mTextView = null;
	// 虚拟摇杆
	private Rudder mRudder = null;

	// 加载动画
	private ImageView mWheelView = null;
	private Animation mAnimation = null;

	private BluetoothDevice mBluetoothDevice = null;
	private BluetoothSocket mSocket = null;
	private OutputStream mOutS = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.activity_main);

		initUi();
	}

	@Override
	protected void onStart() {
		super.onStart();

		initDevice();
	}

	private void initDevice() {
		Bundle bundle = getIntent().getExtras();
		if (bundle != null) {
			mBluetoothDevice = bundle.getParcelable("device");
			if (mBluetoothDevice != null) {
				Logger.d(TAG, mBluetoothDevice.getName() + "------" + mBluetoothDevice.getAddress());
				new Thread(connectRun).start();
			}
		}
	}

	@Override
	protected void onStop() {
		close();

		super.onStop();
	}

	private void initUi() {
		mTextView = (TextView) findViewById(R.id.text_view);
		mRudder = (Rudder) findViewById(R.id.rudder);
		mWheelView = (ImageView) findViewById(R.id.wheel_view);

		mRudder.setOnRudderListener(this);

		// 加载动画
		mAnimation = AnimationUtils.loadAnimation(this, R.anim.rotate);

		// 设置匀速旋转速率
		mAnimation.setInterpolator(new LinearInterpolator());
	}

	/**
	 * 连接蓝牙
	 */
	private Runnable connectRun = new Runnable() {

		@Override
		public void run() {
			connect();
		}
	};

	private void connect() {
		UUID uuid = UUID.fromString(SPP_UUID);

		try {
			mSocket = mBluetoothDevice.createRfcommSocketToServiceRecord(uuid);
		} catch (IOException e) {
			if (mSocket != null) {
				try {
					mSocket.close();
				} catch (IOException e1) {
					Logger.e(TAG, e1.getMessage());
				}
			}
		}

		try {
			mSocket.connect();
		} catch (IOException e) {
			if (mSocket != null) {
				try {
					mSocket.close();
				} catch (IOException e1) {
					Logger.e(TAG, e1.getMessage());
				}
			}
		}

		try {
			mOutS = mSocket.getOutputStream();
		} catch (IOException e) {
			if (mOutS != null) {
				try {
					mOutS.close();
				} catch (IOException e1) {
					Logger.e(TAG, e.getMessage());
				}
			}

			if (mSocket != null) {
				try {
					mSocket.close();
				} catch (IOException e1) {
					Logger.e(TAG, e.getMessage());
				}
			}
		}
	}

	/**
	 * 关闭Socket
	 */
	private void close() {
		if (mOutS != null) {
			try {
				mOutS.close();
			} catch (IOException e) {
				Logger.e(TAG, e.getMessage());
			}
		}

		if (mSocket != null) {
			try {
				mSocket.close();
			} catch (IOException e) {
				Logger.e(TAG, e.getMessage());
			}
		}
	}

	/**
	 * 发送数据
	 * 
	 * @param data
	 */
	private void writeStream(byte[] data) {
		try {
			if (mOutS != null) {
				mOutS.write(data);
				mOutS.flush();
			}
		} catch (IOException e) {
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					Toaster.shortToastShow(MainActivity.this, "连接超时，被服务器君抛弃了::>_<::");
					// 结束程序
					MainActivity.this.finish();
				}
			});
		}
	}

	@Override
	public void onSteeringWheelChanged(int action, Direction direction) {
		if (action == Rudder.ACTION_RUDDER) {
			switch (direction) {

			case LEFT_DOWN_DIR:
				Logger.d(TAG, "[1] --> 左下拐...");
				mTextView.setText("左下拐...");
				data[0] = 0x07;
				break;

			case LEFT_DIR:
				Logger.d(TAG, "[2] --> 左拐...");
				mTextView.setText("左拐...");
				data[0] = 0x03;
				break;

			case LEFT_UP_DIR:
				Logger.d(TAG, "[3] --> 左上拐...");
				mTextView.setText("左上拐...");
				data[0] = 0x05;
				break;

			case UP_DIR:
				Logger.d(TAG, "[4] --> 向前突进...");
				mTextView.setText("向前突进...");
				data[0] = 0x01;
				break;

			case RIGHT_UP_DIR:
				Logger.d(TAG, "[5] --> 右上拐...");
				mTextView.setText("右上拐...");
				data[0] = 0x06;
				break;

			case RIGHT_DIR:
				Logger.d(TAG, "[6] --> 右拐...");
				mTextView.setText("右拐...");
				data[0] = 0x04;
				break;

			case RIGHT_DOWN_DIR:
				Logger.d(TAG, "[7] --> 右下拐...");
				mTextView.setText("右下拐...");
				data[0] = 0x08;
				break;

			case DOWN_DIR:
				Logger.d(TAG, "[8] --> 向后撤退...");
				mTextView.setText("向后撤退...");
				data[0] = 0x02;
				break;

			default:
				break;
			}
			writeStream(data);
		}
	}

	@Override
	public void onAnimated(boolean isAnim) {
		if (isAnim) {
			mWheelView.startAnimation(mAnimation);
		} else {
			mWheelView.clearAnimation();
		}
	}
}
