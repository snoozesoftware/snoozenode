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
package org.inria.myriads.snoozenode.groupmanager.statemachine;

/**
 * Virtual machine commands.
 * 
 * @author Eugen Feller
 */
public enum VirtualMachineCommand 
{
    /** Suspend. */
    SUSPEND("suspend"),
    /** Resume. */
    RESUME("resume"),
    /** Shutdown. */
    SHUTDOWN("shutdown"),
    /** Reboot. */
    REBOOT("reboot"),
    /** Destroy. */
    DESTROY("destroy"),
    /** Migrate. */
    MIGRATE("migrate"),
    /** Resize. */
    RESIZE("resize");

    /** name.*/
    private String name_ = "";
    
    /**
     * @param name  The name.
     */
    VirtualMachineCommand(String name)
    {
        name_ = name;
    }
    
    @Override
    public String toString()
    {
        return name_;
    }
    
    
    /**
     * 
     * Convert from string.
     * 
     * @param text      The strings text to convert.
     * @return  VirtualMachineCommand.
     */
    public static VirtualMachineCommand fromString(String text) 
    {
        if (text != null) 
        {
          for (VirtualMachineCommand v : VirtualMachineCommand.values()) 
          {
            if (text.equalsIgnoreCase(v.toString())) 
            {
              return v;
            }
          }
        }
        throw new IllegalArgumentException("No matching enum found");
      }
}
