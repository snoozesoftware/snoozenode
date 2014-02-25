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
package org.inria.myriads.snoozenode.configurator.api;

import java.io.Serializable;

import org.inria.myriads.snoozecommon.communication.localcontroller.hypervisor.HypervisorSettings;
import org.inria.myriads.snoozenode.configurator.anomaly.AnomalyDetectorSettings;
import org.inria.myriads.snoozenode.configurator.anomaly.AnomalyResolverSettings;
import org.inria.myriads.snoozenode.configurator.database.DatabaseSettings;
import org.inria.myriads.snoozenode.configurator.energymanagement.EnergyManagementSettings;
import org.inria.myriads.snoozenode.configurator.estimator.EstimatorSettings;
import org.inria.myriads.snoozenode.configurator.faulttolerance.FaultToleranceSettings;
import org.inria.myriads.snoozenode.configurator.httpd.HTTPdSettings;
import org.inria.myriads.snoozenode.configurator.imagerepository.ImageRepositorySettings;
import org.inria.myriads.snoozenode.configurator.monitoring.HostMonitoringSettings;
import org.inria.myriads.snoozenode.configurator.monitoring.MonitoringSettings;
import org.inria.myriads.snoozenode.configurator.monitoring.external.ExternalNotifierSettings;
import org.inria.myriads.snoozenode.configurator.networking.NetworkingSettings;
import org.inria.myriads.snoozenode.configurator.node.NodeSettings;
import org.inria.myriads.snoozenode.configurator.provisioner.ProvisionerSettings;
import org.inria.myriads.snoozenode.configurator.scheduler.GroupLeaderSchedulerSettings;
import org.inria.myriads.snoozenode.configurator.scheduler.GroupManagerSchedulerSettings;
import org.inria.myriads.snoozenode.configurator.submission.SubmissionSettings;

/**
 * Node parameters class.
 * 
 * @author Eugen Feller
 */
/**
 * @author msimonin
 *
 */
/**
 * @author msimonin
 *
 */
