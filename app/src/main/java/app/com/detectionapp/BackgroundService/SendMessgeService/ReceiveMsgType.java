package app.com.detectionapp.BackgroundService.SendMessgeService;

public enum ReceiveMsgType {
    UnknownReceiveMsg(0),
    RequestForHashId(1),
    InfoDetectionResult(2);

    private int val;

    ReceiveMsgType(int val) {
        this.val = val;
    }

    public int getVal() {
        return val;
    }

    public static ReceiveMsgType valueOf(int value)
    {
        switch (value)
        {
            case 0:
                return  UnknownReceiveMsg;
            case 1:
                return RequestForHashId;
            case 2:
                return InfoDetectionResult;
            default:
                return UnknownReceiveMsg;
        }
    }
}
