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
package org.inria.myriads.snoozenode.tcpip;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;

import org.apache.commons.io.IOUtils;

import org.inria.myriads.snoozecommon.communication.NetworkAddress;
import org.inria.myriads.snoozecommon.guard.Guard;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TCP data sender.
 * 
 * @author Eugen Feller
 */
public abstract class TCPDataSender 
{    
    /** Define the logger. */
    private static final Logger log_ = LoggerFactory.getLogger(TCPDataSender.class);
    
    /** Client socket. */
    private Socket clientSocket_;
    
    /** Object output stream. */
    private ObjectOutputStream outputStream_;
    
    /**
     * TCP data sender consturctor.
     * 
     * @param networkAddress          The network address
     * @throws IOException            The I/O exception
     */
    protected TCPDataSender(NetworkAddress networkAddress) 
        throws IOException 
    {
        Guard.check(networkAddress);
        log_.debug(String.format("Initializing the TCP data sender for %s : %d", 
                                 networkAddress.getAddress(), networkAddress.getPort()));
        
        clientSocket_ = new Socket(networkAddress.getAddress(), networkAddress.getPort());
        outputStream_ = new ObjectOutputStream(clientSocket_.getOutputStream());
    }           

    /** 
     * Main routine to send data.
     *  
     * @param data          The data object
     * @throws IOException  The I/O exception
     */
    public void send(Object data)
        throws IOException 
    {
        Guard.check(data);
        outputStream_.writeObject(data);
    }
    
    /**
     * Closes the sender.
     */
    public void close() 
    {
        log_.debug("Closing the socket and output stream");
        if (clientSocket_ != null)
        {
            IOUtils.closeQuietly(clientSocket_);
        }
        
        if (outputStream_ != null)
        {
            IOUtils.closeQuietly(outputStream_);
        }
    }
}
