/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.couchbase.sdkdclient.scenario;

import com.couchbase.sdkdclient.cluster.CBCluster;
import com.couchbase.sdkdclient.cluster.ClusterBuilder;
import com.couchbase.sdkdclient.cluster.NodeHost;
import com.couchbase.sdkdclient.context.HarnessError;
import com.couchbase.sdkdclient.context.HarnessException;
import com.couchbase.sdkdclient.options.*;
import com.couchbase.sdkdclient.workload.WorkloadGroup;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * This is the simplest implementation of {@link Scenario}. It will simply
 * set up the cluster once, start the workload, and wait a specified amount
 * of time before stopping the workload and collecting the results.
 */
public class BasicScenario extends Scenario {
  private IntOption optWait =
          OptBuilder.<IntOption>start(new IntOption("wait", "Time to run"))
          .globalAlias("wait")
          .argName("SECONDS")
          .defl("20")
          .build();

  @Override
  public OptionTree getOptionTree() {
    // Add phased options, but don't actually use them.
    OptionTree phasedDummy = new PhaseOptions().getOptionTree();
    OptionUtils.hideAll(phasedDummy);

    OptionTree root = new OptionTree();
    OptionTree tree = new OptionTree(OptionDomains.BASIC_SCENARIO);
    tree.addOption(optWait);
    root.addChild(phasedDummy);
    root.addChild(tree);
    System.out.println("from Tree scenarios");
    printoptions(tree.getAllOptionsInfo());
    System.out.println("from phasedDummy");
    printoptions(phasedDummy.getAllOptionsInfo());


    return root;
  }

  @Override
  protected void doConfigure(ClusterBuilder builder) {
    // NOOP
  }

  @Override
  public void run(CBCluster cluster, WorkloadGroup wlGroup) {



    if (listener != null) {
      listener.onRamp(this);
    }

    try {
      wlGroup.start();
    } catch (IOException ex) {
      throw HarnessException.create(HarnessError.WORKLOAD, ex);
    } catch (Exception ex) {
      logger.warn("ProtocolException: "+ ex);
    }

    try {
      Thread.sleep(optWait.getValue() * 1000);
    } catch (InterruptedException ex) {
      logger.warn("While sleeping", ex);
    }


  }

  final static String descFormat =
          "Configure the cluster and run the workload for %d seconds. " +
          "This secneario does not change the cluster";

  @Override
  public String getConfiguredDescription() {
    return String.format(descFormat, optWait.getValue());
  }
  public static void  printoptions(Collection<OptionInfo> optionsInfo ){
    Iterator<OptionInfo> itr = optionsInfo.iterator();
    while(itr.hasNext()){
      OptionInfo op= itr.next();
      System.out.println("Option Info Name: "+op.getOption().getName());
      System.out.println("Option Info Value: "+op.getOption().getCurrentRawValue());

    }
  }
}
