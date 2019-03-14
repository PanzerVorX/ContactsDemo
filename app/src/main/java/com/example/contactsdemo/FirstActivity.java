package com.example.contactsdemo;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;

public class FirstActivity extends AppCompatActivity {

    ListView listView;
    MyAdapter adapter;
    EditText find;
    Button insert;
    public static ContactItem item;//选择的子项
    public static boolean res;//添加/更新判断变量
    List<ContactItem>contactItemList=new ArrayList<>();//适配器的数据链表

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.first_layout);

        //获取控件
        find=(EditText)findViewById(R.id.find);
        insert=(Button)findViewById(R.id.insert);
        listView=(ListView) findViewById(R.id.list);

        //适配列表
        adapter=new MyAdapter(this,R.layout.item_layout,contactItemList);
        listView.setAdapter(adapter);//先用适配器绑定空数据链表对滑动列表适配，在数据链表变动时刷新列表

        //注册监听器：输入框、添加按钮、滑动列表的长按/点击监听器

        //给文本输入框注册文本变化监听器（实时查询功能）
        find.addTextChangedListener(new inputListener());

        //添加按钮的点击监听器（跳转至添加/更新活动）
        insert.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                res=false;//赋值添加/更新判断变量
                Intent intent=new Intent(FirstActivity.this,SecondActivity.class);
                startActivity(intent);
            }
        });

        //滑动列表的长按监听器（长按拨号功能）
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                item=contactItemList.get(position);//根据位置获取子项对应的数据实体
                AlertDialog.Builder alert=new AlertDialog.Builder(FirstActivity.this);
                alert.setTitle("呼叫该联系人?");
                alert.setCancelable(true);
                alert.setNeutralButton("拨号", new DialogInterface.OnClickListener() {//提示框按钮的响应方法（拨号）

                    public void onClick(DialogInterface dialog, int which) {
                        if (!item.number.equals("")){//判断子项对应的联系人是否设置了手机号码
                            if (ContextCompat.checkSelfPermission(FirstActivity.this,Manifest.permission.CALL_PHONE)!=PackageManager.PERMISSION_GRANTED){//判断已授权拨号权限
                                ActivityCompat.requestPermissions(FirstActivity.this,new String[]{Manifest.permission.CALL_PHONE},2);//若未授权申请拨号权限
                            }
                            else {
                                call();//若已授权，进行拨号动作
                            }
                        }
                        else{
                            Toast.makeText(FirstActivity.this,"该联系人没有手机号码",Toast.LENGTH_SHORT).show();//提示
                        }
                    }
                });
                alert.show();
                return false;
            }
        });

        //滑动列表的点击监听器（更新/删除选项功能）
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                item=contactItemList.get(position);//根据位置获取子项对应的数据实体（从其联系人主键属性确定指定操作的联系人）
                AlertDialog.Builder alert=new AlertDialog.Builder(FirstActivity.this);
                alert.setTitle("更新/删除?");
                alert.setCancelable(true);

                alert.setNeutralButton("更新", new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {
                        res=true;//赋值添加/更新判断变量
                        Intent intent=new Intent(FirstActivity.this,SecondActivity.class);
                        startActivity(intent);//跳转至添加/更新活动
                    }
                });
                alert.setNegativeButton("删除", new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {
                        getContentResolver().delete(Uri.parse("content://com.android.contacts/raw_contacts"),"_id=?",new String[]{Integer.toString(item.id)});//子项对应的数据实体的联系人主键属性删除对应的raw_contacts里的联系人
                        readContact();//刷新数据链表
                        adapter.notifyDataSetChanged();//刷新滑动列表
                    }
                });
                alert.setPositiveButton("关闭",null);
                alert.show();
            }
        });
    }

    //文本变化监听器（实时查询）
    class inputListener implements TextWatcher{

        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        public void onTextChanged(CharSequence s, int start, int before, int count) {//每次文本发生变化后（以一个字符为单位的变化），就执行一次onTextChanged()方法
            readContact();//刷新数据链表
            adapter.notifyDataSetChanged();//刷新滑动列表
        }

        public void afterTextChanged(Editable s) {

        }
    }

    //拨号活动
    public void call(){
        try
        {
            Intent intent=new Intent(Intent.ACTION_CALL);//拨号的action
            intent.setData(Uri.parse("tel:"+item.number));
            startActivity(intent);
        }
        catch (SecurityException e){//拨号活动声明了SecurityException
            e.printStackTrace();
        }
    }

    //每次获得焦点都刷新显示数据链表（每次从其他活动转到显示列表的活动都需刷新列表显示的数据）
    protected void onResume() {
        super.onResume();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)!= PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_CONTACTS)!= PackageManager.PERMISSION_GRANTED){//判断读/写联系人的权限
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.READ_CONTACTS,Manifest.permission.WRITE_CONTACTS},1);//申请权限
        }
        else {
            readContact();//刷新数据链表
            adapter.notifyDataSetChanged();//刷新滑动列表
        }
    }
    //将查询的联系人信息显示在列表：先在data表查询符合约束条件的信息记录，遍历结果集取出每条记录的联系人ID（raw_contact_id），将联系人ID存入链表（需去重复处理），遍历链表时根据联系人ID在data表查询每个联系人的信息集（每个联系人信息集对应数据实体类）
    /*
            ·将查询的每个联系人的信息显示在列表（具体步骤）
              ①先以关键字为约束对data表模糊查询
              ②遍历符合约束条件的信息结果集合获取raw_contact_id，将不重复的raw_contact_id存入raw_contact_id链表（联系人ID链表）
              ③遍历联系人ID链表，将联系人ID作为约束条件在data表中查找指定联系人的信息集
              ④遍历对应联系人的信息集，取出对应类型的信息值作为数据实体类的构造参数构造数据对象
              ⑤数据链表添加对象作为元素
     */
    //联系人ID链表去重复：每次对raw_contact_id链表添加元素前遍历链表，比对是否有重复的联系人ID，若有则不添加
    public void readContact(){
        String selectInfo=find.getText().toString();//获取查关键字
        contactItemList.clear();//清空数据链表
        List<String>rawContactIdList= new ArrayList<>();//构造联系人ID链表
        //先以关键字为约束对data表模糊查询
        Cursor dataCursor=getContentResolver().query(Uri.parse("content://com.android.contacts/data"),null,"mimetype in(?,?) and data1 like"+"'%"+selectInfo+"%'",new String[]{"vnd.android.cursor.item/name","vnd.android.cursor.item/phone_v2"},null);
        if (dataCursor!=null){
            while (dataCursor.moveToNext()){//遍历符合约束条件的信息结果集合获取raw_contact_id，将不重复的raw_contact_id存入raw_contact_id链表（联系人ID链表）
                int rawContactId=dataCursor.getInt(dataCursor.getColumnIndex("raw_contact_id"));
                String rawContactIdStr=Integer.toString(rawContactId);
                Boolean res=true;//raw_contact_id是否重复判断变量（是否为同个联系人ID的判断变量）
                for (int i=0;i<rawContactIdList.size();i++){//联系人ID链表去重复：每次对raw_contact_id链表添加元素前遍历链表，比对是否有重复的联系人ID，若有则不添加
                    if (rawContactIdList.get(i)==rawContactIdStr){
                        res=false;
                        break;
                    }
                    res=true;
                }
                if (res){
                    rawContactIdList.add(rawContactIdStr);
                }
            }
        }
        dataCursor.close();
        for (int i=0;i<rawContactIdList.size();i++){//遍历联系人ID链表，每个循环中将联系人ID作为约束条件在data表中查找指定联系人的信息集
            Cursor dataCursor2=getContentResolver().query(Uri.parse("content://com.android.contacts/data"),null,"raw_contact_id=?",new String[]{rawContactIdList.get(i)},null);//获取每个联系人的信息集
            String name=new String(),number=new String(),email=new String();//创建并初始化信息属性 姓名/手机号码/邮件
            byte[]bytes=null;//data表的data15字段存放联系人头像图片信息（以二进制数组的文本形式存放）
            Bitmap photo=null;//可将图片的二进制数组形式转换为位图
            while (dataCursor2.moveToNext()){//遍历对应联系人的信息记录集，取出信息值作为数据实体类的构造参数构造数据对象
                String data=dataCursor2.getString(dataCursor2.getColumnIndex("data1"));//联系人总姓名/手机号码/邮箱存在data表的data1字段
                String data2=dataCursor2.getString(dataCursor2.getColumnIndex("data2"));//姓名首字段
                String data3=dataCursor2.getString(dataCursor2.getColumnIndex("data3"));//姓名尾字段
                String data5=dataCursor2.getString(dataCursor2.getColumnIndex("data5"));//姓名中字段
                bytes=dataCursor2.getBlob(dataCursor2.getColumnIndex("data15"));//联系人头像存在data15
                String mimetype=dataCursor2.getString(dataCursor2.getColumnIndex("mimetype"));//信息类型
                if (mimetype.equals("vnd.android.cursor.item/photo")){//根据头像图片的二进制数组获取位图
                    if (bytes!=null)//若二进制数组不为空
                        photo= BitmapFactory.decodeByteArray(bytes,0,bytes.length);//根据字节数组获取位图：BitmapFactory的decodeByteArray() 参数①资源数据（字节数组） ②解码的位移量（一般为0） ③解码的数据长度（字节数组长度）
                }
                else if (mimetype.equals("vnd.android.cursor.item/name")){//获取姓名的字符串形式
                    if (data3!=null&&data5!=null){
                        name=data2+" "+data5+" "+data3;//精确显示
                    }
                    else if (data3!=null){
                        name=data2+" "+data3;
                    }
                    else {
                        name=data2;
                    }
                }
                else if(mimetype.equals("vnd.android.cursor.item/phone_v2")){//获取手机号码的字符串形式
                    number=data;
                }
                else if (mimetype.equals("vnd.android.cursor.item/email_v2")){//获取电子邮件的字符串形式
                    email=data;
                }
            }
            dataCursor2.close();
            ContactItem item=new ContactItem(Integer.parseInt(rawContactIdList.get(i)),photo,name,number,email);//将每个联系人的信息集构造为数据实体
            contactItemList.add(item);//数据实体存入数据链表
        }
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {//申请权限后的回调方法
        switch (requestCode){
            case 1:
                if (grantResults.length==2&&grantResults[1]==PackageManager.PERMISSION_GRANTED){//判断读/写联系人权限的授权情况（2个权限）
                    readContact();//若授权则更新数据链表并刷新滑动列表
                    adapter.notifyDataSetChanged();
                }
                else{
                    Toast.makeText(FirstActivity.this,"未授权查看联系人",Toast.LENGTH_SHORT).show();
                    finish();//未授权则退出
                }
                break;
            case 2:
                if (grantResults.length!=0&&grantResults[0]==PackageManager.PERMISSION_GRANTED){//判断拨号权限的授权情况（1个权限）
                    call();//若授权则拨号
                }
                else{
                    Toast.makeText(FirstActivity.this,"未授权拨号",Toast.LENGTH_SHORT).show();
                }
        }
    }
}
