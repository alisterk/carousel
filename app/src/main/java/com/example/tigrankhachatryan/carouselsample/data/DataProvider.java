package com.example.tigrankhachatryan.carouselsample.data;


import com.example.tigrankhachatryan.carouselsample.model.Article;

import java.util.List;

public interface DataProvider {
    List<Article> getArticles();
}
