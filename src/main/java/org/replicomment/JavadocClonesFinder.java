package org.replicomment;

import org.reflections.Reflections;
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
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

public class JavadocClonesFinder {

    public static void main(String[] args) throws IOException {
        final JavadocExtractor javadocExtractor = new JavadocExtractor();
        List<String> sourceFolderNames = FileUtils.readLines(new File(
                JavadocClonesFinder.class.getResource("/sources.txt").getPath()));

        Map<String,String> sourceFolders = new HashMap<>();
        
        for(String source : sourceFolderNames){
            String[] tokens = source.split(":");
            sourceFolders.put(tokens[0], tokens[1]);
        }

        for(String sourceFolderID : sourceFolders.keySet()){
            //Collect all sources
            String sourceFolder = sourceFolders.get(sourceFolderID);
            try {
                Collection<File> list = FileUtils.listFiles(
                        new File(
                                sourceFolder),
                        new RegexFileFilter("(.*).java"),
                        TrueFileFilter.INSTANCE);
                List<String> selectedClassNames  = getClassesInFolder(list, sourceFolder);
                // Prepare results header
                FileWriter localCloneWriter = new FileWriter("2020_JavadocClones_"+sourceFolderID+".csv");
                FileWriter externalCloneWriter = new FileWriter("2020_JavadocClones_ext_"+sourceFolderID+".csv");
                prepareCSVOutput(localCloneWriter, false);
                prepareCSVOutput(externalCloneWriter, true);

                System.out.println("[INFO] Analyzing "+sourceFolder+" ...");
                analyzeClones(localCloneWriter, externalCloneWriter, javadocExtractor, sourceFolder, selectedClassNames);

                // Close result files.
                localCloneWriter.flush();
                localCloneWriter.close();
                externalCloneWriter.flush();
                externalCloneWriter.close();
            }catch(java.lang.IllegalArgumentException exception){
                System.out.println("[ERROR] Are you sure about this path? "+sourceFolder);
            }
        }
        System.out.println("[INFO] Terminating now ...");
    }

    private static void prepareCSVOutput(FileWriter writer, boolean externalClone) throws IOException {
        writer.append("Class");
        writer.append(';');
        if(externalClone){
            writer.append("Class 2");
            writer.append(';');
        }
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
    }

