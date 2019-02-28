package app.com.detectionapp.PrommeInfo.dealMsgFromServer;

import android.app.Activity;
import android.util.Log;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;

/**
 * author : test
 * date : 2019/2/24 20:21
 * description : 用来解析xml 文件 获取message 对象
 */
public class decodeXML2MessageListFactory {

    private String filename;
    private Activity activity;
    private static final String TAG = "decodeXML2MessageListFa";

    public decodeXML2MessageListFactory(String filename, Activity activity) {
        this.filename = filename;
        this.activity = activity;
    }

    public ArrayList<messageLocal>  decodeXML2MessageList()
    {
        ArrayList<messageLocal> messages = new ArrayList<>();
        try
        {
            File file = new File(activity.getFilesDir().getAbsolutePath() + "/" + filename);
            FileInputStream fis = new FileInputStream(file);

            XmlPullParser xp = Xml.newPullParser();
            xp.setInput(fis, "utf-8");

            int type = xp.getEventType();

            messageLocal messageLocal= new messageLocal();;
            while(type != XmlPullParser.END_DOCUMENT)
            {
                switch (type)
                {
                    case XmlPullParser.START_TAG:
                        if("perMessage".equals(xp.getName()))
                        {
                            messageLocal = new messageLocal();
                        }
                        else if("index" .equals(xp.getName()))
                        {
                            int index = Integer.parseInt(xp.nextText());
                            messageLocal.setId(index);
                        }
                        else if("appName".equals(xp.getName()))
                        {
                            String appName = xp.nextText();
                            messageLocal.setAppName(appName);
                        }
                        else if("appType".equals(xp.getName()))
                        {
                            messageLocal.setAppType(xp.nextText());
                        }
                        else if("detectionTime".equals(xp.getName()))
                        {
                            messageLocal.setDetectionTime(xp.nextText());
                        }
                        else if("systemMessage".equals(xp.getName()))
                        {
                            messageLocal.setSystemMessage(xp.nextText());
                        }
                        break;
                    case XmlPullParser.END_TAG:
                        if("perMessage".equals(xp.getName()))
                        {
                            messages.add(messageLocal);
                        }
                }
                type = xp.next();
            }
            Log.d(TAG, "decodeXML2MessageList: jiangzhe get messages from xml :  " + messages.size());

        }catch (Throwable e)
        {
            Log.d(TAG, "decodeXML2MessageList: " +e.getMessage());
        }
        return messages;
    }


    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }
}
