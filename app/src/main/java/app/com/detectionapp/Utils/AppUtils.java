package app.com.detectionapp.Utils;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;


import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.util.Log;

/**
 * 应用程序工具类
 * 
 * @author qiulong
 * 
 */
public class AppUtils {

	private List<ApplicationInfo> appList;
	private PackageManager _pm ;

	private static final String TAG = "AppUtils";

	public AppUtils(Context context) {
		// 通过包管理器，检索所有的应用程序
		_pm = context.getPackageManager();
		appList = _pm
				.getInstalledApplications(PackageManager.GET_UNINSTALLED_PACKAGES);
	}

	/**
	 * 通过包名返回一个应用的Application对象
	 * 
	 * @param pkgName
	 * @return ApplicationInfo
	 */
	public ApplicationInfo getApplicationInfo(String pkgName) {
		if (pkgName == null) {
			return null;
		}
		for (ApplicationInfo appinfo : appList) {
			if (pkgName.equals(appinfo.processName)) {
				return appinfo;
			}
		}
		return null;
	}

	/**
	 *
	 * @param pkgName
	 * @return 通过包名 获取 packageinfo
 	 */
	public PackageInfo getPackageInfo(String pkgName)
	{	try {
			PackageInfo packageInfo = _pm.getPackageInfo(pkgName, PackageManager.GET_ACTIVITIES |PackageManager.GET_PERMISSIONS);
			return packageInfo;
		}catch (Throwable ignore)
		{}
		return null;
	}

	/**
	 * 获取安装时间
	 * @param packageInfo
	 * @return
	 */
	public String getFirstInstallTime(PackageInfo packageInfo)
	{
		if(packageInfo != null) {
			SimpleDateFormat sdf = new SimpleDateFormat(" yyyy年MM月dd日  HH:mm:ss ");
			long installDateTime = packageInfo.firstInstallTime;
			return sdf.format(new Date(installDateTime));
		}
		return null;
	}

	/**
	 * 获取安装的路径
	 * @param applicationInfo
	 * @return
	 */
	public String getInstallPath(ApplicationInfo  applicationInfo)
	{
		if(applicationInfo != null)
		{
			return applicationInfo.sourceDir;
		}
		return null;
	}


}
