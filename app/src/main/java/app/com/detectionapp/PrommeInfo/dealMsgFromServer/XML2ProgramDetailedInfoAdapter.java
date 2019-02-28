package app.com.detectionapp.PrommeInfo.dealMsgFromServer;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.Log;

import java.util.ArrayList;

import app.com.detectionapp.PrommeInfo.ProgramDetailedInfo;
import app.com.detectionapp.PrommeInfo.Programme;
import app.com.detectionapp.R;

/**
 * author : test
 * date : 2019/2/25 9:04
 * description :xml转 messagelist
 */
public class XML2ProgramDetailedInfoAdapter {

    private static final String TAG = "XML2ProgramDetailedInfo";

    public static ArrayList<Programme>  XML2ProgramList(Activity activity, String filename)
    {
        ArrayList<messageLocal> messageLocals = XML2MessageList(filename, activity);
        return messageList2ProgramDetailedList(activity, messageLocals);
    }

    public static ArrayList<messageLocal> XML2MessageList(String filename, Activity activity)
    {
        decodeXML2MessageListFactory  decodeXML2MessageListFactory
                = new decodeXML2MessageListFactory(filename, activity);
        ArrayList<messageLocal> messageLocalList =decodeXML2MessageListFactory.decodeXML2MessageList();
        Log.d(TAG, "XML2MessageList: jiangzhe parser xml get messages length " + messageLocalList.size());
        return messageLocalList;
    }

    public static ArrayList<Programme> messageList2ProgramDetailedList(Activity activity, ArrayList<messageLocal> messageLocals)
    {
        ArrayList<Programme> result  = new ArrayList<>();
        for (messageLocal messageLocal : messageLocals) {
            //会加入种类的判断，恶意软件和 良性软件有不同的图标
            result.add(Msg2ProgramDetailedInfo(activity, messageLocal));
        }
        return result;
    }

    /*
    将msg 转成programdetailed
     */
    public static ProgramDetailedInfo Msg2ProgramDetailedInfo(Activity activity, messageLocal messageLocal)
    {
        Resources resources =activity.getResources();
        Drawable drawable = resources.getDrawable(R.mipmap.ic_launcher_round);
        ProgramDetailedInfo programDetailedInfo = new ProgramDetailedInfo(drawable, messageLocal.getAppName(),
                123, messageLocal.getAppName(), messageLocal.getAppName(), "12341234", "12340");
        programDetailedInfo.set_detection_time(messageLocal.getDetectionTime());
        programDetailedInfo.setIsTagMalware(messageLocal.getAppType());
        programDetailedInfo.set_system_message(messageLocal.getSystemMessage());
        return programDetailedInfo;
    }

}
