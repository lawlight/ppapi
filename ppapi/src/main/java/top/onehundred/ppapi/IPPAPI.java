package top.onehundred.ppapi;

import android.content.Context;

/**
 * the interface for the PPAPI class <br/>
 * Created by LiuPeng on 2018/4/11.
 */

public interface IPPAPI {

    Context getContext();

    /**
     * get the host name. you can write debug/release switch logic here
     * @return
     */
    String getHostname();

    /**
     * get the url. the final url is hostname+url. you can also put url query field here.
     * @return
     */
    String getUrl();

    /**
     * cancel request
     */
    void cancel();

    /**
     * set test mode, you can use this method to test you api offline.
     * @param test if true, do a call will not access the internet, will call parseOutput(null) directly
     */
    void setTest(boolean test);

    /**
     * set debug mode, call this method in your basic PPAPI class is recommended.
     * @param debug if false will not print log
     */
    void setDebug(boolean debug);

    /**
     * set the network timeout. default 10 sec.
     * @param timeout second of timeout
     */
    void setTimeout(int timeout);

    /**
     * set a cache time of GET call. default 0 sec., not cache
     * @param cacheTime second of GET cache time
     */
    void setCacheTime(int cacheTime);

    /**
     * do GET request
     * @param listener
     */
    void doGet(PPAPIListener listener);

    /**
     * do POST request
     * @param listener
     */
    void doPost(PPAPIListener listener);

    /**
     * do PUT request
     * @param listener
     */
    void doPut(PPAPIListener listener);

    /**
     * do DELETE request
     * @param listener
     */
    void doDelete(PPAPIListener listener);

}
