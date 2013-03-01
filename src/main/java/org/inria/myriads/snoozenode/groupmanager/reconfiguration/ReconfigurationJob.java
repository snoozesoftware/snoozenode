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
package org.inria.myriads.snoozenode.groupmanager.reconfiguration;

import org.inria.myriads.snoozecommon.guard.Guard;
import org.inria.myriads.snoozenode.groupmanager.statemachine.api.StateMachine;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reconfiguration job.
 * 
 * @author Eugen Feller
 */
public final class ReconfigurationJob 
    implements Job
{
    /** Define the logger. */
    private static final Logger log_ = LoggerFactory.getLogger(ReconfigurationJob.class);
    
    /**
     * Execute routine called by the scheduler.
     * 
     * @param context      The job execution context
     */
    public void execute(JobExecutionContext context) 
    {
        Guard.check(context);
        log_.debug("Starting the data center reconfiguration procedure");
        
        StateMachine stateMachine = (StateMachine) context.getJobDetail().getJobDataMap().get("stateMachine");
        stateMachine.startReconfiguration();
    }
}
