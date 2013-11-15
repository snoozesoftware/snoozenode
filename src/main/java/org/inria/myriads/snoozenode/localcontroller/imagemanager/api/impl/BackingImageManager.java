package org.inria.myriads.snoozenode.localcontroller.imagemanager.api.impl;

import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Map;

import org.inria.myriads.snoozecommon.communication.virtualcluster.VirtualMachineMetaData;
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
    String source_ ;
    
    /** destination. (local directory path)*/
    String destination_ ;
    
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
            return true;
        }
        log_.error("Failed to fecth vm image disk 3");
        return false;
    }

}
