package top.onehundred.ppapi;

/**
 * listener for client
 * Created by LiuPeng on 2018/4/11.
 */

public interface PPAPIListener {

    /**
     * onStart
     */
    void onStart();

    /**
     * on finish, will call before onCallSuccess or onCallFail
     */
    void onFinish();

    /**
     * on success
     * @param result
     */
    void onSuccess(String result);

    /**
     * on fail, you can call this method manual to interrupt the call
     * @param errCode the error code
     * @param errMessage the error message
     */
    void onFail(int errCode, String errMessage);
}
