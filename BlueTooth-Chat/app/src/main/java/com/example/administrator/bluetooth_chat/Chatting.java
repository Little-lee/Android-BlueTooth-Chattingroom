package com.example.administrator.bluetooth_chat;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Locale;


public class Chatting extends Activity implements View.OnClickListener ,AdapterView.OnItemClickListener {

    float pitchVal;
    float speedVal;
   static TextToSpeech speecher;

    //蓝牙适配器
    private BluetoothAdapter btAdapter;

    String addrs;
    String  targetDevName;


    Button secretBtn;
    ListView messageView;
    EditText editText;
    Button sentBtn;
    Button quitBtn;
    TextView devId;
    int flag ;

    int secretFlag = 1;
    int secretFlag1 = 1;
    int secretFlag2 =  -1;

  //  private ArrayAdapter<String> adapter;

    private MessageAdapter mAdapter;

    private ArrayList<com.example.administrator.bluetooth_chat.Message>  message ;

    BluetoothDevice device;
    public MyService1 myService1 = new MyService1(Chatting.this);


    public  Handler myHandler2 = new Handler(){
        public void handleMessage(Message msg) {
            if(msg.what == 1){
                //收到的 信息
             String ss = msg.obj.toString();
                setMessage(ss);
            }
            else if (msg.what == 2){
                Toast.makeText(Chatting.this,"连接失败，请重新连接！",Toast.LENGTH_LONG).show();
            }

        }
    };

    //将收到的信息显示在listview上
    public void setMessage(String message){

        com.example.administrator.bluetooth_chat.Message message1 = new com.example.administrator.bluetooth_chat.Message(0,message);
        this.message.add(message1);

       mAdapter.notifyDataSetChanged();
   }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.chatting_window);

        //获取本地蓝牙适配器
        btAdapter = BluetoothAdapter.getDefaultAdapter();

        Intent intent = this.getIntent();    //获取已有的intent对象
        Bundle bundle = intent.getExtras();
        //获取intent里面的bundle对象
        flag = bundle.getInt("flag");

        //由上一个Activity的bundle信息判断谁为 服务端or客户端
        if ( flag == 0 ){
            // 服务端

            targetDevName = new String(bundle.getString("client_name"));    //获取Bundle里面的字符串
            addrs = new String(bundle.getString("client_address"));

            myService1.start();
        }
        else if( flag == 1){
            // 客户端
            targetDevName = new String(bundle.getString("server_name"));    //获取Bundle里面的字符串
            addrs = new String(bundle.getString("server_address"));

            myService1.start();
            device = btAdapter.getRemoteDevice(addrs);
            //开启连接设备的线程
            myService1.connect(device);
        }


        devId = (TextView) findViewById(R.id.devId);
        devId.setText(targetDevName);

        quitBtn = (Button) findViewById(R.id.quitbtn);
        quitBtn.setOnClickListener(this);

        editText = (EditText) findViewById(R.id.inputEdit);

        sentBtn = (Button) findViewById(R.id.sendBtn);
        sentBtn.setOnClickListener(this);

        secretBtn = (Button) findViewById(R.id.secretBtn);
        secretBtn.setOnClickListener(this);

        message = new ArrayList<>();

        messageView = (ListView) findViewById(R.id.messageView);
        //适配器处理
        mAdapter = new MessageAdapter(message,Chatting.this);

        //设置listview适配器
        messageView.setAdapter(mAdapter);

messageView.setOnItemClickListener(this);

        speecher = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR) {
                    speecher.setLanguage(Locale.UK);
                }
            }

        });
        speedVal = 1.0f;
        pitchVal = 1.0f;

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        myService1.stop();
        myService1=null;

          if (speecher!=null){
            speecher.stop();
            speecher.shutdown();
        }

    }




    String string =null;
    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.quitbtn:
                finish();
                overridePendingTransition(android.R.anim.fade_in,android.R.anim.fade_out);
                break;

            case R.id.sendBtn:
                //加密模式 未打开
                if (secretFlag == secretFlag1){
                    string = new String(editText.getText().toString());

                    com.example.administrator.bluetooth_chat.Message message1 = new com.example.administrator.bluetooth_chat.Message(1,string);
                    this.message.add(message1);


                    mAdapter.notifyDataSetChanged();
                    myService1.write(string);

                    string = null;
                    editText.setText(null);
                }

                //加密模式 已打开
                if (secretFlag == secretFlag2) {
                    string = new String(editText.getText().toString());

                    com.example.administrator.bluetooth_chat.Message message1 = new com.example.administrator.bluetooth_chat.Message(1,string);
                    this.message.add(message1);

                    mAdapter.notifyDataSetChanged();

                    myService1.secretWrite(string);
                    string = null;
                    editText.setText(null);
                }
                break;

            case R.id.secretBtn:
                //加密模式 关闭
                if (secretFlag == secretFlag2) {
                secretBtn.setText("加密模式");
                sentBtn.setText("发送");
                }
                //加密模式 打开
                if (secretFlag == secretFlag1) {
                    secretBtn.setText("取消加密");
                    sentBtn.setText("加密发送");
                }
                secretFlag = 0-secretFlag;
                break;

        }
    }






    @Override
    public void onBackPressed () {

        finish();
        overridePendingTransition(android.R.anim.fade_in,android.R.anim.fade_out);
    }




    @Override
    protected void onStart() {
        super.onStart();
        Log.e("text", "onStart");
    }
    @Override
    protected void onRestart() {
        super.onRestart();
        Log.e("text", "onRestart");
    }
    @Override
    protected void onResume() {
        super.onResume();
        Log.e("text", "onResume");
    }
    @Override
    protected void onPause() {
        super.onPause();
        Log.e("text", "onPause");
        //有可能在执行完onPause或onStop后,系统资源紧张将Activity杀死,所以有必要在此保存持久数据
    }
    @Override
    protected void onStop() {
        super.onStop();
        Log.e("text", "onStop ");
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Toast.makeText(this, "listview的item被点击了！，点击的位置是-->  " + position,
             Toast.LENGTH_SHORT).show();
    }



   /* public boolean onTouchEvent(MotionEvent event) {
        if(null != this.getCurrentFocus()){
            /**
             * 点击空白位置 隐藏软键盘
             *
            InputMethodManager mInputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            return mInputMethodManager.hideSoftInputFromWindow(this.getCurrentFocus().getWindowToken(), 0);
        }
        return super .onTouchEvent(event);
    }
    */

}
