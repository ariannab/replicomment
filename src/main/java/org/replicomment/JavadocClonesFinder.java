package org.replicomment;

import org.replicomment.extractor.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.replicomment.util.Reflection;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JavadocClonesFinder {

    public static void main(String[] args) throws IOException {
        final JavadocExtractor javadocExtractor = new JavadocExtractor();
//        List<String> sourceFolders = FileUtils.readLines(new File(
//                JavadocClonesFinder.class.getResource("/sources.txt").getPath()));

        Map<String,String> sourceFolders = new HashMap<>();
        sourceFolders.put("guava", "/Users/arianna/toradocu/src/test/resources/guava-19.0-sources/");
        sourceFolders.put("lucene","/Users/arianna/comment-clones/javadoclones/src/resources/src/lucene-core-7.2.1-sources/");
        sourceFolders.put("hadoop","/Users/arianna/comment-clones/javadoclones/src/resources/src/hadoop-2.6.5-src/" +
                "hadoop-common-project/hadoop-common/src/main/java/");
        sourceFolders.put("hadoop-hdfs",
                "/Users/arianna/comment-clones/javadoclones/src/resources/src/hadoop-2.6.5-src/hadoop-hdfs-project/" +
                        "hadoop-hdfs/src/main/java/");
        sourceFolders.put("elastic", "/Users/arianna/comment-clones/javadoclones/src/resources/src/elasticsearch-6.1.1-sources/");
        sourceFolders.put("vertx", "/Users/arianna/comment-clones/javadoclones/src/resources/src/vertx-core-3.5.0-sources/");
        sourceFolders.put("spring", "/Users/arianna/comment-clones/javadoclones/src/resources/src/spring-core-5.0.2-sources/");
        sourceFolders.put("log4j", "/Users/arianna/comment-clones/javadoclones/src/resources/src/log4j-1.2.17-sources/");
        sourceFolders.put("solr", "/Users/arianna/comment-clones/javadoclones/src/resources/src/solr-solrj-7.1.0-sources/");
        sourceFolders.put("rx", "/Users/arianna/comment-clones/javadoclones/src/resources/src/rxjava-1.3.5-sources/");



        for(String sourceFolderID : sourceFolders.keySet()){
            //Collect all sources
            String sourceFolder = sourceFolders.get(sourceFolderID);
            try {
                Collection<File> list = FileUtils.listFiles(
                        new File(
                                sourceFolder),
                        new RegexFileFilter("(.*).java"),
                        TrueFileFilter.INSTANCE);
                String[] selectedClassNames  = getClassesInFolder(list, sourceFolder);
                FileWriter writer = new FileWriter("2018_JavadocClones_"+sourceFolderID+".csv");
                writer.append("Class");
                writer.append(';');
                writer.append("Method1");
                writer.append(';');
                writer.append("Method2");
                writer.append(';');
                writer.append("Type");
                writer.append(';');
                writer.append("Param1");
                writer.append(';');
                writer.append("Param2");
                writer.append(';');
                writer.append("Cloned text");
                writer.append(';');
                writer.append("Legit?");
                writer.append('\n');
                System.out.println("[INFO] Analyzing "+sourceFolder+" ...");
                analyzeClones(writer, javadocExtractor, sourceFolder, selectedClassNames);

                writer.flush();
                writer.close();
            }catch(java.lang.IllegalArgumentException exception){
                System.out.println("[ERROR] Are you sure about this path? "+sourceFolder);
            }
        }

        System.out.println("[INFO] Terminating now ...");

    }

    /**
     * Search for Javadoc clones and stores them.
     *
     * @param writer {@code FileWriter} where to store the clones
     * @param javadocExtractor the {@code JavadocExtractor} that extracts the Javadocs
     * @param sourcesFolder folder containing the Java sources to analyze
     * @param selectedClassNames fully qualified names of the Java classes to be analyzed
     */
    private static void analyzeClones(FileWriter writer,
                                      JavadocExtractor javadocExtractor,
                                      String sourcesFolder,
                                      String[] selectedClassNames){
        for(String className : selectedClassNames) {
            try {
                DocumentedType documentedType = javadocExtractor.extract(
                        className, sourcesFolder);

                if (documentedType != null) {
//                    System.out.println("\nIn class " + className + ":");
                    List<DocumentedExecutable> executables = documentedType.getDocumentedExecutables();
                    for (int i = 0; i < executables.size(); i++) {
                        DocumentedExecutable first = executables.get(i);
                        for (int j = i + 1; j < executables.size(); j++) {
                            DocumentedExecutable second = executables.get(j);

                            String firstJavadoc = first.getWholeJavadocAsString();
                            String secondJavadoc = second.getWholeJavadocAsString();

                            String firstSignature = first.toString();
                            String secondSignature = second.toString();

                            // TODO NEW HEURISTIC, BUT HOW TO EXCLUDE? JUST MUTE?
                            boolean wholeClone = isWholeClone(firstJavadoc, secondJavadoc);
                            if (!wholeClone) {
                                // Not a whole comment clone. Overloading?
                                boolean legit = isOverloading(first.getName(), second.getName());

                                freeTextCloneCheck(writer, className, first, second, legit);
                                returnTagCloneCheck(writer, className, first, second, legit);

                                for (ParamTag firstParamTag : first.paramTags()) {
                                    String cleanFirst = firstParamTag
                                            .getComment()
                                            .getText()
                                            .trim()
                                            .replaceAll("\n ", "");
                                    if (!cleanFirst.isEmpty()) {
                                        for (ParamTag secParamTag : second.paramTags()) {
                                            paramTagsCloneCheck(writer, className, firstSignature, secondSignature,
                                                    legit, firstParamTag, secParamTag);

                                        }
                                    }
                                }

                                for (ThrowsTag firstThrowTag : first.throwsTags()) {
                                    String cleanFirst = firstThrowTag.getComment()
                                            .getText()
                                            .trim()
                                            .replaceAll("\n ", "");
                                    if (!cleanFirst.isEmpty()) {
                                        for (ThrowsTag secThrowTag : second.throwsTags()) {
                                            throwsTagCloneCheck(writer, className, firstSignature, secondSignature,
                                                    firstThrowTag, secThrowTag, legit);
                                        }
                                    }
                                }
                            }else{
                                wholeClonePrint(writer, className, first, second, false, firstJavadoc);
                            }
                        }
                    }
                }
            }catch(IOException e){
                //do nothing
            }
        }
    }

    private static void wholeClonePrint(FileWriter writer, String className,
                                        DocumentedExecutable first, DocumentedExecutable second,
                                        boolean b, String firstJavadoc) throws IOException {
        writer.append(className);
        writer.append(';');
        writer.append(first.toString());
        writer.append(';');
        writer.append(second.toString());
        writer.append(';');
        writer.append("Whole");
        writer.append(';');
        writer.append("");
        writer.append(';');
        writer.append("");
        writer.append(';');
        writer.append(firstJavadoc.replaceAll(";", ","));
        writer.append(';');
        writer.append(String.valueOf(b));
        writer.append("\n");
    }

    private static boolean isWholeClone(String first, String second) {
        return !freeTextToFilter(first) && first.equals(second);
    }

    private static void throwsTagCloneCheck(FileWriter writer,
                                               String className,
                                               String firstMethod,
                                               String secondMethod,
                                               ThrowsTag firstTag,
                                               ThrowsTag secondTag,
                                               boolean legit) throws IOException {

        Comment comment1 = firstTag.getComment();
        Comment comment2 = secondTag.getComment();

        String cleanFirst = comment1.getText().trim().replaceAll("\n ", "");
        String cleanSecond = comment2.getText().trim().replaceAll("\n ", "");

        if (cleanFirst.equals(cleanSecond)) {

            String exceptionNameOne = firstTag.getException();
            String exceptionNameTwo = secondTag.getException();
            // Same exception name: could be a legitimate clone
            legit = legit || exceptionNameOne.equals(exceptionNameTwo);


            String genericConditionEx = "(if|for) (any|an) (error|problem|issue) (that|which)? (occurs|happens|raises)";
            // The following would be < 3 anyway
            //            String genericEx = "(upon|on) (error)";

            // Cloned exception will be tolerated ONLY IF:
            // - same exception name;
            // - no shorter than 4 words;
            // - not generic condition ("if an error occurs" etc.)
            // TODO NEW HEURISTIC
//            legit = legit
//                    && comment1.getText().split(" ").length > 3
//                    && !comment1.getText().matches(genericConditionEx);
            // ...unless it is thrown always
//            legit = legit && !cleanFirst.equals("always");

//          System.out.println("\n@param tag clone: " + firstParamTag.getComment().getText() + "\n" +
//          " among " + first.getSignature() + " \nand " + second.getSignature());

            writer.append(className);
            writer.append(';');
            writer.append(firstMethod);
            writer.append(';');
            writer.append(secondMethod);
            writer.append(';');
            writer.append("@throws");
            writer.append(';');
            writer.append("");
            writer.append(';');
            writer.append("");
            writer.append(';');
            writer.append(comment1.getText().replaceAll(";", ","));
            writer.append(';');
            writer.append(String.valueOf(legit));
            writer.append("\n");
        }
    }

    private static void paramTagsCloneCheck(FileWriter writer,
                                            String className,
                                            String first,
                                            String second,
                                            boolean legit,
                                            ParamTag firstTag,
                                            ParamTag secondTag) throws IOException {

        Comment comment1 = firstTag.getComment();
        Comment comment2 = secondTag.getComment();

        String cleanFirst = comment1.getText().trim().replaceAll("\n ", "");
        String cleanSecond = comment2.getText().trim().replaceAll("\n ", "");

        String paramName1 = firstTag.getParamName();
        String paramName2 = secondTag.getParamName();

        String paramType1 = firstTag.getParamType();
        String paramType2 = secondTag.getParamType();

        // Same parameter name could mean legitimate clone
        legit = legit || paramName1.equals(paramName2);

        if (cleanFirst.equals(cleanSecond)) {
//          System.out.println("\n@param tag clone: " + firstParamTag.getComment().getText() + "\n" +
//          " among " + first.getSignature() + " \nand " + second.getSignature());
            writer.append(className);
            writer.append(';');
            writer.append(first);
            writer.append(';');
            writer.append(second);
            writer.append(';');
            writer.append("@param");
            writer.append(';');
            writer.append(paramType1 + " " +paramName1);
            writer.append(';');
            writer.append(paramType2 + " " + paramName2);
            writer.append(';');
            writer.append(comment1.getText().replaceAll(";", ","));
            writer.append(';');
            writer.append(String.valueOf(legit));
            writer.append("\n");
        }
    }

    private static void returnTagCloneCheck(FileWriter writer,
                                            String className,
                                            DocumentedExecutable first,
                                            DocumentedExecutable second,
                                            boolean legit) throws IOException {
        // Legitimate clone if overloading/overriding or if it's a return tag for same return type
        // TODO NEW HEURISTIC
//        boolean legit = legit
//                || isSameReturnType(first, second);

        if (first.returnTag() != null && second.returnTag() != null) {
            String firstComment = first.returnTag().getComment().getText();
            String cleanFirst = firstComment.trim().replaceAll("\n ", "");

            if (!cleanFirst.isEmpty()) {
                String cleanSecond = second.returnTag().getComment().getText().trim().replaceAll("\n ", "");
                if (cleanFirst.equals(cleanSecond)) {
//              System.out.println("\n@return tag clone: " + first.returnTag().getComment().getText() + "\n" +
//               " among " + first.getSignature() + " \nand " + second.getSignature());

                    writer.append(className);
                    writer.append(';');
                    writer.append(first.toString());
                    writer.append(';');
                    writer.append(second.toString());
                    writer.append(';');
                    writer.append("@return");
                    writer.append(';');
                    writer.append("");
                    writer.append(';');
                    writer.append("");
                    writer.append(';');
                    writer.append(firstComment.replaceAll(";", ","));
                    writer.append(';');
                    writer.append(String.valueOf(legit));
                    writer.append("\n");
                }
            }
        }
    }

    private static void freeTextCloneCheck(FileWriter writer,
                                           String className,
                                           DocumentedExecutable first,
                                           DocumentedExecutable second,
                                           boolean legit) throws IOException {
        if (!freeTextToFilter(first.getJavadocFreeText())) {
            String cleanFirst = first.getJavadocFreeText().trim().replaceAll("\n ", "");
            String cleanSecond = second.getJavadocFreeText().trim().replaceAll("\n ", "");
            if (cleanFirst.equals(cleanSecond)) {
//                                    System.out.println("\nFree text clone: " + first.getJavadocFreeText() + "\n" +
//                                            " among " + first.getSignature() + " \nand " + second.getSignature());

                writer.append(className);
                writer.append(';');
                writer.append(first.toString());
                writer.append(';');
                writer.append(second.toString());
                writer.append(';');
                writer.append("Free text");
                writer.append(';');
                writer.append("");
                writer.append(';');
                writer.append("");
                writer.append(';');
                writer.append(first.getJavadocFreeText().replaceAll(";", ","));
                writer.append(';');
                writer.append(String.valueOf(legit));
                writer.append("\n");
            }
        }
    }

//    /**
//     * Returns true if the exception clone must be filtered out
//     *
//     * @param firstThrowTag first tag to compare
//     * @param secThrowTag second tag to compare
//     * @return true if the exception names are the same
//     */
//    private static boolean excepToFilter(ThrowsTag firstThrowTag, ThrowsTag secThrowTag) {
//        return firstThrowTag.getException().equals(secThrowTag.getException());
//    }

    /**
     * Filter out empty free texts and free texts pointing at inheritDoc.
     *
     * @param freeText the freeText to examine
     * @return true if one of the comment must be filtered out
     */
    private static boolean freeTextToFilter(String freeText) {
        String noBlankFreeText = freeText.trim().replaceAll("\n ", "");
        return  noBlankFreeText.isEmpty()
                || noBlankFreeText.equals("{@inheritDoc}")
                || noBlankFreeText.equals("{@deprecated}");
    }

    /**
     * Check if comment clone could be because of an overloaded method.
     * Originally, this method was also labelling overriding as legitimate
     * clone.
     *
     * Given the amount of actual copy&paste cases happening
     * because of overriding, we now prefer to keep the benefit of the doubt.
     * For overloading, the matter is less borderline: the difference is
     * only in the parameters; if @param are different, it is fine to
     * believe the clone is legitimate. Overload cases which do not differ
     * at least at the @param level will NOT be labeled as legitimate.
     *
     * @param firstName first method name
     * @param secondName second method name
     * @return true if method overloading
     */
    private static boolean isOverloading(String firstName, String secondName) {
        firstName = firstName.toLowerCase();
        secondName = secondName.toLowerCase();
        return firstName.equals(secondName);
//                || firstName.contains(secondName)
//                || secondName.contains(firstName);

    }

    /**
     * Check if @return clone could be because of same (non-primitive) type.
     *
     *
     * @return true if same return type
     */
    private static boolean isSameReturnType(DocumentedExecutable first, DocumentedExecutable second) {
        String firstType = first.getReturnType();
        // Clean possible generic types
        firstType = JavadocExtractor.rawType(firstType);
        String secondType = second.getReturnType();
        secondType = JavadocExtractor.rawType(secondType);

        // Constructors
        if(firstType.trim().isEmpty()){
            return secondType.trim().isEmpty();
        }
        if(Reflection.getPrimitiveClasses().get(firstType)!=null){
            // one of the two is a PRIMITIVE type: always consider potential clone
            return false;
        }
        else return firstType.equals(secondType);
    }

    /**
     * From a list of files in path, finds the fully qualified names of Java classes.
     *
     * @param list collection of {@code Files}
     * @param path the String path to search in
     * @return fully qualified names found, in an array of Strings
     */
    private static String[] getClassesInFolder(Collection<File> list, String path) {
        String[] selectedClassNames = new String[list.size()];
        int i = 0;
        for (File file : list) {
            String fileName = file.getAbsolutePath();
            String[] unnecessaryPrefix = fileName.split(path);
            String className = unnecessaryPrefix[1].replaceAll("/",".");
            selectedClassNames[i++] = className.replace(".java", "");
        }
        return selectedClassNames;

    }
}
