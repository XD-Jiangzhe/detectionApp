package app.com.detectionapp.DumpHeapService.SendMessgeService;

/**
 * author : test
 * date : 2019/1/24 21:28
 * description :
 */

import android.util.Log;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.CharsetUtil;


public class SendMessageHandler extends SimpleChannelInboundHandler<ProgramInfo.ReceiveMsg>{

    private SendMessageClient _sendMessageClient;

    public SendMessageHandler(SendMessageClient sendMessageClient)
    {
        super();
        _sendMessageClient= sendMessageClient;
    }

    private static final String TAG = "SendMessageHandler";

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        Log.d(TAG, "channelActive: jiangzhe channel has been active" );
        //发送连接信息type为1
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, ProgramInfo.ReceiveMsg msg) throws Exception {

        //如果type为 1 则是收到了连接应答，此时设置client 的hashid
        //如果type为 2 则是收到了检测的结果，此时通知界面已经收到的消息
        switch (msg.getType())
        {
            case 1:
                _sendMessageClient.setHashId(msg.getChannelToken());
                Log.d(TAG, "channelRead0: jiangzhe the token is " + msg.getChannelToken());
                break;
            case 2:
                Log.d(TAG, "channelRead0: jiangzhe the app " + msg.getAppName() + " type is  " + msg.getAppType());

                //2.23号 添加
                if(_sendMessageClient.getOnReceiveMsgListener() != null)
                {
                    _sendMessageClient.getOnReceiveMsgListener().onMsgReceive(msg);
                }

        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        Log.d(TAG, "exceptionCaught: jiangzhe " + cause.getMessage());
        ctx.close();
    }
}
