package app.com.detectionapp.TabInfo;


import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.jaredrummler.android.processes.AndroidProcesses;
import com.jaredrummler.android.processes.models.AndroidAppProcess;
import com.jaredrummler.android.processes.models.Stat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import app.com.detectionapp.PrommeInfo.Programme;
import app.com.detectionapp.PrommeInfo.ProgrammeAdapter;
import app.com.detectionapp.R;
import app.com.detectionapp.Utils.AppUtils;

/**
 * author : test
 * date : 2019/1/16 18:51
 * description :
 */
public class ContactsFragment extends Fragment {

    private View view;
    private RecyclerView recyclerView;
    private static final String TAG = "ContactsFragment";
    private ArrayList<Programme> _programmes = new ArrayList<>();
    private HashSet<String> _programmes_name = new HashSet<>();
    private ProgrammeAdapter adapter;

    private static final int RESULT_CANCELED = 0;
    private boolean fromOncreate = false;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        fromOncreate = true;
        Log.d(TAG, "onCreateView: jiangzhe 1234 oncreate");
        if (view == null) {
            view = inflater.inflate(R.layout.fragment_contect, container, false);
            recyclerView = (RecyclerView) view.findViewById(R.id.list_view);
            LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
            recyclerView.setLayoutManager(layoutManager);

            getAllUserProcessChange();

            adapter = new ProgrammeAdapter(getActivity(), _programmes);
            Log.d(TAG, "onCreateView: jiangzhe" + _programmes.toString());

            recyclerView.setAdapter(adapter);
        }
        ViewGroup parent = (ViewGroup) view.getParent();
        if (parent != null) {
            parent.removeView(view);
        }
        return view;
    }

    //获取所有的用户app 的信息
    private AppUtils appUtils;
    private PackageManager packageManager;
    public void getAllUserProcessChange()
    {
        appUtils = new AppUtils(getActivity());
        List<AndroidAppProcess> processes = AndroidProcesses.getRunningAppProcesses();

        packageManager = getActivity().getPackageManager();
        for(AndroidAppProcess process : processes)
        {
            try
            {
                addUserProcess(process);
            }
            catch (IOException e)
            {
                Log.d(TAG, "getAllUserProcess: IO exception");
            }
        }
    }

    public void addUserProcess(AndroidAppProcess process ) throws  IOException
    {
        ApplicationInfo app = appUtils.getApplicationInfo(process.name);

        String processName = process.name;
        Stat stat = process.stat();
        int pid = stat.getPid();

        if(isUserProcess(app)) {
            /*筛选出个人应用*/
            String applicationName = getApplicationName(app);

            if (!_programmes_name.contains(applicationName)) {
                _programmes.add(new Programme(app.loadIcon(packageManager), applicationName, pid, processName));
                _programmes_name.add(applicationName);
            }
        }
    }


    private boolean isUserProcess(ApplicationInfo app)
    {
        //如果为空，或者为当前app时
        if (app == null || getActivity().getPackageName().equals(app.packageName)) {
            return false;
        }
        if((app.flags & app.FLAG_SYSTEM) > 0)
            return false;
        return true;
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult:jiangzhe in this code fragment");
        if (requestCode == Activity.RESULT_FIRST_USER) {
            if (resultCode == RESULT_CANCELED) {
                Bundle bundle = data.getExtras();
                Log.d(TAG, "onActivityResult: jiagnzhe name" + bundle.getString("name"));
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if(!fromOncreate) {
            refresh();
        }
        fromOncreate = false;
    }

    public void refresh()
    {
        AppUtils proutils = new AppUtils(getActivity());
        List<AndroidAppProcess> processes = AndroidProcesses.getRunningAppProcesses();
        Set<String>  nameSet = new HashSet<>();
        for(AndroidAppProcess process : processes)
        {
            nameSet.add(process.name);
        }

        ArrayList<Programme> new_program = new ArrayList<>();
        for(Programme programme : _programmes)
        {
            if(nameSet.contains(programme.get_process_name()))
            {
               new_program.add(programme);
            }
        }
        _programmes = new_program;
        adapter = new ProgrammeAdapter(getActivity(), _programmes);
        recyclerView.setAdapter(adapter);
    }



//    public void getAllUserProcess()
//    {
//        AppUtils proutils = new AppUtils(getActivity());
//        List<AndroidAppProcess> processes = AndroidProcesses.getRunningAppProcesses();
//
//        PackageManager packageManager = getActivity().getPackageManager();
//        for(AndroidAppProcess process  : processes)
//        {
//            try {
//                ApplicationInfo app = proutils.getApplicationInfo(process.name);
//
//                String processName = process.name;
//                Stat stat = process.stat();
//                int pid = stat.getPid();
//                if (app == null || getActivity().getPackageName().equals(app.packageName)) {
//                    continue;
//                }
//                if((app.flags & app.FLAG_SYSTEM) > 0)
//                    continue;
//                Log.v(TAG, "getAllUserProcess: jiangzhe" + getActivity().getPackageManager());
//
//
//
//                String applicationName  = getApplicationName(app);
//
//                if(!_programmes_name.contains(applicationName))
//                {
//                    _programmes.add(new Programme(app.loadIcon(packageManager), applicationName, pid, processName));
//                    _programmes_name.add(applicationName);
//                }
//
//            }catch (IOException e)
//            {
//                Log.d(TAG, "getAllUserProcess: ");
//            }
//        }
//    }




    public  String getApplicationName(ApplicationInfo app)
    {
        return app.loadLabel(getActivity().getPackageManager()).toString();
    }
}

