package org.inria.myriads.snoozenode.groupmanager.leaderpolicies.dispatching.impl;

import java.util.ArrayList;
import java.util.List;

import org.easymock.EasyMock;
import org.inria.myriads.snoozecommon.communication.groupmanager.GroupManagerDescription;
import org.inria.myriads.snoozecommon.communication.virtualcluster.VirtualMachineMetaData;
import org.inria.myriads.snoozecommon.communication.virtualcluster.submission.VirtualMachineGroupManagerLocation;
import org.inria.myriads.snoozecommon.globals.Globals;
import org.inria.myriads.snoozenode.groupmanager.estimator.ResourceDemandEstimator;
import org.inria.myriads.snoozenode.groupmanager.leaderpolicies.dispatching.DispatchingPlan;
import org.inria.myriads.snoozenode.groupmanager.leaderpolicies.dispatching.DispatchingPolicy;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import junit.framework.TestCase;

/**
 * @author msimonin
 *
 */
/**
 * @author msimonin
 *
 */
public class TestStatic extends TestCase
{

    /** The resource demand estimator (mock).*/
    private ResourceDemandEstimator estimator_;
    
    /** The static dispatch policy (under test).*/
    DispatchingPolicy staticDispatch_;
    
      
    /** List of virtual machines.*/
    private List<VirtualMachineMetaData> virtualMachines_;
    
    /** List of group managers.*/
    List<GroupManagerDescription> groupManagers_;
    
    
    
    /** 
     * Setup method.
     * Initializes the estimator.
     * 
     * 
     */
    public void setUp() throws Exception 
    {
      estimator_ = EasyMock.createMock(ResourceDemandEstimator.class);
      staticDispatch_ = new Static(estimator_);
      virtualMachines_ = new ArrayList<VirtualMachineMetaData>();
      groupManagers_ = new ArrayList<GroupManagerDescription>();

    }
    
    public void testDispatchOneGmBound()
    {
        VirtualMachineMetaData vm1_  = new VirtualMachineMetaData();
        GroupManagerDescription gm1_ = new GroupManagerDescription();
        gm1_.setId("1");
        VirtualMachineGroupManagerLocation groupManagerLocation = new VirtualMachineGroupManagerLocation();
        groupManagerLocation.setGroupManagerId("1");
        vm1_.setGroupManagerLocation(groupManagerLocation);
        
        virtualMachines_.add(vm1_);
        groupManagers_.add(gm1_);  
        
        expect(estimator_.hasEnoughGroupManagerCapacity(vm1_, gm1_)).andReturn(true);
        replay(estimator_);
        DispatchingPlan dispatchPlan = staticDispatch_.dispatch(virtualMachines_, groupManagers_);
        List<GroupManagerDescription> groupManagersCandidates = dispatchPlan.getGroupManagers();
        
        //gm1 is assigned for this vm
        assertEquals(groupManagersCandidates.size(),1);
        assertEquals(groupManagersCandidates.get(0), gm1_);
        assertEquals(gm1_.getVirtualMachines().size(), 1);
        assertEquals(gm1_.getVirtualMachines().get(0), vm1_);
    }
    
    
    public void testDispatchNoVm()
    {
        VirtualMachineMetaData vm1_  = new VirtualMachineMetaData();
        GroupManagerDescription gm1_ = new GroupManagerDescription();
        gm1_.setId("1");
        VirtualMachineGroupManagerLocation groupManagerLocation = new VirtualMachineGroupManagerLocation();
        groupManagerLocation.setGroupManagerId("1");
        vm1_.setGroupManagerLocation(groupManagerLocation);
        
        groupManagers_.add(gm1_);  
        
        expect(estimator_.hasEnoughGroupManagerCapacity(vm1_, gm1_)).andReturn(true);
        replay(estimator_);
        DispatchingPlan dispatchPlan = staticDispatch_.dispatch(virtualMachines_, groupManagers_);
        List<GroupManagerDescription> groupManagersCandidates = dispatchPlan.getGroupManagers();
        
        //gm1 is assigned for this vm
        assertEquals(groupManagersCandidates.size(),0);
        assertEquals(gm1_.getVirtualMachines().size(), 0);
    }
    
    
    public void testDispatchNoGm()
    {
        VirtualMachineMetaData vm1_  = new VirtualMachineMetaData();
        GroupManagerDescription gm1_ = new GroupManagerDescription();
        gm1_.setId("1");
        VirtualMachineGroupManagerLocation groupManagerLocation = new VirtualMachineGroupManagerLocation();
        groupManagerLocation.setGroupManagerId("1");
        vm1_.setGroupManagerLocation(groupManagerLocation);
        
        virtualMachines_.add(vm1_);
        
        expect(estimator_.hasEnoughGroupManagerCapacity(vm1_, gm1_)).andReturn(true);
        replay(estimator_);
        DispatchingPlan dispatchPlan = staticDispatch_.dispatch(virtualMachines_, groupManagers_);
        List<GroupManagerDescription> groupManagersCandidates = dispatchPlan.getGroupManagers();
        
        //gm1 is assigned for this vm
        assertEquals(groupManagersCandidates.size(),0);
        assertEquals(gm1_.getVirtualMachines().size(), 0);
    }
    
