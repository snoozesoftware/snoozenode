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
package org.inria.myriads.snoozenode.database.api.impl.memory;


import java.util.List;

import org.inria.myriads.snoozecommon.communication.groupmanager.GroupManagerDescription;
import org.inria.myriads.snoozecommon.communication.localcontroller.LocalControllerList;
import org.inria.myriads.snoozecommon.communication.virtualcluster.VirtualMachineMetaData;
import org.inria.myriads.snoozenode.database.api.BootstrapRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bootstrap "in-memory" repository.
 * 
 * @author msimonin 
 */
public final class BootstrapMemoryRepository 
    implements BootstrapRepository 
{
    /** Logger. */
    private static final Logger log_ = LoggerFactory.getLogger(BootstrapMemoryRepository.class);

    @Override
    public VirtualMachineMetaData getVirtualMachineMetaData(String virtualMachineId, int numberOfMonitoringEntries)
    {
        log_.error("Not implemented yet");
        return null;
    }

    @Override
    public List<GroupManagerDescription> getGroupManagerDescriptions(String firstGroupManagerId, int limit)
    {
        return null;
    }

    @Override
    public LocalControllerList getLocalControllerList()
    {
        // TODO Auto-generated method stub
        return null;
    }
    
}
