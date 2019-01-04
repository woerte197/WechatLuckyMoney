package com.example.wangyang.mywechatplugin;

import android.accessibilityservice.AccessibilityService;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.PowerManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import java.util.List;

/**
 * Created by WangYang on 2019/1/3
 */

/**
 * 抢红包Service,继承AccessibilityService
 */
public class RedPacketService extends AccessibilityService {
    /**
     * 微信几个页面的包名+地址。用于判断在哪个页面
     * LAUCHER-微信聊天界面
     * LUCKEY_MONEY_RECEIVER-点击红包弹出的界面
     * LUCKEY_MONEY_DETAIL-红包领取后的详情界面
     * BACK_DETAIL-红包详情界面返回ID
     * FIRST_ITEM-联系人列表页面第一个Item//代表联系人列表页ID
     * LIST_LUCK_MONEY_TEXT联系人列表页面的[微信红包]字样ID
     * LIST_LUCK_MONEY_COUNT联系人列表页面的回话数量ID
     */
    private String LAUCHER = "com.tencent.mm.ui.LauncherUI";
    private String LUCKEY_MONEY_DETAIL = "com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyDetailUI";
    private String LUCKEY_MONEY_RECEIVER = "com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyReceiveUI";
    private String BACK_DETAIL = "com.tencent.mm:id/jb";
    private String FIRST_ITEM = "com.tencent.mm:id/azj";
    private String LIST_LUCK_MONEY_TEXT="com.tencent.mm:id/azn";
    private String LIST_LUCK_MONEY_COUNT="com.tencent.mm:id/lu";
    /**
     * 用于判断是否点击过红包了
     */
    private boolean isOpenRP;

    private boolean isOpenDetail = false;

    /**
     * 用于判断是否屏幕是亮着的
     */
    private boolean isScreenOn;

    /**
     * 获取PowerManager.WakeLock对象
     */
    private PowerManager.WakeLock wakeLock;

    /**
     * KeyguardManager.KeyguardLock对象
     */
    private KeyguardManager.KeyguardLock keyguardLock;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        int eventType = event.getEventType();
        switch (eventType) {
            //通知栏来信息，判断是否含有微信红包字样，是的话跳转
            case AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED:
                List<CharSequence> texts = event.getText();
                for (CharSequence text : texts) {
                    String content = text.toString();
                    if (!TextUtils.isEmpty(content)) {
                        //判断是否含有[微信红包]字样
                        if (content.contains("[微信红包]")) {
                            if (!isScreenOn()) {
                                wakeUpScreen();
                            }
                            //如果有则打开微信红包页面
                            openWeChatPage(event);
                            isOpenRP = false;
                        }
                    }
                }
                break;
            //当前聊天界面与列表联系人界面
            case AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED:
                if (event.getSource() == null) {
                    return;
                }
                listRed();//列表红包逻辑
                nowRed();//当前页面红包逻辑
                break;
            //界面跳转的监听
            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
                String className = event.getClassName().toString();
                //判断是否是微信聊天界面
                if (LAUCHER.equals(className)) {
                    //获取当前聊天页面的根布局
                    AccessibilityNodeInfo rootNode = null;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        rootNode = getRootInActiveWindow();
                    }
                    //开始找红包
                    findRedPacket(rootNode);
                }

                //判断是否是显示‘开’的那个红包界面
                if (LUCKEY_MONEY_RECEIVER.equals(className)) {
                    AccessibilityNodeInfo rootNode = null;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        rootNode = getRootInActiveWindow();
                    }
                    //开始抢红包
                    openRedPacket(rootNode);
                }
                //判断是否是红包领取后的详情界面
                if (isOpenDetail && LUCKEY_MONEY_DETAIL.equals(className)) {
                    isOpenDetail = false;
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
                        List<AccessibilityNodeInfo> list = getRootInActiveWindow().findAccessibilityNodeInfosByViewId(BACK_DETAIL);
                        if (list.isEmpty() || list.size() < 1) {
                            return;
                        }
                        list.get(0).performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    }
                    //返回桌面
