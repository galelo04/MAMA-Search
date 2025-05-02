package com.mamasearch.Ranker;

public class ScoredDocument implements Comparable<ScoredDocument> {
    private final String url;
    private final String title;
    private  String snippet;
    private Double score;

    ScoredDocument(String url , String title ) {
        this.url = url;
        this.title = title;
        this.score = 0.0;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public Double getScore() {
        return score;
    }

    @Override
    public int compareTo(ScoredDocument other) {
        return Double.compare(other.score, this.score);
    }

    public String getUrl() {
        return url;
    }

    public String getTitle() {
        return title;
    }

    public String getSnippet() {
        return snippet;
    }

    public void setSnippet(String snippet) {
        this.snippet = snippet;
    }
}
