package com.example.contactsdemo;

import android.graphics.Bitmap;

public class ContactItem {//数据实体类
    int id;
    String name;
    String number;
    Bitmap headImage;
    String email;

    public ContactItem(int id,Bitmap headImage,String name,String number,String email){
        this.id=id;
        this.headImage=headImage;
        this.name=name;
        this.number=number;
        this.email=email;
    }
}
