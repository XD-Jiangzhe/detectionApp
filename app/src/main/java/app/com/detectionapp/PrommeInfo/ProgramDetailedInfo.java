package app.com.detectionapp.PrommeInfo;

import android.graphics.drawable.Drawable;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * author : test
 * date : 2019/1/15 18:56
 * description :展示 program 的详细信息
 */

public class ProgramDetailedInfo  extends  Programme implements Serializable {

    private String _description;
    private String _start_time;
    private String _package_name;
    private String _detection_time;//检测时间

    private ArrayList<String> _dangerousPermissionList= null;
    private String isTagMalware = null;
    private String _system_message = null;

    public String getIsTagMalware() {
        return isTagMalware;
    }

    public void setIsTagMalware(String isTagMalware) {
        this.isTagMalware = isTagMalware;
    }

    public ArrayList<String> get_dangerousPermissionList() {
        return _dangerousPermissionList;
    }

    public String get_system_message() {
        return _system_message;
    }

    public void set_system_message(String _system_message) {

        this._system_message = _system_message;
    }


    public ProgramDetailedInfo(Drawable  icon,  String name, int pid, String process_name,String description, String start_time, String package_name) {
        super(icon, name, pid, process_name);
        _description = description;
        _start_time = start_time;
        _package_name = package_name;
    }

    public void set_dangerousPermissionList(ArrayList<String> permissions)
    {
        _dangerousPermissionList = new ArrayList<>(permissions);
    }

    public String get_description() {
        return _description;
    }

    public void set_description(String _description) {
        this._description = _description;
    }

    public String get_start_time() {
        return _start_time;
    }

    public void set_start_time(String _start_time) {
        this._start_time = _start_time;
    }

    public String get_package_name() {
        return _package_name;
    }

    public void set_package_name(String _package_name) {
        this._package_name = _package_name;
    }

    public String get_detection_time() {
        return _detection_time;
    }

    public void set_detection_time(String _detection_time) {
        this._detection_time = _detection_time;
    }
}
