package app.com.detectionapp.TabInfo;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.util.Log;
import android.view.ViewGroup;

import java.util.List;

/**
 * author : test
 * date : 2019/1/16 18:56
 * description :
 */
public class MyFragmentAdapter extends FragmentStatePagerAdapter {

    private static final String TAG = "MyFragmentAdapter";

    private List<Fragment> fragmentList;
    public MyFragmentAdapter(FragmentManager fm, List<Fragment> list) {
        super(fm);
        this.fragmentList = list;
        Log.d(TAG, "MyFragmentAdapter: jiangzhe create new adapter");
    }


    @Override
    public Fragment getItem(int position)
    {
        return fragmentList.get(position);
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        return super.instantiateItem(container, position);
    }

    @Override
    public int getCount() {
        return fragmentList.size();
    }

    //2.25 修改
    @Override
    public int getItemPosition(Object object) {
        return POSITION_NONE;
    }



}