public class NodeConfiguration 
    implements Serializable
{
    /** Default serial version. */
    private static final long serialVersionUID = 1L;
    
    /** General settings. */
    private NodeSettings node_;
    
    /** HTTPd settings. */
    private HTTPdSettings httpd_;
    
    /** Fault tolerance settings. */
    private FaultToleranceSettings faultTolerance_;
    
    /** Hypervisor settings. */
    private HypervisorSettings hypervisor_;
    
    /** Database settings. */
    private DatabaseSettings database_;
    
    /** Estimator settings. */
    private EstimatorSettings estimator_;
    
    /** Group leader scheduler settings. */
    private GroupLeaderSchedulerSettings groupLeaderScheduler_;
    
    /** Group manager scheduler settings. */
    private GroupManagerSchedulerSettings groupManagerScheduler_;
    
    /** Submission settings. */
    private SubmissionSettings submission_;
    
    /** Monitoring settings. */
    private MonitoringSettings monitoring_;
    
    /** Monitoring settings. */
    private ExternalNotifierSettings externalNotifier_;

    /** Energy management settings. */
    private EnergyManagementSettings energyManagement_;

    /** Networking settings. */
    private NetworkingSettings networking_;
    
    /** Image Repository settings.*/
    private ImageRepositorySettings imageRepositorySettings_;
    
    /** Provisioner settings.*/
    private ProvisionerSettings provisionerSettings_;
    
    /** HostMonitoring settings.*/
    private HostMonitoringSettings hostMonitoringSettings_;
    
    /** anomaly detector settings.*/
    private AnomalyDetectorSettings  anomalyDetectorSettings_;
    
    /** anomaly resolver settings.*/
    private AnomalyResolverSettings anomalyResolverSettings_;
    
    /** Empty constructor. */
    public NodeConfiguration()
    {
        node_ = new NodeSettings();
        httpd_ = new HTTPdSettings();
        faultTolerance_ = new FaultToleranceSettings();
        hypervisor_ = new HypervisorSettings();
        database_ = new DatabaseSettings();
        estimator_ = new EstimatorSettings();
        groupLeaderScheduler_ = new GroupLeaderSchedulerSettings();
        groupManagerScheduler_ = new GroupManagerSchedulerSettings();
        submission_ = new SubmissionSettings();
        monitoring_ = new MonitoringSettings();
        externalNotifier_ = new ExternalNotifierSettings();
        energyManagement_ = new EnergyManagementSettings();
        networking_ = new NetworkingSettings();
        imageRepositorySettings_ = new ImageRepositorySettings();
        provisionerSettings_ = new ProvisionerSettings();
        hostMonitoringSettings_ = new HostMonitoringSettings();
        anomalyDetectorSettings_ = new AnomalyDetectorSettings();
        anomalyResolverSettings_ = new AnomalyResolverSettings();
    }

    /**
     * Returns the node settings.
     * 
     * @return  The node settings
     */
    public NodeSettings getNode() 
    {
        return node_;
    }

    /**
     * Returns the fault tolerance settings.
     * 
     * @return  The fault tolerance settings
     */
    public FaultToleranceSettings getFaultTolerance() 
    {
        return faultTolerance_;
    }

    /**
     * Returns the HTTPd settings.
     * 
     * @return  The httpd settings
     */
    public HTTPdSettings getHTTPd() 
    {
        return httpd_;
    }
    
    /**
     * Returns the hypervisor settings.
     * 
     * @return  The hypervisor settings
     */
    public HypervisorSettings getHypervisor() 
    {
        return hypervisor_;
    }

    /**
     * Returns the estimator settings.
     * 
     * @return  The estimator settings
     */
    public EstimatorSettings getEstimator() 
    {
        return estimator_;
    }

    /**
     * Returns the group leader scheduler settings.
     * 
     * @return  The group leader scheduler settings
     */
    public GroupLeaderSchedulerSettings getGroupLeaderScheduler() 
    {
        return groupLeaderScheduler_;
    }
    
    /**
     * Returns the group manager scheduler settings.
     * 
     * @return  The group manager scheduler settings
     */
    public GroupManagerSchedulerSettings getGroupManagerScheduler() 
    {
        return groupManagerScheduler_;
    }

    /**
     * Returns the monitoring settings.
     * 
     * @return  The monitoring settings
     */
    public MonitoringSettings getMonitoring() 
    {
        return monitoring_;
    }

    /**
     * Returns the energy settings.
     * 
     * @return  The energy settings
     */
    public EnergyManagementSettings getEnergyManagement() 
    {
        return energyManagement_;
    }

    /**
     * Returns the networking settings.
     * 
     * @return  The networking settings
     */
    public NetworkingSettings getNetworking() 
    {
        return networking_;
    }
    
    /**
     * Returns the database settings.
     * 
     * @return  The database settings
     */
    public DatabaseSettings getDatabase()
    {
        return database_;
    }

    /**
     * Returns the submission settings.
     * 
     * @return  The submission settings
     */
    public SubmissionSettings getSubmission() 
    {
        return submission_;
    }

    /**
     * @return the monitoringExternal
     */
    public ExternalNotifierSettings getExternalNotifier()
    {
        return externalNotifier_;
    }

    /**
     * @return the imageRepositorySettings
     */
    public ImageRepositorySettings getImageRepositorySettings()
    {
        return imageRepositorySettings_;
    }

    /**
     * @return the provisionerSettings
     */
    public ProvisionerSettings getProvisionerSettings()
    {
        return provisionerSettings_;
    }

    /**
     * @param provisionerSettings the provisionerSettings to set
     */
    public void setProvisionerSettings(ProvisionerSettings provisionerSettings)
    {
        provisionerSettings_ = provisionerSettings;
    }

    /**
     * @return the serialversionuid
     */
    public static long getSerialversionuid()
    {
        return serialVersionUID;
    }

    /**
     * @return the httpd
     */
    public HTTPdSettings getHttpd()
    {
        return httpd_;
    }

    /**
     * @return the hostMonitoringSettings
     */
    public HostMonitoringSettings getHostMonitoringSettings()
    {
        return hostMonitoringSettings_;
    }

    /**
     * @return the anomalyDetectorSettings
     */
    public AnomalyDetectorSettings getAnomalyDetectorSettings()
    {
        return anomalyDetectorSettings_;
    }

    /**
     * @return the anomalyResolverSettings
     */
    public AnomalyResolverSettings getAnomalyResolverSettings()
    {
        return anomalyResolverSettings_;
    }
}
