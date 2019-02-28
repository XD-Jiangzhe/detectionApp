package app.com.detectionapp;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.graphics.drawable.Drawable;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;
import java.util.Set;

import app.com.detectionapp.DumpHeapService.MultiTheadPoolService;
import app.com.detectionapp.PrommeInfo.*;
import app.com.detectionapp.Utils.AppUtils;


public class showProinfoActivity extends AppCompatActivity {

    private static final String TAG = "showProinfoActivity";
    private ArrayList<String> _permissionList = new ArrayList<>();

    /*页面展示的元素*/
    private ImageView programInfoIcon;
    private TextView programName;
    private TextView programInstallTime;
    private ArrayAdapter<String> adapter;
    private ListView mlistView;
    private boolean hasBind = false;
    private String _applicationName = null;

    /*向线程池中添加任务*/
    private MultiTheadPoolService.HeapDumpBinder mBinder ;
    private ServiceConnection connection = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_programinfo);


        ProgramDetailedInfo programDetailedInfo = getProgramDetailedInfoFromIntentAndSetApplicationName();
        initView(programDetailedInfo, _applicationName);
        initConnection(programDetailedInfo);


        /*加入返回箭头*/
        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        if(actionBar != null)
        {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }


    public void initConnection(ProgramDetailedInfo programDetailedInfo)
    {
        String pidString = String.valueOf(programDetailedInfo.get_pid());
//        String packageName = programDetailedInfo.get_package_name();
//        String appName = programDetailedInfo.get_name();
        Log.d(TAG, "initConnection: multithread package name" + pidString);
        connection =  new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                mBinder = (MultiTheadPoolService.HeapDumpBinder) service;
                //向线程池中添加任务
                mBinder.startHeapDump(pidString, _applicationName);
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
            }
        };
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home:
                Bundle bundle = new Bundle();
                bundle.putString("name", programName.getText().toString());
                setResult(RESULT_CANCELED, this.getIntent().putExtras(bundle));
                this.finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }


    private void initView(ProgramDetailedInfo programDetailedInfo, String application_name)
    {
        //初始化类内的成员
        initPromgramToShow(programDetailedInfo, application_name);

        /*初始化结束按钮*/
        Button fininshProcess = (Button)findViewById(R.id.programInfoKillProcess);
        fininshProcess.setOnClickListener(v->{
            ActivityManager am = (ActivityManager)getSystemService(Activity.ACTIVITY_SERVICE);
            try {
                am.killBackgroundProcesses(programDetailedInfo.get_package_name());
                Toast.makeText(getBaseContext(), "已经结束该程序", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "initView: jiangzhe kill " + programDetailedInfo.get_package_name());

            }catch (NullPointerException e)
            {
                Toast.makeText(getBaseContext(), "无法结束该该程序", Toast.LENGTH_SHORT).show();
            }
        });

        //初始化提交按钮
        Button startDumpHeap = (Button) findViewById(R.id.programInfoDumpHeap);
        startDumpHeap.setOnClickListener(v->{
            //绑定服务，并且向线程池中添加任务
            Intent bindIntent = new Intent(this, MultiTheadPoolService.class);
            hasBind = bindService(bindIntent, connection, BIND_AUTO_CREATE);
            Toast.makeText(getBaseContext(), "start dump heap", Toast.LENGTH_SHORT).show();
        });
    }


    private void initPromgramToShow(ProgramDetailedInfo programDetailedInfo, String application_name)
    {
        programInfoIcon = (ImageView)findViewById(R.id.programInfoIcon);
        programName = (TextView)findViewById(R.id.programInfoName);
        programInstallTime = (TextView)findViewById(R.id.programInfoInstallTime);

        mlistView = (ListView)findViewById(R.id.programInfoDangerousId);
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, _permissionList);
        mlistView.setAdapter(adapter);

        programInfoIcon.setImageDrawable(programDetailedInfo.get_icon());
        programName.setText(application_name);
        programInstallTime.setText(programDetailedInfo.get_start_time());
    }


    //从上一个intent中获取信息，并构造一个programDetailedInfo对象
    private ProgramDetailedInfo getProgramDetailedInfoFromIntentAndSetApplicationName()
    {
        Intent intent = getIntent();
        ProgramDetailedInfo programe = (ProgramDetailedInfo)intent.getSerializableExtra("programme");
        String process_name = intent.getStringExtra("process_name");
        String application_name = intent.getStringExtra("application_name");
        int pid = Integer.parseInt(intent.getStringExtra("pid"));

        _applicationName = application_name;

        return getProgramDetailedInfoFrom(process_name, application_name, pid);

    }


    /**
     * 从传入的数据中获取一个ProgramDetailedInfo 对象
     * @param process_name 包名
     * @param application_name 应用名
     * @return
     */
    public ProgramDetailedInfo getProgramDetailedInfoFrom(String process_name, String application_name, int pid)
    {
        AppUtils appUtils = new AppUtils(this);
        PackageInfo packageInfo =  appUtils.getPackageInfo(process_name);

        ApplicationInfo applicationInfo = appUtils.getApplicationInfo(process_name);
        String firstInstallTime = appUtils.getFirstInstallTime(packageInfo);
        String sourceDir = appUtils.getInstallPath(applicationInfo);

        //初始化危险权限
        initDangerPermissionList(packageInfo);

        return new ProgramDetailedInfo(
                applicationInfo.loadIcon(getPackageManager()), process_name, pid, process_name, sourceDir, firstInstallTime, process_name);
    }


    /**
     * 通过包信息来初始化危险权限的列表
     * @param packageInfo
     */
    private void initDangerPermissionList(PackageInfo packageInfo)
    {
        //初始化危险权限
        String[] requestPermissions = packageInfo.requestedPermissions;
        HashMap<String, String> permissionToDesc = getDangerousPermission();
        if(requestPermissions != null)
        {
            for(int i=0; i<requestPermissions.length; i++)
            {
                String key = requestPermissions[i];
                if(permissionToDesc.containsKey(key))
                {
                    _permissionList.add(permissionToDesc.get(key));
                }
            }
            Log.d(TAG, "getProgramDetailedInfoFrom: jiangzhe" + _permissionList.toString());
        }
        else
            Log.d(TAG, "onCreate: jiangzhe permission null");
    }


    /**
     * 从配置文件中获取危险的权限
     * @return
     */
    private HashMap<String ,String> getDangerousPermission()
    {
        HashMap<String, String> res = new HashMap<>();
        try{
            res = getDangerousPermissionFromProfile();
        }catch ( Throwable e)
        {
            Log.d(TAG, "getDangerousPermission: jiangzhe " + e.getMessage() );
        }
        return res;
    }


    private HashMap<String ,String> getDangerousPermissionFromProfile() throws  Throwable
    {
        Properties properties =  new Properties();
        HashMap<String, String> res = new HashMap<>();

        String config_file = "dangerous_permissions.properties";
        InputStream in = getApplicationContext().getAssets().open(config_file);
        properties.load(new InputStreamReader(in, "gb2312"));

        Set<String> keys = properties.stringPropertyNames();
        for (String key : keys) {
            res.put(key, properties.getProperty(key));
        }
        return res;
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(hasBind)
            unbindService(connection);
    }
}
