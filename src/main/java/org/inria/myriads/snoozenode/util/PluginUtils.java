package org.inria.myriads.snoozenode.util;

import java.io.File;
import java.io.FilenameFilter;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

import org.inria.myriads.snoozecommon.guard.Guard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author msimonin
 *
 */
public final class PluginUtils
{
    /** Define the logger. */
    private static final Logger log_ = LoggerFactory.getLogger(PluginUtils.class);
    
    
    public static String pluginsDirectory_ = ".";
    
    
    /**
     * Hide the consturctor.
     */
    private PluginUtils() 
    {
        throw new UnsupportedOperationException();
    }
    
    
    /**
     * 
     * Gets a class from a directory.
     * 
     * @param pluginsDirectory          Directory to look in.
     * @param classToLoad               Name of the class to load.
     * @return                          The class loaded.
     * 
     * @throws MalformedURLException    Malformed url exception.
     * @throws ClassNotFoundException   Class not found exception.
     */
    @SuppressWarnings("rawtypes")
    public static Class getClassFromDirectory(String pluginsDirectory, String classToLoad) 
                throws MalformedURLException, ClassNotFoundException
    {
        
      Guard.check(pluginsDirectory, classToLoad);
      File dir = new File(pluginsDirectory);
      
      log_.debug(dir.getAbsolutePath());
      File[] files = dir.listFiles(new FilenameFilter()
      {
         @Override
          public boolean accept(File dir, String name) 
         {
              return name.endsWith(".jar");
          }
      });

      if (files == null)
      {
          throw new MalformedURLException(String.format("Unable to get files from directory %s", pluginsDirectory));
      }
          
      
      URL[] urls = new URL[files.length];
      
      int i = 0;
      for (File file:files)
      {
          log_.debug("file " + file.toURI().toURL().toString());
          urls[i] = file.toURI().toURL();
          i++; 
      }
      
      ClassLoader loader = new URLClassLoader(urls);
      Class classLoaded = loader.loadClass(String.valueOf(classToLoad));
      
      return classLoaded;
    }


    public static Class getClassFromPluginsDirectory(String classURI) throws MalformedURLException, ClassNotFoundException
    {
        return getClassFromDirectory(pluginsDirectory_, classURI);
    }

    public static Object createFromFQN(String fqn) throws Exception
    {
        log_.debug(String.format("instantiate the class %s from fqn", fqn));
        Class<?> customClass = null;
        Object customObject = null; 
        try
        {
            customClass = PluginUtils.getClassFromPluginsDirectory(fqn);
            customObject =
                customClass.getConstructor().newInstance();
        }
        catch (MalformedURLException | ClassNotFoundException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e)
        {            
            e.printStackTrace();
            throw new Exception("Unable to instantiate the custom class");
        }        
        return customObject;
    }


}
