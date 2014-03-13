import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.easymock.EasyMock;
import org.inria.myriads.snoozecommon.communication.localcontroller.LocalControllerDescription;
import org.inria.myriads.snoozenode.estimator.api.ResourceDemandEstimator;
import org.inria.myriads.snoozenode.estimator.api.impl.StaticDynamicResourceDemandEstimator;
import org.inria.myriads.snoozenode.groupmanager.managerpolicies.comparators.api.SnoozeComparator;
import org.inria.myriads.snoozenode.groupmanager.managerpolicies.comparators.api.impl.LocalControllersL1;

import com.google.common.primitives.Doubles;

import junit.framework.TestCase;


/**
 * @author msimonin
 *
 */
public class TestSnoozeComparator extends TestCase
{

    private SnoozeComparator comparator_;
    
    private ArrayList<LocalControllerDescription> localControllers_;
    
    @Override
    protected void setUp() throws Exception
    {
        comparator_ = new LocalControllersL1();

        ResourceDemandEstimator estimator = EasyMock.createMock(StaticDynamicResourceDemandEstimator.class);
        ArrayList<Double> caplc1 = new ArrayList<Double>(Arrays.asList(0d, 0d, 0d, 0d));
        ArrayList<Double> caplc2 = new ArrayList<Double>(Arrays.asList(1d, 0d, 0d, 0d));
        ArrayList<Double> caplc3 = new ArrayList<Double>(Arrays.asList(2d, 0d, 0d, 0d));
        
        LocalControllerDescription lc1 = new LocalControllerDescription();
        lc1.setId("lc1");
        LocalControllerDescription lc2 = new LocalControllerDescription();
        lc2.setId("lc2");
        LocalControllerDescription lc3 = new LocalControllerDescription();
        lc3.setId("lc3");
        
        expect(estimator.computeLocalControllerCapacity(lc1)).andReturn(caplc1).anyTimes();
        expect(estimator.computeLocalControllerCapacity(lc2)).andReturn(caplc2).anyTimes();
        expect(estimator.computeLocalControllerCapacity(lc3)).andReturn(caplc3).anyTimes();
        
        replay(estimator);
        
        comparator_.setEstimator(estimator);

        
        localControllers_ = new ArrayList<LocalControllerDescription>(Arrays.asList(lc1, lc2, lc3));
    }
    
    public void testLocalControllerL1Increasing()
    {
        comparator_.setDecreasing(false);

        Collections.sort(localControllers_, comparator_);
        assertEquals(localControllers_.get(0).getId(), "lc1");
        assertEquals(localControllers_.get(1).getId(), "lc2");
        assertEquals(localControllers_.get(2).getId(), "lc3");
        
    }
    
    public void testLocalControllerL1Decresing()
    {
        comparator_.setDecreasing(true);

        Collections.sort(localControllers_, comparator_);
        assertEquals(localControllers_.get(0).getId(), "lc3");
        assertEquals(localControllers_.get(1).getId(), "lc2");
        assertEquals(localControllers_.get(2).getId(), "lc1");
        
    }

  
}
