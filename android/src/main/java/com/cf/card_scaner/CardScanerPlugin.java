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
            //是否已经读到回车符
            boolean haveCR=false;
            byte[] value=new byte[1];
            while (!isInterrupted()) {

                int length = driver.ReadData(buffer, 4096);

                if (length > 0) {

                    // 将获取的数据传入
                    //如果
                    for(int i=0;i<length;i++){
                        switch (String.valueOf(buffer[i])){

                            //回车符
                            case "13":
                                haveCR=true;
                                break;
                            case "10":
                                //换行，结束符
                                //上一个为回车符才是完整结束
                                if(haveCR){
                                    String recv = toHexString(sb.toString());        //以16进制输出
                                    // 发送数据
                                    onDataReceived(recv);
                                }else{
                                    haveCR=false;
                                }
                                //清空
                                sb.delete(0,sb.length());
                                break;
                            default:
                                value[0]= buffer[i];
                                sb.append(new String(value));
                                haveCR=false;
                                break;
                        }
                    }



                }

                try {

                    // 延时100ms
                    Thread.sleep(100);

                } catch (Exception ex) {

                }
            }
        }
    }

    /**
     * 数据返回通道
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

            case "configDevice":

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
                break;

            case "closeDevice":

                try {
                    // 关闭设备
                    this.closeDevice();

                    // 返回配置设备成功消息
                    result.success("DeviceCloseSuccess");

                }catch (Exception exception){
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
     * @throws Exception
     */
    private void openDevice() throws Exception {
        System.out.println("步骤1");
        // 先声明设备
        driver = new CH34xUARTDriver(
                (UsbManager) activity.getSystemService(Context.USB_SERVICE),
                activity.getApplicationContext(),
                ACTION_USB_PERMISSION);

        System.out.println("步骤2");

        // 检测是否支持设备
        if (!driver.UsbFeatureSupported()) {
            // 不支持设备
            throw new Exception("DeviceNoSupported");
        }

        System.out.println("步骤3");

        // 恢复设备列表
        int retval = driver.ResumeUsbList();

        System.out.println("步骤4");

        if (retval == -1) {

            System.out.println("步骤5");

            // 设备打开失败应该关闭设备
            driver.CloseDevice();


            System.out.println("步骤6");

            // 设备打开失败
            throw new Exception("DeviceOpenFail");

        } else if (retval == 0) {

            System.out.println("步骤7");

            if (driver.mDeviceConnection == null) {

                // 设备打开失败
                throw new Exception("DeviceOpenFail");

            } else {

                System.out.println("步骤8");

                if (!driver.UartInit()) {

                    System.out.println("步骤9");

                    // 设备初始化失败
                    throw new Exception("DeviceInitialixationFail");

                } else {
                    // 成功打开串口
                    System.out.println("步骤10");
                }
            }
        }
    }

    /**
     * 配置设备
     * @throws Exception
     */
    private void configDevice() throws Exception {

        System.out.println("步骤1-1");

        // 配置设备
        if (driver != null && driver.isConnected()) {
            try {

                // 设备配置：波特率，数据位，停止位，奇偶校验证位，流控制
                int baudRate = 9600;
                byte dataBit = 8;
                byte stopBit = 1;
                byte parity = 0;
                byte flowControl = 0;

                // 配置设备
                driver.SetConfig(baudRate, dataBit, stopBit, parity, flowControl);

                System.out.println("步骤1-3");
                //清空串口遗留数据
                driver.ReadData(new byte[4096], 4096);

                // 声明线程
                mReadThread = new ReadThread();

                // 开始线程
                mReadThread.start();


                System.out.println("步骤1-4");

            } catch (Exception exception) {

                // 配置设备失败
                throw new Exception("DeviceConfigFailed");
            }
        } else {
            System.out.println("步骤1-2");
        }
    }

    /**
     * 关闭设备
     * @throws Exception
     */
    private void closeDevice() throws Exception {
        // 关闭设备
        if (driver != null && driver.isConnected()) {
            try {
                // 调用关闭设备方法
                driver.CloseDevice();

                // 线程并入
                mReadThread.join();
            } catch (Exception exception) {
                // 关闭设备错误
                throw new Exception("CloseDeviceFailed");
            }
        }
    }

    /**
     * 转换16进制
     * @param result
     * @return
     */
    private String toHexString(String result) {
        try {
            //转int
            long value = Long.parseLong(result);
            //转16进制
            result = Long.toHexString(value);
            int resultLength = result.length();
            StringBuilder sb = new StringBuilder();
            //字符串转卡号（按规则排序）
            for (int i = resultLength; i > 1; i = i - 2) {
                sb.append(result.charAt(i-2));
                sb.append(result.charAt(i-1));

            }
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
