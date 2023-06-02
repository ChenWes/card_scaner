package com.cf.card_scaner;

import android.app.Activity;
import android.content.Context;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

import cn.wch.ch34xuartdriver.CH34xUARTDriver;
import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;

/**
 * CardScanerPlugin
 */
public class CardScanerPlugin implements FlutterPlugin, MethodCallHandler, ActivityAware, EventChannel.StreamHandler {
    /// The MethodChannel that will the communication between Flutter and native Android
    ///
    /// This local reference serves to register the plugin with the Flutter Engine and unregister it
    /// when the Flutter Engine is detached from the Activity
    private MethodChannel channel;

    private static final String ACTION_USB_PERMISSION = "cn.wch.wchusbdriver.USB_PERMISSION";

    private Activity activity;
    private CH34xUARTDriver driver;

    // 事件通知
    private EventChannel.EventSink mEventSink;

    // 通过
    private Handler mHandler = new Handler(Looper.getMainLooper());

    // 运行线程
    private ReadThread mReadThread;

    /**
     * 运行线程，持续提取数据
     */
    private class ReadThread extends Thread {
        byte[] buffer = new byte[4096];


        @Override
        public void run() {
            super.run();
            StringBuilder sb = new StringBuilder();

            // 是否已经读到回车符
            boolean haveCR = false;
            // 正常读取数据
            byte[] value = new byte[1];

            while (!isInterrupted()) {

                // 获取串口数据长度
                int length = driver.ReadData(buffer, 4096);

                if (length > 0) {
                    
                    // 将获取的数据循环处理
                    for (int i = 0; i < length; i++) {
                        switch (String.valueOf(buffer[i])) {

                            case "13":
                                //回车符
                                haveCR = true;

                                break;
                            case "10":
                                //换行，结束符
                                //上一个为回车符才是完整结束
                                if (haveCR) {

                                    // 将正常的数据以16进制进行转换（记住，这里是将所有数据整体合并后再进行进制的转换）
                                    String recv = toHexString(sb.toString());

                                    // 发送数据
                                    onDataReceived(recv);
                                } else {
                                    haveCR = false;
                                }

                                // 清空
                                sb.delete(0, sb.length());

                                break;
                            default:
                                // 正常读取
                                value[0] = buffer[i];

                                // 将数据加入
                                sb.append(new String(value));

                                // 正常的数据即将回车符标识设置为Falose
                                haveCR = false;

                                break;
                        }
                    }


                }

                // 将延时取消，看是否会出现乱码数据
//                try {
//
//                    // 延时100ms
//                    Thread.sleep(100);
//
//                } catch (Exception ex) {
//
//                }
            }
        }
    }

