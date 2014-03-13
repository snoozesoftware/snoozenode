package org.inria.myriads.snoozenode.estimator.comparator;

import org.inria.myriads.snoozecommon.communication.groupmanager.GroupManagerDescription;
import org.inria.myriads.snoozecommon.communication.localcontroller.LocalControllerDescription;
import org.inria.myriads.snoozecommon.communication.virtualcluster.VirtualMachineMetaData;
import org.inria.myriads.snoozenode.configurator.estimator.EstimatorSettings;
import org.inria.myriads.snoozenode.estimator.api.ResourceDemandEstimator;
import org.inria.myriads.snoozenode.groupmanager.managerpolicies.comparators.api.SnoozeComparator;
import org.inria.myriads.snoozenode.groupmanager.managerpolicies.comparators.api.impl.GroupManagersL1;
import org.inria.myriads.snoozenode.groupmanager.managerpolicies.comparators.api.impl.LocalControllersL1;
import org.inria.myriads.snoozenode.groupmanager.managerpolicies.comparators.api.impl.VirtualMachinesL1;

/**
 * 
 * Comparator Factory.
 * 
 * @author msimonin
 *
 */
public final class ComparatorFactory
{
    /** Hide constructor. */
    private ComparatorFactory()
    {
        throw new UnsupportedOperationException();
    }

    /**
     * 
     * Creates a new virtual machine comparator.
     * 
     * @param estimatorSettings     Estimator Settings
     * @param estimator             The estimator.
     * @param decreasing            True iff decreasing order.
     * @return  A comparator.
     */
    public static SnoozeComparator<VirtualMachineMetaData> newVirtualMachinesComparator(
            EstimatorSettings estimatorSettings, 
            ResourceDemandEstimator estimator,
            boolean decreasing)
    {
        SnoozeComparator<VirtualMachineMetaData> comparator = new VirtualMachinesL1();
        comparator.setDecreasing(decreasing);
        comparator.setEstimator(estimator);
        comparator.initialize();
        return comparator;
    }

    /**
     * 
     * Creates a new local controller comparator.
     * 
     * @param estimatorSettings     Estimator Settings
     * @param estimator             The estimator
     * @param decreasing            True iff decreasing order.
     * @return  A comparator.
     */
    public static SnoozeComparator<LocalControllerDescription> newLocalControllersComparator(
            EstimatorSettings estimatorSettings,
            ResourceDemandEstimator estimator,
            boolean decreasing)
    {
        SnoozeComparator<LocalControllerDescription> comparator = new LocalControllersL1();
        comparator.setDecreasing(decreasing);
        comparator.setEstimator(estimator);
        comparator.initialize();
        return comparator;
    }

    /**
     * 
     * Creates a group manager comparator.
     * 
     * @param estimatorSettings     Estimator Settings.
     * @param estimator             The estimator.
     * @param decreasing            True iff decreasing.
     * @return  A comparator.
     */
    public static SnoozeComparator<GroupManagerDescription> newGroupManagersComparator(
            EstimatorSettings estimatorSettings,
            ResourceDemandEstimator estimator,
            boolean decreasing)
    {
        SnoozeComparator<GroupManagerDescription> comparator = new GroupManagersL1();
        comparator.setDecreasing(decreasing);
        comparator.setEstimator(estimator);
        comparator.initialize();
        return comparator;
    }
}
