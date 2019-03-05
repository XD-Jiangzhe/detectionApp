package app.com.detectionapp.BackgroundService.SendMessgeService;

import android.util.Log;

import java.net.InetSocketAddress;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;

/**
 * author : test
 * date : 2019/1/24 21:24
 * description :
 */
public class SendMessageClient {

    private static final String TAG = "SendMessageClient";
    private final String host;
    private final int port ;
    private SocketChannel socketChannel;
    private EventLoopGroup group;

    private String _hashId;//每个客户端有一个hashid，连上服务器之后会自动分配一个全局唯一的token，作为长连接的标识

    //23修改
    private onReceiveMsgListener onReceiveMsgListener;//这里是更新前端消息列表的监听器


    public SendMessageClient(String host, int port)
    {
        this.port = port;
        this.host = host;
    }

    //3.4 重构代码，给客户端添加写入的封装，划分客户端的职责
    public void sendMsgWriteAndFlush(ProgramInfo.SendMsg msg)
    {
        if(socketChannel != null)
        {
            Log.d(TAG, "call: channel has exits");
            socketChannel.writeAndFlush(msg);
            Log.d(TAG, "call: jiangzhe the transferString length is " + msg.getTransferTxt().length());
        }
        else
        {
            Log.d(TAG, "call: channel has crash");
        }

    }

    //23 号晚 这里设置了监听器
    public void setOnReceiveMsgListener(onReceiveMsgListener onReceiveMsgListener) {
        this.onReceiveMsgListener = onReceiveMsgListener;
    }

    public onReceiveMsgListener getOnReceiveMsgListener() {
        return onReceiveMsgListener;
    }


    public String getHashId() {
        return _hashId;
    }

    public void setHashId(String hashId)
    {
        _hashId = hashId;
    }

    public SocketChannel getSocketChannel()
    {
        return socketChannel;
    }

    public EventLoopGroup getGroup()
    {
        return group;
    }

    public void start()  {
        group = new NioEventLoopGroup();
        try {
            SendMessageHandler sendMessageHandler = new SendMessageHandler(this);
            Bootstrap b = new Bootstrap();
            b.group(group)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {

                            socketChannel.pipeline().addLast(new ProtobufVarint32FrameDecoder());
                            socketChannel.pipeline().addLast(new ProtobufDecoder(ProgramInfo.ReceiveMsg.getDefaultInstance()));

                            socketChannel.pipeline().addLast(new ProtobufVarint32LengthFieldPrepender());
                            socketChannel.pipeline().addLast(new ProtobufEncoder());

                            socketChannel.pipeline().addLast(sendMessageHandler);
                        }
                    })
                    .connect(new InetSocketAddress(host, port))
                    .addListener((ChannelFutureListener) future->{
                        if(future.isSuccess()) {
                            socketChannel = (SocketChannel) future.channel();
                        }
                        else
                        {
                            future.channel().close();
                            Log.d(TAG, "start: jiangzhe channel connect fail");
                            group.shutdownGracefully();
                        }
                    }).sync();

        }catch (Throwable e)
        {
            Log.d(TAG, "start: jiangzhe errors " + e.getMessage());
        }
        finally {
            Log.d(TAG, "start: jiangzhe finish the send message client");

        }
    }
}
