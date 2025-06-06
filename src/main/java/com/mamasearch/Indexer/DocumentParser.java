package com.mamasearch.Indexer;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class DocumentParser {
    public static List<ParsedWord> ParesingandFilteringDocuments(Integer ID,String htmlContent) {
        List<ParsedWord> parsedWords = new ArrayList<>();
        Set<String> processedText = new HashSet<>(); // To track processed text
        int position = 0;

        Document doc = Jsoup.parse(htmlContent);

        // Title
        String title = doc.title();
        if (!title.isEmpty()) {
            position = addWords(parsedWords, title, "title", position);
            processedText.add(title);
        }

        // h1
        Elements h1Tags = doc.select("h1");
        position = extractAndAdd(parsedWords, h1Tags, "h1", position, processedText);

        // h2
        Elements h2Tags = doc.select("h2");
        position = extractAndAdd(parsedWords, h2Tags, "h2", position, processedText);

        // h3
        Elements h3Tags = doc.select("h3");
        position = extractAndAdd(parsedWords, h3Tags, "h3", position, processedText);

        // Normal (rest of body, excluding already processed elements)
        Elements bodyElements = doc.body().select("*"); // Select all elements in body

        savePlainTextToFile(ID, bodyElements); // Save plain text to file

        for (Element element : bodyElements) {
            // Skip h1, h2, h3, and script/style elements
            if (element.tagName().matches("h1|h2|h3|script|style")) {
                continue;
            }
            String text = element.ownText().trim(); // Use ownText to avoid text from child elements
            if (!text.isEmpty() && !processedText.contains(text)) {
                position = addWords(parsedWords, text, "normal", position);
                processedText.add(text);
            }
        }

        return parsedWords;
    }

    private static int extractAndAdd(List<ParsedWord> list, Elements elements, String tag, int startPosition, Set<String> processedText) {
        for (Element element : elements) {
            String text = element.text().trim();
            if (!text.isEmpty() && !processedText.contains(text)) {
                startPosition = addWords(list, text, tag, startPosition);
                processedText.add(text);
            }
        }
        return startPosition;
    }

    private static int addWords(List<ParsedWord> list, String text, String tag, int startPosition) {
        List<String> words = Arrays.asList(text.split("\\s+"));
        words = Tokenizer.filter(words); // Filter and stem words
        for (String word : words) {
            if (!word.isEmpty()) { // Skip empty words
                list.add(new ParsedWord(word, tag, startPosition++));
            }
        }
        return startPosition;
    }

    private static void savePlainTextToFile(Integer ID, Elements bodyElements) {
        StringBuilder plainTextBuilder = new StringBuilder();
        for (Element element : bodyElements) {
            if (element.tagName().matches("script|style")) continue;

            String text = element.ownText().trim();
            if (!text.isEmpty()) {
                plainTextBuilder.append(text).append("\n");
            }
        }

        String content = plainTextBuilder.toString();

        try {
            // Use reliable relative path from project root
            Path directoryPath = Paths.get("src", "main", "resources", "PlainText");
            Files.createDirectories(directoryPath);

            Path filePath = directoryPath.resolve(ID + ".txt");

            try (BufferedWriter writer = Files.newBufferedWriter(filePath)) {
                writer.write(content);
            }

            // Debugging tip: Print full absolute path
//            System.out.println("Saved file at: " + filePath.toAbsolutePath());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}