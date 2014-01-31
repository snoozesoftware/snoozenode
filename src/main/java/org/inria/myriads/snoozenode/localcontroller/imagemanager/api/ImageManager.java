package org.inria.myriads.snoozenode.localcontroller.imagemanager.api;

import org.inria.myriads.snoozecommon.communication.virtualcluster.VirtualMachineMetaData;
import org.inria.myriads.snoozecommon.communication.virtualcluster.migration.MigrationRequest;
import org.inria.myriads.snoozecommon.virtualmachineimage.VirtualMachineImage;
import org.inria.myriads.snoozenode.configurator.imagerepository.ImageRepositorySettings;

/**
 * 
 * 
 * @author msimonin
 *
 */
public interface ImageManager
{
    /**
     * 
     * Fetch the image.
     * 
     * @param virtualMachine    The virtual machine meta data.
     * @return  true iff the image has been successfully fetched.
     */
    boolean fetchImage(VirtualMachineMetaData virtualMachine);

    /**
     * 
     * Prepare the images before a migration (remote).
     * 
     * @param migrationRequest          migrationRequest
     * @param imageRepositorySettings   imageRepositorySettings
     * @param virtualMachineImage       virtualMachineImage
     * @return true iff everything is fine.
     */
    boolean prepareMigration(MigrationRequest migrationRequest, ImageRepositorySettings imageRepositorySettings,
            VirtualMachineImage virtualMachineImage);

    
    /**
     * 
     * Prepare migration (locally).
     * 
     * @param virtualMachineImage   virtualMachineImage
     * @return true iff everything is fine.
     */
    boolean prepareMigration(VirtualMachineImage virtualMachineImage);

    
    
    /**
     * 
     * Remove the disk.
     * 
     * @param image     the image to remove.
     * @return  true iff everything is fine.
     */
    boolean removeDisk(VirtualMachineImage image, ImageRepositorySettings imageRepositorySettings);
    
}
