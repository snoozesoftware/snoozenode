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
import java.io.ObjectInputStream;

import java.net.Socket;

import org.apache.commons.io.IOUtils;

import org.inria.myriads.snoozecommon.guard.Guard;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Worker thread.
 * 
 * @author Eugen Feller
 */
public final class TCPWorkerThread 
    implements Runnable 
{
    /** Define the logger. */
    private static final Logger log_ = LoggerFactory.getLogger(TCPWorkerThread.class);
    
    /** Object input stream. */
    private ObjectInputStream inputStream_;
    
    /** Client socket. */
    private Socket clientSocket_;
    
    /** Data receiver. */
    private DataListener dataHandler_;

    /** Identifier (host address + port). */
    private String id_;

    /** Signals termination. */
    private boolean isTerminated_;
    
    /**
     * TCP worker thread constructor.
     * 
     * @param clientSocket  The client socket
     * @param dataListener  The data listener
     */
    public TCPWorkerThread(Socket clientSocket, DataListener dataListener)
    {
        Guard.check(clientSocket, dataListener);
        log_.debug("Initializing worker thread");
        
        clientSocket_ = clientSocket;
        dataHandler_ = dataListener;
        isTerminated_ = false;
        id_ = clientSocket_.getInetAddress().getHostAddress() + ":" + clientSocket_.getPort();
    }
    
    /**
     * The run() method.
     */
    public void run() 
    {
        try 
        {
            inputStream_ = new ObjectInputStream(clientSocket_.getInputStream());
            while (!isTerminated_) 
            {
                Object data = inputStream_.readObject();
                log_.debug("Data received");
                dataHandler_.onDataArrival(data, id_);
            }
        } 
        catch (IOException exception)
        {
            if (!isTerminated_)
            {
                log_.debug("I/O exception during read! Treating it as failure!");
                log_.debug(exception.getMessage());
                dataHandler_.onFailure(id_);
            }      
        }
        catch (ClassNotFoundException exception) 
        {
            log_.error("Class not found exception", exception);
        }
        finally
        {
            close();
        }
        
        log_.debug(String.format("Worker thread %s is stopped!", id_));
    }
    
    /**
     * Terminates the thread.
     */
    protected void terminate()
    {
        log_.debug(String.format("Terminating worker thread %s", id_));
        isTerminated_ = true; 
        close();
    }
    
    /**
     * Closes all the communication streams.
     */
    private void close()
    {
        log_.debug("Closing the socket and input stream");
        
        if (clientSocket_ != null)
        {
            IOUtils.closeQuietly(clientSocket_);
        }
        
        if (inputStream_ != null)
        {
            IOUtils.closeQuietly(inputStream_);
        }
    }
}
