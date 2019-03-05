package app.com.detectionapp.BackgroundService;


import android.app.Activity;
import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;


import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import app.com.detectionapp.BackgroundService.SendMessgeService.ProgramInfo;
import app.com.detectionapp.BackgroundService.SendMessgeService.SendMessageClient;
import app.com.detectionapp.BackgroundService.SendMessgeService.SendMsgType;
import app.com.detectionapp.BackgroundService.ahat.convertor.Convertor;
import app.com.detectionapp.BackgroundService.ahat.model.Snapshot;
import app.com.detectionapp.BackgroundService.ahat.parser.Reader;
import app.com.detectionapp.Utils.CommandExecution;
import io.netty.util.CharsetUtil;

import static java.lang.Thread.sleep;

/**
 * author : test
 * date : 2019/1/19 21:51
 * description :
 */

/**
构造一个提交的任务的形式
 构造一个传入String 的 callable 对象，返回处理的结果
 * */
class MyCallable implements Callable<Integer>
{
    private static final String TAG = "MyCallable";

    private String  _pidnum = null;
    private Context _context;
    private String _appName;
    //客户端的指针
    private SendMessageClient _sendMessageClient;
    private Activity activity;


    public MyCallable(String pidnum, Context context, SendMessageClient sendMessageClient, String appName)
    {
        _pidnum = pidnum;
        _context = context;
        _sendMessageClient = sendMessageClient;
        _appName= appName;
    }


    @Override
    public Integer call() throws Exception {

        //2.23 注释用于测试后台的异步刷新功能
//        String transferString = dumpFile2TransferString();
        String transferString = "";
        //创建文件夹，不管失败与否，尝试创建
        testCreateDirInSD();

        Log.d(TAG, "call: jiangzhe transferstring length " + transferString.length());

        Log.d(TAG, "call: jiangzhe the path is " + _context.getFilesDir().toString());

            //发送详细信息的type为1
        ProgramInfo.SendMsg sendmsg = ProgramInfo.SendMsg.newBuilder()
                                                .setAppName(_appName)
                                                .setChannelToken(_sendMessageClient.getHashId())
                                                .setType(SendMsgType.TransferTxt.getVal())
                                                .setTransferTxt(transferString).build();
        //3.4 代码重构修改
        _sendMessageClient.sendMsgWriteAndFlush(sendmsg);
//        if(_sendMessageClient.getSocketChannel() != null)
//        {
//            Log.d(TAG, "call: channel has exits");
//        }
//        else
//        {
//            Log.d(TAG, "call: channel has crash");
//        }
//        _sendMessageClient.getSocketChannel().writeAndFlush(sendmsg);
//        Log.d(TAG, "call: jiangzhe the transferString length is " + transferString.length());

        return 0;
    }


    private void testCreateDirInSD()
    {
        File file  =new File(Environment.getExternalStorageDirectory().getPath(), "detectionDATA");
        boolean result = file.mkdir();
        Log.d(TAG, "testCreateDirInSD: jiangzhe create dir " + file.getAbsoluteFile().toString()+" " +result);
    }


