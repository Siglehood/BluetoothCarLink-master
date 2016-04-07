package com.gec.bluetoothcarlink.adapter;

import java.util.ArrayList;
import java.util.List;

import com.gec.bluetoothcarlink.R;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class DeviceListAdapter extends BaseAdapter {
	private List<BluetoothDevice> mDevices = null;
	private LayoutInflater mLayoutInflater = null;

	public DeviceListAdapter(Context context) {
		mDevices = new ArrayList<BluetoothDevice>();
		mLayoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	public void addDevice(BluetoothDevice device) {
		if (!mDevices.contains(device)) {
			mDevices.add(device);
		}
	}

	public BluetoothDevice getDevice(int position) {
		return mDevices.get(position);
	}

	public void clear() {
		mDevices.clear();
	}

	@Override
	public int getCount() {
		return mDevices.size();
	}

	@Override
	public Object getItem(int i) {
		return mDevices.get(i);
	}

	@Override
	public long getItemId(int i) {
		return i;
	}

	@Override
	public View getView(int i, View view, ViewGroup viewGroup) {
		ViewHolder viewHolder = null;
		if (view == null) {
			view = mLayoutInflater.inflate(R.layout.list_item, viewGroup, false);
			viewHolder = new ViewHolder();
			viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
			viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
			view.setTag(viewHolder);
		} else {
			viewHolder = (ViewHolder) view.getTag();
		}

		String deviceName = mDevices.get(i).getName();
		if (!TextUtils.isEmpty(deviceName)) {
			viewHolder.deviceName.setText(deviceName);
		} else {
			viewHolder.deviceName.setText("未知设备");
		}

		viewHolder.deviceAddress.setText(mDevices.get(i).getAddress());

		return view;
	}

	private static class ViewHolder {
		private TextView deviceName = null;
		private TextView deviceAddress = null;
	}
}
