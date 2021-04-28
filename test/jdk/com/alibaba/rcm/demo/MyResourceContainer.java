package demo;

import com.alibaba.rcm.Constraint;
import com.alibaba.rcm.ResourceType;
import com.alibaba.rcm.internal.AbstractResourceContainer;

import java.util.ArrayList;
import java.util.List;

public class MyResourceContainer extends AbstractResourceContainer {

    public List<String> operations = new ArrayList<>();

    private boolean dead;

    @Override
    protected void attach() {
        super.attach();
        operations.add("attach");
    }

    @Override
    protected void detach() {
        super.detach();
        operations.add("detach");
    }

    @Override
    public State getState() {
        return dead ? State.DEAD : State.RUNNING;
    }

    @Override
    public void updateConstraint(Constraint constraint) {
        operations.add("update " + constraint.getResourceType());
    }

    @Override
    public Iterable<Constraint> getConstraints() {
        return null;
    }

    @Override
    public void destroy() {
        dead = true;
    }

    @Override
    public Long getConsumedAmount(ResourceType resourceType) {
        return 0L;
    }
}
