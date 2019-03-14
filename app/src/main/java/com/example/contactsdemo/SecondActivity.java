package com.example.contactsdemo;

import android.Manifest;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;


public class SecondActivity extends AppCompatActivity implements View.OnClickListener{

    public static final int TAKE_PHTOT=1;
    public static final int CHOOSE_PHOTO=2;
    ImageView mHeadImage;
    EditText mName,mNumber,mEmail;
    Button saveButton,backButton;
    Uri imageUri;//从拍照后存储文件的Uri
    Bitmap headImage;
    //Bitmap headImage=FirstActivity.item.headImage;//获取纯原图
    boolean isChange=false;//图片变动的判断变量（防止更新时未变动的图片被二次压缩）
    String name,number,email;
    byte[] byteArray=null;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.second_layout);

        //获取控件
        mHeadImage=(ImageView)findViewById(R.id.mhead_image);
        mName=(EditText)findViewById(R.id.mname);
        mNumber=(EditText)findViewById(R.id.mnumber);
        mEmail=(EditText)findViewById(R.id.memail);
        saveButton=(Button)findViewById(R.id.save_button);
        backButton=(Button)findViewById(R.id.back_button);

        //注册监听器 头像图片、保存按钮、返回按钮
        mHeadImage.setOnClickListener(this);//给头像图片注册点击监听器是为了选择更改头像
        saveButton.setOnClickListener(this);
        backButton.setOnClickListener(this);
        //从控件实时获取内容，若控件的内容未知，需先设置控件显示指定值进行来确定值，否则不变动控件内容直接从控件获取的内容没有确定值会报错（针对非字符串对象）
        mHeadImage.setImageBitmap(null);//显示头像图片的ImageView控件先显示为空，防止添加模式为变动图片直接从控件获取位图，导致没有确定值（更新模式中控件首先显示子项位图成员，已有确定值）

        //若为更新模式，则显示选择子项的数据
        if (FirstActivity.res){//判断 添加/更新判断变量
            Bitmap headImage=FirstActivity.item.headImage;
            String name=FirstActivity.item.name;
            String number=FirstActivity.item.number;
            String email=FirstActivity.item.email;
            mHeadImage.setImageBitmap(headImage);
            mName.setText(name);
            mNumber.setText(number);
            mEmail.setText(email);
        }
    }

    //点击响应方法
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.mhead_image://头像的点击响应方法
                AlertDialog.Builder alert=new AlertDialog.Builder(this);
                alert.setTitle("选择照片");
                alert.setNeutralButton("拍照", new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {
                        takePhoto();//执行拍照
                    }
                });
                alert.setNegativeButton("相册", new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {
                        if (ContextCompat.checkSelfPermission(SecondActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED){//读取SD卡的权限（相册的照片存于SD卡）
                            ActivityCompat.requestPermissions(SecondActivity.this,new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},1);
                        }
                        else{
                            openAlbum();//打开相册选择
                        }
                    }
                });
                alert.setPositiveButton("删除", new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {
                        mHeadImage.setImageBitmap(null);//使显示头像的ImageView显示空的Bitmap对象
                        isChange=true;
                    }
                });
                alert.show();
                break;

            case R.id.save_button://保存按钮的点击响应方法
               getData();//获取输入的数据
                if (!name.equals("")){
                    if (FirstActivity.res){//添加/更新判断变量
                        update();//更新操作
                        Toast.makeText(SecondActivity.this,"更新联系人成功",Toast.LENGTH_SHORT).show();
                        finish();
                    }
                    else{
                        insert();//添加操作
                        Toast.makeText(SecondActivity.this,"添加联系人成功",Toast.LENGTH_SHORT).show();
                        finish();
                    }
                }
                else{
                    Toast.makeText(SecondActivity.this,"联系人姓名不能为空",Toast.LENGTH_SHORT).show();
                }
                break;

            case R.id.back_button:
                finish();
                break;
        }
    }

        //从控件获取数据
        public void getData(){
            headImage= ((BitmapDrawable)mHeadImage.getDrawable()).getBitmap();//从ImageView获取显示图片的位图：ImageView的getDrawable()获取显示图片的Drawable对象，将Drawable对象转化为BitmapDrawable，在通过BitmapDrawable的getBitmap()获取位图对象
            name=mName.getText().toString();
            number=mNumber.getText().toString();
            email=mEmail.getText().toString();

            //将Bitmap转为byte[]
            if (headImage!=null){//若位图对象不为空，则将位图转对象转化为byte[]
                ByteArrayOutputStream baos = new ByteArrayOutputStream();//创建ByteArrayOutputStream（字节数组输出流）
                headImage.compress(Bitmap.CompressFormat.JPEG, 100, baos);//将Bitmap对象压缩为图片格式存入字节数组输出流（不能对Bitmap空对象压缩）：Bitmap的compress() 参数①图片格式 ②压缩质量 ③字节数组输出流
                byteArray = baos.toByteArray();//从字节数组输出流中取出字节数组：ByteArrayOutputStream的toByteArray()
            }
        }

        public void update(){//更新操作
            ContentValues values=new ContentValues();
            //查询data表中是否存在指定联系人的各种信息类型的记录，若有对应信息类型记录则直接更新，若对应信息类型无记录且控件显示值有效（不为空位图/不为空字符串）则插入指定信息类型的记录
            Cursor phototCursor=getContentResolver().query(Uri.parse("content://com.android.contacts/data"),null,"raw_contact_id=? and mimetype=?",new String[]{Integer.toString(FirstActivity.item.id),"vnd.android.cursor.item/photo"},null);
            Cursor nameCursor=getContentResolver().query(Uri.parse("content://com.android.contacts/data"),null,"raw_contact_id=? and mimetype=?",new String[]{Integer.toString(FirstActivity.item.id),"vnd.android.cursor.item/name"},null);
            Cursor numberCursor=getContentResolver().query(Uri.parse("content://com.android.contacts/data"),null,"raw_contact_id=? and mimetype=?",new String[]{Integer.toString(FirstActivity.item.id),"vnd.android.cursor.item/phone_v2"},null);
            Cursor emailCursor=getContentResolver().query(Uri.parse("content://com.android.contacts/data"),null,"raw_contact_id=? and mimetype=?",new String[]{Integer.toString(FirstActivity.item.id),"vnd.android.cursor.item/email_v2"},null);

            //更新头像图片
            values.put("data15",byteArray);
            if (phototCursor.moveToFirst()&&isChange){//使用图片改变的判断变量防止二次压缩（拍照/相册成功后在回调方法中赋值图片改变的判断变量），对变动的图片才转为字节数组（有压缩作用）
                getContentResolver().update(Uri.parse("content://com.android.contacts/data"),values,"raw_contact_id=? and mimetype=?",new String[]{Integer.toString(FirstActivity.item.id),"vnd.android.cursor.item/photo"});
            }
            else if (byteArray!=null){
                values.put("raw_contact_id",FirstActivity.item.id);//向data表添加记录需指定联系人ID与信息类型
                values.put("mimetype","vnd.android.cursor.item/photo");
                getContentResolver().insert(Uri.parse("content://com.android.contacts/data"),values);
            }
            phototCursor.close();
            values.clear();

            /*
                data表的触发器：①信息类型为姓名的记录中以空白符为间隔分别存入姓名有关字段（分段字段先各自赋值，若为无效值再以data1为参考）
                               ②对应信息类型的信息字段最终值为无效（空/空字符串）则将该信息记录删除
            */
            //更新姓名
            values.put("data3","");//由于data表对姓名类型记录的触发器，为了防止更新信息不对称
            values.put("data5","");
            values.put("data1",name);
            if (nameCursor.moveToFirst()){
                getContentResolver().update(Uri.parse("content://com.android.contacts/data"),values,"raw_contact_id=? and mimetype=?",new String[]{Integer.toString(FirstActivity.item.id),"vnd.android.cursor.item/name"});
            }
            else if (!name.equals("")){
                values.put("raw_contact_id",FirstActivity.item.id);
                values.put("mimetype","vnd.android.cursor.item/name");
                getContentResolver().insert(Uri.parse("content://com.android.contacts/data"),values);
            }
            nameCursor.close();
            values.clear();

            //更新手机号码
            values.put("data1",number);
            if (numberCursor.moveToFirst()){
                getContentResolver().update(Uri.parse("content://com.android.contacts/data"),values,"raw_contact_id=? and mimetype=?",new String[]{Integer.toString(FirstActivity.item.id),"vnd.android.cursor.item/phone_v2"});
            }
            else if (!number.equals("")){
                values.put("raw_contact_id",FirstActivity.item.id);
                values.put("mimetype","vnd.android.cursor.item/phone_v2");
                getContentResolver().insert(Uri.parse("content://com.android.contacts/data"),values);
            }
            numberCursor.close();
            values.clear();

            //更新邮件
            values.put("data1",email);
            if (emailCursor.moveToFirst()){
                getContentResolver().update(Uri.parse("content://com.android.contacts/data"),values,"raw_contact_id=? and mimetype=?",new String[]{Integer.toString(FirstActivity.item.id),"vnd.android.cursor.item/email_v2"});
            }
            else if (!email.equals("")){
                values.put("raw_contact_id",FirstActivity.item.id);
                values.put("mimetype","vnd.android.cursor.item/email_v2");
                getContentResolver().insert(Uri.parse("content://com.android.contacts/data"),values);
            }
            emailCursor.close();
            values.clear();
        }

        //添加操作
        //先向raw_contacts表（联系人统计表）添加联系人，再根据添加后的返回值（新记录的Uri）查询联系人统计表获取新联系人ID，再在data表中插入新联系人各种信息类型的记录
        public void insert(){
            ContentValues values=new ContentValues();
            Uri rawContactUri=getContentResolver().insert(Uri.parse("content://com.android.contacts/raw_contacts"),values);//向raw_contacts表插入联系人
            Cursor rawContactCursor=getContentResolver().query(rawContactUri,null,null,null,null);//查询返回的新记录的Uri（查询添加的新联系人）
            rawContactCursor.moveToFirst();
            int rawContactId=rawContactCursor.getInt(rawContactCursor.getColumnIndex("_id"));//获取新联系人的ID
            rawContactCursor.close();
            //在data表添加联系人信息

            //添加对应联系人的头像图片记录
            if (byteArray!=null){
                values.put("data15",byteArray);
                values.put("raw_contact_id",rawContactId);
                values.put("mimetype","vnd.android.cursor.item/photo");
                getContentResolver().insert(Uri.parse("content://com.android.contacts/data"),values);
                values.clear();
            }

            //添加对应联系人的姓名记录
            if (!name.equals("")){
                values.put("data1",name);
                values.put("raw_contact_id",rawContactId);
                values.put("mimetype","vnd.android.cursor.item/name");
                getContentResolver().insert(Uri.parse("content://com.android.contacts/data"),values);
                values.clear();
            }

            //添加对应联系人的手机号码记录
            if (!number.equals("")){
                values.put("data1",number);
                values.put("raw_contact_id",rawContactId);
                values.put("mimetype","vnd.android.cursor.item/phone_v2");
                getContentResolver().insert(Uri.parse("content://com.android.contacts/data"),values);
                values.clear();
            }

            //添加对应联系人的邮件记录
            if (!email.equals("")){
                values.put("data1",email);
                values.put("raw_contact_id",rawContactId);
                values.put("mimetype","vnd.android.cursor.item/email_v2");
                getContentResolver().insert(Uri.parse("content://com.android.contacts/data"),values);
                values.clear();
            }
        }

        //拍照
        public void takePhoto(){
            File outputImage=new File(getExternalCacheDir(),"output_image.jpg");//创建存储存储照片路径的File对象（路径为应用的缓存目录）
            try
            {
                if (outputImage.exists()){
                    outputImage.delete();
                }
                outputImage.createNewFile();//通过File对象创建文件：File的createNewFile()（声明了IOException）
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
            /*
                获取File对应的普通Uri：①Android7.0前：Uri的静态方法fromFile()（直接获取file格式Uri）
                                      ②Android7.0开始：FileProvider的静态方法getUriForFile()获取File对应的content格式Uri（参数①Context ②authority标签 ③File）
		   		                      （使用本地（真实路径）file格式Uri被认为不安全，会抛出FileUriExposedException ,使用文件的内容Uri通过FileProvider保护数据））
            */
            if (Build.VERSION.SDK_INT>=24){
                imageUri= FileProvider.getUriForFile(SecondActivity.this,"com.example.contactsdemo.fileprovider",outputImage);
            }
            else {
                imageUri=Uri.fromFile(outputImage);
            }
            //启动相机程序
            Intent intent=new Intent("android.media.action.IMAGE_CAPTURE");
            intent.putExtra(MediaStore.EXTRA_OUTPUT,imageUri);//将拍照存入Uri对应文件的键值对intent.putExtra(MediaStore.EXTRA_OUTPUT,存储文件的Uri)
            startActivityForResult(intent,TAKE_PHTOT);
        }

    //打开相册
    public void openAlbum(){
        Intent intent=new Intent("android.intent.action.GET_CONTENT");
        intent.setType("image/*");
        startActivityForResult(intent,CHOOSE_PHOTO);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {//回调方法
        switch (requestCode){
            case TAKE_PHTOT:
                if (resultCode==RESULT_OK){//判断是否拍照成功
                    try
                    {
                        Bitmap bitmap= BitmapFactory.decodeStream(getContentResolver().openInputStream(imageUri));//从Uri读取数据
                        isChange=true;//赋值图片改变的判断变量
                        //this.headImage=bitmap;//获取纯原图
                        mHeadImage.setImageBitmap(bitmap);//ImageView控件显示
                    }
                    catch (FileNotFoundException e)
                    {
                        e.printStackTrace();
                    }
                }
                break;
            case CHOOSE_PHOTO:
                if (resultCode==RESULT_OK){//判断选择相册中照片是否成功
                    try
                    {
                        Bitmap bitmap1= BitmapFactory.decodeStream(getContentResolver().openInputStream(data.getData()));//从Uri读取数据
                        isChange=true;//赋值图片改变的判断变量
                        //this.headImage=bitmap1;//获取纯原图
                        mHeadImage.setImageBitmap(bitmap1);//ImageView控件显示
                    }
                    catch (FileNotFoundException e)
                    {
                        e.printStackTrace();
                    }
                }
        }
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions,int[] grantResults) {//申请权限后的回调方法
        switch (requestCode){
            case 1:
                if (grantResults.length!=0 && grantResults[0]==PackageManager.PERMISSION_GRANTED){//判断读取SD卡权限授权情况
                    openAlbum();
                }
                else{
                    Toast.makeText(SecondActivity.this,"未授权读取相册",Toast.LENGTH_SHORT).show();
                }
        }
    }
}
