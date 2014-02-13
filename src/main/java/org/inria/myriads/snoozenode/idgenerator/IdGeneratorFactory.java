package org.inria.myriads.snoozenode.idgenerator;

import org.inria.myriads.snoozenode.configurator.node.NodeSettings;
import org.inria.myriads.snoozenode.idgenerator.api.IdGenerator;
import org.inria.myriads.snoozenode.idgenerator.api.impl.HostnameIdGenerator;
import org.inria.myriads.snoozenode.idgenerator.api.impl.HostnamePortIdGenerator;
import org.inria.myriads.snoozenode.idgenerator.api.impl.RandomIdGenerator;
import org.inria.myriads.snoozenode.idgenerator.api.impl.ShortnameIdGenerator;

/**
 * 
 * Id generator.
 * 
 * @author msimonin
 *
 */
public final class IdGeneratorFactory
{
    /**
     * Hide the constructor.
     */
    private IdGeneratorFactory() 
    {
        throw new UnsupportedOperationException();
    }
    
    
    /**
     * 
     * Create a generator.
     * 
     * @param nodeSettings  The node settings.
     * @return  The id generator.
     */
    public static IdGenerator createIdGenerator(NodeSettings nodeSettings)
    {
        switch(nodeSettings.getIdGenerator())
        {
        case random:
            return new RandomIdGenerator();
        case hostname:
            return new HostnameIdGenerator();
        case hostnameport:
            return new HostnamePortIdGenerator();
        case shortname:
            return new ShortnameIdGenerator();
        default:
            return new RandomIdGenerator();
        }
    }


    
}
