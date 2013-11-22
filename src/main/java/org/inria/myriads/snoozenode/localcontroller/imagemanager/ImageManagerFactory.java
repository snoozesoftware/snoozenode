package org.inria.myriads.snoozenode.localcontroller.imagemanager;

import org.inria.myriads.snoozenode.configurator.imagerepository.DiskHostingType;
import org.inria.myriads.snoozenode.configurator.imagerepository.ImageRepositorySettings;
import org.inria.myriads.snoozenode.localcontroller.imagemanager.api.ImageManager;
import org.inria.myriads.snoozenode.localcontroller.imagemanager.api.impl.BackingImageManager;
import org.inria.myriads.snoozenode.localcontroller.imagemanager.api.impl.LocalBackingImageManager;

/**
 * 
 * Image manager factory.
 * 
 * @author msimonin
 *
 */
public final class ImageManagerFactory
{
    /**
     * Hide the consturctor.
     */
    private ImageManagerFactory() 
    {
        throw new UnsupportedOperationException();
    }
    
    
    /**
     * 
     * creates a new Image manager.
     * 
     * @param settings  The image repository settings.
     * @return  the Image Manager (default to backing).
     */
    public static ImageManager newImageManager(ImageRepositorySettings settings)
    {
        DiskHostingType diskType = settings.getDiskType();
        switch(diskType)
        {
        case backing:
            return new BackingImageManager(settings);
        case localBacking:
            return new LocalBackingImageManager();
        default:
            return new BackingImageManager(settings);
        }
        
    }
}
