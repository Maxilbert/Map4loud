package edu.njit.map4noise.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.yuan.map4noise.R;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;

import ch.boye.httpclientandroidlib.HttpEntity;
import ch.boye.httpclientandroidlib.NameValuePair;
import ch.boye.httpclientandroidlib.client.entity.UrlEncodedFormEntity;
import ch.boye.httpclientandroidlib.client.methods.CloseableHttpResponse;
import ch.boye.httpclientandroidlib.client.methods.HttpPost;
import ch.boye.httpclientandroidlib.impl.client.CloseableHttpClient;
import ch.boye.httpclientandroidlib.impl.client.HttpClients;
import ch.boye.httpclientandroidlib.message.BasicNameValuePair;


/**
 * Login Activity
 */
public class Login extends FragmentActivity {

    private Button mBtnLgn = null;
    private Button mBtnReg = null;
    private EditText mEtName = null;
    private EditText mEtPwd = null;

    String name = null;
    String pwd = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkPermission();
    }

    private void checkPermission (){
        if (ActivityCompat.checkSelfPermission(getApplication(), android.Manifest.permission.INTERNET)
                != PackageManager.PERMISSION_GRANTED) {
            int REQUEST_CODE = 100;
            ActivityCompat.requestPermissions(Login.this, new String[]{Manifest.permission.INTERNET}, REQUEST_CODE);
        }
        else if (ActivityCompat.checkSelfPermission(getApplication(), android.Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            int REQUEST_CODE = 101;
            ActivityCompat.requestPermissions(Login.this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_CODE);
        }
        else if (ActivityCompat.checkSelfPermission(getApplication(), android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            int REQUEST_CODE = 102;
            ActivityCompat.requestPermissions(Login.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE);
        }
        else if (ActivityCompat.checkSelfPermission(getApplication(), android.Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            int REQUEST_CODE = 103;
            ActivityCompat.requestPermissions(Login.this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_CODE);
        }
        else if (ActivityCompat.checkSelfPermission(getApplication(), android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            int REQUEST_CODE = 104;
            ActivityCompat.requestPermissions(Login.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE);
        } else {
            setContentView(R.layout.activity_login);
            initView();
            setListener();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 100:
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    checkPermission();
                } else {
                    onPause();
                    onStop();
                    onDestroy();
                }
                break;
            case 101:
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    checkPermission();
                } else {
                    onPause();
                    onStop();
                    onDestroy();
                }
                break;
            case 102:
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    checkPermission();
                } else {
                    onPause();
                    onStop();
                    onDestroy();
                }
                break;
            case 103:
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    checkPermission();
                } else {
                    onPause();
                    onStop();
                    onDestroy();
                }
                break;
            case 104:
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    checkPermission();
                } else {
                    onPause();
                    onStop();
                    onDestroy();
                }
                break;
            default:
                break;
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_login, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Initial button members
     */
    private void initView(){
        mEtName = (EditText)findViewById(R.id.etName);
        mEtPwd = (EditText)findViewById(R.id.etPwd);
        mBtnLgn=(Button)findViewById(R.id.btnLgn);
        mBtnReg=(Button)findViewById(R.id.btnReg);
    }

    /**
     * Set Button Listeners
     */
    private void setListener(){
        mBtnLgn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                name = mEtName.getText().toString();
                pwd = mEtPwd.getText().toString();
                if (name.equals("") || name == null) {
                    Toast.makeText(Login.this, "Please input your username.",
                            Toast.LENGTH_SHORT).show();
                    return;
                } else if (pwd.equals("") || pwd == null){
                    Toast.makeText(Login.this, "Please input your password.",
                            Toast.LENGTH_SHORT).show();
                    return;
                } else {
                    //String jsonStr = "{ \"username\": \"" + name + "\", \"password\":\"" + pwd + "\"}";
                    //String url = "http://128.235.40.185:8080/MyWebAppTest/Verify";
                    new Thread(verifyThread).start();
                }
            }
        });

        mBtnReg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(Login.this, Register.class);
                Login.this.startActivity(intent);
            }
        });
    }

    /**
     * 网络访问线程甩出的句柄，可以在这里做UI操作
     */
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
                SQLiteDatabase db = openOrCreateDatabase("recentUser.db", Login.MODE_PRIVATE, null);
                db.execSQL("CREATE TABLE IF NOT EXISTS person (id INTEGER PRIMARY KEY AUTOINCREMENT, name VARCHAR)");
                db.execSQL("REPLACE INTO person (id, name) VALUES(1, ?)", new String[]{name});
                db.close();
                Toast.makeText(Login.this, "Welcome back, " + name + "!",
                        Toast.LENGTH_LONG).show();
                Intent intent = new Intent();
                intent.setClass(Login.this, SoundMap.class);
                //Bundle bundle=new Bundle();
                //bundle.putString("username", name);
                //intent.putExtras(bundle);
                intent.putExtra("username", name);
                intent.putExtra("from", "Login");
                Login.this.startActivity(intent);
                //Clear password for security consideration
                mEtPwd.setText(null, TextView.BufferType.EDITABLE);
                pwd = null;
            } else if (val.equals("0")){
                Toast.makeText(Login.this, "Wrong username or/and password.",
                        Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(Login.this, "Sorry. Fail to connect server. Please try later.",
                        Toast.LENGTH_LONG).show();
            }
        }
    };

    /**
     * 网络访问线程，功能为验证用户身份
     */
    Runnable verifyThread = new Runnable(){
        @Override
        public void run() {
            // TODO: http post.
            String result = "-1";
            //noinspection deprecation
            //HttpClient httpClient = new DefaultHttpClient();
            CloseableHttpClient httpClient = HttpClients.createDefault();
            //String url = "https://web.njit.edu/~yl768/webapps7/Verify";
            //String url = "http://128.235.40.185:8080/MyWebAppTest/Verify";
            String url = "https://map4noise.njit.edu/Verify.php";
            //第二步：生成使用POST方法的请求对象
            HttpPost httpPost = new HttpPost(url);
            //NameValuePair对象代表了一个需要发往服务器的键值对
            NameValuePair pair1 = new BasicNameValuePair("username", name);
            NameValuePair pair2 = new BasicNameValuePair("password", pwd);
            //将准备好的键值对对象放置在一个List当中
            ArrayList<NameValuePair> pairs = new ArrayList<>();
            pairs.add(pair1);
            pairs.add(pair2);
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
            data.putString("value",result);
            msg.setData(data);
            httpHandler.sendMessage(msg);
        }
    };

}


