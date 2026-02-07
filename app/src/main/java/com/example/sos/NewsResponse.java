package com.example.sos;

import java.util.List;

public class NewsResponse {
    public String status;
    public int totalResults;
    public List<Article> articles;

    public static class Article {
        public Source source;
        public String author;
        public String title;
        public String description;
        public String url;
        public String urlToImage;
        public String publishedAt;
        public String content;
    }

    public static class Source {
        public String id;
        public String name;
    }
}
