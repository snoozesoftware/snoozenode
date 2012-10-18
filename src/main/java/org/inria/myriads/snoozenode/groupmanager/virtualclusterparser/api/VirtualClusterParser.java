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
package org.inria.myriads.snoozenode.groupmanager.virtualclusterparser.api;

import java.util.ArrayList;
import java.util.List;

import org.inria.myriads.snoozecommon.communication.virtualcluster.VirtualMachineMetaData;
import org.inria.myriads.snoozecommon.communication.virtualcluster.submission.VirtualClusterSubmissionRequest;
import org.inria.myriads.snoozenode.exception.VirtualClusterParserException;

/**
 * Virtual cluster parster interface.
 * 
 * @author Eugen Feller
  */
public interface VirtualClusterParser 
{
    /**
     * Creates virtual machine meta data.
     * 
     * @param cluster                           The virtual cluster description
     * @return                                  The virtual machine meta data map
     * @throws VirtualClusterParserException 
     */
    ArrayList<VirtualMachineMetaData> createVirtualMachineMetaData(VirtualClusterSubmissionRequest cluster) 
        throws VirtualClusterParserException;

    /**
     * Get the network interfaces for the a xml description.
     * 
     * @param                                   xmlDescription    
     * @return                                  List of network interfaces
     * @throws VirtualClusterParserException
     */
    List<String> getNetworkInterfaces(String xmlDescription) throws VirtualClusterParserException;
    
    
    /**
     *  Gets the MAC from template.
     * 
     * @param xmlDescription        xml template
     * @return                      mac address
     */
    String getMacAddress(String xmlDescription);
    
    /**
     *  Replace the MAC from template.
     * 
     * @param xmlDesc           template
     * @param newMacAddress     the new address
     * @return                  mac address
     */
    String replaceMacAddressInTemplate(String xmlDesc, String newMacAddress);
    
}