    private  String dumpFile2TransferString()
    {
        //提取转换的部分
        //这里写的很奇葩。root 先在 /data/local/tmp 中heapdump 成一个文件
        //然后再将这个文件mv  到sd卡的路径下，变成全局可见
        /*
            理由：
                踩坑：
                    1、android 6+ 不允许 heapdump 到 sd卡上，只允许到 /data/local/tmp中
                    2、/data/local/tmp 这个文件夹 普通用户没有权限能够接触到
                    3、因此先dump 之后再move 到sd卡下，大坑，试了n久，妈蛋！！！！
                    4、只在真机上出现，6 的模拟器以及6以下都不会出现这个问题
        */
        String filepath = Environment.getExternalStorageDirectory().getPath() + "/detectionDATA";
        // /storage/emulated/0/detectionDATA
        String tempFiledir = "/data/local/tmp";
        Log.d(TAG, "dumpFile2TransferString: jiangzhe " + filepath);

        String filename = filepath +"/heapdata_" +Thread.currentThread().getId() + ".nhprof";
        String tempFilepath = tempFiledir + "/heapdata_" +Thread.currentThread().getId() + ".nhprof";

        CommandExecution.CommandResult result = CommandExecution.execCommand("am dumpheap " + _pidnum +" "+ tempFilepath, true);
        CommandExecution.CommandResult res = CommandExecution.execCommand("chmod 777 " + tempFilepath, true);

        Log.d(TAG, "dumpFile2TransferString: jiangzhe the command is " + "am dumpheap " + _pidnum +" "+ tempFilepath);


        File file = new File(filename);
        try{
            sleep(3000);
            CommandExecution.CommandResult ressdf = CommandExecution.execCommand("mv " + tempFilepath +" "+ filename, true);
            sleep(500);
            while (file.length() == 0) {
                sleep(2000);
                Log.d(TAG, "dumpFile2TransferString: jiangzhe filelength "+ file.exists()+ " file length : " + file.length());
            }
        }catch (InterruptedException e)
        {
            Log.d(TAG, "dumpFile2TransferString: jiangzhe interruptedException");
        }


        Log.d(TAG, "call: jiangzhe nhprof file length : "  + file.length());

        //开始转换
        String NewFileName = filepath + "/" + Thread.currentThread().getId() + ".hprof";
        File newFile = new File(NewFileName);
        String platformClassFile =
                filepath + "/" + Thread.currentThread().getId() + "_platform_class" + ".txt";

        file.renameTo(newFile);
        CommandExecution.CommandResult res1 =  CommandExecution.execCommand("cp " + filename +" "+ NewFileName, true);
        CommandExecution.CommandResult res2 = CommandExecution.execCommand("chmod 777 " + NewFileName, true);
        Log.d(TAG, "dumpFile2TransferString: jiangzhe " + res2.successMsg +" " + res2.errorMsg);
//        boolean re = false;
//        while(!re)
//        {
//            re = file.renameTo(newFile);
//            Log.d(TAG, "dumpFile2TransferString: jiangzhe rename is " + re);
//            try {
//                sleep(5000);
//            }catch (InterruptedException e)
//            {
//
//            }
//        }

        if(!newFile.exists())
        {
            Log.d(TAG, "dumpFile2TransferString: jiangzhe new file is not exists " );
        }
        Convertor convertor = new Convertor();
        if(!convertor.startConvert(NewFileName))
        {
            Log.d(TAG, "call: jiangzhe error");
            return null;
        }else
        {
            try {

                Log.d(TAG, "call: start convertor");

                Snapshot model = null;
                boolean callStack = true;
                boolean calculateRefs = true;
                int debugLevel = 0;

                model = Reader.readFile(NewFileName + Convertor.SUFFIX, callStack, debugLevel);
                model.resolve(calculateRefs);
                QueryClassInfo qci = new QueryClassInfo(model, _context);
                model = null;

                qci.process(NewFileName, platformClassFile);
                final String SUFFIX = ".txt";
                File targetfile = new File(NewFileName + SUFFIX);
                BufferedReader reader = null, reader1 = null, reader2 = null;
                String temp = null;
                String dest;
                Pattern p;
                Matcher m;

                final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                PrintStream ps = new PrintStream(baos, true, "UTF-8");

                PrintStream out = System.out;
//                PrintStream ps = new PrintStream(
//                        new File(Environment.getExternalStorageDirectory().getPath() + "/dumpdata/data_transfer_"+Thread.currentThread().getId() + SUFFIX));

                System.setOut(ps);
                reader = new BufferedReader(new FileReader(targetfile));
//            reader1 = new BufferedReader(new FileReader(permission));
                reader2 = new BufferedReader(new FileReader(platformClassFile));

                while ((temp = reader.readLine()) != null) {
                    dest = "";
                    if (temp != null) {
                        p = Pattern.compile("\\[");
                        m = p.matcher(temp);
                        dest = m.replaceAll(" ");

                        p = Pattern.compile("\\]");
                        m = p.matcher(dest);
                        dest = m.replaceAll("	");
                        p = Pattern.compile(" \\t");
                        m = p.matcher(dest);
                        dest = m.replaceAll(" ");
                        p = Pattern.compile("\\s+");
                        m = p.matcher(dest);
                        dest = m.replaceAll("#");
                    }
                    p = Pattern.compile("#[cC]lass#");
                    m = p.matcher(dest);
                    dest = m.replaceAll("#class ");
                    p = Pattern.compile("#by#");
                    m = p.matcher(dest);
                    dest = m.replaceAll(" by ");

                    p = Pattern.compile("Package");
                    m = p.matcher(dest);
                    dest = m.replaceAll("#Package");

                    p = Pattern.compile("<Default##Package>");
                    m = p.matcher(dest);
                    dest = m.replaceAll("<Default Package>");

                    System.out.print(dest);
                }

//                System.out.println("");
//                System.out.println("");
//
//                System.out.println("");
//                while ((temp = reader2.readLine()) != null) {
//                    System.out.println(temp);
//                }

                String transferString = new String(baos.toByteArray(), CharsetUtil.UTF_8);

                System.setOut(out);
                Log.d(TAG, "call: jiangzhe generator transfer file  success");

                return transferString;
            }catch (IOException e)
            {
                e.printStackTrace();
                return null;
            }
        }
    }
}
