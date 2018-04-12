package top.onehundred.ppapidemo;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by LiuPeng on 2018/4/11.
 */

public class GetBooksAPI extends BasicAPI<List<Book>> {

    public String keyword;

    List<Book> books = new ArrayList<>();

    public GetBooksAPI(Context context) {
        super(context);
        setCacheTime(10);
    }

    @Override
    protected void putInputs() throws Exception {
        super.putInputs();
        putParam("keyword", keyword);
        putHeader("Authorization", "Basic QWxhZGRpbjpvcGVuIHNlc2FtZQ==");

    }


    @Override
    protected void parseOutput(String output) throws Exception {
        books.clear();
        JSONArray array = new JSONArray(output);
        for(int i = 0 ; i< array.length(); i++){
            JSONObject o = array.getJSONObject(i);
            Book book = new Book();
            book.id = o.optString("id");
            book.name = o.optString("name");
            book.author = o.optString("author");
            books.add(book);
        }
    }

    @Override
    public String getUrl() {
        return "/api/books.php";
    }

}
