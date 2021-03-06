package com.example.mobilephonemanager;
import androidx.annotation.LongDef;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.baidu.aip.asrwakeup3.core.mini.AutoCheck;
import com.baidu.aip.asrwakeup3.core.recog.MyRecognizer;
import com.baidu.aip.asrwakeup3.core.recog.listener.IRecogListener;
import com.baidu.aip.asrwakeup3.core.recog.listener.MessageStatusRecogListener;
import com.baidu.aip.asrwakeup3.core.util.bluetooth.OfflineRecogParams;
import com.baidu.aip.asrwakeup3.core.wakeup.MyWakeup;
import com.baidu.speech.asr.SpeechConstant;

import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.litepal.LitePal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import resource.HashName;
import resource.SpecialHashName;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private Button start;
    private Button stop;
    private Button cancel;
    private AutoCompleteTextView requirement;
    public boolean isfirst=true;
    private WakeUpService.WakeUpBinder wakeUpBinder;
    protected boolean enableOffline;
    public static final String TAG="weather";
    public static final int MANIFEST_CODE=0;
    private HashName AppName=new HashName();
    private SpecialHashName specialHashName=new SpecialHashName();
    private NLP nlp;
    public static boolean ELE_RANDOM;
    public TextToVoice textToVoice;
    private static final String APP_ID = "20201025000598370";
    private static final String SECURITY_KEY = "DBIUQCmFkIERihfjHJsx";
    public static String[] COUNTRIES={
            "打开...",
            "搜索以...为关键词的外卖",
            "用备忘录记录...",
            "用备忘录查询有关于...(关键词)的内容",
            "用备忘录查询日期为...(年月日)的内容",
            "用备忘录删除所有内容",
            "用备忘录删除有关于...(关键词)的内容",
            "备忘录删除日期为...(年月日)的内容",
            "备忘录删除日期为“...”，有关于...的内容",
            "查询...市的天气",
            "给...发送QQ消息...",
            "给...发送微信消息...",
            "打开微信扫一扫",
            "打开朋友圈",
            "给...(联系人)打电话",
            "给...(联系人)发短信，内容为...",
            "打开朋友圈",
            "打开微信扫一扫",
            "打开课程表"
    };
    private ServiceConnection connection=new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            wakeUpBinder=(WakeUpService.WakeUpBinder)service;
            wakeUpBinder.start();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };
    /**
     * 识别控制器，使用MyRecognizer控制识别的流程
     */
    protected MyRecognizer myRecognizer;
    Handler handler=new Handler() {
        @Override
        public void handleMessage(Message msg) {//语音识别的通信
            super.handleMessage(msg);
            handleMsg(msg);
        }
    };
    private Handler handler_son;//传递给自然语音分析模块的子线程的通信

    /**
     * 首先分析是否存在特殊App名，如果存在则直接发送给openActivity;若没有再发送给子线程
     * @param message
     */
    private void handleMsg(final Message message){
        final String requirement=message.obj.toString();
        if(requirement.contains("[过程值]")) {
            String process=requirement.substring(0,requirement.length()-6);
            printTxt(process);
            return;
        }
        printTxt(requirement);
        String judgement=Judge.judge(requirement);
        Log.d("testFriend",judgement);
        if(judgement.equals("1")) {
            Iterator iter = specialHashName.maps.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry entry = (Map.Entry) iter.next();
                Object key = entry.getKey();
                if (requirement.contains((String) key)) {
                    List<String> list = new ArrayList<>();
                    list.add((String) key);
                    openActivity(list);
                    return;
                }
            }
            sendMessage(requirement);
        }
        else{
            if(judgement.equals(Judge.Memorandum)){
                DataBaseUtils.addAppItems("内嵌备忘录");
                MemorandumHelper memorandumHelper=new MemorandumHelper(textToVoice);
                Log.d("testMem",requirement);
                memorandumHelper.process(requirement);
            }
            if(judgement.equals(Judge.Weather)){
                Log.d("testEle",judgement);
                DataBaseUtils.addAppItems("内嵌天气");
                WeatherHelper weatherHelper=new WeatherHelper(this,textToVoice);
                String city=weatherHelper.getCity(requirement);
                Log.d("testWeather","all ready to open weather"+" "+city);
                weatherHelper.process(city);
            }
            if(judgement.equals(Judge.QQ)){
                DataBaseUtils.addAppItems("QQ");
                Toast.makeText(MainActivity.this,"正在进入"+"QQ",Toast.LENGTH_SHORT).show();
                String people= QHelper.getPeople(requirement);
                String content=QHelper.getContent(requirement);
                if(people==null||content==null)
                    return;
                AccessService.QQ_saying=content;
                Log.d("testQQ","原本的内容为"+content);
                AccessService.QQ_people=people;
               // QHelper.openQQ(MainActivity.this,1,people);
                List<String> list=new ArrayList<>();
                list.add("QQ");
                openActivity(list);
            }
            if(judgement.equals(Judge.WeChat)){
                DataBaseUtils.addAppItems("微信");
                String people=QHelper.getPeople(requirement);
                String content=QHelper.getContent(requirement);
                if(people==null||content==null)
                    return;
                AccessService.WeChat_people=people;
                AccessService.WeChat_saying=content;
                Log.d("testWechat",people+"  "+content);
                List<String> list=new ArrayList<>();
                list.add("微信");
                openActivity(list);
            }
            if(judgement.equals(Judge.SCAN)){
                DataBaseUtils.addAppItems("微信");
                AccessService.Scan=true;
                List<String> list=new ArrayList<>();
                list.add("微信");
                openActivity(list);
            }
            if(judgement.equals(Judge.FRIEND)){
                DataBaseUtils.addAppItems("微信");
                AccessService.Friend=true;
                List<String> list=new ArrayList<>();
                list.add("微信");
                openActivity(list);
            }
            if(judgement.equals(Judge.ELE)){
                Log.d("testEle",ELE_RANDOM+"");
                if(ELE_RANDOM) {
                    DataBaseUtils.addAppItems("饿了吗");
                    randomSelect();
                }
                if(!ELE_RANDOM){
                    Log.d("testEle","已进入饿了吗专区");
                    DataBaseUtils.addAppItems("饿了吗");
                    String key = EleHelper.getKey(requirement);
                    if(key==null)
                        return;
                    AccessService.ELE_saying = key;
                    List<String> list = new ArrayList<>();
                    list.add("饿了吗");
                    openActivity(list);
                }
                ELE_RANDOM=false;
            }
            if(judgement.equals(Judge.DIAL)){
                DataBaseUtils.addAppItems("电话");
                String name=PhoneHelper.getPeopleTele(requirement);
                if(name==null)
                    return;
                Phone.callPhoneThroughContacts(name,MainActivity.this);
            }
            if(judgement.equals(Judge.SMS)){
                DataBaseUtils.addAppItems("短信");
                String name=PhoneHelper.getPeopleSMS(requirement);
                String content=PhoneHelper.getContent(requirement);
                if(name==null||content==null)
                    return;
                Phone.sendSMS(name,content,MainActivity.this);
            }
            if(judgement.equals(Judge.Class_Table)){
                DataBaseUtils.addAppItems("内置课程表");
                Intent intent=new Intent(MainActivity.this,kechengbiaoActivity.class);
                startActivity(intent);
            }
            if(judgement.equals(Judge.kuaidi)){
                DataBaseUtils.addAppItems("内置查快递");
                Intent intent=new Intent(MainActivity.this,KuaiDiActivity.class);
                startActivity(intent);
            }
        }
    }
    /**
     * 用于自然语言分析线程
     * @param msg
     */
    private void sendMessage(String msg){
        if(handler_son!=null){
            Message message=Message.obtain();
            message.obj=msg;
            handler_son.sendMessage(message);
        }
    }

    /**
     * 目前存在饿了吗类似于问句的应用无法处理
     * @param names
     */
    public void openActivity(List<String> names){
        if(names.isEmpty()){
            return;
        }
        String name=names.get(0);
        if(AppName.maps.containsKey(name)){
            DataBaseUtils.addAppItems(name);
            Toast.makeText(MainActivity.this,"正在进入"+name,Toast.LENGTH_SHORT).show();
            PackageManager packageManager = getPackageManager();
            Intent intent =packageManager.getLaunchIntentForPackage(AppName.maps.get(name));
            startActivity(intent);
        }
    }
    public void randomSelect(){
        String store=EleHelper.analyse();
        Log.d("testEle","已选择"+store);
        if(store!=null) {
            AccessService.ELE_saying = store;
            AccessService.random = true;
            List<String> list=new LinkedList<>();
            list.add("饿了吗");
            openActivity(list);
        }
    }
    public void initAccess(){
        if (!AccessService.isStart()) {
            try {
                this.startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
            } catch (Exception e) {
                this.startActivity(new Intent(Settings.ACTION_SETTINGS));
                e.printStackTrace();
            }
        }
    }
    public void printFreq(){
        Intent intent=new Intent(MainActivity.this,FreqActivity.class);
        startActivity(intent);
        Log.d("testFreq","ready to open FreqActivity");
    }
    public void printTxt(String content){
        this.requirement.setText(content);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d("testLive","start");
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d("testLive","create");
        initView();
        initPermission();
        initMyRecognizer();
        nlp=new NLP();
        new ConnectThread().start();
        this.textToVoice=new TextToVoice(this);
        LitePal.getDatabase();
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.acc:
                initAccess();
                Toast.makeText(this, "进入无障碍服务的修改", Toast.LENGTH_SHORT).show();
                break;
            case R.id.freq:
                printFreq();
                Toast.makeText(this, "显示app使用频率", Toast.LENGTH_SHORT).show();
                break;
            case R.id.database:
                LitePal.getDatabase();
                Toast.makeText(this, "创建数据库成功", Toast.LENGTH_SHORT).show();
                break;
            case R.id.help:
                Toast.makeText(this, "即将进入帮助界面", Toast.LENGTH_SHORT).show();
                printHelp();
                break;
            default:
                break;
        }
        return true;
    }
    public void printHelp(){
        Intent intent=new Intent(MainActivity.this,HelpActivity.class);
        startActivity(intent);
    }
    @Override
    protected void onPause() {
        super.onPause();
        Log.d("testLive","pause");
    }

    @Override
    protected void onStop() {
        myRecognizer.release();
        startService();
        super.onStop();
        Log.d("testLive","stop");
    }

    /**
     * 第一次启动时，方法不执行
     * 接下来如果遇到语音唤醒相当于重启，所以不执行
     * 但如果是遇到手动返回，则执行该方法，手动注销服务
     */
    @Override
    protected void onResume() {
        super.onResume();
        Intent intent = new Intent(this, WakeUpService.class);
        stopService(intent);
        Log.d("WakeUpService", "null");
        Log.d("testLive","resume");
    }
    public void startService(){
        myRecognizer.release();
        Intent intent=new Intent(MainActivity.this,WakeUpService.class);
        bindService(intent,connection,BIND_AUTO_CREATE);
        Log.d("WakeUpService","now binding");
    }
    /**
     * 开始录音，点击“开始”按钮后调用。
     * 基于DEMO集成2.1, 2.2 设置识别参数并发送开始事件
     */
    protected void start() {
        // DEMO集成步骤2.1 拼接识别参数： 此处params可以打印出来，直接写到你的代码里去，最终的json一致即可。
        final Map<String, Object> params = new HashMap<>();
        params.put(SpeechConstant.PID, 1537); // 中文输入法模型，有逗号
        // params 也可以根据文档此处手动修改，参数会以json的格式在界面和logcat日志中打印
        Log.i(TAG, "设置的start输入参数：" + params);

        // 复制此段可以自动检测常规错误
        (new AutoCheck(getApplicationContext(), new Handler() {
            public void handleMessage(Message msg) {
                if (msg.what == 100) {
                    AutoCheck autoCheck = (AutoCheck) msg.obj;
                    synchronized (autoCheck) {
                        //String message = autoCheck.obtainErrorMessage(); // autoCheck.obtainAllMessage();
                        //txtLog.append(message + "\n");
                        ; // 可以用下面一行替代，在logcat中查看代码
                        // Log.w("AutoCheckMessage", message);
                    }
                }
            }
        }, enableOffline)).checkAsr(params);

        // 这里打印出params， 填写至您自己的app中，直接调用下面这行代码即可。
        // DEMO集成步骤2.2 开始识别
        myRecognizer.start(params);
    }

    /**
     * 开始录音后，手动点击“停止”按钮。
     * SDK会识别不会再识别停止后的录音。
     * 基于DEMO集成4.1 发送停止事件 停止录音
     */
    protected void stop() {
       myRecognizer.stop();
    }

    /**
     * 开始录音后，手动点击“取消”按钮。
     * SDK会取消本次识别，回到原始状态。
     * 基于DEMO集成4.2 发送取消事件 取消本次识别
     */
    protected void cancel() {
       this.requirement.setText("");
        myRecognizer.cancel();
        textToVoice.stop();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d("testLive","restart");
        unbindService(connection);
        initMyRecognizer();
        requirement.setText("");
    }

    /**
     * 销毁时需要释放识别资源。
     */
    @Override
    protected void onDestroy() {

        // 如果之前调用过myRecognizer.loadOfflineEngine()， release()里会自动调用释放离线资源
        // 基于DEMO5.1 卸载离线资源(离线时使用) release()方法中封装了卸载离线资源的过程
        // 基于DEMO的5.2 退出事件管理器
        myRecognizer.release();
        this.textToVoice.onDestroy();
        //Log.i(TAG, "onDestory");

        // BluetoothUtil.destory(this); // 蓝牙关闭
        Log.d("testLive","destory");

        super.onDestroy();
    }
    private void initView(){
        start=(Button)findViewById(R.id.start);
        stop=(Button)findViewById(R.id.stop);
        cancel=(Button)findViewById(R.id.cancel);
        requirement=(AutoCompleteTextView)findViewById(R.id.requirment);
        ArrayAdapter adapter = new ArrayAdapter(this, //定义匹配源的adapter
                android.R.layout.simple_dropdown_item_1line, COUNTRIES);
        requirement.setThreshold(1); //设置最小提示字符数量为1
        requirement.setAdapter(adapter); //为AutoCompleteTextView控件设置适配器
        requirement.setDropDownHeight(800); //设置下拉框的高度
        start.setOnClickListener(this);
        stop.setOnClickListener(this);
        cancel.setOnClickListener(this);
    }
    private void initPermission() {
        String permissions[] = {
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.INTERNET,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.CALL_PHONE,
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.SEND_SMS
        };
        ArrayList<String> toApplyList = new ArrayList<String>();
        for (String perm : permissions) {
            if (PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(this, perm)) {
                toApplyList.add(perm);
                // 进入到这里代表没有权限.
            }
        }
        String tmpList[] = new String[toApplyList.size()];
        if (!toApplyList.isEmpty()) {
            ActivityCompat.requestPermissions(this, toApplyList.toArray(tmpList), MANIFEST_CODE);
        }
    }
    public void test(){
        Message message=new Message();
        message.obj="用备忘录删除有关于天气的内容";
        handleMsg(message);
    }
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode){
            case MANIFEST_CODE:
                if(grantResults.length>0){
                    for(int grant:grantResults){
                        if(grant!=PackageManager.PERMISSION_GRANTED){
                            Toast.makeText(MainActivity.this,"您需要同意全部权限",Toast.LENGTH_LONG).show();
                            finish();
                            return;
                        }
                    }
                }
                break;
            default:
                break;
        }
    }
    private void initMyRecognizer(){
        // 基于DEMO集成第1.1, 1.2, 1.3 步骤 初始化EventManager类并注册自定义输出事件
        // DEMO集成步骤 1.2 新建一个回调类，识别引擎会回调这个类告知重要状态和识别结果
        IRecogListener listener = new MessageStatusRecogListener(handler);
        // DEMO集成步骤 1.1 1.3 初始化：new一个IRecogListener示例 & new 一个 MyRecognizer 示例,并注册输出事件
        myRecognizer = new MyRecognizer(this, listener);
        if (enableOffline) {
            // 基于DEMO集成1.4 加载离线资源步骤(离线时使用)。offlineParams是固定值，复制到您的代码里即可
            Map<String, Object> offlineParams = OfflineRecogParams.fetchOfflineParams();
            myRecognizer.loadOfflineEngine(offlineParams);
        }

        // BluetoothUtil.start(this,BluetoothUtil.FULL_MODE); // 蓝牙耳机开始，注意一部分手机这段代码无效
    }

    class ConnectThread extends Thread{
        @Override
        public void run() {
            super.run();
            Looper.prepare();
            handler_son=new Handler(){
                @Override
                public void handleMessage(Message msg) {
                    super.handleMessage(msg);
                    String requirement=(String)msg.obj;
                    JSONObject jsonObject=nlp.getJSONObject(requirement);
                    List<String> name=nlp.fromPos(jsonObject,"nz");
                    openActivity(name);
                }
            };
            Looper.loop();
        }
    }
    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.start:
                start();
                break;
            case R.id.stop:
                stop();
                break;
            case R.id.cancel:
                cancel();
                break;
            default:
                break;
        }
    }


}
