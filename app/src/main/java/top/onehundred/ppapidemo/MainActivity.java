package top.onehundred.ppapidemo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import top.onehundred.ppapi.PPAPIListener;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    ProgressBar progressBar;

    TextView textView;

    Button button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        textView = (TextView) findViewById(R.id.textView);
        button = (Button) findViewById(R.id.button);

        button.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        final GetBooksAPI getBooksAPI = new GetBooksAPI(this);
        getBooksAPI.keyword = "harry potter";
        getBooksAPI.doPost(new PPAPIListener() {
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
                textView.setText("");
                for(Book book : getBooksAPI.books){
                    textView.append(book.name + " " + book.author + "\n");
                }
            }

            @Override
            public void onFail(int errCode, String errMessage) {
                Toast.makeText(MainActivity.this, errMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
