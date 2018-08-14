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

/**
 * The type Ble adapter.
 */
public class BleAdapter extends ArrayAdapter<BleDevice> {
    private int resourceId;

    /**
     * Instantiates a new Ble adapter.
     *
     * @param context  the context
     * @param resource the resource
     * @param objects  the objects
     * @return
     * @decs
     * @author H
     * @time 2018 /8/14 15:10
     */
    public BleAdapter(@NonNull Context context, int resource, @NonNull List<BleDevice> objects) {
        super(context, resource, objects);
        this.resourceId = resource;
    }

    @NonNull
    @Override
    /**
     *@decs
     *@author H
     *@time 2018/8/14 15:10
     * @param [position, convertView, parent]
     * @return android.view.View
     */
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

    /**
     * The type View holder.
     *
     * @param
     * @author H
     * @decs
     * @time 2018 /8/14 15:10
     * @return
     */
    class ViewHolder{
        TextView bleDeviceName;
        TextView bleDeviceAddress;
        TextView bleDeviceBondState;
    }
}
