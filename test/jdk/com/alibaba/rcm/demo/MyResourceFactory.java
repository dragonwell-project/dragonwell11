package demo;

import com.alibaba.rcm.Constraint;
import com.alibaba.rcm.ResourceContainer;
import com.alibaba.rcm.ResourceContainerFactory;

public class MyResourceFactory implements ResourceContainerFactory {

    public final static ResourceContainerFactory INSTANCE = new MyResourceFactory();

    private MyResourceFactory() { /*pass*/}

    @Override
    public ResourceContainer createContainer(Iterable<Constraint> constraints) {
        MyResourceContainer container = new MyResourceContainer();
        for (Constraint constraint : constraints) {
            container.updateConstraint(constraint);
        }
        return container;
    }
}
