package org.inria.myriads.snoozenode.monitoring.connectionlistener;

import org.inria.myriads.snoozenode.monitoring.datasender.api.DataSender;

public interface ConnectionListener
{
    void onConnectionSuccesfull(DataSender dataSender);
}
