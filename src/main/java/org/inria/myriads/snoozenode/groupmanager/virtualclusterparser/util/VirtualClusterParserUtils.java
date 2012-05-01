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
package org.inria.myriads.snoozenode.groupmanager.virtualclusterparser.util;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.inria.myriads.snoozecommon.guard.Guard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Virtual cluster parser utilities.
 * 
 * @author Eugen Feller
 */
public final class VirtualClusterParserUtils 
{
    /** Define the logger. */
    private static final Logger log_ = LoggerFactory.getLogger(VirtualClusterParserUtils.class);
    
    /** Hide constructor. */
    private VirtualClusterParserUtils()
    {
        throw new UnsupportedOperationException();
    }
    
    /**
     * Replaces the MAC address inside libvirt template.
     * 
     * @param template          The template
     * @param newMacAddress     The new mac address
     * @return                  Modified template string
     */
    public static String replaceMacAddressInLibVirtTemplate(String template, String newMacAddress)
    {
        Guard.check(template, newMacAddress);        
        log_.debug("Replacing MAC address in the libvirt template with: " + newMacAddress);
        
        String currentMacAddress = getMacAddressFromLibVirtTemplate(template);        
        log_.debug("Current MAC address is: " + currentMacAddress);
        String newTemplate = template.replaceAll(currentMacAddress, newMacAddress);        
        return newTemplate;
    }
    
    /**
     * Gets the MAC of the libvirt template.
     * 
     * @param template      The template
     * @return              The mac address
     */
    public static String getMacAddressFromLibVirtTemplate(String template)
    {
        Guard.check(template);
        StringTokenizer tokenizer = new StringTokenizer(template, "\n");
        String finalMacAddress = null;
        
        while (tokenizer.hasMoreTokens())
        {
            String line = tokenizer.nextToken().trim();     
            int macAddressPosition = line.indexOf("mac address");            
            if (macAddressPosition <= 0)
            {
                continue;
            }
            
            String macAddressCandidate = line.substring(macAddressPosition + 13).trim();
            finalMacAddress = macAddressCandidate.substring(0, macAddressCandidate.length() - 3);
        }
        
        return finalMacAddress;       
    }

    /**
     * Returns a list of networks attached to a domain.
     * 
     * Note: This implementation is fragile! Libvirt 0.9.4 is missing an API to get network 
     * interfaces attached to a domain! Hence, we need to dump and parse domain XML description! 
     * Open problem: The default network interface names (i.e., vnetX) could have been overridden by the users!
     *
     * @param xmlDescription    The xml description
     * @return                  The list of network interfaces
     */
    public static List<String> getNetworkInterfacesFromXml(String xmlDescription) 
    {
        Guard.check(xmlDescription);
        
        StringTokenizer tokenizer = new StringTokenizer(xmlDescription, "\n");
        List<String> networkInterfaces = new ArrayList<String>();
        
        while (tokenizer.hasMoreTokens())
        {
            String line = tokenizer.nextToken().trim();     
            int devPosition = line.indexOf("vnet");            
            if (devPosition <= 0)
            {
                continue;
            }   
            
            log_.debug("Line: " + line);
            String interfaceCandidate = line.substring(devPosition).trim();
            
            log_.debug("Candidate: " + interfaceCandidate);
            String finalInterface = interfaceCandidate.substring(0, interfaceCandidate.length() - 3);
            
            log_.debug("Found interface name: " + finalInterface);
            networkInterfaces.add(finalInterface);
        }
        
        return networkInterfaces;   
    }
}
