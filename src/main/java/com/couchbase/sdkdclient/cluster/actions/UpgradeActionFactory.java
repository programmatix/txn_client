package com.couchbase.sdkdclient.cluster.actions;

/**
 * Created by jaekwon.park on 6/8/17.
 */
import com.couchbase.sdkdclient.options.*;

public class UpgradeActionFactory implements OptionConsumer, RebalanceConfig {

    IntOption optCount =
            OptBuilder.<IntOption>start(new IntOption("count"))
                    .help("How many nodes to add or remove. For swap, this number is " +
                            "doubled")
                    .defl("1")
                    .build();

    StringOption services =
            OptBuilder.<StringOption>start(new StringOption("services"))
                    .help("Which service to add/remove/swap")
                    .defl("")
                    .build();

    @Override
    public OptionTree getOptionTree() {
        return new OptionTreeBuilder()
                .prefix(OptionDomains.REBALANCE)
                .source(this, UpgradeActionFactory.class)
                .group("rebalance")
                .description("Options controlling rebalance")
                .build();
    }

    @Override
    public int getNumNodes() {
        return optCount.getValue();
    }
    @Override
    public boolean shouldUseEpt() {
        return false;
    }

    public RebalanceConfig getConfig() {
        return this;
    }

    /**
     * Creates a new, uninitialized RebalanceAction based on the configuration.
     * @return A new instance. The cluster will need to be configured with the
     * number of nodes specified by this instance's getRequiredActiveCount and
     * getRequiredFreeCount methods. The scenario should doConfigure the cluster
     * based on these parameters.
     *
     * Once the cluster has been configured, this instance' setup() method should
     * be called. Then the cluster can be started
     */
    public RebalanceAction createAction() {
        return new UpgradeSwapAction(services.getValue());
    }

    public String getDescription() {
        StringBuilder sb = new StringBuilder();

        sb.append("add ").append(getNumNodes()).append(" nodes and ");
        sb.append("remove ").append(getNumNodes()).append( "nodes ");
        sb.append(" and rebalance");
        return sb.toString();
    }
}
