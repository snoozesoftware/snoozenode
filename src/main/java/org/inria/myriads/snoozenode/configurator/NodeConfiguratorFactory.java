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
package org.inria.myriads.snoozenode.configurator;

import java.io.IOException;

import org.inria.myriads.snoozenode.configurator.api.NodeConfigurator;
import org.inria.myriads.snoozenode.configurator.api.impl.JavaPropertyNodeConfigurator;
import org.inria.myriads.snoozenode.exception.NodeConfiguratorException;

/**
 * Node configurator factory.
 * 
 * @author Eugen Feller
 */
public final class NodeConfiguratorFactory 
{
    /** Hide constructor. */
    private NodeConfiguratorFactory()
    {
        throw new UnsupportedOperationException();
    }
    
    /**
     * Creates a new node configurator.
     * 
     * @param configurationFile             The configuration file
     * @return                              The node configurator
     * @throws NodeConfiguratorException    The node configuration exception
     * @throws IOException                  The I/O exception
     */
    public static NodeConfigurator newNodeConfigurator(String configurationFile) 
        throws NodeConfiguratorException, IOException
    {
        return new JavaPropertyNodeConfigurator(configurationFile);
    } 
}
