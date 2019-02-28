package app.com.detectionapp.PrommeInfo.dealMsgFromServer;

/**
 * author : test
 * date : 2019/2/24 18:58
 * description :保存到本地的message
 */
public class messageLocal {
    private int id;
    private String appName;
    private String appType;
    private String detectionTime;
    private String systemMessage;

    public messageLocal() {}

    public messageLocal(int id, String appName, String appType, String detectionTime, String systemMessage) {
        this.id = id;
        this.appName = appName;
        this.appType = appType;
        this.detectionTime = detectionTime;
        this.systemMessage = systemMessage;
    }

    public String getSystemMessage() {
        return systemMessage;
    }

    public void setSystemMessage(String systemMessage) {
        this.systemMessage = systemMessage;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getAppType() {
        return appType;
    }

    public void setAppType(String appType) {
        this.appType = appType;
    }

    public String getDetectionTime() {
        return detectionTime;
    }

    public void setDetectionTime(String detectionTime) {
        this.detectionTime = detectionTime;
    }
}
