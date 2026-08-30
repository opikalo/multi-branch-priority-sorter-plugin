package io.jenkins.plugins.multibranch;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import hudson.Extension;
import hudson.model.Job;
import hudson.model.Queue;
import jenkins.advancedqueue.PrioritySorterConfiguration;
import jenkins.advancedqueue.priority.strategy.AbstractDynamicPriorityStrategy;
import jenkins.branch.Branch;
import jenkins.scm.api.mixin.ChangeRequestSCMHead2;
import org.jenkinsci.plugins.workflow.multibranch.BranchJobProperty;
import org.kohsuke.stapler.DataBoundConstructor;
import java.util.logging.Logger;

public class BranchPriorityStrategy extends AbstractDynamicPriorityStrategy {
    private static final Logger LOGGER = Logger.getLogger(BranchPriorityStrategy.class.getName());
    @Extension
    public static class BranchPriorityStrategyDescriptor extends AbstractDynamicPriorityStrategyDescriptor {

        public BranchPriorityStrategyDescriptor() {
            super("Set Priority from branch name");
        }
    };

    private String branchName;
    private Boolean pullRequestMatchOriginName;
    private int priority;

    @DataBoundConstructor
    public BranchPriorityStrategy(String branchName, Boolean pullRequestMatchOriginName, int priority) {
        this.branchName = branchName;
        this.pullRequestMatchOriginName = pullRequestMatchOriginName;
        this.priority = priority;
    }

    @CheckForNull
    private Integer getPriorityInternal(Queue.Item item) {
        if (item.task instanceof Job<?, ?>) {
            Job<?, ?> job = (Job<?, ?>) item.task;
            BranchJobProperty branchProperty = job.getProperty(BranchJobProperty.class);
            if (branchProperty != null) {
                Branch branch = branchProperty.getBranch();
                String originName = branch.getName();
                if (Boolean.TRUE.equals(pullRequestMatchOriginName) && branch.getHead() instanceof ChangeRequestSCMHead2) {
                    originName = ((ChangeRequestSCMHead2) branch.getHead()).getOriginName();
                }
                LOGGER.warning("BranchPriorityStrategy: branch name seen = '" + originName + "', pattern = '" + branchName + "'");
                if (originName.matches(branchName)) {
                    return priority;
                }
            }
        }
        return null;
    }

    @Override
    public boolean isApplicable(Queue.Item item) {
        return getPriorityInternal(item) != null;
    }

    @Override
    public int getPriority(Queue.Item item) {
        final Integer p = getPriorityInternal(item);
        return p != null ?
            p : PrioritySorterConfiguration.get().getStrategy().getDefaultPriority();
    }

    public String getBranchName() {
        return branchName;
    }

    public Boolean getPullRequestMatchOriginName() {
        return pullRequestMatchOriginName;
    }

    public int getPriority() {
        return priority;
    }
}
