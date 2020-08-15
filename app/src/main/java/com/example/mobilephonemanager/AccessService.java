package com.example.mobilephonemanager;
import android.accessibilityservice.AccessibilityService;
import android.os.Bundle;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import org.litepal.LitePal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * 饿了吗外卖的接入：
 * 1、搜索关键词
 * 2、第一个数据为用户点击位置的第一个逗号之前的信息为店名
 * 3、进入第二个界面后，第二个数据为第一个逗号之前的数据（这个还需要实验加号是否也是），直到用户按下结账的按钮，第三个数据就是总价格，第四个数据就是日期（详细）
 * 那么我们需要思考的是，怎么判断按下的按钮一定是我们想要的界面呢？
 * 第一个界面的判断条件是点击的事件来自于这个应用且第一个括号中含有的所有关键词中含有该关键词，此时增加布尔量已经打开第一个界面
 * 第二个界面的判断机制是第一个布尔量为true，注意把“饿了吗”关键词踢出去，或者将没有“¥”符号的全部踢出去，当用户点击结账或者退出按钮的时候或者各种意外退出的时候第一个布尔量为false
 * 当用户点击结账后，提交数据
 */
public class AccessService extends AccessibilityService {
    public static AccessService mAccservice;
    public static String QQ_saying;
    public static String WeChat_saying;
    public static String WeChat_people;
    public static String ELE_saying;
    public static boolean random;
    public static boolean isStart() {
        return mAccservice != null;
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        mAccservice = this;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onInterrupt() {

    }

    @Override
    public void onAccessibilityEvent(final AccessibilityEvent event) {
        int eventType = event.getEventType();
        switch (eventType) {
            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
                if ("com.tencent.mobileqq".equals(event.getPackageName())) {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
                        AccessibilityNodeInfo QQNodeInfo = mAccservice.getRootInActiveWindow();
                        List<AccessibilityNodeInfo> textInfoList = QQNodeInfo.findAccessibilityNodeInfosByViewId("com.tencent.mobileqq:id/input");
                        List<AccessibilityNodeInfo> buttonInfoList = QQNodeInfo.findAccessibilityNodeInfosByText("发送");
                        if (textInfoList.size() != 0 && buttonInfoList.size() != 0) {
                            AccessibilityNodeInfo textInfo = textInfoList.get(0);
                            Bundle arguments = new Bundle();
                            arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, QQ_saying);
                            textInfo.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments);
                            AccessibilityNodeInfo buttonInfo = buttonInfoList.get(0);
                            buttonInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        }
                    }
                }
                if ("om.tencent.mm".equals(event.getPackageName())) {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
                        WechatUtils.findTextAndClick(mAccservice, "通讯录");
                        WechatUtils.findTextAndClick(mAccservice, WeChat_people);
                        WechatUtils.findTextAndClick(mAccservice, "发消息");
                        AccessibilityNodeInfo WeChatNodeInfo = mAccservice.getRootInActiveWindow();
                        List<AccessibilityNodeInfo> textList = WeChatNodeInfo.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/ajs");
                        List<AccessibilityNodeInfo> buttonList = WeChatNodeInfo.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/amb");
                        if (textList.size() != 0 && buttonList.size() != 0) {
                            AccessibilityNodeInfo textInfo = textList.get(0);
                            AccessibilityNodeInfo buttonInfo = buttonList.get(0);
                            Bundle arguments = new Bundle();
                            arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, WeChat_saying);
                            textInfo.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments);
                            buttonInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        }
                    }
                }
                if ("me.ele".equals(event.getPackageName())) {
                    if (WechatUtils.findViewIdAndClick(mAccservice, "me.ele:id/aex")) {
                        try {
                            Thread.sleep(5000);
                            WechatUtils.findViewIdAndClick(mAccservice, "me.ele:id/aex");
                            Thread.sleep(5000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        AccessibilityNodeInfo eleNodeInfo = mAccservice.getRootInActiveWindow();
                        List<AccessibilityNodeInfo> editNodeList = null;
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
                            editNodeList = eleNodeInfo.findAccessibilityNodeInfosByViewId("me.ele:id/a15");
                        }
                        if (editNodeList.size() != 0) {
                            AccessibilityNodeInfo editInfo = editNodeList.get(0);
                            Bundle arguments = new Bundle();
                            arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, ELE_saying);
                            editInfo.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments);
                            WechatUtils.findTextAndClick(mAccservice, "搜索");
                            if(random=true) {
                                random=false;
                                WechatUtils.findTextAndClick(mAccservice, ELE_saying);
                            }
                        }
                    }
                }
            case AccessibilityEvent.TYPE_VIEW_CLICKED:
                String source = event.getPackageName() + "";
                String text = event.getText() + "";
                String store="";
                Log.d("ssssaaaa",source);
                if(source.equals("me.ele")&&text.contains(ELE_saying)&&text.contains("km")&&text.contains("更多")){
                    String store1=text.split(",")[0].substring(1);
                    String store2=text.split(",")[1].substring(1);
                    try {
                        int test = Integer.parseInt(store1);
                        store=store2;
                    }catch(NumberFormatException e){
                        store=store1;
                    }finally {
                        LitePal.getDatabase();
                        Log.d("ssssaaaa","get success");
                        Date date = new Date();
                        SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd/HH/mm/ss");
                        String time = formatter.format(date);
                        Log.d("ssssaaaa","time success");
                        String[] times = time.split("/");
                        int year = Integer.parseInt(times[0]);
                        int month = Integer.parseInt(times[1]);
                        int day = Integer.parseInt(times[2]);
                        int hour = Integer.parseInt(times[3]);
                        int min = Integer.parseInt(times[4]);
                        Log.d("ssssaaaa","ready");
                        TakeOutFood takeOutFood = new TakeOutFood();
                        takeOutFood.setStore(store);
                        takeOutFood.setYear(year);
                        takeOutFood.setMonth(month);
                        takeOutFood.setDay(day);
                        takeOutFood.setHour(hour);
                        takeOutFood.setMin(min);
                        takeOutFood.save();
                        Log.d("ssssaaaa", "success");
                    }
                }
               /* final String[] store = new String[2];
                store[0] = "";
                AccessibilityNodeInfo root = getRootInActiveWindow();
                if (text.contains("km") && text.contains("更多"))
                    this.iniSelected = true;
                final List<String> food = new ArrayList<>();
                if (!source.equals("me.ele")) {
                    firstSelected = false;
                    Log.d("asasas", event.getPackageName() + "");
                }
                if (this.iniSelected && source.equals("me.ele") && text.contains(ELE_saying)) {
                    store[0] = text.split(",")[0].substring(1);
                    firstSelected = true;
                    iniSelected = true;
                    Log.d("asasas", "已进入第一个界面" + firstSelected + "     " + iniSelected);
                }
                if (event.getPackageName().equals("me.ele")&&"[]".equals(event.getText())) {
                    AccessibilityNodeInfo nodeSource = event.getSource();
                    nodeSource = nodeSource.getParent();
                    if (nodeSource != null) {
                        nodeSource = nodeSource.getParent();
                        if (nodeSource != null) {
                            nodeSource = nodeSource.getChild(0);
                            food.add(nodeSource.getText() + "");
                            Log.d("asasas", nodeSource.getText() + "");
                        }
                    }
                }
                Log.d("asasas", event.getText() + "");
                if ("[去结算]".equals(event.getText() + "")) {
                    Log.d("asasas", "已点击去结算");
                    Log.d("asasas",store+"      "+food.size());
                    if (!store.equals("") && food.size() != 0) {
                        Log.d("asasas", "正在初始化线程");
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                LitePal.getDatabase();
                                Date date = new Date();
                                SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd/HH/mm/ss");
                                String time = formatter.format(date);
                                String[] times = time.split("/");
                                int year = Integer.parseInt(times[0]);
                                int month = Integer.parseInt(times[1]);
                                int day = Integer.parseInt(times[2]);
                                int hour = Integer.parseInt(times[3]);
                                int min = Integer.parseInt(times[4]);
                                for (int i = 0; i < food.size(); i++) {
                                    TakeOutFood takeOutFood = new TakeOutFood();
                                    takeOutFood.setStore(store[0]);
                                    takeOutFood.setFood(food.get(0));
                                    takeOutFood.setYear(year);
                                    takeOutFood.setMonth(month);
                                    takeOutFood.setDay(day);
                                    takeOutFood.setHour(hour);
                                    takeOutFood.setMin(min);
                                    takeOutFood.save();
                                    firstSelected = false;
                                    Log.d("asasas", "success");
                                }
                            }
                        }).start();
                    }
                }*/
        }
    }
}