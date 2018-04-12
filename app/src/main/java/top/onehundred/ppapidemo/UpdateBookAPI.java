package top.onehundred.ppapidemo;

import android.content.Context;

/**
 * Created by LiuPeng on 2018/4/12.
 */

public class UpdateBookAPI extends BasicAPI {

    public String bookId;
    public String name;
    public String author;

    public UpdateBookAPI(Context context) {
        super(context);
    }

    @Override
    protected void putInputs() throws Exception {
        super.putInputs();
        putParam("name", name);
        putParam("author", author);
        putHeader("Authorization", "Basic QWxhZGRpbjpvcGVuIHNlc2FtZQ==");
    }

    @Override
    public String getUrl() {
        return "/api/books/"+bookId;
    }

    @Override
    protected void parseOutput(String output) throws Exception {
        //do nothing~~
    }
}
