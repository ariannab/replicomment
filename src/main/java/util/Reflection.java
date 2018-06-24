package util;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Reflection {

  private static final Map<String, Class> primitiveClasses = initializePrimitivesMap();

  private static Map<String, Class> initializePrimitivesMap() {
    Map<String, Class> map = new HashMap<>(9);
    map.put("int", Integer.TYPE);
    map.put("long", Long.TYPE);
    map.put("double", Double.TYPE);
    map.put("float", Float.TYPE);
    map.put("bool", Boolean.TYPE);
    map.put("char", Character.TYPE);
    map.put("byte", Byte.TYPE);
    map.put("void", Void.TYPE);
    map.put("short", Short.TYPE);
    return map;
  }

  /** Makes constructor private to prevent the instantiation of this class objects. */
  private Reflection() {}

  /**
   * Returns the {@code Class} object for the class with the given name or null if the class could
   * not be retrieved.
   *
   * @param className the fully qualified name of a class
   * @return the {@code Class} object for the given class
   * @throws ClassNotFoundException if class {@code className} cannot be loaded
   */
  public static Class<?> getClass(String className) throws ClassNotFoundException {
    if (primitiveClasses.containsKey(className)) {
      return primitiveClasses.get(className);
    }

    // The order here is important. We have to first look in the paths specified by the user and
    // then in the default class path. The default classpath contains the dependencies of Toradocu
    // that could clash with the system under analysis.
    final List<URL> urls = new ArrayList<>();
    try {
      urls.add(new URL("file:/home/arianna/comment-clones/javadoclones/src/resources/bin/junit-4.12.jar"));
      urls.add(new URL("file:/home/arianna/comment-clones/javadoclones/src/resources/bin/lucene-core-7.2.1.jar"));
      urls.add(new URL("file:/home/arianna/comment-clones/javadoclones/src/resources/bin/solr-solrj-7.1.0.jar"));
      urls.add(new URL("file:/home/arianna/comment-clones/javadoclones/src/resources/bin/guava-19.0.jar"));
      urls.add(new URL("file:/home/arianna/comment-clones/javadoclones/src/resources/bin/hadoop-hdfs-2.6.5.jar"));
      urls.add(new URL("file:/home/arianna/comment-clones/javadoclones/src/resources/bin/hadoop-common-2.6.5.jar"));
      urls.add(new URL("file:/home/arianna/comment-clones/javadoclones/src/resources/bin/vertx-core-3.5.0.jar"));
      urls.add(new URL("file:/home/arianna/comment-clones/javadoclones/src/resources/bin/elasticsearch-6.1.1.jar"));
      urls.add(new URL("file:/home/arianna/comment-clones/javadoclones/src/resources/bin/spring-core-5.0.2.jar"));
      urls.add(new URL("file:/home/arianna/comment-clones/javadoclones/src/resources/bin/log4j-1.2.17.jar"));
      urls.add(new URL("file:/home/arianna/comment-clones/javadoclones/src/resources/bin/rxjava-1.3.5.jar"));
    } catch (MalformedURLException e) {
    }
    final URLClassLoader loader = new URLClassLoader(urls.toArray(new URL[urls.size()]), null);
    try {
      return loader.loadClass(className);
    } catch (ClassNotFoundException e) {
      return Class.forName(className);
    }
  }
}
