package org.inria.myriads.snoozenode.configurator.globals;

/**
 * 
 * Globals settings.
 * 
 * @author msimonin
 *
 */
public class GlobalsSettings 
{
    /** plugin directory.*/
    private String pluginsDirectory_;

    /**
     * Constructor. 
     */
    public GlobalsSettings()
    {
    }

    /**
     * @return the pluginDirectory
     */
    public String getPluginsDirectory()
    {
        return pluginsDirectory_;
    }

    /**
     * @param pluginsDirectory the pluginDirectory to set
     */
    public void setPluginsDirectory(String pluginsDirectory)
    {
        pluginsDirectory_ = pluginsDirectory;
    }
    
    
    
}
