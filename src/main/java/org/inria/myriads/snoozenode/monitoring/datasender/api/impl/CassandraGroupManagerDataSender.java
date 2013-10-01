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
package org.inria.myriads.snoozenode.monitoring.datasender.api.impl;

import java.io.IOException;

import org.inria.myriads.snoozenode.configurator.database.DatabaseSettings;
import org.inria.myriads.snoozenode.database.api.impl.cassandra.CassandraRepository;
import org.inria.myriads.snoozenode.groupmanager.monitoring.transport.GroupManagerDataTransporter;
import org.inria.myriads.snoozenode.monitoring.datasender.api.DataSender;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cassandra data sender.
 * 
 * @author msimonin
 */
public class CassandraGroupManagerDataSender extends CassandraRepository implements DataSender 
{    
    /** Define the logger. */
    private static final Logger log_ = LoggerFactory.getLogger(CassandraGroupManagerDataSender.class);
    
    
    
    
    /**
     * 
     * Cassandra direct sender constructor.
     * 
     * @param databaseSettings  The database settings.
     */
    public CassandraGroupManagerDataSender(DatabaseSettings databaseSettings)
    {
        super(
                databaseSettings.getCassandraSettings().getHosts(),
                databaseSettings.getNumberOfEntriesPerGroupManager(),
                databaseSettings.getNumberOfEntriesPerVirtualMachine()
                );
        log_.debug("Cassandra Data Sender Initialized...");
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
       try
       {
           GroupManagerDataTransporter dataTransporter = (GroupManagerDataTransporter) data;
           this.addGroupManagerSummaryInformationCassandra(
                   dataTransporter.getId(),
                   dataTransporter.getSummary()
                   );
       }
       catch (Exception exception)
       {
           log_.error("Cast error before sending it to the cassandra repository");
           throw new IOException();
       }
    }
    
    /**
     * Closes the sender.
     */
    public void close() 
    {
        log_.debug("Closing the socket and output stream");
       
    }

    @Override
    public void send(Object data, String senderId) throws IOException
    {
        send(data);
        
    }
}
