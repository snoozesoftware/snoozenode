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
package org.inria.myriads.snoozenode.localcontroller.connector.impl;

import org.inria.myriads.snoozecommon.communication.localcontroller.hypervisor.HypervisorSettings;
import org.inria.myriads.snoozecommon.guard.Guard;
import org.inria.myriads.snoozenode.exception.ConnectorException;
import org.inria.myriads.snoozenode.localcontroller.connector.Connector;
import org.inria.myriads.snoozenode.localcontroller.connector.util.LibVirtUtil;
import org.libvirt.Connect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Libvirt connector implementation.
 * 
 * @author Eugen Feller
 */
public final class LibVirtConnector 
    implements Connector 
{
    /** Logger. */
    private static final Logger log_ = LoggerFactory.getLogger(LibVirtConnector.class);
    
    /** Connector object. */
    private Connect connect_;
    
    /**
     * Libvirt connector.
     * 
     * @param listenAddress         The listen address
     * @param hypervisorSettings    The hypervisor settings
     * @throws ConnectorException   The connector exception
     */
    public LibVirtConnector(String listenAddress, HypervisorSettings hypervisorSettings) 
        throws ConnectorException 
    {
        Guard.check(listenAddress, hypervisorSettings);
        log_.debug("Initializing the libvirt connector");
        connect_ = LibVirtUtil.connectToHypervisor(listenAddress, hypervisorSettings);
    }
    
    /**
     * Returns the connect object.
     * 
     * @return      The connector 
     */
    public Object getConnector() 
    {
        log_.debug("Returning the connect object");
        return connect_;
    }
}