    /**
     * 数据返回通道
     *
     * @param newString
     */
    protected void onDataReceived(final String newString) {
        if (mEventSink != null) {

            mHandler.post(new Runnable() {
                @Override
                public void run() {

                    // 通过数据流发送数据
                    mEventSink.success(newString);
                }
            });
        }
    }

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
        channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "card_scaner");
        channel.setMethodCallHandler(this);

        // 声明将有数据返回
        final EventChannel eventChannel = new EventChannel(flutterPluginBinding.getBinaryMessenger(), "card_scaner/event");
        eventChannel.setStreamHandler(this);
    }

    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
        switch (call.method) {
            case "getPlatformVersion":

                // 回复获取平台版本消息
                result.success("Android " + android.os.Build.VERSION.RELEASE);
                break;

            case "openDevice":

                try {
                    // 打开设备
                    this.openDevice();

                    // 返回打开设备成功消息
                    result.success("DeviceOpenSuccess");

                } catch (Exception exception) {
                    System.out.println("打开设备出现错误，Ex：" + exception.getMessage());

                    // 返回打开设备失败消息
                    result.error("DeviceOpenFail", "DeviceOpenFail", exception);
                }
                break;

           /* case "configDevice":

                try {
                    // 配置设备
                    this.configDevice();

                    // 返回配置设备成功消息
                    result.success("DeviceConfigSuccess");

                } catch (Exception exception) {
                    System.out.println("配置设备出现错误，Ex：" + exception.getMessage());

                    // 返回配置设备失败消息
                    result.error("DeviceConfigFail", "DeviceConfigFail", exception);
                }
                break;*/

            case "closeDevice":

                try {
                    // 关闭设备
                    this.closeDevice();

                    // 返回配置设备成功消息
                    result.success("DeviceCloseSuccess");

                } catch (Exception exception) {
                    System.out.println("关闭设备出现错误，Ex：" + exception.getMessage());

                    // 返回配置设备失败消息
                    result.error("DeviceCloseFail", "DeviceCloseFail", exception);
                }

                break;

            default:
                // 回复未提供方法消息
                result.notImplemented();
        }
    }

    /**
     * 打开设备
     *
     * @throws Exception
     */
    private void openDevice() throws Exception {
        // 先声明设备
        driver = new CH34xUARTDriver(
                (UsbManager) activity.getSystemService(Context.USB_SERVICE),
                activity.getApplicationContext(),
                ACTION_USB_PERMISSION);

        // 检测是否支持设备
        if (!driver.UsbFeatureSupported()) {
            // 不支持设备
            throw new Exception("DeviceNoSupported");
        }
        //获取权限
        int retval = driver.ResumeUsbPermission();
        if(retval==0){
            // 恢复设备列表
            retval = driver.ResumeUsbList();

            if (retval == -1) {

                // 设备打开失败应该关闭设备
                driver.CloseDevice();

                // 设备打开失败
                throw new Exception("DeviceOpenFail");

            } else if (retval == 0) {

                if (driver.mDeviceConnection == null) {

                    // 设备打开失败
                    throw new Exception("DeviceOpenFail");

                } else {

                    if (!driver.UartInit()) {

                        System.out.println("步骤9");

                        // 设备初始化失败
                        throw new Exception("DeviceInitialixationFail");

                    }
                    //配置设备
                    configDevice();
                }
            }
        }else{
            driver.CloseDevice();
            // 设备打开失败
            throw new Exception("DeviceOpenFail");
        }


    }

    /**
     * 配置设备
     *
     * @throws Exception
     */
    private void configDevice() throws Exception {

        // 配置设备
        if (driver != null && driver.isConnected()) {
            try {

                // 设备配置：波特率，数据位，停止位，奇偶校验证位，流控制
                // 后期如果需要增加配置功能，需要将该接口进行扩展
                int baudRate = 9600;
                byte dataBit = 8;
                byte stopBit = 1;
                byte parity = 0;
                byte flowControl = 0;

                // 配置设备
                driver.SetConfig(baudRate, dataBit, stopBit, parity, flowControl);

                //清空串口遗留数据
                driver.ReadData(new byte[4096], 4096);

                // 声明线程
                mReadThread = new ReadThread();

                // 开始线程
                mReadThread.start();

            } catch (Exception exception) {

                // 配置设备失败
                throw new Exception("DeviceConfigFailed");
            }
        } else {
            throw new Exception("DeviceNotOpen");
        }
    }

    /**
     * 关闭设备
     *
     * @throws Exception
     */
    private void closeDevice() throws Exception {
        // 关闭设备
        if (driver != null && driver.isConnected()) {
            try {
                // 调用关闭设备方法
                driver.CloseDevice();

                // 线程停止
                mReadThread.interrupt();
            } catch (Exception exception) {
                // 关闭设备错误
                throw new Exception("CloseDeviceFailed");
            }
        }
    }

    /**
     * 转换16进制
     *
     * @param result
     * @return
     */
    private String toHexString(String result) {
        try {
            //转int
            long value = Long.parseLong(result);

            //转16进制，是将整组数据转换成16进制，而不是单个转换
            result = Long.toHexString(value);

            // 返回数据
            StringBuilder sb = new StringBuilder();

            //字符串转卡号（按规则排序），每次取两位并倒序组合
            for (int i = result.length(); i > 1; i = i - 2) {

                sb.append(result.charAt(i - 2));
                sb.append(result.charAt(i - 1));
            }

            // 将数据
            return sb.toString().toUpperCase();

        } catch (Exception e) {
            Log.d("toHexString", e.toString());
        }

        return result;
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        channel.setMethodCallHandler(null);
    }

    @Override
    public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {
        activity = binding.getActivity();
    }

    @Override
    public void onDetachedFromActivityForConfigChanges() {

    }

    @Override
    public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {

    }

    @Override
    public void onDetachedFromActivity() {

    }

    @Override
    public void onListen(Object arguments, EventChannel.EventSink events) {
        mEventSink = events;
    }

    @Override
    public void onCancel(Object arguments) {
        mEventSink = null;
    }
}
