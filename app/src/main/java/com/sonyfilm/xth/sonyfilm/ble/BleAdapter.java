package com.sonyfilm.xth.sonyfilm.ble;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.sonyfilm.xth.sonyfilm.R;

import java.util.List;

public class BleAdapter extends ArrayAdapter<BleDevice> {
    private int resourceId;


    public BleAdapter(@NonNull Context context, int resource, @NonNull List<BleDevice> objects) {
        super(context, resource, objects);
        this.resourceId = resource;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        BleDevice bleDevice = getItem(position);
        View view;
        ViewHolder viewHolder;
        if(convertView == null){
            view = LayoutInflater.from(getContext()).inflate(resourceId,parent,false);
            viewHolder = new ViewHolder();
            viewHolder.bleDeviceName = (TextView)view.findViewById(R.id.ble_device_name);
            viewHolder.bleDeviceAddress = (TextView)view.findViewById(R.id.ble_device_address);
            viewHolder.bleDeviceBondState = (TextView)view.findViewById(R.id.ble_device_bond_state);
            view.setTag(viewHolder);
        }else{
            view = convertView;
            viewHolder = (ViewHolder) view.getTag();
        }
            viewHolder.bleDeviceName.setText(bleDevice.getName());
            viewHolder.bleDeviceAddress.setText(bleDevice.getAddress());
            viewHolder.bleDeviceBondState.setText(bleDevice.getState());
        return view;
    }

    class ViewHolder{
        TextView bleDeviceName;
        TextView bleDeviceAddress;
        TextView bleDeviceBondState;
    }
}
