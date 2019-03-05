package app.com.detectionapp.TabInfo;


import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;

import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import app.com.detectionapp.BackgroundService.MultiTheadPoolService;
import app.com.detectionapp.BackgroundService.SendMessgeService.ProgramInfo;
import app.com.detectionapp.BackgroundService.SendMessgeService.onReceiveMsgListener;
import app.com.detectionapp.PrommeInfo.MalwareProgrammeInfoAdapter;
import app.com.detectionapp.PrommeInfo.ProgramDetailedInfo;
import app.com.detectionapp.PrommeInfo.Programme;
import app.com.detectionapp.PrommeInfo.RemoveRecycleView.ItemRemoveRecyclerView;
import app.com.detectionapp.PrommeInfo.RemoveRecycleView.OnItemClickListener;
import app.com.detectionapp.PrommeInfo.dealMsgFromServer.XML2ProgramDetailedInfoAdapter;
import app.com.detectionapp.PrommeInfo.dealMsgFromServer.programDetailedInfos2MsgAdapter;
import app.com.detectionapp.R;

/**
 * author : test
 * date : 2019/1/16 19:02
 * description :
 */
public class MessageFragment extends Fragment implements  Updateable{

    private View _view;
    private ItemRemoveRecyclerView _recyclerView;
    private static final String TAG = "MessageFragment";
    private ArrayList<Programme> _programmes = new ArrayList<>();
    private MalwareProgrammeInfoAdapter _adapter;


    private boolean hasBeenFreshing = false;

    //2.23 修改，增加 service 的引用
    private MultiTheadPoolService multiTheadPoolService;
    ServiceConnection conn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {

            multiTheadPoolService = ((MultiTheadPoolService.HeapDumpBinder)service).getService();
            multiTheadPoolService.get_sendMessageClient().setOnReceiveMsgListener(new onReceiveMsgListener() {
                @Override
                public void onMsgReceive(ProgramInfo.ReceiveMsg msg) {

                    getActivity().runOnUiThread(new Runnable() {

                        //从服务器发送的msg中构造一个programinfo
                        private ProgramDetailedInfo ReceiveMsg2ProgramDetailedInfo(Activity activity, ProgramInfo.ReceiveMsg msg)
                        {
                            Resources resources =activity.getResources();
                            Drawable drawable = resources.getDrawable(R.mipmap.ic_launcher_round);

                            ProgramDetailedInfo programinfo = new ProgramDetailedInfo(drawable, msg.getAppName()
                                    , 1, "123", "123", "haha", "123123");
                            programinfo.setIsTagMalware(msg.getAppType());
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
                            ProgramDetailedInfo programinfo = ReceiveMsg2ProgramDetailedInfo(getActivity(), msg);

                            //从xml 文件中读取
                            ArrayList<Programme> tempProgramList = XML2ProgramDetailedInfoAdapter.XML2ProgramList(getActivity(), "message.xml");

                            tempProgramList.add(0, programinfo);
                            //将改动dump 到本地xml文件
                            programDetailedInfos2MsgAdapter.dumpProgramList2MsgXML(getActivity(), tempProgramList, "message.xml");
                            _adapter.updateProgramInfoList(tempProgramList);


                            //2.25 修复debug 将该页直接detach 掉，然后重新attach 到上面即可
                            try {
                                Fragment currentFragment = getFragmentManager().getFragments().get(1);
                                getFragmentManager().beginTransaction().detach(currentFragment).attach(currentFragment).commit();
                            }catch (IllegalStateException e)
                            {
                                Log.d(TAG, "run: jiangzhe " + e.getMessage());
                            }

                            Log.d(TAG, "onMsgReceive: jiangzhe _programmes length " + _adapter.getmProgramList().size() + " "
                            + _adapter.getmProgramList().get(0).get_name());
                        }
                    });
                }
            });
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    @Override
    public void onSaveInstanceState(Bundle outState) {
        //super.onSaveInstanceState(outState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        _view = inflater.inflate(R.layout.fragment_message, container, false);
        _recyclerView = (ItemRemoveRecyclerView) _view.findViewById(R.id.system_message_list);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        _recyclerView.setLayoutManager(layoutManager);


        //2.23 修改, 增加了 服务的绑定
        Intent intent = new Intent(getActivity(), MultiTheadPoolService.class);
        getActivity().bindService(intent, conn, Context.BIND_AUTO_CREATE);

        //从xml中读取programlist
        ArrayList<Programme> tempProgrammes = new ArrayList<>(XML2ProgramDetailedInfoAdapter.XML2ProgramList(getActivity(), "message.xml"));

        _programmes = new ArrayList<>(tempProgrammes);
        if(_adapter == null)
        {
            _adapter = new MalwareProgrammeInfoAdapter(getActivity(), _programmes);
        }


        _recyclerView.setAdapter(_adapter);
        _adapter.updateProgramInfoList(tempProgrammes);

        _recyclerView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                Toast.makeText(getActivity(), "jiangzhe clicker", Toast.LENGTH_SHORT  ).show();
            }

            @Override
            public void onDeleteClick(int position) {
                _adapter.removeItem(position);
                Log.d(TAG, "onDeleteClick: jiangzhe " + _adapter.hashCode());
                //删除后 dump 到本地 xml中保存
                programDetailedInfos2MsgAdapter.dumpProgramList2MsgXML(getActivity(), _adapter.getmProgramList(), "message.xml");
                Log.d(TAG, "onDeleteClick: jiangzhe the programmes length is " + _programmes.size());
            }
        });

        return  _view;
    }


    //更新页面列表的update函数
    public void update()
    {
        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        _recyclerView.setLayoutManager(layoutManager);
        _adapter = new MalwareProgrammeInfoAdapter(getActivity(), _programmes);
        _recyclerView.setAdapter(_adapter);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy: jiangzhe messagefragment has destroyed");
        super.onDestroy();
    }

