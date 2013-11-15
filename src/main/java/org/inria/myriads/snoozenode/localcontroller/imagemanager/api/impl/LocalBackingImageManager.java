package org.inria.myriads.snoozenode.localcontroller.imagemanager.api.impl;

import java.io.File;
import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Map;

import org.inria.myriads.snoozecommon.communication.virtualcluster.VirtualMachineMetaData;
import org.inria.myriads.snoozecommon.virtualmachineimage.VirtualMachineImage;
import org.inria.myriads.snoozenode.localcontroller.imagemanager.api.ImageManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LocalBackingImageManager implements ImageManager
{
    /** Define the logger. */
    private static final Logger log_ = LoggerFactory.getLogger(LocalBackingImageManager.class);
     
    /** Cache.*/
    private ArrayList<String> cache_; 
    
    /**
     * Constructor. 
     */
    public LocalBackingImageManager()
    {
        cache_ = new ArrayList<String>();
    }

    @Override
    public boolean fetchImage(VirtualMachineMetaData virtualMachine)
    {
        int exitCode = 0;
        VirtualMachineImage image = virtualMachine.getImage();
        String sourcePath = image.getPath();
        String destinationDirectory = "/var/lib/libvirt/images/";
//        File dir = new File(destinationDirectory);
//        dir.mkdirs();
        String destinationPathMaster = destinationDirectory + image.getName()+"2";
        String destinationPathSlave = destinationDirectory + virtualMachine.getVirtualMachineLocation().getVirtualMachineId();
        try
        {   
            if (!cache_.contains(image.getName()))
            {
                log_.debug("copying the master file");
                Path from = Paths.get(sourcePath);
                Path to = Paths.get(destinationPathMaster);
                //overwrite existing file, if exists
                CopyOption[] options = new CopyOption[]{
                  StandardCopyOption.REPLACE_EXISTING,
                  StandardCopyOption.COPY_ATTRIBUTES
                }; 
                Files.copy(from, to, options);    
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
            cache_.add(image.getName());
            virtualMachine.getImage().setPath(destinationPathSlave);
            virtualMachine.getImage().setFormat("qcow2");
            return true;
        }
        log_.error("Failed to fecth vm image disk 3");
        return false;
    }

}
