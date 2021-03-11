# lucene_search_engine_application



### **Purpose:**

Information Retrieval system which parses text and html documents and retrieves top 10 relevant documents gor a given search query which includes parameters like filename, title, rank, and score respectively. 

### **Program Flow**

The functionality has been modularized and implemented into the following logical blocks:

- Pre-processing of the documents is carried out. Steps including Tokenization and Stop Word Elimination and use of a Porter Stemmer with English Analyzer.
- Indexing
- Search using an ranking algorithm to sort the collection of documents based on their similarity to retrieve the best possible results based on the user’s query.  



##### Libraries Used:

Apache Lucene lucene-core-8.2.0 - For indexing and searching text document

jsoup-1.12.1 - For parsing HTML document 



### **Output**