package com.example.administrator.bluetooth_chat;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Set;

public class SelectDevice extends Activity implements View.OnClickListener,AdapterView.OnItemClickListener{


    String targetDev;
    public BluetoothAdapter mBluetoothAdapter;
    private ArrayAdapter<String> adapter;
    private ArrayList<String> mNameList = new ArrayList<String>();
    private ArrayList<BluetoothDevice> mDeviceList = new ArrayList<BluetoothDevice>();
    public MyService myService = new MyService(SelectDevice.this);

    Button backBtn;
    Button lookBtn;
    Button scanBtn;
    private ListView mDevList;
    String server_name;
    String server_address;
    TextView state;
    String client_name;
    String client_address;

   public Handler myHandler = new Handler(){
        public void handleMessage(Message msg) {
            if(msg.what == 1){
             //客户端
                server_name = new String(msg.getData().getString("server_name"));
                server_address = new String(msg.getData().getString("server_address"));

                Log.e("text", "server_address:  " + server_address);

                Intent chat = new Intent(SelectDevice.this, Chatting.class);

                Bundle bundle = new Bundle();                           //创建Bundle对象
                bundle.putString("server_name", server_name);     //装入数据
                bundle.putString("server_address", server_address);
                bundle.putInt("flag",1);
                chat.putExtras(bundle);                                //把Bundle塞入Intent里面

                myService.stop();
                myService=null;
                //把Bundle塞入Intent里面
                startActivity(chat);
                overridePendingTransition(android.R.anim.fade_in,android.R.anim.fade_out);

            }
            else if (msg.what == 2)
            {
                Toast.makeText(SelectDevice.this,"连接失败，请重试！",Toast.LENGTH_SHORT).show();
            }
            else if (msg.what == 3)
            {
                //服务端
                client_name = new String(msg.getData().getString("client_name"));
                client_address = new String(msg.getData().getString("client_address"));

                Log.e("text", "client_address:  " + client_address);

                Intent chat = new Intent(SelectDevice.this, Chatting.class);

                Bundle bundle = new Bundle();                           //创建Bundle对象
                bundle.putString("client_name", client_name);     //装入数据
                bundle.putString("client_address", client_address);
                bundle.putInt("flag",0);
                chat.putExtras(bundle);                                //把Bundle塞入Intent里面

                myService.stop();
                myService=null;
                //把Bundle塞入Intent里面
                startActivity(chat);
                overridePendingTransition(android.R.anim.fade_in,android.R.anim.fade_out);
            }

        }
    };

    @Override
    protected void onRestart(){
        super.onRestart();
        Log.e("text", " onRestart" );
        mDeviceList.clear();
        mNameList.clear();
        adapter.notifyDataSetChanged();
        //使蓝牙设备可见，方便配对
        Intent in=new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        in.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        startActivity(in);

        //设配器中寻找已匹配过的设备
        findDevice();
        myService = new MyService(SelectDevice.this);
        myService.start();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.device_list);


          /*
        **返回键 */
        backBtn= (Button) findViewById(R.id.back);
        backBtn.setOnClickListener(this);
         /*
        **可见性键 */
        lookBtn= (Button) findViewById(R.id.look);
        lookBtn.setOnClickListener(this);
         /*
        **刷新键 */
        scanBtn= (Button) findViewById(R.id.scan);
        scanBtn.setOnClickListener(this);




        state = (TextView) findViewById(R.id.state);
        state.setText("搜索中...");
        //初始化listview
        mDevList = (ListView) findViewById(R.id.dev_list);
        mDevList.setOnItemClickListener(this);
        //适配器处理
        adapter = new ArrayAdapter<String>(
                this,
                android.R.layout.simple_list_item_1,
                mNameList);
        //设置listview适配器
        mDevList.setAdapter(adapter);

        //获取本地蓝牙适配器
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Log.e("text", "没有蓝牙模块!");
            return;
        }



        // 如果蓝牙设置使能未打开，请求打开设备
        if (!mBluetoothAdapter.isEnabled()) {
            // 打开蓝牙设备
           Intent enable = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enable,1);


            //使蓝牙设备可见，方便配对
            Intent in=new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            in.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(in);

            findDevice();

        }
        else{


            //使蓝牙设备可见，方便配对
            Intent in=new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            in.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(in);
            //设配器中寻找已匹配过的设备
            findDevice();

            myService.start();
            }


        //mBluetoothAdapter.startDiscovery();


        //注册广播接收器
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(mReceiver, filter);

        mBluetoothAdapter.startDiscovery();




    }

    private void findDevice(){
        // 获得已经保存的配对设备
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();

        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                mNameList.add(device.getName() + "\n" + device.getAddress());
                mDeviceList.add(device);
            }
        }

        adapter.notifyDataSetChanged();
    }


    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {

                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // 已包含该设备
                if(mDeviceList.contains(device)){
                    return;
                }

                mNameList.add(device.getName() + "\n" + device.getAddress());

                mDeviceList.add(device);
                adapter.notifyDataSetChanged();
            }
            else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)){
                // 取消扫描进度显示
                if(mNameList.isEmpty())
                {
                    mNameList.add("\n无蓝牙设备" );
                    adapter.notifyDataSetChanged();
                    mNameList.clear();
                }

                state.setText("可用设备（搜索完成）");

            }
        }
    };


    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);

        myService.stop();
        myService=null;

         }

    @Override
    public void onBackPressed () {
        finish();
        overridePendingTransition(android.R.anim.fade_in,android.R.anim.fade_out);
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.back:
                finish();
                overridePendingTransition(android.R.anim.fade_in,android.R.anim.fade_out);

            case R.id.look:
                //使蓝牙设备可见，方便配对
                Intent in=new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                in.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
                startActivity(in);

            case R.id.scan:
                if (mBluetoothAdapter.isDiscovering()) {// 如果正在搜索，取消本次搜索
                    mBluetoothAdapter.cancelDiscovery();
                }
                mDeviceList.clear();
                mNameList.clear();
                adapter.notifyDataSetChanged();
                state.setText("搜索中...");
               mBluetoothAdapter.startDiscovery();// 开始搜索
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
         targetDev = mNameList.get(position);


        if (mDeviceList.size() != 0){
                 BluetoothDevice targettDev=  mDeviceList.get(position);
            //开启连接设备的线程
              myService.connect(targettDev);

        }


    }




    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == 1){
            if(resultCode ==  RESULT_OK){
                Log.e("text"," 设备打开成功");
                myService.start();
            }else{
                Log.e("text"," 设备打开失败");
            }
        }
    }



    @Override
    protected void onStart() {
        super.onStart();
        Log.e("text", "onStart");
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

}
