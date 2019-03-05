package app.com.detectionapp.BackgroundService.SendMessgeService;

/**
 * author : test
 * date : 2019/2/23 19:34
 * description :
 */
public interface onReceiveMsgListener {

    void onMsgReceive(ProgramInfo.ReceiveMsg msg);
    //这里为设置了更新前端列表的回调
}
