/**
 * Copyright (C) 2010-2012 Eugen Feller, INRIA <eugen.feller@inria.fr>
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
package org.inria.myriads.snoozenode.bootstrap;

import org.inria.myriads.snoozecommon.communication.groupmanager.GroupManagerDescription;
import org.inria.myriads.snoozecommon.communication.rest.api.BootstrapAPI;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bootstrap resource.
 * 
 * @author Eugen Feller
 */
public final class BootstrapResource extends ServerResource 
    implements BootstrapAPI
{
    /** Define the logger. */
    private static final Logger log_ = LoggerFactory.getLogger(BootstrapResource.class);
          
    /** Backend backend reference. */
    private BootstrapBackend backend_;
             
    /**
     * Constructor.
     */
    public BootstrapResource()
    {
        log_.debug("Starting bootstrap resource");
        backend_ = (BootstrapBackend) getApplication().getContext().getAttributes().get("backend");
    }

    /** 
     * Assign local controller to a group manager.
     * (called by the local controller)
     *  
     * @return   The group leader description
     */
    public GroupManagerDescription getGroupLeaderDescription() 
    {
        log_.debug("Received group leader information request");
        
        if (!isBackendActive())
        {
            log_.debug("Backend is not initialized yet!");
            return null;
        }
        
        GroupManagerDescription groupLeaderDescription = backend_.getGroupLeaderDescription();        
        if (groupLeaderDescription != null)
        {
            log_.debug(String.format("Returning group leader %s:%d", 
                                      groupLeaderDescription.getListenSettings().getControlDataAddress().getAddress(),
                                      groupLeaderDescription.getListenSettings().getControlDataAddress().getPort()));
        }
        
        return groupLeaderDescription;
    }
    
    /** 
     * Check backend activity.
     * 
     * @return  true if active, false otherwise
     */
    private boolean isBackendActive()
    {
        if (backend_ == null) 
        {
            return false;
        }
        
        return true;
    }
}