    public void testDispatchNoGmBound()
    {
        VirtualMachineMetaData vm1_  = new VirtualMachineMetaData();
        GroupManagerDescription gm1_ = new GroupManagerDescription();
        gm1_.setId("1");
        VirtualMachineGroupManagerLocation groupManagerLocation = new VirtualMachineGroupManagerLocation();
        groupManagerLocation.setGroupManagerId("2");
        vm1_.setGroupManagerLocation(groupManagerLocation);
        
        virtualMachines_.add(vm1_);
        groupManagers_.add(gm1_);  
        
        expect(estimator_.hasEnoughGroupManagerCapacity(vm1_, gm1_)).andReturn(true);
        replay(estimator_);
        DispatchingPlan dispatchPlan = staticDispatch_.dispatch(virtualMachines_, groupManagers_);
        List<GroupManagerDescription> groupManagersCandidates = dispatchPlan.getGroupManagers();
        
        //no gm assigned for this vm
        assertEquals(groupManagersCandidates.size(),0);
        assertEquals(gm1_.getVirtualMachines().size(), 0);
    }
    
    public void testDispatchUnknownGmBound()
    {
        VirtualMachineMetaData vm1_  = new VirtualMachineMetaData();
        GroupManagerDescription gm1_ = new GroupManagerDescription();
        gm1_.setId("1");
        VirtualMachineGroupManagerLocation groupManagerLocation = new VirtualMachineGroupManagerLocation();
        groupManagerLocation.setGroupManagerId(Globals.DEFAULT_INITIALIZATION);
        vm1_.setGroupManagerLocation(groupManagerLocation);
        
        virtualMachines_.add(vm1_);
        groupManagers_.add(gm1_);  
                
        expect(estimator_.hasEnoughGroupManagerCapacity(vm1_, gm1_)).andReturn(true);
        replay(estimator_);
        
        DispatchingPlan dispatchPlan = staticDispatch_.dispatch(virtualMachines_, groupManagers_);
        List<GroupManagerDescription> groupManagersCandidates = dispatchPlan.getGroupManagers();
        
        //no gm assigned for this vm
        assertEquals(groupManagersCandidates.size(),0);
        assertEquals(gm1_.getVirtualMachines().size(), 0);
    }
    
    public void testDispatchMultiple()
    {
        VirtualMachineMetaData vm1_  = new VirtualMachineMetaData();
        GroupManagerDescription gm1_ = new GroupManagerDescription();
        gm1_.setId("1");
        groupManagers_.add(gm1_);  
        GroupManagerDescription gm2_ = new GroupManagerDescription();
        gm2_.setId("2");
        groupManagers_.add(gm2_);
        GroupManagerDescription gm3_ = new GroupManagerDescription();
        gm3_.setId("3");
        groupManagers_.add(gm3_);
        GroupManagerDescription gm4_ = new GroupManagerDescription();
        gm4_.setId("4");
        groupManagers_.add(gm4_);
        
        
        VirtualMachineGroupManagerLocation groupManagerLocation = new VirtualMachineGroupManagerLocation();
        groupManagerLocation.setGroupManagerId("1");
        vm1_.setGroupManagerLocation(groupManagerLocation);
        virtualMachines_.add(vm1_);
        
        VirtualMachineMetaData vm2_ = new VirtualMachineMetaData();
        VirtualMachineGroupManagerLocation groupManagerLocation2 = new VirtualMachineGroupManagerLocation();
        groupManagerLocation2.setGroupManagerId("4");
        vm2_.setGroupManagerLocation(groupManagerLocation2);
        virtualMachines_.add(vm2_);
        
        VirtualMachineMetaData vm3_ = new VirtualMachineMetaData();
        VirtualMachineGroupManagerLocation groupManagerLocation3 = new VirtualMachineGroupManagerLocation();
        groupManagerLocation3.setGroupManagerId("4");
        vm3_.setGroupManagerLocation(groupManagerLocation3);
        virtualMachines_.add(vm3_);
        
        VirtualMachineMetaData vm4_ = new VirtualMachineMetaData();
        VirtualMachineGroupManagerLocation groupManagerLocation4 = new VirtualMachineGroupManagerLocation();
        vm4_.setGroupManagerLocation(groupManagerLocation4);
        virtualMachines_.add(vm4_);
        
        assertEquals(virtualMachines_.size(),4);
        assertEquals(groupManagers_.size(),4);
        
        expect(estimator_.hasEnoughGroupManagerCapacity(vm1_, gm1_)).andReturn(true);
        expect(estimator_.hasEnoughGroupManagerCapacity(vm2_, gm4_)).andReturn(true);
        expect(estimator_.hasEnoughGroupManagerCapacity(vm3_, gm4_)).andReturn(true);
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
        assertEquals(1, gm1_.getVirtualMachines().size());
        assertEquals(vm1_, gm1_.getVirtualMachines().get(0));
        
        //gm2 -> no vm
        assertEquals(0, gm2_.getVirtualMachines().size());
        //gm3 -> no vm
        assertEquals(0, gm3_.getVirtualMachines().size());
        
        //gm4 -> vm2 & vm3
        assertEquals(2, gm4_.getVirtualMachines().size());
        assertEquals(vm2_, gm4_.getVirtualMachines().get(0));
        assertEquals(vm3_, gm4_.getVirtualMachines().get(1));
        
        
    }
}
