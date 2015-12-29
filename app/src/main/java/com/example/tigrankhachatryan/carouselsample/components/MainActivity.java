package com.example.tigrankhachatryan.carouselsample.components;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;

import com.example.tigrankhachatryan.carouselsample.R;
import com.example.tigrankhachatryan.carouselsample.data.DataProvider;
import com.example.tigrankhachatryan.carouselsample.data.FakeDataProvider;
import com.example.tigrankhachatryan.carouselsample.model.Article;

import java.util.List;


public class MainActivity extends AppCompatActivity {

    private DataProvider dataProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recycler_view);

        ArticleAdapter adapter = new ArticleAdapter();
        adapter.setHasStableIds(true); // important to set
        recyclerView.setAdapter(adapter);
        dataProvider = new FakeDataProvider(this);
        List<Article> articles = dataProvider.getArticles();
        adapter.setArticles(articles);

    }

    @Override
    protected void onDestroy() {
        dataProvider = null;
        super.onDestroy();
    }


}