//                    back2Home();
                    //如果之前是锁着屏幕的则重新锁回去
                    release();
                }
                break;
        }


    }

    private void nowRed() {
        List<AccessibilityNodeInfo> byText = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            byText = getRootInActiveWindow().findAccessibilityNodeInfosByText("领取红包");
        }
        if (!byText.isEmpty()) {
            for (AccessibilityNodeInfo accessibilityNodeInfo : byText) {
                if (accessibilityNodeInfo != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        AccessibilityNodeInfo parent = accessibilityNodeInfo.getParent();
                        while (parent != null) {
                            if (parent.isClickable()) {
                                //模拟点击
                                parent.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                                isOpenRP = true;
                                break;
                            }
                            parent = parent.getParent();
                        }
                    }
                }
            }
        }
    }

    private void listRed() {
        List<AccessibilityNodeInfo> list = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            list = getRootInActiveWindow().findAccessibilityNodeInfosByViewId(FIRST_ITEM);
        }
        if (!list.isEmpty()) {
            for (AccessibilityNodeInfo nodeInfo : list) {
                if (nodeInfo == null) {
                    continue;
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    List<AccessibilityNodeInfo> redList = nodeInfo.findAccessibilityNodeInfosByViewId(LIST_LUCK_MONEY_TEXT);
                    List<AccessibilityNodeInfo> countList = nodeInfo.findAccessibilityNodeInfosByViewId(LIST_LUCK_MONEY_COUNT);
                    for (AccessibilityNodeInfo accessibilityNodeInfo : redList) {
                        if (accessibilityNodeInfo.getText() != null && accessibilityNodeInfo.getText().toString().contains("[微信红包]")) {
                            for (AccessibilityNodeInfo countNode : countList) {
                                if (accessibilityNodeInfo.getText() != null && Integer.parseInt(countNode.getText().toString()) != 0) {
                                    nodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                                }
                            }
                        }
                    }
                }

            }
        }
    }

    /**
     * 开始打开红包
     */
    private void openRedPacket(AccessibilityNodeInfo rootNode) {
        for (int i = 0; i < rootNode.getChildCount(); i++) {
            AccessibilityNodeInfo node = rootNode.getChild(i);
            if ("android.widget.Button".equals(node.getClassName())) {
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                isOpenDetail = true;
            }
            openRedPacket(node);
        }
    }


    /**
     * 遍历查找红包
     */
    private void findRedPacket(AccessibilityNodeInfo rootNode) {
        if (rootNode != null) {
            //从最后一行开始找起
            for (int i = rootNode.getChildCount() - 1; i >= 0; i--) {
                AccessibilityNodeInfo node = rootNode.getChild(i);
                //如果node为空则跳过该节点
                if (node == null) {
                    continue;
                }
                CharSequence text = node.getText();
                if (text != null && text.toString().equals("领取红包")) {
                    AccessibilityNodeInfo parent = node.getParent();
                    //while循环,遍历"领取红包"的各个父布局，直至找到可点击的为止
                    while (parent != null) {
                        if (parent.isClickable()) {
                            //模拟点击
                            parent.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                            //isOpenRP用于判断该红包是否点击过
                            isOpenRP = true;
                            break;
                        }
                        parent = parent.getParent();
                    }
                }
                //判断是否已经打开过那个最新的红包了，是的话就跳出for循环，不是的话继续遍历
                if (isOpenRP) {
                    break;
                } else {
                    findRedPacket(node);
                }

            }
        }
    }

    /**
     * 开启红包所在的聊天页面
     */
    private void openWeChatPage(AccessibilityEvent event) {
        //A instanceof B 用来判断内存中实际对象A是不是B类型，常用于强制转换前的判断
        if (event.getParcelableData() != null && event.getParcelableData() instanceof Notification) {
            Notification notification = (Notification) event.getParcelableData();
            //打开对应的聊天界面
            PendingIntent pendingIntent = notification.contentIntent;
            try {
                pendingIntent.send();
            } catch (PendingIntent.CanceledException e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * 服务连接
     */
    @Override
    protected void onServiceConnected() {
//        Toast.makeText(this, "抢红包服务开启", Toast.LENGTH_SHORT).show();
        super.onServiceConnected();
    }

    /**
     * 必须重写的方法：系统要中断此service返回的响应时会调用。在整个生命周期会被调用多次。
     */
    @Override
    public void onInterrupt() {
        Toast.makeText(this, "我快被终结了啊-----", Toast.LENGTH_SHORT).show();
    }

    /**
     * 服务断开
     */
    @Override
    public boolean onUnbind(Intent intent) {
        Toast.makeText(this, "抢红包服务已被关闭", Toast.LENGTH_SHORT).show();
        return super.onUnbind(intent);
    }

    /**
     * 返回桌面
     */
    private void back2Home() {
        Intent home = new Intent(Intent.ACTION_MAIN);
        home.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        home.addCategory(Intent.CATEGORY_HOME);
        startActivity(home);
    }

    /**
     * 判断是否处于亮屏状态
     *
     * @return true-亮屏，false-暗屏
     */
    private boolean isScreenOn() {
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        isScreenOn = pm.isScreenOn();
        Log.e("isScreenOn", isScreenOn + "");
        return isScreenOn;
    }

    /**
     * 解锁屏幕
     */
    private void wakeUpScreen() {

        //获取电源管理器对象
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        //后面的参数|表示同时传入两个值，最后的是调试用的Tag
        wakeLock = pm.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.FULL_WAKE_LOCK, "bright");

        //点亮屏幕
        wakeLock.acquire();

        //得到键盘锁管理器
        KeyguardManager km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        keyguardLock = km.newKeyguardLock("unlock");

        //解锁
        keyguardLock.disableKeyguard();
    }

    /**
     * 释放keyguardLock和wakeLock
     */
    public void release() {
        if (keyguardLock != null) {
            keyguardLock.reenableKeyguard();
            keyguardLock = null;
        }
        if (wakeLock != null) {
            wakeLock.release();
            wakeLock = null;
        }
    }

}