package com.lucene_assignment;

import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;

public class Stemmer {
    public static void Stem(Path file) {
        try (InputStream stream = Files.newInputStream(file)) {
            String text = new String(Files.readAllBytes(file));
            StringReader reader = new StringReader(text);
            StandardTokenizer standardTokenizer = new StandardTokenizer();
            standardTokenizer.setReader(reader);
//            System.out.println("-- Normal Text --" + "\n" + text + "\n");
            TokenStream tokenStream = StopWordElimination(standardTokenizer);
            CharTermAttribute term = tokenStream.addAttribute(CharTermAttribute.class);
            tokenStream.reset();
//            System.out.println("-- After Porter Stemmer --");
            tokenStream = new PorterStemFilter(tokenStream);
            while (tokenStream.incrementToken()) {
//                System.out.print(term.toString() + " ");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static TokenStream StopWordElimination(StandardTokenizer standardTokenizer) {
        CharArraySet STOP_WORDS_SET = EnglishAnalyzer.ENGLISH_STOP_WORDS_SET;
        return new StopFilter(standardTokenizer, STOP_WORDS_SET);
    }
}
