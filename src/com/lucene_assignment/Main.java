package com.lucene_assignment;

import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        if(args.length != 1) {
            System.err.println("Parameter missing: Run jar Example=> java -jar IR_P01.jar [path to document folder]");
            System.exit(1);
        }

        final String docsPath = args[0];
//        String docsPath = "/Users/nandish21/Downloads/BBB";
        final String indexPath = docsPath+"/IndexedFiles";
        Indexer indexer = new Indexer();
        indexer.performIndex(docsPath, indexPath);
        System.out.println("Enter the query to start search");
        Searcher searcher = new Searcher();

        try(Scanner scan = new Scanner(System.in)) {
            while(true) {
                String query = scan.nextLine();
                if (query.equals("end")) {
                    break;
                }
                searcher.checkDirectoryAndSearch(docsPath, indexPath, query);
                System.out.println("Enter your next query OR Type 'end' to end searching further queries");
            }
        }

        System.out.println("Thank you, bye!");
    }
}
