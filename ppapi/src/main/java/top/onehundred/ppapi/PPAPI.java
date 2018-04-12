package top.onehundred.ppapi;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.squareup.okhttp.Call;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.MultipartBuilder;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.CertificateFactory;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import top.onehundred.ppapi.utils.ACache;
import top.onehundred.ppapi.utils.AppUtils;
import top.onehundred.ppapi.utils.SystemUtils;

/**
 * PPAPI is a simple solution for network api, use just like a java bean object in your project.
 * define a basic abstract class extends PPAPI to handle the project common logic is recommended.<br/>
 * then you can define specific api class extends your basic abstract PPAPI class, and use it like a bean class.<br/>
 * you can call onFail or cancel to interrupt api.
 * Created by LiuPeng on 2018/4/11.
 */

public abstract class PPAPI implements IPPAPI {

    private Context mContext;

    public PPAPI(Context context) {
        this.mContext = context;
        if (mOkHttpClient == null) {
            mOkHttpClient = new OkHttpClient();
            mOkHttpClient.setConnectTimeout(timeout, TimeUnit.SECONDS);
        }
    }

    @Override
    final public Context getContext() {
        return mContext;
    }

    /**
     * OKHttpClient singleton
     */
    private static OkHttpClient mOkHttpClient;

    final public static OkHttpClient getOkHttpClient() {
        return mOkHttpClient;
    }

    //remark current task for cancel it
    private long callTag;

    private boolean isDebug = true;
    private boolean isTest = false;
    private boolean isCancel = false;

    //support File upload
    private boolean isMultipartForm = false;

    //debug tag
    private static final String TAG = "PPAPI";

    /** return code */
    /**
     * success
     */
    public static final int CODE_SUCCESS = 200;

    /**
     * error code
     */
    public static final int CODE_ERROR = -1;

    PPAPIListener mListener;

    final public void setListener(PPAPIListener listener) {
        this.mListener = listener;
    }

    private int timeout = 10;

    /**
     * the mark of a GET request
     */
    private String cacheMark;
    private int cacheTime = 0;

    private HashMap<String, Object> mParams = new HashMap<>();
    private HashMap<String, String> mHeaders = new HashMap<>();

    /**
     * use putParam or putHeader to put your api params here.<br />
     * you can override this method in your basic PPAPI class to put common param.
     */
    abstract protected void putInputs() throws Exception;

    /**
     * add a param to this api. if doGet, params will add to url as url query filed, else params are in the post body
     *
     * @param key   key of param
     * @param value value of param, if the value is a File, will auto use multipart form data mode.
     */
    final protected void putParam(String key, Object value) {
        mParams.put(key, value);
        if (value instanceof File) {
            isMultipartForm = true;
        }
    }

    /**
     * add a header to this api. there is a default header User-Agent with app basic information.
     *
     * @param key
     * @param value
     */
    final protected void putHeader(String key, String value) {
        mHeaders.put(key, value);
    }

    /**
     * first filter the output string , you can override this method in your basic PPAPI class to handle common data logic.
     *
     * @param output the result of web api return.
     * @return
     */
    abstract protected String filterOutput(String output) throws Exception;

    /**
     * parse the api output to your object. you can also write result to the public filed of you api class;
     *
     * @param output the result of filterOutput return.
     * @return the result Object, you can ignore this return and write all your result to local public filed.
     */
    abstract protected void parseOutput(String output) throws Exception;

    @Override
    final public void cancel() {
        this.isCancel = true;
        mOkHttpClient.cancel(callTag);
    }

    @Override
    final public void setTest(boolean test) {
        this.isTest = test;
    }

    @Override
    final public void setDebug(boolean debug) {
        this.isDebug = debug;
    }

    @Override
    final public void setTimeout(int timeout) {
        this.timeout = timeout;
        mOkHttpClient.setConnectTimeout(timeout, TimeUnit.SECONDS);
    }

    @Override
    final public void setCacheTime(int cacheTime) {
        this.cacheTime = cacheTime;
    }

    @Override
    final public void doGet(PPAPIListener listener) {
        doConnect(listener, "GET");
    }

    @Override
    final public void doPost(PPAPIListener listener) {
        doConnect(listener, "POST");
    }

    @Override
    final public void doPut(PPAPIListener listener) {
        doConnect(listener, "PUT");
    }

    @Override
    final public void doDelete(PPAPIListener listener) {
        doConnect(listener, "DELETE");
    }

