package app.com.detectionapp.DumpHeapService;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import app.com.detectionapp.DumpHeapService.SendMessgeService.SendMessageClient;
import app.com.detectionapp.MainActivity;
import app.com.detectionapp.Utils.readPropertyFile;

/**
 * 线程捕获异常的函数
 */
class MyUncaughtExecptionHandler implements  Thread.UncaughtExceptionHandler
{
    private static final String TAG = "UncaughtExecptionHandle";
    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        Log.d(TAG, "uncaughtException: jiangzhe caught exception");
    }
}


class HandlerThreadFactory implements ThreadFactory
{
    @Override
    public Thread newThread( Runnable r) {
        Thread t = new Thread(r);
        t.setUncaughtExceptionHandler(new MyUncaughtExecptionHandler());
        return t;
    }
}


public class MultiTheadPoolService extends Service {
    private static final String TAG = "MultiTheadPoolService";
    private ExecutorService fixedThreadPool;

    //将其与activity 绑定
    private HeapDumpBinder  mBinder = new HeapDumpBinder();

    //客户端
    private SendMessageClient _sendMessageClient;

    //2.24 添加
    private MainActivity activity;
    public void setActivity(MainActivity activity) {
        this.activity = activity;
    }

    /*
    * 绑定器，用来向后台的线程池中投放任务
    * */
    public class HeapDumpBinder extends Binder
    {
        public  void startHeapDump(String process_name, String appName)
        {
            fixedThreadPool.submit(new MyCallable(process_name, getBaseContext(), _sendMessageClient, appName));
            Log.d(TAG, "startHeapDump multithread: jiangzhe");
        }
        public void  finishHeapDump()
        {
            Log.d(TAG, "finishHeapDump multithread: jiangzhe");
        }

        public MultiTheadPoolService getService()
        {
            return MultiTheadPoolService.this;
        }
    }


    public SendMessageClient get_sendMessageClient() {
        return _sendMessageClient;
    }

    public MultiTheadPoolService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();

        verifyStoragePermissions(activity);

        //后台线程池创建
        Properties threadPoolProperties = new readPropertyFile().propertiesFactory(getBaseContext(), "threadPool.properties");
        int threadNum = threadPoolProperties == null ? 0 : Integer.parseInt(threadPoolProperties.getProperty("threadNum"));
        fixedThreadPool = Executors.newFixedThreadPool(threadNum, new HandlerThreadFactory());

        //客户端开始运行
        try {
            runSendMessageClient();
        }catch ( Exception e)
        {
            Log.d(TAG, "onCreate: jiangzhe  client throw exception " + e.getMessage() );
        }
    }


    //客户端开始运行
    private void runSendMessageClient() throws  Exception
    {
        //读配置文件，获取ip 和端口
        Properties client_properties = new readPropertyFile().propertiesFactory(getBaseContext(), "client_info.properties");
        if(client_properties == null)
        {
            Log.e(TAG, "onCreate: jiangzhe client properties read fail");
            return;
        }
        String serverIp = client_properties.getProperty("serverIp");
        int serverPort = Integer.parseInt(client_properties.getProperty("serverPort"));
        Log.d(TAG, "onCreate: jiangzhe client  server ip " + serverIp + " server port " + serverPort);
        _sendMessageClient = new SendMessageClient(serverIp, serverPort);
        _sendMessageClient.start();
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        fixedThreadPool.shutdown();
        Log.d(TAG, "onDestroy: jiangzhe shutdown the threadPool multithread" );

        //关闭客户端的长连接
        if(_sendMessageClient != null)
        {
            _sendMessageClient.getSocketChannel().close();
            _sendMessageClient.getGroup().shutdownGracefully();
        }

    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;

    }

    //申请额外的存储权限
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE" };

    public static void verifyStoragePermissions(Activity context) {

        try {
            //检测是否有写的权限
            int permission = ActivityCompat.checkSelfPermission(context,
                    "android.permission.WRITE_EXTERNAL_STORAGE");
            if (permission != PackageManager.PERMISSION_GRANTED) {
                // 没有写的权限，去申请写的权限，会弹出对话框
                ActivityCompat.requestPermissions(context, PERMISSIONS_STORAGE,REQUEST_EXTERNAL_STORAGE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
