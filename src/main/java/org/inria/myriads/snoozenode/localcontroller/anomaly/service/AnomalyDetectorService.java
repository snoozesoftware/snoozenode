package org.inria.myriads.snoozenode.localcontroller.anomaly.service;

import java.io.IOException;

import org.inria.myriads.snoozecommon.communication.localcontroller.LocalControllerDescription;
import org.inria.myriads.snoozecommon.guard.Guard;
import org.inria.myriads.snoozenode.configurator.anomaly.AnomalyDetectorSettings;
import org.inria.myriads.snoozenode.configurator.database.DatabaseSettings;
import org.inria.myriads.snoozenode.configurator.estimator.EstimatorSettings;
import org.inria.myriads.snoozenode.configurator.monitoring.HostMonitoringSettings;
import org.inria.myriads.snoozenode.database.api.LocalControllerRepository;
import org.inria.myriads.snoozenode.estimator.ResourceEstimatorFactory;
import org.inria.myriads.snoozenode.estimator.api.ResourceDemandEstimator;
import org.inria.myriads.snoozenode.exception.ResourceDemandEstimatorException;
import org.inria.myriads.snoozenode.localcontroller.anomaly.detector.AnomalyDetectorFactory;
import org.inria.myriads.snoozenode.localcontroller.anomaly.detector.api.AnomalyDetector;
import org.inria.myriads.snoozenode.localcontroller.anomaly.listener.AnomalyDetectorListener;
import org.inria.myriads.snoozenode.localcontroller.anomaly.runner.AnomalyDetectorRunner;
import org.inria.myriads.snoozenode.localcontroller.monitoring.enums.LocalControllerState;
import org.inria.myriads.snoozenode.localcontroller.monitoring.service.InfrastructureMonitoring;
import org.inria.myriads.snoozenode.localcontroller.monitoring.transport.LocalControllerDataTransporter;
import org.inria.myriads.snoozenode.monitoring.comunicator.api.MonitoringCommunicator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AnomalyDetectorService implements AnomalyDetectorListener
{
    /** Define the logger. */
    private static final Logger log_ = LoggerFactory.getLogger(AnomalyDetectorService.class);
    
    /** Local controller repository. */
    private LocalControllerRepository repository_;
    
    /** Local controller description. */
    private LocalControllerDescription localController_;

    /** Anomaly Detector Runner.*/
    private AnomalyDetectorRunner anomalyDetectorRunner_;

    /** Monitoring estimator.*/
    private ResourceDemandEstimator monitoringEstimator_;

    /** Anomaly detector.*/
    private AnomalyDetector anomalyDetector_;

    /** Communicator with upper level*/
    private MonitoringCommunicator communicator_;

    /** Virtual machine estimator settings*/
    private EstimatorSettings estimatorSettings_;

    /** host monitor monitoring settings.*/
    private HostMonitoringSettings hostMonitoringSettings_;

    private AnomalyDetectorSettings anomalyDetectorSettings_;
    
    /**
     * Constructor.
     * 
     * @param localController       The local controller description
     * @param repository            The local controller repository
     * @param monitoring            The infrastructure monitoring
     * @param databaseSettings      The database settings
     * @throws ResourceDemandEstimatorException 
     */
    public AnomalyDetectorService(
            LocalControllerDescription localController,
            LocalControllerRepository repository,
            DatabaseSettings databaseSettings,
            InfrastructureMonitoring monitoring,
            EstimatorSettings estimatorSettings,
            HostMonitoringSettings hostMonitoringSettings,
            AnomalyDetectorSettings anomalyDetectorSettings
            ) throws ResourceDemandEstimatorException
    {
        Guard.check(localController, repository);
        log_.debug("Initializing anomaly detector service");
        
        localController_ = localController;
        repository_ = repository;
        estimatorSettings_ = estimatorSettings;
        hostMonitoringSettings_ = hostMonitoringSettings;
        anomalyDetectorSettings_ = anomalyDetectorSettings;
        
        // two steps first 
        // (1) the metrics estimator 
        // (2) the detector logic (based on this estimator)
        //monitoringEstimator_ = new MonitoringEstimator(estimatorSettings_, hostMonitoringSettings_);
        monitoringEstimator_ = ResourceEstimatorFactory.newResourceDemandEstimator(
                estimatorSettings,
                monitoring.getMonitoringSettings(),
                hostMonitoringSettings);
        
        anomalyDetector_ = 
                AnomalyDetectorFactory.newAnomalyDetectorEstimator(
                        monitoringEstimator_,
                        localController_,
                        anomalyDetectorSettings_
                        );
    }

    /**
     * Starts the virtual machine monitor service.
     * 
     * @param groupManagerAddress      The group manager address
     * @throws Exception               The exception
     */
    public synchronized void startService(MonitoringCommunicator communicator) 
        throws Exception
    {
        log_.debug("Starting the anomaly detector service");
        communicator_ = communicator;
        startAnomalyDetectorRunner();
    }
    
    

    private void startAnomalyDetectorRunner()
    {
        log_.debug("Start Anomaly detector");
        anomalyDetectorRunner_ = new AnomalyDetectorRunner(repository_, anomalyDetector_, this);
        new Thread(anomalyDetectorRunner_, "AnomalyDetectorRunner").start(); 
    }

    /**
     * Stops the service.
     * 
     * @throws InterruptedException 
     */
    public void stopService() 
    {
        log_.debug("Stopping the anomaly detector service");
        anomalyDetectorRunner_.terminate();
    }
    
        
    /**
     * Anomaly detected.
     * 
     * 
     */
    @Override
    public synchronized void onAnomalyDetected(Object anomaly)
    {
        log_.error("onAnomalydDetected received");
        LocalControllerDataTransporter anomalyTransporter =
                new LocalControllerDataTransporter(localController_.getId());
        anomalyTransporter.setState(LocalControllerState.ANOMALY);
        anomalyTransporter.setAnomaly(anomaly);
        try
        {
            communicator_.sendAnomalyData(anomalyTransporter);
        }
        catch (IOException exception)
        {
            log_.debug("Unable to send anomaly data to group manager.");
            log_.debug(exception.getMessage());
            stopService();
        }
    }


}
