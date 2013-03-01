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

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;

import java.util.ArrayList;
import java.util.List;

import org.inria.myriads.snoozecommon.communication.NetworkAddress;
import org.inria.myriads.snoozecommon.guard.Guard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TCP data receiver.
 * 
 * @author Eugen Feller
 */
public abstract class TCPDataReceiver 
    implements Runnable 
{
    /** Define the logger. */
    private static final Logger log_ = LoggerFactory.getLogger(TCPDataReceiver.class);
    
    /** Server socket. */
    private ServerSocket serverSocket_;
    
    /** Failure event handler reference. */
    private DataListener dataHandler_;
    
    /** Worker threads list. */
    private List<TCPWorkerThread> workerThreads_;
    
    /** Timeout. */
    private int timeout_;
    
    /** Signals termination. */
    private boolean isTerminated_;
    
    /**
     * Constructor.
     * 
     * @param networkAddress    The network address
     * @param timeout           The timeout
     * @throws IOException      The I/O exception
     */
    public TCPDataReceiver(NetworkAddress networkAddress, int timeout) 
        throws IOException 
    {
        Guard.check(networkAddress, timeout);
        log_.debug(String.format("Initializing the data receiver at address: %s, port: %d, timeout: %s", 
                                 networkAddress.getAddress(), networkAddress.getPort(), timeout));
              
        timeout_ = timeout;
        workerThreads_ = new ArrayList<TCPWorkerThread>();
        serverSocket_ = new ServerSocket();
        SocketAddress socketAddress = new InetSocketAddress(networkAddress.getAddress(), networkAddress.getPort());
        serverSocket_.bind(socketAddress);   
    }
    
    /** 
     *  Sets the failure event handler.
     *  
     * @param danaHandler   The data handler
     */
    public void setHandler(DataListener danaHandler) 
    {
        Guard.check(danaHandler);
        dataHandler_ = danaHandler;
    }

    /** 
     * Accepts new clients and reads data.
     */
    public void run()
    {
        Socket clientSocket = null;
        try
        {
            while (!isTerminated_) 
            {            
                log_.debug("Waiting for incoming connections"); 
                clientSocket = serverSocket_.accept();
                clientSocket.setSoTimeout(timeout_);
                
                log_.debug("New connected estabilished");
                TCPWorkerThread workerThread = new TCPWorkerThread(clientSocket, dataHandler_);
                workerThreads_.add(workerThread);
                new Thread(workerThread).start();
            }
        }
        catch (IOException exception) 
        {
            if (!isTerminated_)
            {
                log_.error("I/O exception during communication", exception);
                try
                { 
                    closeSocket(); 
                } 
                catch (Exception socketException)
                {
                    log_.error("Unable to close the server socket", socketException);
                }
            }
        } 
    }    
    
    /**
     * Terminates the receiver.
     */
    public void terminate()
    {
        log_.debug("Terminating all worker threads");
        for (TCPWorkerThread worrkerThread : workerThreads_)
        {
            worrkerThread.terminate();
        }
        
        isTerminated_ = true;
        try 
        {
            closeSocket();
        } 
        catch (IOException exception) 
        {
            log_.error("I/O exception while closing the server socket", exception);
        }
    }
    
    /**
     * Closes the socket.
     * 
     * @throws IOException  The I/O exception
     */
    private synchronized void closeSocket() 
        throws IOException
    {
        if (serverSocket_ != null)
        {
            serverSocket_.close();
        }
    }
}