    /**
     * do request!
     *
     * @param listener
     * @param mode     one of GET/POST/DELETE/PUT
     */
    private void doConnect(PPAPIListener listener, String mode) {
        this.mListener = listener;
        if (isTest) {
            doTestCall();
            return;
        }
        onStart();
        try {
            //put input params
            putInputs();
        } catch (Exception e) {
            e.printStackTrace();
            onFail(CODE_ERROR, "putInputs exception：" + e.getMessage());
            return;
        }

        //new okHttp request
        final Request request = getRequest(mode);
        log(request.toString());
        //read from cache
        if (cacheMark != null) {
            String data = ACache.get(getContext()).getAsString(cacheMark);
            if (data != null) {
                log("read from cache: " + data);
                try {
                    parseOutput(data);
                    onSuccess(data);
                } catch (Exception e) {
                    e.printStackTrace();
                    onFail(CODE_ERROR, "parseOutput exception：" + e.getMessage());
                }
                return;
            }
        }
        callRequest(request);
    }

    /**
     * in test mode, call parseOutput directly, and simulate 500ms delay
     */
    private void doTestCall() {
        log("test mode");
        new Thread() {
            @Override
            public void run() {
                try {
                    onStart();
                    Thread.sleep(500);
                    parseOutput(null);
                    onSuccess(null);
                } catch (Exception e) {
                    e.printStackTrace();
                    onFail(CODE_ERROR, "parseOutput exception：" + e.getMessage());
                }
            }
        }.start();
    }

