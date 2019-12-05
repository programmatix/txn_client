package com.couchbase.sdkdclient.scenario;

import com.couchbase.cbadmin.client.RestApiException;
import com.couchbase.sdkdclient.cluster.CBCluster;
import com.couchbase.sdkdclient.cluster.ClusterBuilder;
import com.couchbase.sdkdclient.cluster.NodeHost;
import com.couchbase.sdkdclient.context.HarnessException;

import java.util.Collection;

/**
 * Created by akshata.trivedi on 11/26/17.
 */
public class IndexFailoverScenario extends PhasedScenario {
    private String bucketName;
    private String secondaryIndexName;
    private String indexFieldsName;

    @Override
    protected void doConfigure(ClusterBuilder clb) throws HarnessException {
        bucketName = clb.getBucketOptions().getName();
        secondaryIndexName = clb.getClusterOptions().getN1QLIndexName();
        indexFieldsName = clb.getClusterOptions().getn1qlFieldsToIndex();
    }

    @Override
    protected void executeChange(CBCluster cluster) throws HarnessException {
        NodeHost n1qlNode = null;
        Collection<NodeHost> n1qlNodes = cluster.getN1QLNodes();
        for (NodeHost node : n1qlNodes) {
            n1qlNode = node;
            break;
        }

        try {
            n1qlNode.getAdmin().dropN1QLIndex(secondaryIndexName, bucketName, "secondary");
        } catch (RestApiException ex) {
            throw new HarnessException(ex);
        }
    }

    @Override
    protected String getChangeDescription() {
        return "Failing over indexes";
    }

    @Override
    protected void preExecPhase(CBCluster cluster) throws HarnessException {
        NodeHost n1qlNode = null;
        Collection<NodeHost> n1qlNodes = cluster.getN1QLNodes();
        for (NodeHost node : n1qlNodes) {
            n1qlNode = node;
            break;
        }

        try {
            n1qlNode.getAdmin().setupN1QLIndex(secondaryIndexName,
                    "secondary",
                    bucketName,
                    indexFieldsName.split(","),
                    null);
        } catch (RestApiException ex) {
            throw new HarnessException(ex);
        }
    }
}
