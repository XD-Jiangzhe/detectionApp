package app.com.detectionapp.TabInfo;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import app.com.detectionapp.BackgroundService.SendMessgeService.ProgramInfo;
import app.com.detectionapp.PrommeInfo.MalwareProgrammeInfoAdapter;
import app.com.detectionapp.PrommeInfo.ProgramDetailedInfo;
import app.com.detectionapp.PrommeInfo.Programme;
import app.com.detectionapp.PrommeInfo.dealMsgFromServer.XML2ProgramDetailedInfoAdapter;
import app.com.detectionapp.PrommeInfo.dealMsgFromServer.programDetailedInfos2MsgAdapter;
import app.com.detectionapp.R;

/**
 * author : test
 * date : 2019/2/25 17:13
 * description : 该类为在 ui thread 中执行的 用于 从 服务器发来的message 中解析 得到 programinfoDetailed
 * 然后再将其加载
 * 该类并未用到，无关紧要，仅仅想着重构，但是后来未遂
 */
public class decodeMsgAndrunToUpdateUI implements  Runnable
{
    private ProgramInfo.ReceiveMsg _msg;
    private MalwareProgrammeInfoAdapter _adapter;
    private Activity _activity;

    private static final String TAG = "decodeMsgAndrunToUpdate";


    public decodeMsgAndrunToUpdateUI(Activity activity, ProgramInfo.ReceiveMsg msg, MalwareProgrammeInfoAdapter adapter ) {
        this._msg = msg;
        this._adapter =adapter;
        _activity = activity;
    }

    //从服务器发送的msg中构造一个programinfo
    public ProgramDetailedInfo ReceiveMsg2ProgramDetailedInfo()
    {
        Resources resources =_activity.getResources();
        Drawable drawable = resources.getDrawable(R.mipmap.ic_launcher_round);

        ProgramDetailedInfo programinfo = new ProgramDetailedInfo(drawable, _msg.getAppName()
                , 1, "123", "123", "haha", "123123");
        programinfo.setIsTagMalware(_msg.getAppType());
        SimpleDateFormat df = new SimpleDateFormat("yyyy-mm-dd HH:mm:ss");
        programinfo.set_detection_time(df.format(new Date()));
        ((ProgramDetailedInfo)programinfo).set_system_message(df.format(new Date()) + " 系统检测出软件 <b>"
                + programinfo.get_name() +  "</b> 行为类似为<font color='#ff0000'><b>"
                + ((ProgramDetailedInfo) programinfo).getIsTagMalware()+ "</b></font>，请尽快处理");
        return programinfo;
    }

    @Override
    public void run() {
        //定义一个回调函数，用来向arraylist 中添加新来的program
        ProgramDetailedInfo programinfo = ReceiveMsg2ProgramDetailedInfo();

        //从xml 文件中读取
        ArrayList<Programme> tempProgramList = XML2ProgramDetailedInfoAdapter.XML2ProgramList(_activity, "message.xml");

        tempProgramList.add(0, programinfo);
        //将改动dump 到本地xml文件
        programDetailedInfos2MsgAdapter.dumpProgramList2MsgXML(_activity, tempProgramList, "message.xml");
        _adapter.updateProgramInfoList(tempProgramList);

        Log.d(TAG, "onMsgReceive: jiangzhe _programmes length " + _adapter.getmProgramList().size() + " "
                + _adapter.getmProgramList().get(0).get_name());
    }
}
