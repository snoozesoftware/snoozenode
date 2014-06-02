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
package org.inria.myriads.snoozenode.main.applications;

import org.inria.myriads.snoozenode.groupmanager.GroupManagerResource;
import org.restlet.Application;
import org.restlet.Context;
import org.restlet.Restlet;
import org.restlet.routing.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Group manager application.
 * 
 * @author Eugen Feller
 */
public final class GroupManagerApplication extends Application 
{    
    /** Define the logger. */
    private static final Logger log_ = LoggerFactory.getLogger(GroupManagerApplication.class);

    /**
     * Constructor.
     * 
     * @param parentContext     The parent context
     * @throws Exception        Exception 
     */
    public GroupManagerApplication(Context parentContext) 
        throws Exception 
    {
        super(parentContext);
        log_.debug("Starting the group manager application");
    }
    
    /**
     * Creates the inbound restlet.
     * 
     * @return  The restlet router
     */
    public Restlet createInboundRoot()
    {  
         Router router = new Router(getContext());  
         router.attach("/groupmanager", GroupManagerResource.class);
         return router;  
    }
}
