package org.replicomment;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import org.apache.commons.lang3.StringUtils;
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

public class JavadocClonesFinder {

    public static final JavadocExtractor javadocExtractor = new JavadocExtractor();
    public static HashMap<String, DocumentedType> documentedTypes = new HashMap<>();

    public static void main(String[] args) throws IOException {
        List<String> sourceFolderNames = FileUtils.readLines(new File(
                JavadocClonesFinder.class.getResource("/sources.txt").getPath()));
//                JavadocClonesFinder.class.getResource("/sources.txt").getPath()));

        Map<String, String> sourceFolders = new HashMap<>();

        for (String source : sourceFolderNames) {
            String[] tokens = source.split(":");
            sourceFolders.put(tokens[0], tokens[1]);
        }

        for (String sourceFolderID : sourceFolders.keySet()) {
            //Collect all sources
            String sourceFolder = sourceFolders.get(sourceFolderID);

            Collection<File> list = FileUtils.listFiles(
                    new File(
                            sourceFolder),
                    new RegexFileFilter("(.*).java"),
                    TrueFileFilter.INSTANCE);
            List<String> selectedClassNames = getClassesInFolder(list, sourceFolder);

            System.out.println("[INFO] Analyzing " + sourceFolder + " ...");
            analyzeClones(sourceFolder, sourceFolderID, selectedClassNames);
            documentedTypes = new HashMap<>();
        }
        System.out.println("[INFO] Terminating now ...");
    }

    private static void prepareCSVOutput(FileWriter writer, boolean hierarchyClone, boolean crossFile, boolean fields)
            throws IOException {

        writer.append("Main Class");
        writer.append(';');
        if (hierarchyClone) {
            writer.append("Extended Class");
            writer.append(';');
        } else if (crossFile) {
            writer.append("External Class");
            writer.append(';');
        }
        if(!fields) {
            writer.append("Method1");
            writer.append(';');
            writer.append("Method2");
            writer.append(';');
        }else{
            writer.append("Field1");
            writer.append(';');
            writer.append("Field2");
            writer.append(';');
            writer.append("Type1");
            writer.append(';');
            writer.append("Type2");
            writer.append(';');
        }
        writer.append("Kind");
        writer.append(';');
        if(!fields) {
            writer.append("Param1");
            writer.append(';');
            writer.append("Param2");
            writer.append(';');
        }
        writer.append("Cloned text");
        writer.append(';');
        writer.append("Legit?");
        writer.append('\n');
    }