    /**
     * Search for Javadoc clones and stores them.
     *
     * @param writer {@code FileWriter} where to store the clones
     * @param javadocExtractor the {@code JavadocExtractor} that extracts the Javadocs
     * @param sourcesFolder folder containing the Java sources to analyze
     * @param selectedClassNames fully qualified names of the Java classes to be analyzed
     */
    private static void analyzeClones(FileWriter lwriter, FileWriter ewriter,
                                      JavadocExtractor javadocExtractor,
                                      String sourcesFolder,
                                      List<String> selectedClassNames){
        for(String className : selectedClassNames) {
            try {
                DocumentedType documentedType = javadocExtractor.extract(
                        className, sourcesFolder);

                if (documentedType != null) {
//                    System.out.println("\nIn class " + className + ":");
                    // TODO here, each method from the SAME documented type is compared against
                    // TODO all its other methods. How do we get the sub-types to extend the comparison?
                    // TODO And, how should the comparison be carried out?
                    // TODO Each method of type A' compared against each method of type A''?
                    // TODO In this case we will encounter overriding, right?

                    // TODO after retrieving the local executables, I also want to retrieve
                    // TODO the external (sub-type) executables for the inner for.
                    List<DocumentedExecutable> localExecutables = documentedType.getDocumentedExecutables();
                    for (int i = 0; i < localExecutables.size(); i++) {
                        DocumentedExecutable first = localExecutables.get(i);
                        clonesSearch(lwriter, className, "", localExecutables, i, first);

                        // FIXME "com.google" is just a test for guava
                        Reflections reflections = new Reflections("com.google");

                        // TODO subtypes are classes, not sources. If we want to get their documentation,
                        // TODO too, we need to look again into our source files (look by name then parse
                        // TODO w/ Javaparser. I hope there's an elegant way to do all this.
                        Set<Class<?>> subTypes = (Set<Class<?>>) reflections.getSubTypesOf(first.getDeclaringClass());
                        if(!subTypes.isEmpty()){
                            // We found a bunch of subtypes. Time to retrieve their doc.
                            for(Class<?> subType : subTypes){
                                String externalClass = subType.getName();
                                if(selectedClassNames.contains(externalClass)){
                                    // Found subtype source.
                                    DocumentedType documentedSubType = javadocExtractor.extract(
                                            externalClass, sourcesFolder);
                                    if(documentedSubType!=null) {
                                        List<DocumentedExecutable> externalExecutables =
                                                documentedSubType.getDocumentedExecutables();
                                        for (int j = 0; j < externalExecutables.size(); j++) {
                                            clonesSearch(ewriter, className, externalClass, externalExecutables, j, first);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }catch(IOException e){
                //do nothing
            }
        }
    }

    private static void clonesSearch(FileWriter writer, String className, String externalClass,
                                     List<DocumentedExecutable> docExecutables, int i,
                                     DocumentedExecutable first) throws IOException {
        for (int j = i + 1; j < docExecutables.size(); j++) {
            // i+1 to avoid comparing A and B and then again B and A
            // (in a positive case, it would count as 2 clones, while
            //  we actually count 1)
            DocumentedExecutable second = docExecutables.get(j);

            String firstJavadoc = first.getWholeJavadocAsString();
            String secondJavadoc = second.getWholeJavadocAsString();

            String firstSignature = first.toString();
            String secondSignature = second.toString();

            boolean wholeClone = isWholeClone(firstJavadoc, secondJavadoc);
            if (!wholeClone) {
                // Not a whole comment clone. Overloading?
                // FIXME does this check work for overriding, too? If not, add one
                boolean legit = isOverloading(first.getName(), second.getName());

                // FIXME these methods need some refactoring, it's a lot of message chaining.
                freeTextCloneCheck(writer, className, externalClass, first, second, legit);
                returnTagCloneCheck(writer, className, externalClass, first, second, legit);
                paramTagsSearch(writer, className, externalClass, first,
                        second, firstSignature, secondSignature, legit);
                exTagsSearch(writer, className, externalClass, first,
                        second, firstSignature, secondSignature, legit);
            }else{
                // A whole clone is never legitimate (-> false)
                wholeClonePrint(writer, className, first, second, false, firstJavadoc);
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

    private static void exTagsSearch(FileWriter writer, String className, String externalClass, DocumentedExecutable first, DocumentedExecutable second, String firstSignature, String secondSignature, boolean legit) throws IOException {
        for (ThrowsTag firstThrowTag : first.throwsTags()) {
            String cleanFirst = firstThrowTag.getComment()
                    .getText()
                    .trim()
                    .replaceAll("\n ", "");
            if (!cleanFirst.isEmpty()) {
                for (ThrowsTag secThrowTag : second.throwsTags()) {
                    throwsTagCloneCheck(writer, className, externalClass, firstSignature, secondSignature,
                            firstThrowTag, secThrowTag, legit);
                }
            }
        }
    }

    private static void throwsTagCloneCheck(FileWriter writer,
                                            String className, String extClassName,
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
            // ---------------------------
            // -----> NEW HEURISTIC <-----
            // ---------------------------
            legit = legit
                    && comment1.getText().split(" ").length > 3
                    && !comment1.getText().matches(genericConditionEx);

            // ...unless it is thrown always
            legit = legit && !cleanFirst.equals("always");

//          System.out.println("\n@param tag clone: " + firstParamTag.getComment().getText() + "\n" +
//          " among " + first.getSignature() + " \nand " + second.getSignature());

            writer.append(className);
            writer.append(';');
            if(!"".equals(extClassName)){
                writer.append(extClassName);
                writer.append(';');
            }
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

    private static void paramTagsSearch(FileWriter writer, String className,
                                        String externalClass, DocumentedExecutable first,
                                        DocumentedExecutable second, String firstSignature,
                                        String secondSignature, boolean legit) throws IOException {
        for (ParamTag firstParamTag : first.paramTags()) {
            String cleanFirst = firstParamTag
                    .getComment()
                    .getText()
                    .trim()
                    .replaceAll("\n ", "");
            if (!cleanFirst.isEmpty()) {
                for (ParamTag secParamTag : second.paramTags()) {
                    paramTagsSearch(writer, className, externalClass, firstSignature, secondSignature,
                            legit, firstParamTag, secParamTag);

                }
            }
        }
    }

    private static void paramTagsSearch(FileWriter writer,
                                        String className, String extClassName,
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
            if(!"".equals(extClassName)){
                writer.append(extClassName);
                writer.append(';');
            }
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
                                            String className, String extClassName,
                                            DocumentedExecutable first,
                                            DocumentedExecutable second,
                                            boolean legit) throws IOException {
        // Legitimate clone if overloading/overriding or if it's a return tag for same return type

        // ---------------------------
        // -----> NEW HEURISTIC <-----
        // ---------------------------
        // Same, non-primitive return type
        legit = legit || isSameReturnType(first, second);

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
                    if(!"".equals(extClassName)){
                        writer.append(extClassName);
                        writer.append(';');
                    }
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
                                           String className, String extClassName,
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
                if(!"".equals(extClassName)){
                    writer.append(extClassName);
                    writer.append(';');
                }
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
     * @return fully qualified names found, as a list of Strings
     */
    private static List<String> getClassesInFolder(Collection<File> list, String path) {
        List<String> selectedClassNames = new ArrayList<>();
        int i = 0;
        for (File file : list) {
            String fileName = file.getAbsolutePath();
            String[] unnecessaryPrefix = fileName.split(path);
            String className = unnecessaryPrefix[1].replaceAll("/",".");
            selectedClassNames.add(className.replace(".java", ""));
        }
        return selectedClassNames;

    }
}
