package app.com.detectionapp.AppInstallationReceiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;

import app.com.detectionapp.BackgroundService.MultiTheadPoolService;
import app.com.detectionapp.Utils.AppUtils;

/**
 * author : test
 * date : 2019/3/4 15:36
 * description : 广播接收器，当有软件安装时触发，将软件的权限以及label 传到service 中
 */
public class AppInstallReceiver extends BroadcastReceiver {

    private static final String TAG = "AppInstallReceiver";
    private AppUtils appUtils;

    //有一个netty的client 和 threadpool 的引用
    PackageManager packageManager;

    public AppInstallReceiver (){}



    @Override
    public void onReceive(Context context, Intent intent) {

        packageManager = context.getPackageManager();
        appUtils = new AppUtils(context);
        if(TextUtils.equals(intent.getAction(), Intent.ACTION_PACKAGE_ADDED))
        {
            dealWithAddedPackage(context, intent);
        }
    }


    private void dealWithAddedPackage(Context context, Intent intent)
    {
        PackageInfo packageInfo = getPackageInfo(intent);
        ArrayList<String> permissions = getPermissionList(packageInfo);
        String transferText =  serializeArraylist(permissions);
        String packageLabel = getPackageLabel(packageInfo);

        Intent newIntent = new Intent(context, MultiTheadPoolService.class);
        newIntent.putExtra("packageLabel", packageLabel);
        newIntent.putExtra("transferText", transferText);
        context.startService(newIntent);
    }


    private PackageInfo getPackageInfo(Intent intent)
    {
        String packageName = intent.getData().getSchemeSpecificPart();
        PackageInfo packageInfo = appUtils.getPackageInfo(packageName);
        return packageInfo;
    }

    private String getPackageLabel(PackageInfo packageInfo)
    {
        String packageName=  packageInfo.packageName;
        try
        {
            ApplicationInfo applicationInfo = packageManager.getApplicationInfo(packageName, 0);
            return (String) packageManager.getApplicationLabel(applicationInfo);
        }catch (Throwable e)
        {
            return "unknown";
        }
    }

    private ArrayList<String> getPermissionList(PackageInfo packageInfo)
    {
        ArrayList<String> permissions = new ArrayList<>();
        try {
            String[] requestPermissions = packageInfo.requestedPermissions;
            Log.d(TAG, "getPermissionList: jiangzhe permission length : " + requestPermissions.length);
            permissions = new ArrayList<String>(Arrays.asList(requestPermissions));

        }catch (Throwable e)
        {
            Log.d(TAG, "getPermissionList: jiangzhe " + e.getMessage());
        }
        return permissions;
    }

    private String serializeArraylist(ArrayList<String> permissions)
    {
        StringBuilder stringBuilder = new StringBuilder();
        for(String s : permissions)
        {
            stringBuilder.append(s);
            stringBuilder.append("\t");//这里每个权限用tab进行分隔开
        }
        return  stringBuilder.toString();
    }
}
