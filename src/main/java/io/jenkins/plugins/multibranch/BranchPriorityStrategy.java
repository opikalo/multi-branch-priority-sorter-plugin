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
    private Boolean pullRequestMatchTargetName;
    private int priority;

    @DataBoundConstructor
    public BranchPriorityStrategy(String branchName, Boolean pullRequestMatchOriginName, Boolean pullRequestMatchTargetName, int priority) {
        this.branchName = branchName;
        this.pullRequestMatchOriginName = pullRequestMatchOriginName;
        this.pullRequestMatchTargetName = pullRequestMatchTargetName;
        this.priority = priority;
    }

    @CheckForNull
    private Integer getPriorityInternal(Queue.Item item) {
        if (item.task instanceof Job<?, ?>) {
            Job<?, ?> job = (Job<?, ?>) item.task;
            BranchJobProperty branchProperty = job.getProperty(BranchJobProperty.class);
            if (branchProperty != null) {
                Branch branch = branchProperty.getBranch();
                String branchOriginName = branch.getName();
                String prOriginName = null;
                String matchedName = branchOriginName;
                if (Boolean.TRUE.equals(pullRequestMatchOriginName) && branch.getHead() instanceof ChangeRequestSCMHead2) {
                    prOriginName = ((ChangeRequestSCMHead2) branch.getHead()).getOriginName();
                    matchedName = prOriginName;
                    LOGGER.info("BranchPriorityStrategy: branch name = '" + branchOriginName + "', PR origin name = '" + prOriginName + "', matching against PR origin name, pattern = '" + branchName + "'");
                } else if (Boolean.TRUE.equals(pullRequestMatchTargetName) && branch.getHead() instanceof ChangeRequestSCMHead2) {
                    String prTargetName = ((ChangeRequestSCMHead2) branch.getHead()).getTarget().getName();
                    matchedName = prTargetName;
                    LOGGER.info("BranchPriorityStrategy: branch name = '" + branchOriginName + "', PR target name = '" + prTargetName + "', matching against PR target name, pattern = '" + branchName + "'");
                } else {
                    LOGGER.info("BranchPriorityStrategy: branch name = '" + branchOriginName + "', matching against branch name, pattern = '" + branchName + "'");
                }
                if (matchedName.matches(branchName)) {
                    LOGGER.info("BranchPriorityStrategy: matched '" + matchedName + "' against pattern '" + branchName + "', returning priority " + priority);
                    return priority;
                } else {
                    LOGGER.info("BranchPriorityStrategy: no match for '" + matchedName + "' against pattern '" + branchName + "', no priority assigned");
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

    public Boolean getPullRequestMatchTargetName() {
        return pullRequestMatchTargetName;
    }

    public int getPriority() {
        return priority;
    }
}
