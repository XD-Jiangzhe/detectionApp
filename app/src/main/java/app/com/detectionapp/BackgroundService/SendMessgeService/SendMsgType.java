package app.com.detectionapp.BackgroundService.SendMessgeService;

public enum SendMsgType {
    Unused(0),
    TransferTxt(1),
    PermissionList(2);

    private int val;

    SendMsgType(int val) {
        this.val = val;
    }

    public int getVal() {
        return val;
    }

    public static SendMsgType valueOf(int value)
    {
        switch (value)
        {
            case 0:
                return  Unused;
            case 1:
                return TransferTxt;
            case 2:
                return PermissionList;
            default:
                return Unused;
        }
    }

}


