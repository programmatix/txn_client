package com.couchbase.sdkdclient.cluster.actions;

import com.couchbase.sdkdclient.options.*;

public class RebalanceActionFactory implements OptionConsumer, RebalanceConfig {
  public enum Action {

    /**
     * Add nodes the cluster.
     */
    IN,

    /**
     * Remove nodes from the cluster.
     */
    OUT,

    /**
     * Add and remove nodes from the cluster in a single operation.
     */
    SWAP,

    /**
     * Remove, restart and add the node one by one back to cluster in single operation.
     */
    REFRESH
  }

  EnumOption<Action> optAction =
          OptBuilder.start("mode", Action.class)
          .help("rebalance mode; in, out, refresh or swap")
          .required()
          .build();

  BoolOption optEpt =
          OptBuilder.<BoolOption>start(new BoolOption("ept"))
          .help("When removing nodes, also include the EPT")
          .defl("false")
          .build();

  IntOption optCount =
          OptBuilder.<IntOption>start(new IntOption("count"))
          .help("How many nodes to add or remove. For swap, this number is " +
                "doubled")
          .defl("1")
          .build();

  BoolOption optAddRebalance =
          OptBuilder.<BoolOption>start(new BoolOption("add-rebalance"))
          .help("This does nothing. Here for backwards compatibility")
          .hidden()
          .build();

  BoolOption ignoreRebalance =
          OptBuilder.startBool("rebalanceignore")
                  .help("Whether rebalance should be done or not")
                  .defl("false")
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
            .source(this, RebalanceActionFactory.class)
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
    return optEpt.getValue();
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
    switch (optAction.getValue()) {
      case IN:
        return new RebalanceInAction(services.getValue());
      case OUT:
        return new RebalanceOutAction(services.getValue());
      case SWAP:
        return new RebalanceSwapAction(services.getValue());
      case REFRESH:
        return new RebalanceRefreshAction(services.getValue(), ignoreRebalance.getValue());
      default:
        assert false : "Invalid option";
        return null;
    }
  }

  public String getDescription() {
    StringBuilder sb = new StringBuilder();

    switch (optAction.getValue()) {
      case IN:
        sb.append("add ").append(getNumNodes()).append(" nodes");
        break;
      case OUT:
        sb.append("remove ").append(getNumNodes()).append(" nodes");
        if (shouldUseEpt()) {
          sb.append(" (including the EPT)");
        }
        break;
      case SWAP:
        sb.append("add ").append(getNumNodes()).append(" nodes and ");
        sb.append("remove ").append(getNumNodes()).append( "nodes ");
        if (shouldUseEpt()) {
          sb.append(" (including EPT) ");
        }
        break;
      case REFRESH:
        sb.append("remove and rebalance, restart, add and rebalance ")
                .append(getNumNodes())
                .append(" nodes one by one");
    }
    sb.append(" and rebalance");
    return sb.toString();
  }
}