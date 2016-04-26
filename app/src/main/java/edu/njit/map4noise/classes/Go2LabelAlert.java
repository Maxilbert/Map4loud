package edu.njit.map4noise.classes;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.ArrayList;

import com.example.yuan.map4noise.R;

import ch.boye.httpclientandroidlib.HttpEntity;
import ch.boye.httpclientandroidlib.NameValuePair;
import ch.boye.httpclientandroidlib.client.entity.UrlEncodedFormEntity;
import ch.boye.httpclientandroidlib.client.methods.CloseableHttpResponse;
import ch.boye.httpclientandroidlib.client.methods.HttpPost;
import ch.boye.httpclientandroidlib.impl.client.CloseableHttpClient;
import ch.boye.httpclientandroidlib.impl.client.HttpClients;
import ch.boye.httpclientandroidlib.message.BasicNameValuePair;


/**
 * Created by yuan on 4/25/16.
 */
public class Go2LabelAlert {

    private String name;
    private Context context = null;
    private View view = null;
    private static final String[] str = {"Please select", "indoor", "outdoor", "mixture"};
    private static final String[] envStr = {"Please select", "office", "lecture", "loudTalk", "dialog", "sleep", "drive", "machine", "street", "siren", "others"};
    private static final String[] noiseStr = {"Please select (optional)", "none", "construction", "machine", "traffic", "music", "people", "plane", "train", "others"};

    private AlertDialog.Builder builder = null;

    private Spinner io_spinner;
    private Spinner env_spinner;
    private Spinner noise_spinner;
//    private List<String> data_list;
    private ArrayAdapter<String> arr_adapter;

    private String ioDoor;
    private String env;
    private String noise;

    private int i, j, k;

    public Go2LabelAlert(Context c, View v, String name, int i, int j, int k) {
        this.context = c;
        this.view = v;
        this.name = name;
        this.i = i; ioDoor = str[i];
        this.j = j; env = envStr[j];
        this.k = k; noise = noiseStr[k];
        this.init();
    }


    public Go2LabelAlert(Context c, View v, String name){
        this(c, v, name, 0, 0, 0);
    }



    private void init(){
        io_spinner = (Spinner) view.findViewById(R.id.io_spinner);
        env_spinner = (Spinner) view.findViewById(R.id.env_spinner);
        noise_spinner = (Spinner) view.findViewById(R.id.noise_spinner);

        arr_adapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, str);
        arr_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        io_spinner.setAdapter(arr_adapter);
        io_spinner.setOnItemSelectedListener(new IOSpinnerSelectedListener());
        io_spinner.setSelection(i, true);

