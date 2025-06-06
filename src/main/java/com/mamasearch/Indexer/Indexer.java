package com.mamasearch.Indexer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.ArrayList;

public class Indexer {
    private final Map<String, Map<Integer, WordData>> invertedIndex = new HashMap<>();



    public void processDocuments(List<DocumentData> documents) {
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        for (DocumentData document : documents) {
            executor.submit(() -> {
                List<ParsedWord> filteredWords = document.getFilteredWords();
                for (ParsedWord pw : filteredWords) {
                    synchronized (this) {
                        addWord(pw.getWord(), document.getID(), pw.getPosition());
                    }
                }
            });
        }

        executor.shutdown();
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Map<String, Map<Integer, Double>> tfidfScores = TFIDFScorer.calculateTFIDF(documents);
        for (String word : tfidfScores.keySet()) {
            Map<Integer, Double> docs = tfidfScores.get(word);
            for (Integer ID : docs.keySet()) {
                synchronized (this) {
                    addScore(word, ID, docs.get(ID));
                }
            }
        }
    }
//    public void processDocuments(List<DocumentData> documents) {
//
//        for (DocumentData document : documents) {
//            List<ParsedWord> filteredWords = document.getFilteredWords();
//            for (ParsedWord pw : filteredWords) {
//                addWord(pw.getWord(), document.getUrl(), pw.getPosition());
//            }
//        }
//
//        Map<String, Map<String, Double>> tfidfScores = TFIDFScorer.calculateTFIDF(documents);
//
//        for (String word : tfidfScores.keySet()) {
//            Map<String, Double> docs = tfidfScores.get(word);
//            for (String url : docs.keySet()) {
//                addScore(word, url, docs.get(url));
//            }
//        }
//    }


    public Map<String, Map<Integer, WordData>> getInvertedIndex() {
        return invertedIndex;
    }

    public void addWord(String word, Integer ID, int position) {
        invertedIndex
                .computeIfAbsent(word, k -> new HashMap<>())
                .computeIfAbsent(ID, k -> new WordData())
                .addPosition(position);
    }

    public void addScore(String word, Integer ID, double score) {
        if (invertedIndex.containsKey(word) && invertedIndex.get(word).containsKey(ID)) {
            invertedIndex.get(word).get(ID).setScore(score);
        }
    }

    public void printIndex() {
        for (String word : invertedIndex.keySet()) {
            System.out.println("Word: " + word);
            Map<Integer, WordData> docs = invertedIndex.get(word);
            for (Integer ID : docs.keySet()) {
                WordData data = docs.get(ID);
                System.out.println("  ID: " + ID);
                System.out.println("    Positions: " + data.getPositions());
                System.out.println("    TF-IDF Score: " + data.getScore());
            }
        }
    }

}