//    如果该fragment visible 则 说明该fragment 在最前面，则将该fragment detach 掉，然后重新生成，保证为最新的
    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            Log.d(TAG, "setUserVisibleHint: jiangzhe is visible to User");
            getFragmentManager().beginTransaction().detach(this).attach(this).commit();
        }
    }

    @Override
    public void onResume() {
        //3.4 添加 来检测如果放到后台，服务器发送消息，导致前台无法直接显示的问题
        try {
            MessageFragment currentFragment = (MessageFragment)getFragmentManager().getFragments().get(1);
            ArrayList<Programme> tempProgrammes = new ArrayList<>(XML2ProgramDetailedInfoAdapter.XML2ProgramList(getActivity(), "message.xml"));
            if(currentFragment._programmes.size() != tempProgrammes.size())
            {
                getFragmentManager().beginTransaction().detach(currentFragment).attach(currentFragment).commit();
                Log.d(TAG, "onResume: jiangzhe has update the messageFragment");
            }
        } catch (IllegalStateException e) {
            Log.d(TAG, "run: jiangzhe " + e.getMessage());
        }

       super.onResume();
    }

    /*
            用来做测试用，无他用
         */
    private void test_RecycleView()
    {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-mm-dd HH:mm:ss");
        Resources resources =getActivity().getResources();
        for (int i = 0; i < 10; i++) {
            Drawable drawable = resources.getDrawable(R.mipmap.ic_launcher_round);
            Programme programme = (Programme)new ProgramDetailedInfo(drawable, Integer.toString(i), 1, "123", "123123", "haha", "123123");
            ((ProgramDetailedInfo)programme).setIsTagMalware("广告");
            ((ProgramDetailedInfo) programme).set_detection_time(df.format(new Date()));
            ((ProgramDetailedInfo)programme).set_system_message(df.format(new Date()) + " 系统检测出软件 <b>" + programme.get_name() +  "</b> 行为类似为<font color='#ff0000'><b>" + ((ProgramDetailedInfo) programme).getIsTagMalware()+ "</b></font>，请尽快处理");
            _programmes.add(programme);
        }
    }
}