        arr_adapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, envStr);
        arr_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        env_spinner.setAdapter(arr_adapter);
        env_spinner.setOnItemSelectedListener(new IOSpinnerSelectedListener());
        env_spinner.setSelection(j, true);

        arr_adapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, noiseStr);
        arr_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        noise_spinner.setAdapter(arr_adapter);
        noise_spinner.setOnItemSelectedListener(new IOSpinnerSelectedListener());
        noise_spinner.setSelection(k, true);

        ViewGroup parent = ((ViewGroup) view.getParent());
        if(parent != null)
            parent.removeView(view);
        builder = new AlertDialog.Builder(context)
                .setTitle("Labeling")
                .setIcon(R.drawable.label)
                .setView(view);
        setPositiveButton(builder);
        setNegativeButton(builder);
        builder.setCancelable(true);
        builder.create().show();
    }




    //使用数组形式操作
    class IOSpinnerSelectedListener implements AdapterView.OnItemSelectedListener {

        public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
            if (arg0==io_spinner) {
                i = arg2;
                ioDoor = str[i];
                System.out.println(ioDoor);
            } else if (arg0==env_spinner){
                j = arg2;
                env = envStr[j];
                System.out.println(env);
            } else if (arg0==noise_spinner){
                k = arg2;
                noise = noiseStr[k];
                System.out.println(noise);
            }
        }

        public void onNothingSelected(AdapterView<?> arg0) {}
    }



    private Runnable labelThread = new Runnable(){
        @Override
        public void run() {


            // TODO: http post.
            String result = "-1";
            //noinspection deprecation
            CloseableHttpClient httpClient = HttpClients.createDefault();
            String url = "https://map4noise.njit.edu/Label.php";
            //String url = "http://128.235.40.185:8080/MyWebAppTest/Label";
            //第二步：生成使用POST方法的请求对象
            HttpPost httpPost = new HttpPost(url);
            //NameValuePair对象代表了一个需要发往服务器的键值对
            NameValuePair pair1 = new BasicNameValuePair("username", name);
            NameValuePair pair2 = new BasicNameValuePair("env", ioDoor);
            NameValuePair pair3 = new BasicNameValuePair("label", env);
            if(noise.equals("Please select (optional)"))
                noise = "none";
            NameValuePair pair4 = new BasicNameValuePair("noise", noise);
            //将准备好的键值对对象放置在一个List当中
            ArrayList<NameValuePair> pairs = new ArrayList<>();
            pairs.add(pair1);
            pairs.add(pair2);
            pairs.add(pair3);
            pairs.add(pair4);
            try {
                //创建代表请求体的对象（注意，是请求体）
                HttpEntity requestEntity = new UrlEncodedFormEntity(pairs);
                //将请求体放置在请求对象当中
                httpPost.setEntity(requestEntity);
                //执行请求对象
                try {
                    //第三步：执行请求对象，获取服务器返还的相应对象
                    CloseableHttpResponse response = httpClient.execute(httpPost);
                    //第四步：检查相应的状态是否正常：检查状态码的值是200表示正常
                    if (response.getStatusLine().getStatusCode() == 200) {
                        //第五步：从相应对象当中取出数据，放到entity当中
                        HttpEntity entity = response.getEntity();
                        BufferedReader reader = new BufferedReader(
                                new InputStreamReader(entity.getContent()));
                        result = reader.readLine();
                        //Log.d("HTTP", "POST:" + result);
                    } else {
                        result = "" + response.getStatusLine().getStatusCode();
                        //Log.d("HTTP", "ERROR:" + result);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            Message msg = new Message();
            Bundle data = new Bundle();
            data.putString("value", result);
            msg.setData(data);
            httpHandler.sendMessage(msg);
        }
    };



    Handler httpHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Bundle data = msg.getData();
            String val = data.getString("value");
            //Log.d("Http","请求结果:" + val);
            //可以开始处理UI
            //Toast.makeText(Login.this, "The result is " + val,
            //        Toast.LENGTH_LONG).show();
            if(val.equals("1")) {
                Toast.makeText(context, "Label is submitted", Toast.LENGTH_LONG).show();
            } else if (val.equals("0")){
                Toast.makeText(context, "Label is NOT submitted", Toast.LENGTH_LONG).show();
            }
        }
    };



    private void setPositiveButton(AlertDialog.Builder builder){

        builder.setPositiveButton("Submit label", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(!ioDoor.equals("Please select") && !env.equals("Please select")) {
                    new Thread(labelThread).start();
                    Toast.makeText(context, "Thank you for the contribution", Toast.LENGTH_SHORT).show();
                    //TODO: HTTP to submit labels here;
                } else {
                    Toast.makeText(context, "Please select labels", Toast.LENGTH_SHORT).show();
                    new Go2LabelAlert(context, view, name, i, j, k).visualizeRemind();
                }
            }
        });

    }



    private void setNegativeButton(AlertDialog.Builder builder){
        builder.setNegativeButton("Give up label", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //mSlidingLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
            }
        });
    }

    private void visualizeRemind(){
        TextView tvRemind = (TextView) view.findViewById(R.id.tvRemind);
        tvRemind.setVisibility(View.VISIBLE);
    }

}