    /**
     * get okHttp request
     *
     * @return okHttp request
     */
    private Request getRequest(String mode) {

        String url = getHostname() + getUrl();
        log(mode + "->" + url);
        RequestBody requestBody;
        if (isMultipartForm) {
            //file mode
            log("multipart form data, upload file.");
            requestBody = getMultipartRequestBody();
        } else {
            //normal mode
            requestBody = getRequestBody();
        }

        //build request
        Request.Builder builder = new Request.Builder();
        builder.tag(callTag);
        if (mode.equals("POST")) {
            builder.post(requestBody);
        } else if (mode.equals("GET")) {
            String paramsStr = getGetParams();
            if (!"".equals(paramsStr)) {
                if (url.contains("?")) {
                    url = url + "&" + paramsStr;
                } else {
                    url = url + "?" + paramsStr;
                }
            }
            if (cacheTime > 0) {
                cacheMark = url;
            }
        } else if (mode.equals("PUT")) {
            builder.put(requestBody);
        } else if (mode.equals("DELETE")) {
            builder.delete(requestBody);
        }
        builder.url(url);

        //add headers
        Iterator iter = mHeaders.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            String key = entry.getKey().toString();
            if (entry.getValue() == null) {
                continue;
            }
            String val = entry.getValue().toString();
            builder.addHeader(key, val);
        }
        builder.header("User-Agent",AppUtils.getAppName(getContext()) + "/" +
                AppUtils.getVersionName(getContext()) +
                " ("+ SystemUtils.getDeviceBrand()+SystemUtils.getSystemModel() +
                ", android "+SystemUtils.getSystemVersion()+")");
        return builder.build();
    }

    /**
     * get GET url params
     *
     * @return
     */
    private String getGetParams() {
        String paramsStr = "";
        Iterator iter = mParams.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            String key = entry.getKey().toString();
            if (entry.getValue() == null) {
                continue;
            }
            String val = entry.getValue().toString();
            paramsStr += key + "=" + val + (iter.hasNext() ? "&" : "");
        }
        return paramsStr;
    }

    /**
     * get POST/PUT/DELETE request body
     *
     * @return
     */
    private RequestBody getRequestBody() {
        Iterator iter = mParams.entrySet().iterator();
        FormEncodingBuilder formBody = new FormEncodingBuilder();
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            String key = entry.getKey().toString();
            if (entry.getValue() == null) {
                continue;
            }
            String val = entry.getValue().toString();
            formBody.add(key, val);
            log(key + ":" + val.toString());
        }
        //avoid null exception, put a default param app=android
        formBody.add("app", "android");
        return formBody.build();
    }

    /**
     * get multipart form request body
     *
     * @return
     */
    private RequestBody getMultipartRequestBody() {
        Iterator iter = mParams.entrySet().iterator();
        MultipartBuilder multipartBuilder = new MultipartBuilder();
        multipartBuilder.type(MultipartBuilder.FORM);
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            String key = entry.getKey().toString();
            Object val = entry.getValue();
            if (val == null) {
                continue;
            }
            if (val instanceof File) {
                //文件字段
                File file = new File(val.toString());
                RequestBody fileBody = RequestBody.create(MediaType.parse("application/octet-stream"), file);
                multipartBuilder.addFormDataPart(key, file.getName(), fileBody);
            } else {
                //普通字段
                multipartBuilder.addFormDataPart(key, val.toString());
            }
            log(key + ":" + val.toString());
        }
        return multipartBuilder.build();
    }

    /**
     * call the request
     *
     * @param request
     */
    private void callRequest(final Request request) {
        Call call = mOkHttpClient.newCall(request);
        //enqueue request
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {
                onFail(CODE_ERROR, e.getMessage());
                e.printStackTrace();
            }

            @Override
            public void onResponse(final Response response) throws IOException {
                if (response.code() == 200) {
                    try {
                        String output = response.body().string();
                        log(request.urlString() + " output：" + output);
                        //filter
                        output = filterOutput(output);
                        if (output != null) {
                            //save this output to cache
                            if (cacheMark != null) {
                                log("save cache for " + cacheTime + " second!");
                                ACache.get(getContext()).put(cacheMark, output, cacheTime);
                            }
                            parseOutput(output);
                            onSuccess(output);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        onFail(CODE_ERROR, "parseOutput exception：" + e.getMessage());
                    }
                } else {
                    String output = response.body().string();
                    log(getUrl() + " output：" + output);
                    onFail(CODE_ERROR, output);
                }
            }
        });
    }

    /**
     * callback methods
     */

    final private void onStart() {
        log("PPAPI start call At (" + getClass().getSimpleName() + ".java:0)");
        //generate new call tag;
        callTag = new Date().getTime();
        //reset states
        isCancel = false;
        isMultipartForm = false;

        cacheMark = null;

        //clear inputs
        mParams.clear();
        mHeaders.clear();

        if (mListener != null) {
            callHandler.sendEmptyMessage(100);
        }

    }

    final private void onSuccess(String result) {
        if (!isCancel && mListener != null) {
            callHandler.obtainMessage(101, result).sendToTarget();
        }
    }

    final protected void onFail(int errCode, String errMessage) {
        if (!isCancel && mListener != null) {
            callHandler.obtainMessage(102, errCode, 0, errMessage).sendToTarget();
            isCancel = true;
        }
    }

    private Handler callHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 100:
                    mListener.onStart();
                    break;
                case 101:
                    mListener.onFinish();
                    mListener.onSuccess(msg.obj.toString());
                    break;
                case 102:
                    mListener.onFinish();
                    mListener.onFail(msg.arg1, msg.obj.toString());
                    break;
            }
        }
    };

    /**
     * print log
     *
     * @param text log text
     */
    private void log(String text) {
        if (isDebug) {
            Log.d(TAG, text);
        }
    }

    /**
     * set key and certificates. if need https ,call this method in your Application onCreate()
     *
     * @param bksKey key bks InputStream，null is unilateral authentication
     * @param password password, if bksKey not null
     * @param certificates certificates，you can put .cer files in assets , use getAssets().open("xxx.cer") to get InputStream,
     *                     else you can put your cer in a String, and use new Buffer().writeUtf8(CER_XXX).inputStream() to get InputStream.
     */
    public static void setCertificates(InputStream bksKey, String password, InputStream... certificates) {
        if (mOkHttpClient == null) {
            mOkHttpClient = new OkHttpClient();
        }
        try {
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null);
            int index = 0;
            for (InputStream certificate : certificates) {
                String certificateAlias = Integer.toString(index++);
                keyStore.setCertificateEntry(certificateAlias, certificateFactory.generateCertificate(certificate));
                try {
                    if (certificate != null)
                        certificate.close();
                } catch (IOException e) {
                }
            }

            SSLContext sslContext = SSLContext.getInstance("TLS");
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.
                    getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(keyStore);

            //if bks not null
            if(bksKey != null){
                if(password == null){
                    throw new UnsupportedOperationException("password is null!");
                }
                KeyStore clientKeyStore = KeyStore.getInstance("BKS");
                clientKeyStore.load(bksKey, password.toCharArray());

                KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                keyManagerFactory.init(clientKeyStore, password.toCharArray());

                sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), new SecureRandom());
            }else{
                sslContext.init(null, trustManagerFactory.getTrustManagers(), new SecureRandom());
            }
            mOkHttpClient.setSslSocketFactory(sslContext.getSocketFactory());
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
