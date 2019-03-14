package com.example.contactsdemo;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;


public class MyAdapter extends ArrayAdapter<ContactItem> {//滑动列表的适配器

    public MyAdapter(Context context, int resource, List<ContactItem> list){
        super(context,resource,list);
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        ContactItem contactItem=getItem(position);
        ViewHolder viewHolder=null;
        if (convertView==null){
            convertView= LayoutInflater.from(parent.getContext()).inflate(R.layout.item_layout,parent,false);
            viewHolder=new ViewHolder();
            viewHolder.headImage=(ImageView) convertView.findViewById(R.id.headImage);
            viewHolder.nameText=(TextView) convertView.findViewById(R.id.name);
            viewHolder.numberText=(TextView) convertView.findViewById(R.id.number);
            viewHolder.emailText=(TextView) convertView.findViewById(R.id.emailText);

            convertView.setTag(viewHolder);
        }
        else {
            viewHolder=(ViewHolder)convertView.getTag();
        }
        viewHolder.headImage.setImageBitmap(contactItem.headImage);
        viewHolder.nameText.setText(contactItem.name);
        viewHolder.numberText.setText(contactItem.number);
        viewHolder.emailText.setText(contactItem.email);
        return convertView;
    }

    class ViewHolder{
        ImageView headImage;
        TextView nameText;
        TextView numberText;
        TextView emailText;
    }
}
