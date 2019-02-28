package app.com.detectionapp.PrommeInfo;

import android.graphics.drawable.Drawable;

import java.io.Serializable;

public class Programme implements Serializable {
    /**
     *描述一个正在后台运行的进程
     图标，名称，pid，所用内存,包名
     */

    private  Drawable _icon;
    private  String _name;
    private  int _pid;
    private String _process_name;


    public Programme(Drawable _icon, String _name, int _pid, String process_name) {
        this._icon = _icon;
        this._name = _name;
        this._pid = _pid;
        this._process_name = process_name;
    }

    public void set_process_name(String _process_name) {
        this._process_name = _process_name;
    }

    public String get_process_name() {

        return _process_name;
    }

    public Drawable get_icon() {
        return _icon;
    }

    public String get_name() {
        return _name;
    }

    public int get_pid() {
        return _pid;
    }


    public void set_icon(Drawable _icon) {
        this._icon = _icon;
    }

    public void set_name(String _name) {
        this._name = _name;
    }

    public void set_pid(int _pid) {
        this._pid = _pid;
    }

}
