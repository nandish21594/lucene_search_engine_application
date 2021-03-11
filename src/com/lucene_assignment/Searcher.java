package com.lucene_assignment;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.store.FSDirectory;

public class Searcher {

    private static Path DOCSDIR = null;
    private static String INDEXPATH = null;
    private static String QUERY = null;
    private static String[] fields = {"title", "contents"};
    private static int hitsPerPage = 10;

    public void checkDirectoryAndSearch(String docsPath, String indexPath, String query) {
        DOCSDIR = Paths.get(docsPath);
        INDEXPATH = indexPath;
        QUERY = query;
        if (!Files.isReadable(DOCSDIR)) {
            System.out.println("Document directory '" + DOCSDIR.toAbsolutePath() + "' Doesn't exist");
            System.exit(1);
        }
        parseQueryAndSearch();
    }

    private void parseQueryAndSearch() {
        try {
            IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(INDEXPATH)));
            IndexSearcher searcher = new IndexSearcher(reader);
            searcher.setSimilarity(new BM25Similarity(1.2f, 0.75f));
            Query query = queryBuilder();
            if (query != null) {
                doSearch(searcher, query, hitsPerPage);
            }
            reader.close();
        } catch (IOException ioe) {
            System.out.println("IOException CAUSE :: " + ioe.getClass() + "\n" + "MESSAGE " + ioe.getMessage());
        }
    }

    private Query queryBuilder() {
        Query query = null;
        try {
            Analyzer analyzer = new EnglishAnalyzer();
            MultiFieldQueryParser parser = new MultiFieldQueryParser(fields, analyzer);
            parser.setDefaultOperator(QueryParser.AND_OPERATOR);
            QUERY = QUERY.trim();
            if (QUERY == null || QUERY.length() == -1) {
                return null;
            }
            if (QUERY.length() == 0) {
                return null;
            }
            query = parser.parse(QUERY);
            System.out.println("Searching for: " + query.toString(fields.toString()));

        } catch (ParseException pe) {
            System.out.println("ParseException CAUSE :: " + pe.getClass() + "\n" + "MESSAGE " + pe.getMessage());
        }
        return query;
    }

    private void doSearch(IndexSearcher searcher, Query query,
                          int hitsPerPage) throws IOException {
        TopDocs results = searcher.search(query, 5 * hitsPerPage);
        ScoreDoc[] hits = results.scoreDocs;

        int numTotalHits = Math.toIntExact(results.totalHits.value);
        System.out.println(numTotalHits + " total matching documents");

        int start = 0;
        int end = Math.min(numTotalHits, hitsPerPage);

        BufferedReader in = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));

        while (true) {
            if (end > hits.length) {
                System.out.println("Only results 1 - " + hits.length + " of " + numTotalHits + " total matching documents collected.");
                String line = in.readLine();
                if (line.length() == 0 || line.charAt(0) == 'n') {
                    break;
                }
                hits = searcher.search(query, numTotalHits).scoreDocs;
            }
            end = Math.min(hits.length, start + hitsPerPage);

            printDocumentFields(start, end, searcher, hits);

            if (numTotalHits >= end) {
                boolean quit = false;
                while (true) {
                    System.out.print("Press ");
                    if (start - hitsPerPage >= 0) {
                        System.out.print("(p)revious page, ");
                    }
                    if (start + hitsPerPage < numTotalHits) {
                        System.out.print("(n)ext page, ");
                    }
                    System.out.println("Press q to end the current search");

                    String line = in.readLine();
                    if (line.length() == 0 || line.charAt(0) == 'q') {
                        quit = true;
                        break;
                    }
                    if (line.charAt(0) == 'p') {
                        start = Math.max(0, start - hitsPerPage);
                        break;
                    } else if (line.charAt(0) == 'n') {
                        if (start + hitsPerPage < numTotalHits) {
                            start += hitsPerPage;
                        }
                        break;
                    }
                }
                if (quit) break;
                end = Math.min(numTotalHits, start + hitsPerPage);
            }
        }
    }

    private void printDocumentFields(int start, int end, IndexSearcher searcher, ScoreDoc[] hits) throws IOException {
        for (int i = start; i < end; i++) {
            Document doc = searcher.doc(hits[i].doc);
            System.out.println(i + 1 + ". \"" + doc.get(FieldConstants.FIELD_FILE_NAME) + "\"");
            if (doc.get(FieldConstants.FIELD_TITLE) != null)
                System.out.println("Title: " + doc.get(FieldConstants.FIELD_TITLE));
            if (doc.get(FieldConstants.FIELD_DATE) != null)
                System.out.println("Date: " + doc.get(FieldConstants.FIELD_DATE));
            if (doc.get(FieldConstants.FIELD_SUMMARY) != null && doc.get(FieldConstants.FIELD_SUMMARY).length() > 0)
                System.out.println("Summary: " + doc.get(FieldConstants.FIELD_SUMMARY));
            System.out.println("Path: " + doc.get(FieldConstants.FIELD_PATH));
            System.out.println("Last Modifified: " + doc.get(FieldConstants.FIELD_LAST_MODIFIED));
            System.out.println("Relevance Score: " + hits[i].score);
            System.out.println("");
        }
    }
}

