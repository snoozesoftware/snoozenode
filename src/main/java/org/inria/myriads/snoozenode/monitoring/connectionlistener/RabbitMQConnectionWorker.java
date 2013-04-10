package org.inria.myriads.snoozenode.monitoring.connectionlistener;

import org.inria.myriads.snoozenode.configurator.monitoring.external.MonitoringExternalSettings;
import org.inria.myriads.snoozenode.monitoring.datasender.api.DataSender;
import org.inria.myriads.snoozenode.monitoring.datasender.api.impl.RabbitMQDataSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author msimonin
 *
 */
public class RabbitMQConnectionWorker extends Thread
{
    /** Define the logger. */
    private static final Logger log_ = LoggerFactory.getLogger(RabbitMQConnectionWorker.class);

    private boolean isRunning_ ;
    
    private boolean isTerminated_;
    
    /** Lock object.*/
    private Object lockObject_; 
    
    /** Listener.*/
    private ConnectionListener listener_;
    
    /** Interval Retry.*/
    private int intervalRetry_;
    
    /** Exchange name.*/
    private String exchangeName_;

    
    private MonitoringExternalSettings monitoringExternalSettings_;
    /**
     * @param listener
     * @param intervalRetry
     * @param exchangeName
     */
    public RabbitMQConnectionWorker(ConnectionListener listener, 
            int intervalRetry, 
            String exchangeName,
            MonitoringExternalSettings monitoringExternalSettings
            )
    {
        log_.debug("Initialize the connection worker to the queue");
        listener_ = listener;
        intervalRetry_ = intervalRetry;
        exchangeName_ = exchangeName;
        lockObject_ = new Object();
        isTerminated_ = false;
        monitoringExternalSettings_ = monitoringExternalSettings;
    }

    @Override
    public void run()
    {
        isRunning_=true;
        int waitTime = intervalRetry_;
        while (!isTerminated_)
        {        
            try 
            {

                if (!isRunning_)
                {
                    //wakeUp
                    log_.debug("Start periodic retry");
                    waitTime=intervalRetry_;
                    setRunning();
                }
                try{
                    log_.debug("Try to reconnect to the rabbit mq service");
                    log_.debug("host " + monitoringExternalSettings_.getAddress().getAddress() );
                    log_.debug("username " + monitoringExternalSettings_.getUsername() );
                    log_.debug("password " + monitoringExternalSettings_.getPassword() );
                    log_.debug("vhost " + monitoringExternalSettings_.getVhost() );
                    DataSender dataSender = new RabbitMQDataSender(exchangeName_, monitoringExternalSettings_);
                    listener_.onConnectionSuccesfull(dataSender);
                    isRunning_ = false;
                    waitTime = 0;
                }
                catch(Exception exception)
                {
                    log_.debug(String.format("Failed to connect... retry in %d seconds", intervalRetry_));
                    exception.printStackTrace();
                }
                
                synchronized (lockObject_)
                {
                    log_.debug("Sleeping for " + waitTime);
                    lockObject_.wait(waitTime);
                    log_.debug("Restart new connection loop");
                }   

            }
            catch (InterruptedException e) 
            {
                e.printStackTrace();
            }
        }
 
    }
    
    /**
     * @return the isRunning
     */
    public boolean isRunning()
    {
        return isRunning_;
    }

    /**
     * @param isRunning the isRunning to set
     */
    public void setRunning()
    {
        isRunning_ = true;
    }

    public void restart()
    {
        synchronized (lockObject_)
        {
            lockObject_.notify();
            log_.debug("Restarting the connection thread");
        }

    }
    /** 
     * Terminating the thread.
     */
    public synchronized void terminate() 
    {
        log_.debug("Terminating the connection thread");
        synchronized (lockObject_)
        {
            isTerminated_ = true;
            lockObject_.notify();
        }
    }
    


}
