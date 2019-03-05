package app.com.detectionapp.AppInstallationReceiver;

import java.util.concurrent.Callable;

import app.com.detectionapp.BackgroundService.SendMessgeService.ProgramInfo;
import app.com.detectionapp.BackgroundService.SendMessgeService.SendMessageClient;
import app.com.detectionapp.BackgroundService.SendMessgeService.SendMsgType;

/**
 * author : test
 * date : 2019/3/4 16:45
 * description : 此类为放到线程池中执行的类
 */
public class SendPermissionsCallable implements Callable<Integer> {

    private String _permissionString;
    private String _packageLabel;
    private SendMessageClient _sendMessageClient;

    public SendPermissionsCallable(String _permissionString, String _packageLabel, SendMessageClient _sendMessageClient) {
        this._permissionString = _permissionString;
        this._packageLabel = _packageLabel;
        this._sendMessageClient = _sendMessageClient;
    }

    @Override
    public Integer call() throws Exception {

        ProgramInfo.SendMsg sendMsg = ProgramInfo.SendMsg.newBuilder()
                .setType(SendMsgType.PermissionList.getVal())
                .setAppName(_packageLabel)
                .setChannelToken(_sendMessageClient.getHashId())
                .setTransferTxt(_permissionString)
                .build();
        _sendMessageClient.sendMsgWriteAndFlush(sendMsg);
        return null;
    }
}
