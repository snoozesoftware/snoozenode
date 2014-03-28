package org.inria.myriads.snoozenode.localcontroller.imagemanager.api.impl;

import java.io.File;
import java.io.IOException;

import org.inria.myriads.snoozecommon.communication.NetworkAddress;
import org.inria.myriads.snoozecommon.communication.rest.api.LocalControllerAPI;
import org.inria.myriads.snoozecommon.communication.rest.api.impl.RESTLocalControllerCommunicator;
import org.inria.myriads.snoozecommon.communication.virtualcluster.VirtualMachineMetaData;
import org.inria.myriads.snoozecommon.communication.virtualcluster.migration.MigrationRequest;
import org.inria.myriads.snoozecommon.guard.Guard;
import org.inria.myriads.snoozecommon.virtualmachineimage.VirtualMachineImage;
import org.inria.myriads.snoozenode.configurator.imagerepository.ImageRepositorySettings;
import org.inria.myriads.snoozenode.localcontroller.imagemanager.api.ImageManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * Backing image manager. 
 * 
 * master file on a shared directory.
 * slave file local.
 * 
 * @author msimonin
 *
 */
public class BackingImageManager implements ImageManager
{
    /** Define the logger. */
    private static final Logger log_ = LoggerFactory.getLogger(BackingImageManager.class);
     
    /** Source. (snapshot local directory path)*/
    private String source_;
    
    /** destination. (local directory path)*/
    private String destination_;
    
    /**
     * 
     * Constructor.
     * 
     * @param settings  The image repository settings.
     */
    public BackingImageManager(ImageRepositorySettings settings)
    {
        source_ = settings.getSource();
        destination_ = settings.getDestination();
    }

    @Override
    public boolean fetchImage(VirtualMachineMetaData virtualMachine)
    {
        int exitCode = 0;
        String sourcePath = source_ + "/" + virtualMachine.getImage().getName();
        String destinationPath = destination_ + "/" + virtualMachine.getVirtualMachineLocation().getVirtualMachineId();
        try
        {
            
            String command = String.format(
                    "qemu-img create -b %s -f qcow2 %s",
                    sourcePath,
                    destinationPath
                    );
            log_.debug("executing command : " + command);
            Process process = Runtime.getRuntime().exec(command);
            exitCode = process.waitFor();
        }
        catch (IOException e)
        {
            e.printStackTrace();
            log_.error("Failed to fecth vm image disk");
            return false;
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
            log_.error("Failed to fecth vm image disk 2");
            return false;
        }
        if (exitCode == 0)
        {
            virtualMachine.getImage().setPath(destinationPath);
            virtualMachine.getImage().setFormat("qcow2");
            virtualMachine.getImage().setBackingStore(sourcePath);
            return true;
        }
        log_.error("Failed to fecth vm image disk 3");
        return false;
    }


    @Override
    public boolean prepareMigration(MigrationRequest migrationRequest,
            ImageRepositorySettings imageRepositorySettings,
            VirtualMachineImage virtualMachineImage)
    {
        Guard.check(migrationRequest, imageRepositorySettings, virtualMachineImage);
        
        String sourcePath = imageRepositorySettings.getSource();
        String destinationPath = imageRepositorySettings.getDestination();
        if (sourcePath.equals(destinationPath))
        {
            // Meaning that everything is on a shared directory.
            // nothing to do
            return true;
        }
        // we should create a container for the new image disk.
        NetworkAddress destinationAddress = migrationRequest.getDestinationVirtualMachineLocation().getLocalControllerControlDataAddress();
        LocalControllerAPI localControllerCommunicator = new RESTLocalControllerCommunicator(destinationAddress);
        boolean isPrepared = localControllerCommunicator.prepareMigration(virtualMachineImage);
        
        return isPrepared;
    }

    @Override
    public boolean prepareMigration(VirtualMachineImage virtualMachineImage)
    {
        log_.debug("Preparing the migration");
        String name = virtualMachineImage.getName();
        if (name == null)
        {
            log_.debug("Unable to extract the name of the virtual machine under migration");
        }
        
        Long capacity = virtualMachineImage.getCapacity();
        if (capacity == 0)
        {
            log_.debug("Unable to extract the capacity of the image disk");
        }
        
        int exitCode = 0;
        
        String backingStore = virtualMachineImage.getBackingStore();
        String sourcePath = "";
        String destinationPath = virtualMachineImage.getPath();
        
        try{
            String command = "";
            if (backingStore != null)
            {
                sourcePath = backingStore;
                command = String.format(
                        "qemu-img create -b %s -f qcow2 %s %s",
                        sourcePath,
                        destinationPath,
                        capacity.toString()
                        );
            }
            else
            {
                command = String.format(
                        "qemu-img create  -f qcow2 %s %s",
                        destinationPath,
                        capacity.toString()
                        );
            }
            log_.debug("executing command : " + command);
            
            Process process = Runtime.getRuntime().exec(command);
            exitCode = process.waitFor();
        }
        catch (Exception exception)
        {
            log_.debug("Unable to create the empty image container.");
            return false;
        }
        
        return true;
    }

    @Override
    public boolean removeDisk(VirtualMachineImage image, ImageRepositorySettings imageRepositorySettings)
    {
        Guard.check(image, imageRepositorySettings);
        
        log_.debug("removing the disk image");
        File imagePath = new File(image.getPath());
        
        if (!imagePath.exists())
        {
            log_.debug("The file doesn't exist");
            return true;
        }
        
        boolean isDeleted = imagePath.delete();
        log_.debug("Has the file been deleted ? " + isDeleted);
        
        return isDeleted;
    }

}
