package app.com.detectionapp;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTabHost;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import app.com.detectionapp.BackgroundService.MultiTheadPoolService;
import app.com.detectionapp.PrommeInfo.*;
import app.com.detectionapp.TabInfo.ContactsFragment;
import app.com.detectionapp.TabInfo.MessageFragment;
import app.com.detectionapp.TabInfo.MyFragmentAdapter;
import app.com.detectionapp.TabInfo.Tab;


public class MainActivity extends AppCompatActivity implements NoScrollViewPager.OnPageChangeListener,TabHost.OnTabChangeListener {

    private static final String TAG = "MainActivity";
    private ArrayList<Programme> _programmes = new ArrayList<>();


    /*底层tab*/
    private LayoutInflater mInflater;
    private FragmentTabHost mTabHost;
    private NoScrollViewPager mViewPager;
    private List<Tab> mTabs = new ArrayList<>(2);
    private List<Fragment> list = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /*添加tab*/
        initTab();//初始化tab
        initPager();

        /*启动服务*/
        Intent startIntent = new Intent(this, MultiTheadPoolService.class);
        startService(startIntent);

        /*申请代码权限*/
        verifyStoragePermissions(this);

    }



    //申请额外的存储权限
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE" };


    public static void verifyStoragePermissions(Activity activity) {

        try {
            //检测是否有写的权限
            int permission = ActivityCompat.checkSelfPermission(activity,
                    "android.permission.WRITE_EXTERNAL_STORAGE");
            if (permission != PackageManager.PERMISSION_GRANTED) {
                // 没有写的权限，去申请写的权限，会弹出对话框
                ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE,REQUEST_EXTERNAL_STORAGE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Intent stopIntent = new Intent(this, MultiTheadPoolService.class);
        stopService(stopIntent);
    }

    /**
     * 初始化tab
     */
    private void initPager() {
        Fragment messageFragment = new MessageFragment();
        Fragment contactsFragment = new ContactsFragment();

        list.add(contactsFragment);
        list.add(messageFragment);

        //2.25 修改
        MyFragmentAdapter myFragmentAdapter = new MyFragmentAdapter(getSupportFragmentManager(), list);

        //绑定Fragment适配器
        mViewPager.setNoScroll(true);
        mViewPager.setAdapter(myFragmentAdapter);
    }

    private View buildIndicator(Tab tab) {
        View view = mInflater.inflate(R.layout.tab_indicator,null);
        ImageView icon = (ImageView) view.findViewById(R.id.tab_icon);
        TextView text = (TextView) view.findViewById(R.id.tab_text);

        icon.setBackgroundResource(tab.getIcon());
        text.setText(tab.getTitle());
        return view;
    }


    private void initTab() {
        mViewPager = (NoScrollViewPager) findViewById(R.id.viewPager);
        mViewPager.addOnPageChangeListener(this);

        Tab tab_process = new Tab(R.string.iworker_tab_home,R.drawable.tab_icon_contact_selector ,ContactsFragment.class);
        Tab tab_message = new Tab(R.string.iworker_tab_address,R.drawable.tab_icon_message_selector, MessageFragment.class);


        mTabs.add(tab_process);
        mTabs.add(tab_message);


        mInflater = LayoutInflater.from(this);
        mTabHost = (FragmentTabHost) findViewById(android.R.id.tabhost);
        mTabHost.setup(this,getSupportFragmentManager(),R.id.viewPager);

        mTabHost.setOnTabChangedListener(this);
        /*
         *给每个Tab按钮设置图标、文字和内容
         */
        for(Tab tab:mTabs){
            TabHost.TabSpec tabSpec = mTabHost.newTabSpec(getString(tab.getTitle()));
            tabSpec.setIndicator(buildIndicator(tab));
            // 将Tab按钮添加进Tab选项卡中
            mTabHost.addTab(tabSpec,tab.getFragment(),null);

        }

        mTabHost.getTabWidget().setShowDividers(LinearLayout.SHOW_DIVIDER_MIDDLE);//设置分割线不可见
        mTabHost.setCurrentTab(0);//默认选择第一项Tab
    }
    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {

        mTabHost.setCurrentTab(position);//根据位置Postion设置当前的Tab

    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }

    @Override
    public void onTabChanged(String s) {
        int position = mTabHost.getCurrentTab();
        mViewPager.setCurrentItem(position);//把选中的Tab的位置赋给适配器，让它控制页面切换

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        SubMenu subMenu = menu.addSubMenu("");
        subMenu.add("刷新").setIcon(R.drawable.ic_action_refresh).setOnMenuItemClickListener(item->{
            return false;
        });
        subMenu.add("消息").setIcon(R.drawable.ic_action_new_email).setOnMenuItemClickListener(item->{
           return false;
        });

        MenuItem item = subMenu.getItem();
        item.setIcon(R.drawable.ic_toolbar);
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        return true;
    }

    /**
     * 设置toolbar 的属性
     */

    public void showAllProgramName()
    {
        ArrayList<String> _names  = new ArrayList<String>();
        for(Programme pro : _programmes)
        {
            _names.add(pro.get_name());
        }
        Toast.makeText(this, _names.toString(), Toast.LENGTH_SHORT).show();;
    }


    public  String getApplicationName(ApplicationInfo app)
    {
        return app.loadLabel(this.getPackageManager()).toString();
    }



}
