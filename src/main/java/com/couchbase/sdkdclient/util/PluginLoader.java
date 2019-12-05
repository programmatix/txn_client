/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.couchbase.sdkdclient.util;

import com.couchbase.sdkdclient.logging.LogUtil;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class to load plugins. It will first try to load by absolute class name; upon
 * failure it will load by relative name, and if that fails, it will invoke a
 * callback trying to process a name
 *
 * @param <T> the class whose instance shall be returned
 */
public class PluginLoader<T> {

  private List<Throwable> errorLog = new ArrayList<Throwable>();
  private final Map<String,String> aliasMap = new HashMap<String, String>();
  private final String classPrefix;
  private final ClassLoader loader = ClassLoader.getSystemClassLoader();
  private final Class retCls;
  private final static Logger logger = LogUtil.getLogger(PluginLoader.class);
  private int numTries = 0;

  public PluginLoader(Class cls, String prefix) {
    retCls = cls;
    classPrefix = prefix;
  }

  /**
   * Convert the user provided name into a class name
   *
   * @param input The user-provided name
   * @return A canonical classname, or null if it cannot be mapped
   */
  public String convertToClassname(String input) {
    return null;
  }

  /**
   * Adds the prefix to the name
   *
   * @param name The relative name
   * @return The fully qualified name, or null if no prefix was set
   */
  private String expandName(String name) {
    if (classPrefix != null) {
      return String.format("%s.%s", classPrefix, name);
    }

    return null;
  }

  /**
   * Given a class, instantiate a new plugin from this class
   *
   * @param cls The loaded class
   * @return a new instance
   * @throws ClassNotFoundException If an error occurred during instantiation.
   */
  @SuppressWarnings("unchecked")
  private T instantiatePlugin(Class cls) throws ClassNotFoundException {
    Class subCls = cls.asSubclass(retCls);
    T ret = null;
    try {
      ret = (T) subCls.newInstance();
    } catch (IllegalAccessException ex1) {
      errorLog.add(ex1);
    } catch (InstantiationException ex2) {
      errorLog.add(ex2);
    }
    if (ret == null) {
      throw new ClassNotFoundException();
    }
    return ret;
  }

  /**
   * Get the proper class depending on the stage
   *
   * @param uName User provided name
   * @return a class instance, or null if it could not be found
   */
  private Class getPluginClass(String uName) {
    if (numTries == 1) {
      uName = expandName(uName);
    } else if (numTries == 2) {
      String aliasName = getAlias(uName);
      if (aliasName != null) {
        uName = aliasName;
      } else {
        uName = convertToClassname(uName);
      }
    }

    if (uName == null) {
      return null;
    }

    try {
      return loader.loadClass(uName);
    } catch (ClassNotFoundException ex) {
      errorLog.add(ex);
      return null;
    }
  }

  /**
   * Return a new instance of the given plugin name
   *
   * @param name
   * @return A new instance
   * @throws ClassNotFoundException If item could not be found.
   */
  public T getPluginInstance(String name) throws ClassNotFoundException {
    numTries = 0;

    while (numTries < 3) {
      Class cls = getPluginClass(name);

      if (cls != null) {
        return instantiatePlugin(cls);
      }
      numTries++;
    }

    for (Throwable e : errorLog) {
      logger.error("Error loading " + name, e);
    }

    throw new ClassNotFoundException();
  }

  private void addAlias(String from, String to) {
    aliasMap.put(from, to);
    aliasMap.put(from.toLowerCase(), to);
  }

  public void addAlias(String from, Class<? extends T> to) {
    addAlias(from, to.getCanonicalName());
  }

  private String getAlias(String uInput) {
    String ret = aliasMap.get(uInput);
    if (ret != null) {
      return ret;
    }
    uInput = uInput.toLowerCase();
    return aliasMap.get(uInput);
  }
}