    /**
     * Search for Javadoc clones and stores them.
     *
     * @param sourcesFolder      folder containing the Java sources to analyze
     * @param selectedClassNames fully qualified names of the Java classes to be analyzed
     */
    private static void analyzeClones(String sourcesFolder, String sourceFolderID,
                                      List<String> selectedClassNames) throws IOException {
        // Prepare results header
        FileWriter localCloneWriter = new FileWriter("2020_JavadocClones_" + sourceFolderID + ".csv");
        FileWriter hierarchyCloneWriter = new FileWriter("2020_JavadocClones_h_" + sourceFolderID + ".csv");
        FileWriter crossCloneWriter = new FileWriter("2020_JavadocClones_cf_" + sourceFolderID + ".csv");
        FileWriter fieldCloneWriter = new FileWriter("2020_JavadocClones_fields_" + sourceFolderID + ".csv");
        prepareCSVOutput(localCloneWriter, false, false, false);
        prepareCSVOutput(hierarchyCloneWriter, true, false, false);
        prepareCSVOutput(crossCloneWriter, false, true, false);
        prepareCSVOutput(fieldCloneWriter, true, false, true);


        for (String className : selectedClassNames) {
            try {
                DocumentedType documentedType;
                NodeList<ClassOrInterfaceType> extendedTypes = new NodeList<>();

                if (documentedTypes.containsKey(className)) {
                    documentedType = documentedTypes.get(className);
                } else {
                    documentedType = javadocExtractor.extract(
                            className, sourcesFolder);
                    documentedTypes.put(className, documentedType);
                }

                if (documentedType != null) {

                    if(documentedType.getSourceClass()!=null) {
                           extendedTypes = documentedType.getSourceClass().getExtendedTypes();
                    }
//                    System.out.println("\nIn class " + className + ":");

                    // Retrieve methods in current class.
                    List<DocumentedExecutable> localExecutables =
                            documentedType.getDocumentedExecutables();
                    for (int i = 0; i < localExecutables.size(); i++) {
                        DocumentedExecutable firstMethod = localExecutables.get(i);
                        // Look for local (same-file) doc clones, method level.
//                        methodLevelClonesSearch(localCloneWriter, className, "", localExecutables, i, first);

                        // Look for hierarchy doc clones, method level.
//                        exploreSourceHierarchy(hierarchyCloneWriter, sourcesFolder,
//                                selectedClassNames, className, extendedTypes, first);

                        // Finally, look for clones cross-file, method level (excluding subtypes, addressed above).
                        // Instead of passing selected selectedClassNames, exclude the supertypes already
                        // FIXME Make replicomment a decent command-line tool and introduce an option to
                        // FIXME explicitly invoke this (we don't want to run cross-file by default).
//                        if (sourceFolderID.equals("guava") ||
//                                sourceFolderID.equals("log4j") ||
//                                sourceFolderID.equals("spring")) {
////                            // FIXME Ugly trick to skip awfully big projects atm (just pick a few for statistics).
//                            List<String> externalClasses = new ArrayList<>(selectedClassNames);
//                            String packageName = className.substring(0, className.lastIndexOf("."));
//                            for (ClassOrInterfaceType superType : extendedTypes) {
//                                String externalClassName = packageName + "." + superType.getNameAsString();
//                                externalClasses.remove(externalClassName);
//                            }
//                            crossFileClonesSearch(crossCloneWriter, sourcesFolder,
//                                    className, externalClasses, first);
//                        }
                    }
                    List<DocumentedField> documentedFields = documentedType.getDocumentedFields();
                    for (int i = 0; i < documentedFields.size(); i++) {
                        DocumentedField firstField = documentedFields.get(i);
                        // Look for local (same-file) doc clones, field level.
                        fieldLevelClonesSearch(fieldCloneWriter, className, "", documentedFields, i, firstField);

                        // Look for hierarchy doc clones, field level.
                        exploreHierarchyFields(fieldCloneWriter, sourcesFolder,
                                selectedClassNames, className, extendedTypes, firstField);
                        if (sourceFolderID.equals("guava") ||
                                sourceFolderID.equals("log4j") ||
                                sourceFolderID.equals("spring")) {
//                            // FIXME Ugly trick to skip awfully big projects atm (just pick a few for statistics).
                            List<String> externalClasses = new ArrayList<>(selectedClassNames);
                            String packageName = className.substring(0, className.lastIndexOf("."));
                            for (ClassOrInterfaceType superType : extendedTypes) {
                                String externalClassName = packageName + "." + superType.getNameAsString();
                                externalClasses.remove(externalClassName);
                            }
                            crossFileFieldClonesSearch(fieldCloneWriter, sourcesFolder,
                                    className, externalClasses, firstField);
                        }
                    }


                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Close result files.
        localCloneWriter.flush();
        localCloneWriter.close();

        fieldCloneWriter.flush();
        fieldCloneWriter.close();

        hierarchyCloneWriter.flush();
        hierarchyCloneWriter.close();

        crossCloneWriter.flush();
        crossCloneWriter.close();
    }

    private static void methodLevelClonesSearch(FileWriter writer, String className, String externalClass,
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
                boolean legit = isOverloading(first.getName(), second.getName());

                // FIXME these methods need some refactoring, it's a lot of message chaining.
                freeTextCloneCheck(writer, className, externalClass, first, second, legit);
                returnTagCloneCheck(writer, className, externalClass, first, second, legit);
                paramTagsSearch(writer, className, externalClass, first,
                        second, firstSignature, secondSignature, legit);
                exTagsSearch(writer, className, externalClass, first,
                        second, firstSignature, secondSignature, legit);
            } else {
                // A whole clone is never legitimate, unless it's overriding (assuming inherited method
                // behaviour does not change, then doc also doesn't)
                boolean legit = isOverriding(first.getName(), second.getName(), externalClass);
                wholeClonePrint(writer, className, externalClass, first, second, legit, firstJavadoc);
            }
        }
    }

    private static void fieldLevelClonesSearch(FileWriter writer,
                                               String className,
                                               String extClassName,
                                               List<DocumentedField> documentedFields,
                                               int i,
                                               DocumentedField first) throws IOException {
        String firstJavadoc = first.getJavadocFreeText();
        if(firstJavadoc.isEmpty()){
            return;
        }

        for (int j = i + 1; j < documentedFields.size(); j++) {
            // i+1 to avoid comparing A and B and then again B and A
            // (in a positive case, it would count as 2 clones, while
            //  we actually count 1)
            DocumentedField second = documentedFields.get(j);
            String secondJavadoc = second.getJavadocFreeText();

            String firstName = first.getName();
            String secondName = second.getName();

            String firstType = first.getTypeName();
            String secondType = second.getTypeName();

            if(!firstJavadoc.equals(secondJavadoc)){
                continue;
            }else{
                // TODO in case we include external classes, too: && firstName.equals(secondName)
                boolean legit = firstType.equals(secondType);
                writer.append(className);
                writer.append(';');
                writer.append(extClassName);
                writer.append(';');
                writer.append(firstName);
                writer.append(';');
                writer.append(secondName);
                writer.append(';');
                writer.append(firstType);
                writer.append(';');
                writer.append(secondType);
                writer.append(';');
                writer.append("Field");
                writer.append(';');
                writer.append(StringUtils.replace(firstJavadoc, ";", ","));
                writer.append(';');
                writer.append(String.valueOf(legit));
                writer.append("\n");
            }

        }
    }

    private static void crossFileClonesSearch(FileWriter crossCloneWriter,
                                              String sourcesFolder,
                                              String className,
                                              List<String> selectedClassNames,
                                              DocumentedExecutable first) throws IOException {
        for (String externalClass : selectedClassNames) {
            DocumentedType documentedExtType;
            if (!externalClass.equals(className)) {

                if (documentedTypes.containsKey(externalClass)) {
                    documentedExtType = documentedTypes.get(externalClass);
                } else {
                    documentedExtType = javadocExtractor.extract(
                            externalClass, sourcesFolder);
                    documentedTypes.put(externalClass, documentedExtType);
                }

                if (documentedExtType != null) {
                    List<DocumentedExecutable> externalExecutables =
                            documentedExtType.getDocumentedExecutables();
                    for (int j = 0; j < externalExecutables.size(); j++) {
                        methodLevelClonesSearch(crossCloneWriter, className,
                                externalClass, externalExecutables, j, first);
                    }
                }
            }
        }
    }


    private static void crossFileFieldClonesSearch(FileWriter crossCloneWriter,
                                                   String sourcesFolder,
                                                   String className,
                                                   List<String> externalClasses,
                                                   DocumentedField firstField) throws IOException {
        for (String externalClass : externalClasses) {
            DocumentedType documentedExtType;
            if (!externalClass.equals(className)) {

                if (documentedTypes.containsKey(externalClass)) {
                    documentedExtType = documentedTypes.get(externalClass);
                } else {
                    documentedExtType = javadocExtractor.extract(
                            externalClass, sourcesFolder);
                    documentedTypes.put(externalClass, documentedExtType);
                }

                if (documentedExtType != null) {
                    List<DocumentedField> externalFields =
                            documentedExtType.getDocumentedFields();
                    for (int j = 0; j < externalFields.size(); j++) {
                        fieldLevelClonesSearch(crossCloneWriter, className,
                                externalClass, externalFields, j, firstField);
                    }
                }
            }
        }

    }

    private static void exploreSourceHierarchy(FileWriter ewriter,
                                               String sourcesFolder,
                                               List<String> selectedClassNames,
                                               String className,
                                               NodeList<ClassOrInterfaceType> extendedTypes,
                                               DocumentedExecutable first) throws IOException {
        if(extendedTypes.isEmpty()){
            return;
        }

        String packageName = className.substring(0, className.lastIndexOf("."));

        for (ClassOrInterfaceType superType : extendedTypes) {
            // We found a bunch of subtypes. Time to retrieve their doc.
            String externalClass = packageName + "." + superType.getNameAsString();
            DocumentedType documentedSubType;
            if (selectedClassNames.contains(externalClass)) {
                // Found subtype source.

                if (documentedTypes.containsKey(externalClass)) {
                    documentedSubType = documentedTypes.get(externalClass);
                } else {
                    documentedSubType = javadocExtractor.extract(
                            externalClass, sourcesFolder);
                    documentedTypes.put(externalClass, documentedSubType);
                }

                if (documentedSubType != null) {
                    List<DocumentedExecutable> externalExecutables =
                            documentedSubType.getDocumentedExecutables();
                    for (int j = 0; j < externalExecutables.size(); j++) {
                        methodLevelClonesSearch(ewriter, className, externalClass, externalExecutables, j, first);
                    }
                }
            }
        }
    }

    private static void exploreHierarchyFields(FileWriter ewriter,
                                               String sourcesFolder,
                                               List<String> selectedClassNames,
                                               String className,
                                               NodeList<ClassOrInterfaceType> extendedTypes,
                                               DocumentedField first) throws IOException {
        if(extendedTypes.isEmpty()){
            return;
        }

        String packageName = className.substring(0, className.lastIndexOf("."));

        for (ClassOrInterfaceType superType : extendedTypes) {
            // We found a bunch of subtypes. Time to retrieve their doc.
            String externalClass = packageName + "." + superType.getNameAsString();
            DocumentedType documentedSubType;
            if (selectedClassNames.contains(externalClass)) {
                // Found subtype source.

                // FIXME the following shouldn't be necessary having populated the hashmap for methods.
                if (documentedTypes.containsKey(externalClass)) {
                    documentedSubType = documentedTypes.get(externalClass);
                } else {
                    documentedSubType = javadocExtractor.extract(
                            externalClass, sourcesFolder);
                    documentedTypes.put(externalClass, documentedSubType);
                }

                if (documentedSubType != null) {
                    List<DocumentedField> externalFields =
                            documentedSubType.getDocumentedFields();
                    for (int j = 0; j < externalFields.size(); j++) {
                        fieldLevelClonesSearch(ewriter, className, externalClass,
                                externalFields, j, first);
                    }
                }
            }
        }
    }

//    /**
//     * Finds clones in subtypes by means of reflection. A pretty expensive approach, not advised.
//     *
//     * @param ewriter the external clones writer
//     * @param javadocExtractor javadoc extractor to extract source comments
//     * @param sourcesFolder the folders from which to grab sources
//     * @param selectedClassNames class names we are looking for in sources
//     * @param className the class for which we need subtypes
//     * @param first the first documented executable for which to fetch clones
//     * @throws IOException if cannot read reflection prefixes file
//     */
//    private static void exploreReflectionHierarchy(FileWriter ewriter, JavadocExtractor javadocExtractor,
//                                                   String sourcesFolder, List<String> selectedClassNames,
//                                                   String className, DocumentedExecutable first) throws IOException {
//        List<String> reflectionPrefixList = FileUtils.readLines(new File(
//                JavadocClonesFinder.class.getResource("/reflections.txt").getPath()));
//
//        Map<String,String> reflectionPrefixes = new HashMap<>();
//
//        for(String source : reflectionPrefixList){
//            String[] tokens = source.split(":");
//            reflectionPrefixes.put(tokens[0], tokens[1]);
//        }
//
//        for(String refPrefix : reflectionPrefixes.keySet()) {
//            Reflections reflections = new Reflections(refPrefix);
//            Set<Class<?>> subTypes = (Set<Class<?>>) reflections.getSubTypesOf(first.getDeclaringClass());
//            if (!subTypes.isEmpty()) {
//                // We found a bunch of subtypes. Time to retrieve their doc.
//                for (Class<?> subType : subTypes) {
//                    String externalClass = subType.getName();
//                    if (selectedClassNames.contains(externalClass)) {
//                        // Found subtype source.
//                        DocumentedType documentedSubType = javadocExtractor.extract(
//                                externalClass, sourcesFolder);
//                        if (documentedSubType != null) {
//                            List<DocumentedExecutable> externalExecutables =
//                                    documentedSubType.getDocumentedExecutables();
//                            for (int j = 0; j < externalExecutables.size(); j++) {
//                                methodLevelClonesSearch(ewriter, className, externalClass, externalExecutables, j, first);
//                            }
//                        }
//                    }
//                }
//            }
//        }
//    }


    private static boolean isOverriding(String firstName, String secondName, String externalClass) {
        if (!"".equals(externalClass)) {
            firstName = firstName.toLowerCase();
            secondName = secondName.toLowerCase();
            return firstName.equals(secondName);
        }
        return false;
    }


    private static void wholeClonePrint(FileWriter writer, String className, String extClassName,
                                        DocumentedExecutable first, DocumentedExecutable second,
                                        boolean b, String firstJavadoc) throws IOException {
        writer.append(className);
        writer.append(';');
        if (!"".equals(extClassName)) {
            writer.append(extClassName);
            writer.append(';');
        }
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
        writer.append(StringUtils.replace(firstJavadoc, ";", ","));
        writer.append(';');
        writer.append(String.valueOf(b));
        writer.append("\n");
    }

    private static boolean isWholeClone(String first, String second) {
        return !freeTextToFilter(first) && first.equals(second);
    }

    private static void exTagsSearch(FileWriter writer, String className, String externalClass, DocumentedExecutable first, DocumentedExecutable second, String firstSignature, String secondSignature, boolean legit) throws IOException {
        for (ThrowsTag firstThrowTag : first.throwsTags()) {
            String cleanFirst = StringUtils.replace(firstThrowTag.getComment()
                            .getText()
                            .trim()
                    , "\n ", "");
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

        String cleanFirst = StringUtils.replace(comment1.getText().trim(), "\n ", "");
        String cleanSecond = StringUtils.replace(comment2.getText().trim(), "\n ", "");

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
            if (!"".equals(extClassName)) {
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
            writer.append(StringUtils.replace(comment1.getText(), ";", ","));
            writer.append(';');
            writer.append(String.valueOf(legit));
            writer.append("\n");
        }
    }

    private static void paramTagsSearch(FileWriter writer,
                                        String className,
                                        String externalClass, DocumentedExecutable first,
                                        DocumentedExecutable second, String firstSignature,
                                        String secondSignature, boolean legit) throws IOException {
        for (ParamTag firstParamTag : first.paramTags()) {
            String cleanFirst = StringUtils.replace(firstParamTag
                            .getComment()
                            .getText()
                            .trim()
                    , "\n ", "");
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

        String cleanFirst = StringUtils.replace(comment1.getText().trim(), "\n ", "");
        String cleanSecond = StringUtils.replace(comment2.getText().trim(), "\n ", "");

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
            if (!"".equals(extClassName)) {
                writer.append(extClassName);
                writer.append(';');
            }
            writer.append(first);
            writer.append(';');
            writer.append(second);
            writer.append(';');
            writer.append("@param");
            writer.append(';');
            writer.append(paramType1 + " " + paramName1);
            writer.append(';');
            writer.append(paramType2 + " " + paramName2);
            writer.append(';');
            writer.append(StringUtils.replace(comment1.getText(), ";", ","));
            writer.append(';');
            writer.append(String.valueOf(legit));
            writer.append("\n");
        }
    }

    private static void returnTagCloneCheck(FileWriter writer,
                                            String className,
                                            String extClassName,
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
            String cleanFirst = StringUtils.replace(firstComment.trim(), "\n ", "");

            if (!cleanFirst.isEmpty()) {
                String cleanSecond = StringUtils.replace(second.returnTag().getComment().getText().trim(),
                        "\n ", "");
                if (cleanFirst.equals(cleanSecond)) {
//              System.out.println("\n@return tag clone: " + first.returnTag().getComment().getText() + "\n" +
//               " among " + first.getSignature() + " \nand " + second.getSignature());

                    writer.append(className);
                    writer.append(';');
                    if (!"".equals(extClassName)) {
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
                    writer.append(StringUtils.replace(firstComment, ";", ","));
                    writer.append(';');
                    writer.append(String.valueOf(legit));
                    writer.append("\n");
                }
            }
        }
    }

    private static void freeTextCloneCheck(FileWriter writer,
                                           String className,
                                           String extClassName,
                                           DocumentedExecutable first,
                                           DocumentedExecutable second,
                                           boolean legit) throws IOException {
        if (!freeTextToFilter(first.getJavadocFreeText())) {
            String cleanFirst = first.getJavadocFreeText().trim(); //.replaceAll("\n ", "");
            String cleanSecond = second.getJavadocFreeText().trim(); //.replaceAll("\n ", "");

            StringUtils.replace(cleanFirst, "\n ", "");
            StringUtils.replace(cleanSecond, "\n ", "");

            if (cleanFirst.equals(cleanSecond)) {
//                                    System.out.println("\nFree text clone: " + first.getJavadocFreeText() + "\n" +
//                                            " among " + first.getSignature() + " \nand " + second.getSignature());

                writer.append(className);
                writer.append(';');
                if (!"".equals(extClassName)) {
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
                writer.append(StringUtils.replace(first.getJavadocFreeText(), ";", ","));
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
        String noBlankFreeText = StringUtils.replace(freeText.trim(), "\n ", "");
        return noBlankFreeText.isEmpty()
                || noBlankFreeText.equals("{@inheritDoc}")
                || noBlankFreeText.equals("{@deprecated}");
    }

    /**
     * Check if comment clone could be because of an overloaded method.
     * Originally, this method was also labelling overriding as legitimate
     * clone.
     * <p>
     * Given the amount of actual copy&paste cases happening
     * because of overriding, we now prefer to keep the benefit of the doubt.
     * For overloading, the matter is less borderline: the difference is
     * only in the parameters; if @param are different, it is fine to
     * believe the clone is legitimate. Overload cases which do not differ
     * at least at the @param level will NOT be labeled as legitimate.
     *
     * @param firstName  first method name
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
     * @return true if same return type
     */
    private static boolean isSameReturnType(DocumentedExecutable first, DocumentedExecutable second) {
        String firstType = first.getReturnType();
        // Clean possible generic types
        firstType = JavadocExtractor.rawType(firstType);
        String secondType = second.getReturnType();
        secondType = JavadocExtractor.rawType(secondType);

        // Constructors
        if (firstType.trim().isEmpty()) {
            return secondType.trim().isEmpty();
        }
        if (Reflection.getPrimitiveClasses().get(firstType) != null) {
            // one of the two is a PRIMITIVE type: always consider potential clone
            return false;
        } else return firstType.equals(secondType);
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
        for (File file : list) {
            if (!file.getName().endsWith(".java") || file.getName().contains("package-info")) {
                continue;
            }
            String fileName = file.getAbsolutePath();
            String[] unnecessaryPrefix = fileName.split(path);
            String className = StringUtils.replace(unnecessaryPrefix[1], "/", ".");
            selectedClassNames.add(className.replace(".java", ""));
        }
        return selectedClassNames;

    }
}
