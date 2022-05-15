package org.replicomment.util;

import org.apache.commons.io.FileUtils;
import org.replicomment.JavadocClonesFinder;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Reflection {

  private static final Map<String, Class> primitiveClasses = initializePrimitivesMap();

  private static Map<String, Class> initializePrimitivesMap() {
    Map<String, Class> map = new HashMap<>(9);
    map.put("int", Integer.TYPE);
    map.put("long", Long.TYPE);
    map.put("double", Double.TYPE);
    map.put("float", Float.TYPE);
    map.put("boolean", Boolean.TYPE);
    map.put("char", Character.TYPE);
    map.put("byte", Byte.TYPE);
    map.put("void", Void.TYPE);
    map.put("short", Short.TYPE);
    return map;
  }

  /** Makes constructor private to prevent the instantiation of this class objects. */
  private Reflection() {}

  public static Map<String, Class> getPrimitiveClasses() {
    return primitiveClasses;
  }

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

    final List<URL> urls = new ArrayList<>();
    try {

      InputStream resourceAsStream = JavadocClonesFinder.class.getResourceAsStream("/jars.txt");
      List<String> jars =
              new BufferedReader(new InputStreamReader(resourceAsStream,
                      StandardCharsets.UTF_8)).lines().collect(Collectors.toList());


//      List<String> jars = FileUtils.readLines(new File(
//              JavadocClonesFinder.class
//                      .getResource("/jars.txt").getPath()));
      for(String jar : jars){
        urls.add(new URL("file:"+jar));
      }
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
