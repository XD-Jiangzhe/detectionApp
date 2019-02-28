package app.com.detectionapp.Utils;

import android.util.Log;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * author : test
 * date : 2019/1/20 10:21
 * description :
 */
public class ThreadUtils {

    private static final String TAG = "ThreadUtils";

    /**
     * 检测file 是否存在
     * @param dirname
     * @return
     */
    public static boolean fileExits (String dirname)
    {
        File file = new File(dirname);
        if(!file.exists())
        {
            return false;
        }
        return true;
    }

    public static void  RunCommandExecutionInThread(String cmd, boolean  isRoot)
    {
        new Thread()
        {
            @Override
            public void run() {
                CommandExecution.CommandResult res = CommandExecution.execCommand(cmd, isRoot);
            }
        }.start();
    }

}
