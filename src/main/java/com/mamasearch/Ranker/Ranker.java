package com.mamasearch.Ranker;

import DBClient.MongoDBClient;
import com.mamasearch.Utils.ProcessorData;
import com.mamasearch.Utils.SnippetGenerator;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.conversions.Bson;


import java.util.*;

import static com.mongodb.client.model.Filters.in;
import static com.mongodb.client.model.Projections.*;


public class Ranker {


    public List<ScoredDocument> rankDocument(ProcessorData processorData) {


        Map<Integer, Double> id_scoreMap = new HashMap<>();
        ArrayList<Document> documents = processorData.relevantDocuments;
        for (Document document : documents) {
            int id = document.getInteger("id");
            double score = document.getDouble("score");
            id_scoreMap.put(id, id_scoreMap.getOrDefault(id, 0.0) + score);
        }

        Set<Integer> relevantDocIds = id_scoreMap.keySet();
        Map<Integer, Document> docDetailsMap = new HashMap<>();
        MongoDatabase database = MongoDBClient.getDatabase();
        String COLLECTION_NAME1 = "id_data";
        if (!relevantDocIds.isEmpty()) { // Avoid empty query if map is empty
            MongoCollection<Document> collection1 = database.getCollection(COLLECTION_NAME1); // Get your collection

            // Define which fields to retrieve
            Bson projection = fields(include("id", "popularityScore", "url", "title"), excludeId()); // Exclude MongoDB's default _id if not needed

            // Execute the single query
            try (MongoCursor<Document> cursor = collection1.find(in("id", relevantDocIds))
                    .projection(projection)
                    .iterator()) {
                // Load results into an in-memory map for fast lookup
                while (cursor.hasNext()) {
                    Document doc = cursor.next();
                    docDetailsMap.put(doc.getInteger("id"), doc);
                }
            }
        }



        long totalsnippet = 0;
        long startr = System.currentTimeMillis();
        List<ScoredDocument> scoredDocumentsList = new ArrayList<>(); // Assuming you have this list

        for (Map.Entry<Integer, Double> entry : id_scoreMap.entrySet()) {
            int docId = entry.getKey();
            double relevanceScore = entry.getValue(); // TF-IDF based score

            // --- Fast Lookup from In-Memory Map ---
            Document doc = docDetailsMap.get(docId);

            // Handle cases where a doc ID from id_scoreMap might not be in collection1 (optional, depends on data consistency)
            if (doc == null) {
                System.err.println("Warning: Details not found for doc ID: " + docId);
                continue; // Skip this document
            }

            Double popularityScore = doc.getDouble("popularityScore");
            double pageRankScore = (popularityScore != null) ? popularityScore : 0.0;
            String url = doc.getString("url");
            String title = doc.getString("title");

            // --- Calculate Final Score ---
            double alpha = 0.7, beta = 0.3; // Consider making these constants
            double finalScore = alpha * relevanceScore + beta * pageRankScore;

            // --- Generating Snippets (Consider moving this - see optimization below) ---
            int TARGET_LENGTH = 250; // Consider making this a constant
            SnippetGenerator snippetGenerator = new SnippetGenerator(); // Consider creating this once outside the loop

            long start = System.currentTimeMillis();
            String snippet = snippetGenerator.generateSnippet(docId, processorData.words, TARGET_LENGTH);
            long end = System.currentTimeMillis();
            totalsnippet += (end - start);

            // --- Add to results ---
            ScoredDocument scoredDocument = new ScoredDocument(url, title, snippet, finalScore);
            scoredDocumentsList.add(scoredDocument);
        }

        long endr = System.currentTimeMillis();
        System.out.println("Total ranking loop time: " + (endr - startr) + " ms");
        System.out.println("Total snippet generation time (within loop): " + totalsnippet + " ms");

        Collections.sort(scoredDocumentsList);
        return scoredDocumentsList;
    }




        public static void main (String[]args){

//        SnippetGenerator snippetGenerator = new SnippetGenerator();
//
//        String[] arr = {"field","method","iron"};
//
//        long start = System.currentTimeMillis();
//        String s = snippetGenerator.generateSnippet(1,arr,250);
//        long end = System.currentTimeMillis();

//            System.out.println(s);
//            System.out.println("time taken" + (end-start));
            MongoDatabase database = MongoDBClient.getDatabase();
            String COLLECTION_NAME = "url_graph";
            String COLLECTION_NAME2 = "crawled_data";
            MongoCollection<Document> collection = database.getCollection(COLLECTION_NAME);
            MongoCollection<Document> collection2 = database.getCollection(COLLECTION_NAME2);

            Map<Integer, ArrayList<Integer>> pagesGraph = new HashMap<>();

            FindIterable<Document> documents = collection.find();
            for (Document doc : documents) {
                Integer id = doc.getInteger("id");
                List<Integer> links = doc.getList("extractedUrlsIds", Integer.class);
                pagesGraph.put(id, new ArrayList<>(links));
            }
            for(Map.Entry<Integer,ArrayList<Integer>> entry : pagesGraph.entrySet()){
                System.out.print(entry.getKey()+": [");
                for(Integer i : entry.getValue()){
                    System.out.print(i+", ");

                }
                System.out.println("]");
            }
//
//
            PageRanker pageRanker = new PageRanker();
//        long start = System.nanoTime();
            long start = System.currentTimeMillis();
            pageRanker.rank(pagesGraph);
//        long end = System.nanoTime();
            pageRanker.insertPages(collection2);
            long end = System.currentTimeMillis();

pageRanker.printPages();
            System.out.println("Time taken to rank: " + (end - start) + " ms");


        }
    }
