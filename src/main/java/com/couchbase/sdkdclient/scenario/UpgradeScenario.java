package com.couchbase.sdkdclient.scenario;

import com.couchbase.cbadmin.client.RestApiException;
import com.couchbase.sdkdclient.cluster.CBCluster;
import com.couchbase.sdkdclient.cluster.ClusterBuilder;
import com.couchbase.sdkdclient.cluster.actions.RebalanceAction;
import com.couchbase.sdkdclient.cluster.actions.RebalanceActionFactory;
import com.couchbase.sdkdclient.cluster.actions.UpgradeActionFactory;
import com.couchbase.sdkdclient.context.HarnessError;
import com.couchbase.sdkdclient.context.HarnessException;
import com.couchbase.sdkdclient.options.OptionTree;
import com.couchbase.sdkdclient.util.NetworkIO;

import java.util.concurrent.ExecutionException;

/**
 * Created by jaekwon.park on 6/7/17.
 */

public class UpgradeScenario extends PhasedScenario {
    protected UpgradeActionFactory upOptions = new UpgradeActionFactory();
    protected RebalanceAction upAction;

    @Override
    @NetworkIO
    protected void doConfigure(ClusterBuilder clb) throws HarnessException {
        upAction = upOptions.createAction();
        upAction.setup(clb.getNodelistBuilder(), upOptions);
    }

    @Override
    protected void executeChange(CBCluster cluster) throws HarnessException {
        try {
            upAction.start(cluster).get();
        } catch (RestApiException ex) {
            throw new HarnessException(HarnessError.CLUSTER, ex);
        } catch (ExecutionException ex) {
            throw HarnessException.create(HarnessError.CLUSTER, ex);
        } catch (InterruptedException ex) {
            throw HarnessException.create(HarnessError.CLUSTER, ex);
        }
    }

    @Override
    public OptionTree getOptionTree() {
        OptionTree tree = new OptionTree();
        tree.addChild(upOptions.getOptionTree());
        tree.addChild(super.getOptionTree());
        return tree;
    }

    @Override
    protected String getChangeDescription() {
        return upOptions.getDescription();
    }
}
