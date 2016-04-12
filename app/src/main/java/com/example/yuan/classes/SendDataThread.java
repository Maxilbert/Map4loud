package com.example.yuan.classes;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import ch.boye.httpclientandroidlib.HttpEntity;
import ch.boye.httpclientandroidlib.HttpResponse;
import ch.boye.httpclientandroidlib.client.HttpClient;
import ch.boye.httpclientandroidlib.client.methods.HttpPost;
import ch.boye.httpclientandroidlib.entity.ContentType;
import ch.boye.httpclientandroidlib.entity.mime.HttpMultipartMode;
import ch.boye.httpclientandroidlib.entity.mime.MultipartEntityBuilder;
import ch.boye.httpclientandroidlib.impl.client.HttpClients;

/**
 * Created by yuan on 2/16/16.
 */
public class SendDataThread implements Runnable {

    private double dBA;
    private Handler mSendDataHandler;
    private File audioFile;
    double lon, lat;
    String name;
    Context context;

    public SendDataThread(Context context, double dBA, double currentLoc1, double currentLoc0, String name, Handler httpHandler, File audioFile){
        this.context = context;
        this.dBA = dBA;
        this.mSendDataHandler = httpHandler;
        this.audioFile = audioFile;
        this.lon = currentLoc1;
        this.lat = currentLoc0;
        this.name = name;
    }

    public SendDataThread(Context context, double dBA, double currentLoc1, double currentLoc0, String name, File audioFile) {
        this(context, dBA, currentLoc1, currentLoc0, name, null, audioFile);
    }

    @Override
    public void run() {
        // TODO: http post.
        String result = "0";
        //Get the instance of ClosealbeHttpClient
        HttpClient httpClient = HttpClients.createDefault();
        //The url of servlet
        //String url = "https://web.njit.edu/~yl768/webapps7/ReceiveData";
        //String url = "http://128.235.40.185:8080/MyWebAppTest/ReceiveData1";
        String url = "https://map4noise.njit.edu/ReceiveData1.php";
        //String url = "https://web.njit.edu/~yl768/ReceiveData1.php";
        //New HTTP Post request
        HttpPost httpPost = new HttpPost(url);
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);//设置浏览器兼容模式
        builder.addBinaryBody("audioFile", audioFile, ContentType.DEFAULT_BINARY, "sampling.wav");
        builder.addTextBody("lon", "" + lon);
        builder.addTextBody("lat", "" + lat);
        builder.addTextBody("dB", "" + dBA);
        builder.addTextBody("user", "" + name);
        builder.addTextBody("fileName", audioFile.toString());
        //Add Name Value Pairs to HTTP request
//            String parameters = "{\"lon\":\"" + currentLatLon[1]
//                    + "\",\"lat\":\"" + currentLatLon[0]
//                    + "\",\"dB\":\"" + dBA
//                    + "\",\"user\":\"" + name
//                    + "\",\"fileName\":\"" + audioFile.toString() + "\"}";
//            builder.addTextBody("parameters", parameters);
//            NameValuePair pair1 = new BasicNameValuePair("lon", "" + currentLatLon[1]);
//            NameValuePair pair2 = new BasicNameValuePair("lat", "" + currentLatLon[0]);
//            NameValuePair pair3 = new BasicNameValuePair("dB", "" + dBA);
//            NameValuePair pair4 = new BasicNameValuePair("user", "" + name);
//            NameValuePair pair5 = new BasicNameValuePair("fileName", audioFile.toString());
//            ArrayList<NameValuePair> pairs = new ArrayList<>();
//            pairs.add(pair1);
//            pairs.add(pair2);
//            pairs.add(pair3);
//            pairs.add(pair4);
//            pairs.add(pair5);

        //Send Http post request
        try {
            HttpEntity httpEntity = builder.build();
            //HttpEntity httpEntity = new UrlEncodedFormEntity(pairs);
            httpPost.setEntity(httpEntity);
            HttpResponse response = httpClient.execute(httpPost);
            if (response.getStatusLine().getStatusCode() == 200) {
                HttpEntity entity = response.getEntity();
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(entity.getContent()));
                result = reader.readLine();
                Log.d("HTTP", "POST:" + result);
            } else {
                result = "" + response.getStatusLine().getStatusCode();
                Log.d("HTTP", "ERROR:" + result);
            }
        } catch (UnsupportedEncodingException e1) {
            e1.printStackTrace();
        } catch (IOException e2) {
            Toast.makeText(context, "Cannot access server, please connect NJIT LAN or VPN.", Toast.LENGTH_LONG).show();
            e2.printStackTrace();
        }
        Message msg = new Message();
        Bundle data = new Bundle();
        data.putString("result", result);
        data.putDouble("lon", lon);
        data.putDouble("lat", lat);
        data.putDouble("dBA", dBA);
        msg.setData(data);
        if(mSendDataHandler!=null)
            mSendDataHandler.sendMessage(msg);
    }

}
