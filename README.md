# ppapi
easy manage android http apis, base on okHttp

Step 1. Add it in your root build.gradle at the end of repositories:
```
allprojects {
    repositories {
    ...
    maven { url 'https://jitpack.io' }
    }
}
```
Step 2. Add the dependency
```
dependencies {
    compile 'com.github.lawlight:ppapi:0.1.0'
}
```
#make you api class extends PPAPI
we use two calsses ：PPAPI and PPAPIListener。

demo: create a BasicAPI abstract class extends PPAPI.
```
public abstract class BasicAPI extends PPAPI {

    public DemoBasicAPI(Context context) {
        super(context);
    }

    @Override
    public String getHostname() {
        if(BuildConfig.DEBUG){
            return "http://www.google.com/debug";
        }else{
            return "http://www.google.com/release";
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
```
You can do some global common operation in this BasicAPI abstract class, like put global input params, getHostname by debug or release mode.
in override method filterOutput you can analysis your json, and filter a pure data json to next step parseOutput.

create specific api class GetBooksAPI extends your BasicAPI. 
```
public class GetBooksAPI extends BasicAPI {

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
    }


    @Override
    protected void parseOutput(String output) throws Exception {
        books.clear();
        JSONArray array = new JSONArray(output);
        for(int i = 0 ; i< array.length(); i++){
            JSONObject o = array.getJSONObject(i);
            Book book = new Book();
            book.name = o.optString("name");
            book.author = o.optString("author");
            book.price = o.optString("price");
            books.add(book);
        }
    }

    @Override
    public String getUrl() {
        return "/api/books";
    }
}
```
we define a public intput field keyword and a public output field books.
override putInputs method to set the keyword to api param "keyword". we can do some operation in this method like urlencode, base64.
and override getUrl method to return the api url. the final url is getHostname() + getUrl().
because we alreay handle some logic in the BasicAPI, so in this class we need not to check json validity, just analysis the pure json data in parseOutput method.
we put all json analysis logic in this api class, Activity can not consider what the json form is.
and we set a 10 second cache time in the constructor, it just available for GET method.

Then wo can use this api object in our Activity, is so easy:
```
GetBooksAPI api = new GetBooksAPI(this);
api.keyword = "harry potter";
api.doGet(this); 
```
the doGet method need a PPAPIListener, if we use anonymous class:
```
final GetBooksAPI api = new GetBooksAPI(context);
api.keyword = "harry potter";
api.doGet(new PPAPIListener() {
    @Override
    public void onStart() {
        progressBar.setVisibility(View.VISIBLE);
    }

    @Override
    public void onFinish() {
        progressBar.setVisibility(View.GONE);
    }

    @Override
    public void onSuccess(String result) {
        for(Book book : api.books){
            textView.append(book.name + book.author);
        }
    }

    @Override
    public void onFail(int errCode, String errMessage) {
        Toast.makeText(context, errMessage, Toast.LENGTH_SHORT).show();
    }
});
```
here are 4 callback in PPAPIListener：onStart、onFinish、onSuccess、onFail。
those will run on UI thread, so we can write the UI command we want.

you see ,wo set api.keyword，and int onSuccess get api.books, just like use a java bean class。
Book is a simple bean:
```
public class Book {
    public String id;
    public String name;
    public String author;
}
```
Next, we create a POST api class UpdateBookAPI:
```
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
    }
}
```
and call it:
```
UpdateBookAPI updateBookAPI = new UpdateBookAPI(context);
updateBookAPI.bookId = "1";
updateBookAPI.author = "J.K.Rowling";
updateBookAPI.name = "Harry Potter";
updateBookAPI.doPost(new PPAPIListener() {
    @Override
    public void onStart() {
        progressBar.setVisibility(View.VISIBLE);
    }

    @Override
    public void onFinish() {
        progressBar.setVisibility(View.GONE);
    }

    @Override
    public void onSuccess(String result) {
        Toast.makeText(context, "update success!", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onFail(int errCode, String errMessage) {
        Toast.makeText(context, errMessage, Toast.LENGTH_SHORT).show();
    }
});
```
except doGet and doPost, you can also call doPut or doDelete.

and we put the id input param to url as a url path, but to Activity, it not care the param should be body or url query.
we also add a header to this api by addHeader(String key, String value).the PPAPI will put a default User-Agent header with base app and phone infromation.
and int the parseOutut, we do nothing becasue the work has done in BasicAPI.

at last, int putInputs、filterOutput、parseOutput, you can throws Exception，or call onFail manual to interrupt the api。

Then we can create all out project's api to this class, and use those just like java bean. it is a easy way to manage all apis in your app project.
#other support
- test mode, user setTest(true), the api will no access internet, call parseOutput directly，simulate 500ms delay.
- cache，setCacheTime(int time), also use clearCache() to remove cache。
- https supports
- File upload supports, use putParam(String key,Object value), if value is intance of File,api will build a multipart form body request.

