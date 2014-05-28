/**
 * Copyright (C) 2010-2013 Eugen Feller, INRIA <eugen.feller@inria.fr>
 *
 * This file is part of Snooze, a scalable, autonomic, and
 * energy-aware virtual machine (VM) management framework.
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see <http://www.gnu.org/licenses>.
 */
package org.inria.myriads.snoozenode.configurator.imagerepository;

import org.inria.myriads.snoozecommon.communication.NetworkAddress;
import org.inria.myriads.snoozecommon.globals.Globals;

/**
 * Database settings.
 * 
 * @author Matthieu Simonin
 */
public final class ImageRepositorySettings 
{
    /** Image repository address. */
    private NetworkAddress imageRepositoryAddress_;
    
    /** disks type.*/
    private DiskHostingType diskType_;
    
    
    /** Source path (mounted locally).*/
    private String source_;
    
    /** Source path (stored locally).*/
    private String destination_;
    
    /**
     * Constructor. 
     */
    public ImageRepositorySettings()
    {
        source_ = Globals.DEFAULT_INITIALIZATION;
        destination_ = Globals.DEFAULT_INITIALIZATION;
    }

    /**
     * @return the imageRepositoryAddress
     */
    public NetworkAddress getImageRepositoryAddress()
    {
        return imageRepositoryAddress_;
    }


    /**
     * @param imageRepositoryAddress the imageRepositoryAddress to set
     */
    public void setImageRepositoryAddress(NetworkAddress imageRepositoryAddress)
    {
        imageRepositoryAddress_ = imageRepositoryAddress;
    }

    /**
     * @return the diskType
     */
    public DiskHostingType getDiskType()
    {
        return diskType_;
    }

    /**
     * @param diskType the diskType to set
     */
    public void setDiskType(DiskHostingType diskType)
    {
        this.diskType_ = diskType;
    }

    /**
     * @return the source
     */
    public String getSource()
    {
        return source_;
    }

    /**
     * @param source the source to set
     */
    public void setSource(String source)
    {
        source_ = source;
    }

    /**
     * @return the destination
     */
    public String getDestination()
    {
        return destination_;
    }

    /**
     * @param destination the destination to set
     */
    public void setDestination(String destination)
    {
        destination_ = destination;
    }
    
    
}
