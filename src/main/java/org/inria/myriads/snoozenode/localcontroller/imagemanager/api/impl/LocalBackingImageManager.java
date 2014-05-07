package org.inria.myriads.snoozenode.localcontroller.imagemanager.api.impl;

import java.io.File;
import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;

import org.inria.myriads.snoozecommon.communication.virtualcluster.VirtualMachineMetaData;
import org.inria.myriads.snoozecommon.communication.virtualcluster.migration.MigrationRequest;
import org.inria.myriads.snoozecommon.virtualmachineimage.VirtualMachineImage;
import org.inria.myriads.snoozenode.configurator.imagerepository.ImageRepositorySettings;
import org.inria.myriads.snoozenode.localcontroller.imagemanager.api.ImageManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * Local Backing Image manager.
 * 
 * @author msimonin
 *
 */
public class LocalBackingImageManager implements ImageManager
{
    /** Define the logger. */
    private static final Logger log_ = LoggerFactory.getLogger(LocalBackingImageManager.class);
     
    /** Cache.*/
    private ArrayList<String> cache_;

    /** The source of the file (absolute pathe to the nfs shared directory).*/
    private String source_;

    /** The destination path (where the images will be stored).*/
    private String destination_; 
    
    
    /**
     * 
     * constructor.
     * 
     * @param settings      The image repository settings.      
     */
    public LocalBackingImageManager(ImageRepositorySettings settings)
    {
        cache_ = new ArrayList<String>();
        source_ = settings.getSource();
        destination_ = settings.getDestination();
    }

    @Override
    public boolean fetchImage(VirtualMachineMetaData virtualMachine)
    {
        int exitCode = 0;
        VirtualMachineImage image = virtualMachine.getImage();
        String sourcePath = source_ + "/" + virtualMachine.getImage().getName();
        String destinationDirectory = destination_;
        String destinationPathMaster = destinationDirectory + "/" + image.getName();
        String destinationPathSlave = 
                destinationDirectory + "/" +  virtualMachine.getVirtualMachineLocation().getVirtualMachineId();
        
        try
        {   
            if (!cache_.contains(image.getName()))
            {
                log_.debug(String.format("copying the master file from %s to %s", 
                        sourcePath,
                        destinationPathMaster));
                Path from = Paths.get(sourcePath);
                Path to = Paths.get(destinationPathMaster);
                //overwrite existing file, if exists
                CopyOption[] options = new CopyOption[]{
                  StandardCopyOption.REPLACE_EXISTING,
                  StandardCopyOption.COPY_ATTRIBUTES
                }; 
                Files.copy(from, to, options);  
                log_.debug("Master file copied");
            }
            
            log_.debug("creating the snapshot");
            String command = String.format(
                    "qemu-img create -b %s -f qcow2 %s",
                    destinationPathMaster,
                    destinationPathSlave
                    );
            
            log_.debug("executing command : " + command);
            Process process = Runtime.getRuntime().exec(command);
            exitCode = process.waitFor();    
        
        }
        catch (IOException e)
        {
            log_.error("Failed to fetch vm image disk " + e.getMessage());
            return false;
        }
        catch (InterruptedException e)
        {
            log_.error("Failed to fecth vm image disk " + e.getMessage());
            return false;
        }
        
        if (exitCode == 0)
        {
            cache_.add(image.getName());
            virtualMachine.getImage().setPath(destinationPathSlave);
            virtualMachine.getImage().setFormat("qcow2");
            return true;
        }
        log_.error("Failed to fetch vm image disk");
        return false;
    }

    @Override
    public boolean prepareMigration(MigrationRequest migrationRequest,
            ImageRepositorySettings imageRepositorySettings,
            VirtualMachineImage virtualMachineImage
            )
    {
        log_.debug("Migration is not implemented yet when using localBacking image type !");
        return false;
    }

    @Override
    public boolean prepareMigration(VirtualMachineImage virtualMachineImage)
    {
        log_.debug("Migration is not implemented yet when using localBacking image type !");
        return false;
    }
    
    @Override
    public boolean removeDisk(VirtualMachineImage image, ImageRepositorySettings imageRepositorySettings)
    {
        
        String sourcePath = imageRepositorySettings.getSource();
        String destinationPath = imageRepositorySettings.getDestination();
        if (sourcePath.equals(destinationPath))
        {
            // Meaning that everything is on a shared directory.
            // nothing to do
            return true;
        }
        
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
