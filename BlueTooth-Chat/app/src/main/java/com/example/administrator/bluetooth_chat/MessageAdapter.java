package com.example.administrator.bluetooth_chat;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.view.View.OnClickListener;
import android.widget.Toast;

import java.util.ArrayList;

/**
 * Created by Administrator on 2017/2/17.
 */
public class MessageAdapter extends BaseAdapter  {


    String s;
    public ArrayList<Message> msgList;
    private LayoutInflater mInflater;

Chatting i;

    public MessageAdapter(ArrayList<Message> msgList,Context context){
        this.msgList = msgList;
        mInflater = LayoutInflater.from(context);

    }
    @Override
    public int getCount() {
        return msgList.size();
    }

    @Override
    public Object getItem(int position) {
        return msgList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        Message msg = msgList.get(position);
        return msg.getType();
    }

    @Override
    public int getViewTypeCount() {
        return 5;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        View view;
        ViewHolder viewHolder=null;
        MyListener myListener = null;


        if(convertView == null){
            if(getItemViewType(position) == 0){

                view = mInflater.inflate(R.layout.other_message,null);
                viewHolder = new ViewHolder();

                viewHolder.button = (Button) view.findViewById(R.id.say);

                myListener=new MyListener(position);

                viewHolder.button.setTag(position);
                //给Button添加单击事件  添加Button之后ListView将失去焦点  需要的直接把Button的焦点去掉
                viewHolder.button.setOnClickListener( myListener);

                viewHolder.text = (TextView)view.findViewById(R.id.textLeft);
            }else {
                view = mInflater.inflate(R.layout.self_message,null);
                viewHolder = new ViewHolder();

                viewHolder.text = (TextView)view.findViewById(R.id.textRight);
            }

            view.setTag(viewHolder);
        }
        else {
            if(getItemViewType(position) == 0){
                view = convertView;
                viewHolder = (ViewHolder)view.getTag();

                myListener=new MyListener(position);
                viewHolder.button.setTag(position);
                //给Button添加单击事件  添加Button之后ListView将失去焦点  需要的直接把Button的焦点去掉
                viewHolder.button.setOnClickListener( myListener);
            }
            else{
            view = convertView;
            viewHolder = (ViewHolder)view.getTag();

            }
        }


       // viewHolder.button.setTag(position);
        //给Button添加单击事件  添加Button之后ListView将失去焦点  需要的直接把Button的焦点去掉
      //  viewHolder.button.setOnClickListener( myListener);

        viewHolder.text.setText(msgList.get(position).getContent());


        return view;
    }


    private class MyListener implements OnClickListener{
        int mPosition;
        public MyListener(int inPosition){
            mPosition= inPosition;
        }
        @Override
        public void onClick(View v) {
            s = msgList.get(mPosition).getContent();

            System.out.println(mPosition+"....");

            Chatting.speecher.speak(s, TextToSpeech.QUEUE_FLUSH, null, null);
            s = null;

        }

    }

    public final class ViewHolder{
         Button button;
        TextView text;
    }


}