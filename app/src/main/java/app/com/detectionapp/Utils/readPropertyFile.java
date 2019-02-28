package app.com.detectionapp.Utils;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;



/**
 * author : test
 * date : 2019/2/22 16:05
 * description : 读取配置文件返回property
 */
public class readPropertyFile {


    private static final String TAG = "readpropertyFile";

    /*
        上下文，配置文件名
        返回property对象
     */
    public Properties propertiesFactory(Context context, String filename)
    {
        try {
            Properties properties = new Properties();
            InputStreamReader ir = new InputStreamReader(context.getAssets().open(filename));
            properties.load(ir);
            return properties;
        }catch (Throwable e)
        {
            Log.d(TAG, "propertiesFactory: jiangzhe" + e.getMessage());
            return null;
        }
    }
}
