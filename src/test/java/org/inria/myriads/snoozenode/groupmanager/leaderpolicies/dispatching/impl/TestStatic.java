package org.inria.myriads.snoozenode.groupmanager.leaderpolicies.dispatching.impl;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.inria.myriads.snoozecommon.communication.groupmanager.GroupManagerDescription;
import org.inria.myriads.snoozecommon.communication.virtualcluster.VirtualMachineMetaData;
import org.inria.myriads.snoozecommon.communication.virtualcluster.submission.VirtualMachineLocation;
import org.inria.myriads.snoozenode.groupmanager.estimator.ResourceDemandEstimator;
import org.inria.myriads.snoozenode.groupmanager.leaderpolicies.dispatching.DispatchingPlan;
import org.inria.myriads.snoozenode.groupmanager.leaderpolicies.dispatching.DispatchingPolicy;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;





/**
 * 
 * Test static dispatching policy (at GL level).
 * 
 * @author msimonin
 *
 */
public class TestStatic extends TestCase
{

    /** The resource demand estimator (mock).*/
    private ResourceDemandEstimator estimator_;
    
    /** The static dispatch policy (under test).*/
    private DispatchingPolicy staticDispatch_;
    
      
    /** List of virtual machines.*/
    private List<VirtualMachineMetaData> virtualMachines_;
    
    /** List of group managers.*/
    private List<GroupManagerDescription> groupManagers_;
    
    
    
    /** 
     * Setup method.
     * Initializes the estimator.
     * 
     */
    public void setUp() 
    {
      estimator_ = EasyMock.createMock(ResourceDemandEstimator.class);
      staticDispatch_ = new Static(estimator_);
      virtualMachines_ = new ArrayList<VirtualMachineMetaData>();
      groupManagers_ = new ArrayList<GroupManagerDescription>();

    }
    
    /**
     * 
     * vm1 is bound to gm1.
     * 
     */
    public void testDispatchOneGmBound()
    {
        VirtualMachineMetaData vm1  = new VirtualMachineMetaData();
        GroupManagerDescription gm1 = new GroupManagerDescription();
        gm1.setId("1");
        VirtualMachineLocation location = new VirtualMachineLocation();
        location.setGroupManagerId("1");
        vm1.setVirtualMachineLocation(location);
        
        virtualMachines_.add(vm1);
        groupManagers_.add(gm1);  
        
        expect(estimator_.hasEnoughGroupManagerCapacity(vm1, gm1)).andReturn(true);
        replay(estimator_);
        DispatchingPlan dispatchPlan = staticDispatch_.dispatch(virtualMachines_, groupManagers_);
        List<GroupManagerDescription> groupManagersCandidates = dispatchPlan.getGroupManagers();
        
        //gm1 is assigned for this vm
        assertEquals(groupManagersCandidates.size(), 1);
        assertEquals(groupManagersCandidates.get(0), gm1);
        assertEquals(gm1.getVirtualMachines().size(), 1);
        assertEquals(gm1.getVirtualMachines().get(0), vm1);
    }
    
    
    /**
     * 
     * no vm to dispatch.
     * 
     */
    public void testDispatchNoVm()
    {
        VirtualMachineMetaData vm1  = new VirtualMachineMetaData();
        GroupManagerDescription gm1 = new GroupManagerDescription();
        gm1.setId("1");
        VirtualMachineLocation location = new VirtualMachineLocation();
        location.setGroupManagerId("1");
        vm1.setVirtualMachineLocation(location);
        
        groupManagers_.add(gm1);  
        
        expect(estimator_.hasEnoughGroupManagerCapacity(vm1, gm1)).andReturn(true);
        replay(estimator_);
        DispatchingPlan dispatchPlan = staticDispatch_.dispatch(virtualMachines_, groupManagers_);
        List<GroupManagerDescription> groupManagersCandidates = dispatchPlan.getGroupManagers();
        
        //gm1 is assigned for this vm
        assertEquals(groupManagersCandidates.size(), 0);
        assertEquals(gm1.getVirtualMachines().size(), 0);
    }
    
    
    /**
     * 
     * vm1 is bound but no gm exist in the repository.
     * 
     */
    public void testDispatchNoGm()
    {
        VirtualMachineMetaData vm1  = new VirtualMachineMetaData();
        VirtualMachineLocation location = new VirtualMachineLocation();
        location.setGroupManagerId("1");
        vm1.setVirtualMachineLocation(location);
        
        virtualMachines_.add(vm1);

        DispatchingPlan dispatchPlan = staticDispatch_.dispatch(virtualMachines_, groupManagers_);
        List<GroupManagerDescription> groupManagersCandidates = dispatchPlan.getGroupManagers();

        assertEquals(groupManagersCandidates.size(), 0);
    }
    
