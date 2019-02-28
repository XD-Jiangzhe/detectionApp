package app.com.detectionapp.PrommeInfo.dealMsgFromServer;

import android.app.Activity;
import android.util.Log;

import java.util.ArrayList;

import app.com.detectionapp.PrommeInfo.ProgramDetailedInfo;
import app.com.detectionapp.PrommeInfo.Programme;

/**
 * author : test
 * date : 2019/2/24 18:57
 * description : 将 programmes 的信息 转成 msg 结构体的adapter
 */
public class programDetailedInfos2MsgAdapter {

    private static final String TAG = "programDetailedInfos2Ms";
    public messageLocal programDetailed2MsgLocal(int index, Programme programDetailedInfo)
    {
        ProgramDetailedInfo temp_programDetailedInfo = (ProgramDetailedInfo)programDetailedInfo;
        return new messageLocal(index,
                temp_programDetailedInfo.get_name(),
                temp_programDetailedInfo.getIsTagMalware(),
                temp_programDetailedInfo.get_detection_time(),
                temp_programDetailedInfo.get_system_message());
    }

    public ArrayList<messageLocal> programDetailedList2MsgLocal(ArrayList<Programme> programDetailedInfos)
    {
        ArrayList<messageLocal> messageLocals = new ArrayList<>();
        for (int i = 0; i < programDetailedInfos.size(); i++) {
            messageLocals.add(programDetailed2MsgLocal(i, programDetailedInfos.get(i)));
        }
        return messageLocals;
    }

    //将programes dump 到本地message
    public  static void dumpProgramList2MsgXML(Activity activity, ArrayList<Programme> programmes, String filename)
    {
        ArrayList<messageLocal> messages = new programDetailedInfos2MsgAdapter().programDetailedList2MsgLocal( programmes);
        dumpMsgListToLocalFactory dumpMsgListToLocalFactory
                = new dumpMsgListToLocalFactory(messages, filename, activity);
        dumpMsgListToLocalFactory.dumpMsgListToLocalFile();
        Log.d(TAG, "dumpProgramList2MsgXML: jiangzhe has success dump program list to message xml");
    }
}
