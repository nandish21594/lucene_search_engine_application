package com.lucene_assignment;

import org.apache.commons.io.FileUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Indexer {
    private static Path DOCSDIR = null;
    private static String INDEXPATH = null;

    public  void performIndex(String docsPath, String indexPath) {
        DOCSDIR = Paths.get(docsPath);
        INDEXPATH = indexPath;
        if (!Files.isReadable(DOCSDIR)) {
            System.out.println("Document directory '" + DOCSDIR.toAbsolutePath() + "' does not exist or is not readable, please check the path" + "\n");
            System.exit(1);
        }
        startIndex();
    }

    public void startIndex() {
        Date start = new Date();
        try {
            System.out.println("\n"+"Indexing to directory '" + INDEXPATH + "'..."+"\n");
            Directory dir = FSDirectory.open(Paths.get(INDEXPATH));

            Analyzer analyzer = new EnglishAnalyzer();
            IndexWriterConfig iwc = new IndexWriterConfig(analyzer);

            iwc.setSimilarity(new BM25Similarity(1.2f, 0.75f));
            iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);	/** create or update index */

            IndexWriter writer = new IndexWriter(dir, iwc);
            indexDocs(writer, DOCSDIR);
            writer.close();

            Date end = new Date();
            System.out.println();
            System.out.println(end.getTime() - start.getTime() + " total milliseconds"+"\n");


        } catch (IOException ioe) {
            System.out.println(" CAUSE " + ioe.getClass() + "\n" + "MESSAGE "+ioe.getMessage());
        }
    }



    public void indexDocs(final IndexWriter writer, Path path) throws IOException {
        if (Files.isDirectory(path)) {
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    try {
                        indexDoc(writer, file, attrs.lastModifiedTime().toMillis());
                    } catch (IOException ignore) {
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } else {
            indexDoc(writer, path, Files.getLastModifiedTime(path).toMillis());
        }
    }

    public void indexDoc(IndexWriter writer, Path path, long lastModified) throws IOException {
        try (InputStream stream = Files.newInputStream(path)) {
            if(!(path.toString().endsWith(".txt") || (path.toString().endsWith(".html")) || (path.toString().endsWith(".htm")))){
                return;
            }
            Stemmer.Stem(path);

            Document doc = new Document();
            File file = path.toFile();

            Field pathField = new StringField(FieldConstants.FIELD_PATH, path.toString(), Field.Store.YES);
            doc.add(pathField);

            String filename = file.getName();
            Field fileNameField = new StringField(FieldConstants.FIELD_FILE_NAME, filename, Field.Store.YES);
            doc.add(fileNameField);

            String lastModifiedTime = FormatDate(lastModified);
            doc.add(new TextField(FieldConstants.FIELD_LAST_MODIFIED, lastModifiedTime, Field.Store.YES));

            if(path.toString().endsWith(".txt")) {
                doc.add(new TextField("contents", new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))));
            }
            else if(path.toString().endsWith(".html") || path.toString().endsWith(".htm")){
//                  addHtmlFields(doc, file);
                org.jsoup.nodes.Document htmlFile = Jsoup.parse(file, "utf-8");
                String title = htmlFile.title();
                doc.add(new TextField(FieldConstants.FIELD_TITLE, title, Field.Store.YES));

                String body = htmlFile.body().text();
                doc.add(new TextField(FieldConstants.FIELD_CONTENT, body, Field.Store.YES));

                String summary = htmlFile.getElementsByTag("summary").text();

                doc.add(new TextField(FieldConstants.FIELD_SUMMARY, summary, Field.Store.YES));

                StringBuilder dates = new StringBuilder();
                dates.setLength(0);
                Pattern pattern = Pattern.compile("(0?[1-9]|[12][0-9]|3[01])/(0?[1-9]|1[012])/((19|20)\\d\\d)");
                Elements elements = htmlFile.getElementsMatchingText(pattern);
                List<Element> finalElements = elements.stream().filter(elem -> isLastElem(elem, pattern)).collect(Collectors.toList());
                finalElements.stream().forEach(elem ->
                        dates.append(elem.html())
                );
                if (dates.length() > 0)
                    doc.add(new TextField(FieldConstants.FIELD_DATE, dates.toString(), Field.Store.YES));
            }
            System.out.println("Parsing File: " + filename);
            writer.updateDocument(new Term("path", INDEXPATH), doc);
        }
    }

//    private void addHtmlFields(Document doc, File file) throws IOException {
//        org.jsoup.nodes.Document htmlFile = Jsoup.parse(file, "utf-8");
//        String title = htmlFile.title();
//        doc.add(new TextField(FieldConstants.FIELD_TITLE, title, Field.Store.YES));
//
//        String body = htmlFile.body().text();
//        doc.add(new TextField(FieldConstants.FIELD_CONTENT, body, Field.Store.YES));
//
//        String summary = htmlFile.getElementsByTag("summary").text();
//        if(summary.length()>0){
//            System.out.println("**********************************************"+title);
//        }
//        doc.add(new TextField(FieldConstants.FIELD_SUMMARY, summary, Field.Store.YES));
//
//        StringBuilder dates = new StringBuilder();
//        dates.setLength(0);
//        Pattern pattern = Pattern.compile("(0?[1-9]|[12][0-9]|3[01])/(0?[1-9]|1[012])/((19|20)\\d\\d)");
//        Elements elements = htmlFile.getElementsMatchingText(pattern);
//        List<Element> finalElements = elements.stream().filter(elem -> isLastElem(elem, pattern)).collect(Collectors.toList());
//        finalElements.stream().forEach(elem ->
//                dates.append(elem.html())
//        );
//        if (dates.length() > 0)
//            doc.add(new TextField(FieldConstants.FIELD_DATE, dates.toString(), Field.Store.YES));
//    }

    public boolean isLastElem(Element elem, Pattern pattern) {
        return elem.getElementsMatchingText(pattern).size() <= 1;
    }

    private ArrayList<Field>  indexTxtFile(File file) throws IOException {
        ArrayList<Field> fields = new ArrayList<Field>();

        @SuppressWarnings("deprecation")
        String text = FileUtils.readFileToString(file);
        fields.add(new TextField(FieldConstants.FIELD_CONTENT, text, Field.Store.YES));

        String lastModified = FormatDate(file.lastModified());
        fields.add(new TextField(FieldConstants.FIELD_LAST_MODIFIED, lastModified, Field.Store.YES));

        return fields;
    }
    public String FormatDate(long time){
        Date date = new Date(time);
        Format format = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        return format.format(date);
    }
}