    /**
     * 
     * vm1 is bound but the binding doesn't exist in the repository.
     * 
     */
    public void testDispatchNoGmBound()
    {
        VirtualMachineMetaData vm1  = new VirtualMachineMetaData();
        GroupManagerDescription gm1 = new GroupManagerDescription();
        gm1.setId("1");
        VirtualMachineLocation location = new VirtualMachineLocation();
        location.setGroupManagerId("2");
        vm1.setVirtualMachineLocation(location);
        
        virtualMachines_.add(vm1);
        groupManagers_.add(gm1);  
        
        expect(estimator_.hasEnoughGroupManagerCapacity(vm1, gm1)).andReturn(true);
        replay(estimator_);
        DispatchingPlan dispatchPlan = staticDispatch_.dispatch(virtualMachines_, groupManagers_);
        List<GroupManagerDescription> groupManagersCandidates = dispatchPlan.getGroupManagers();
        
        //no gm assigned for this vm
        assertEquals(groupManagersCandidates.size(), 0);
        assertEquals(gm1.getVirtualMachines().size(), 0);
    }
    
    /**
     * 
     * vm is not bound to any gm.
     * 
     */
    public void testDispatchUnknownGmBound()
    {
        VirtualMachineMetaData vm1  = new VirtualMachineMetaData();
        GroupManagerDescription gm1 = new GroupManagerDescription();
        gm1.setId("1");
        VirtualMachineLocation location = new VirtualMachineLocation();
        vm1.setVirtualMachineLocation(location);
        
        virtualMachines_.add(vm1);
        groupManagers_.add(gm1);  
                
        expect(estimator_.hasEnoughGroupManagerCapacity(vm1, gm1)).andReturn(true);
        replay(estimator_);
        
        DispatchingPlan dispatchPlan = staticDispatch_.dispatch(virtualMachines_, groupManagers_);
        List<GroupManagerDescription> groupManagersCandidates = dispatchPlan.getGroupManagers();
        
        //no gm assigned for this vm
        assertEquals(groupManagersCandidates.size(), 0);
        assertEquals(gm1.getVirtualMachines().size(), 0);
    }
    
    /**
     * 
     * More comple submisstion request. 
     *  vm1 -> gm1 ; 
     *  vm2 -> gm4 ; 
     *  vm3 -> gm4 ;
     *  vm4 -> unassigned ;
     * 
     */
    public void testDispatchMultiple()
    {
        //repo contains 4 gms.
        GroupManagerDescription gm1 = new GroupManagerDescription();
        gm1.setId("1");
        groupManagers_.add(gm1);  
        GroupManagerDescription gm2 = new GroupManagerDescription();
        gm2.setId("2");
        groupManagers_.add(gm2);
        GroupManagerDescription gm3 = new GroupManagerDescription();
        gm3.setId("3");
        groupManagers_.add(gm3);
        GroupManagerDescription gm4 = new GroupManagerDescription();
        gm4.setId("4");
        groupManagers_.add(gm4);
        
        // request contains 4 vms.
        VirtualMachineMetaData vm1  = new VirtualMachineMetaData();
        VirtualMachineLocation location = new VirtualMachineLocation();
        location.setGroupManagerId("1");
        vm1.setVirtualMachineLocation(location);
        virtualMachines_.add(vm1);
        
        VirtualMachineMetaData vm2 = new VirtualMachineMetaData();
        VirtualMachineLocation location2 = new VirtualMachineLocation();
        location2.setGroupManagerId("4");
        vm2.setVirtualMachineLocation(location2);
        virtualMachines_.add(vm2);
        
        VirtualMachineMetaData vm3 = new VirtualMachineMetaData();
        VirtualMachineLocation location3 = new VirtualMachineLocation();
        location3.setGroupManagerId("4");
        vm3.setVirtualMachineLocation(location3);
        virtualMachines_.add(vm3);
        
        VirtualMachineMetaData vm4 = new VirtualMachineMetaData();
        VirtualMachineLocation location4 = new VirtualMachineLocation();
        vm4.setVirtualMachineLocation(location4);
        virtualMachines_.add(vm4);
        
        assertEquals(virtualMachines_.size(), 4);
        assertEquals(groupManagers_.size(), 4);
        
        expect(estimator_.hasEnoughGroupManagerCapacity(vm1, gm1)).andReturn(true);
        expect(estimator_.hasEnoughGroupManagerCapacity(vm2, gm4)).andReturn(true);
        expect(estimator_.hasEnoughGroupManagerCapacity(vm3, gm4)).andReturn(true);
        replay(estimator_);
        
        // vm1 -> gm1 ; 
        // vm2 -> gm4 ; 
        // vm3 -> gm4 ;
        // vm4 -> unassigned ;
        DispatchingPlan dispatchPlan = staticDispatch_.dispatch(virtualMachines_, groupManagers_);
        List<GroupManagerDescription> groupManagersCandidates = dispatchPlan.getGroupManagers();
        
        //no gm assigned for this vm
        assertEquals(2, groupManagersCandidates.size());
        //gm1 -> vm1
        assertEquals(1, gm1.getVirtualMachines().size());
        assertEquals(vm1, gm1.getVirtualMachines().get(0));
        
        //gm2 -> no vm
        assertEquals(0, gm2.getVirtualMachines().size());
        //gm3 -> no vm
        assertEquals(0, gm3.getVirtualMachines().size());
        
        //gm4 -> vm2 & vm3
        assertEquals(2, gm4.getVirtualMachines().size());
        assertEquals(vm2, gm4.getVirtualMachines().get(0));
        assertEquals(vm3, gm4.getVirtualMachines().get(1));
        
        
    }
}
