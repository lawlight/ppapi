package top.onehundred.ppapidemo;

import android.content.Context;

import org.json.JSONObject;

import top.onehundred.ppapi.PPAPI;

/**
 * Created by LiuPeng on 2018/4/11.
 */

public abstract class BasicAPI<T> extends PPAPI {

    public BasicAPI(Context context) {
        super(context);
    }

    @Override
    public String getHostname() {
        //these url are invalid ,just for demo
        if(BuildConfig.DEBUG){
            return "http://www.onehundred.top/debug";
        }else{
            return "http://www.onehundred.top/release";
        }
    }

    @Override
    protected void putInputs() throws Exception {
        putParam("token", "123456");
    }

    @Override
    protected String filterOutput(String output) throws Exception {
        JSONObject jsonObject = new JSONObject(output);
        if(jsonObject.getInt("code") == 200){
            return jsonObject.getString("data");
        }else{
            onFail(jsonObject.getInt("code"), jsonObject.getString("message"));
            return null;
        }
    }
}
