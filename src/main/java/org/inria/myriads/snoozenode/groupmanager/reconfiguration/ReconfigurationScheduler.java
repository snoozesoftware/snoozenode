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
package org.inria.myriads.snoozenode.groupmanager.reconfiguration;

import java.io.IOException;
import java.text.ParseException;

import org.inria.myriads.snoozecommon.guard.Guard;
import org.inria.myriads.snoozenode.groupmanager.statemachine.api.StateMachine;
import org.quartz.CronTrigger;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

/**
 * Reconfigutation loop implementation based on the Quartz scheduler.
 * 
 * @author Eugen Feller
 */
public final class ReconfigurationScheduler
{
    /** Define the logger. */
    private static final Logger log_ = LoggerFactory.getLogger(ReconfigurationScheduler.class);
    
    /** Scheduler object. */
    private Scheduler scheduler_;
    
    /** Group manager logic. */
    private StateMachine stateMachine_;
    
    /** Cron expression. */
    private String cronExpression_;
    
    /**
     * Constructor.
     * 
     * @param stateMachine        The group manager state machine
     * @param cronExpression      The cron expression
     */
    public ReconfigurationScheduler(StateMachine stateMachine, String cronExpression)
    {
        Guard.check(stateMachine, cronExpression);
        log_.debug("Initializing the reconfiguration scheduler");
        
        stateMachine_ = stateMachine;
        cronExpression_ = cronExpression;
    }
    
    /**
     * Start the reconfiguration loop.
     * 
     * @throws SchedulerException   The scheduler exception
     * @throws ParseException       The parser exception
     */
    public void run() 
        throws SchedulerException, ParseException 
    {
        SchedulerFactory schedulerFactory = new StdSchedulerFactory();
        scheduler_ = schedulerFactory.getScheduler();
       
        JobDetail job = newJob(ReconfigurationJob.class).withIdentity("reconfiguration_loop_trigger").build();      
        job.getJobDataMap().put("stateMachine", stateMachine_);
                        
        CronTrigger trigger = newTrigger().withIdentity("reconfiguration_loop_trigger", "group1")
                                                        .withSchedule(cronSchedule(cronExpression_))
                                                        .build();

        scheduler_.scheduleJob(job, trigger);
        scheduler_.start();
    }
    
    /** 
     * Shutdown the reconfiguration loop. 
     * 
     * @throws SchedulerException   The scheduler exception
     * @throws IOException          The I/O exception
     */
    public void shutdown() 
        throws SchedulerException, IOException 
    {
        if (scheduler_ != null)
        {
            scheduler_.shutdown();
        }
    }
}
