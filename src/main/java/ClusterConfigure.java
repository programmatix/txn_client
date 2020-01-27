import com.couchbase.cbadmin.client.RestApiException;
import com.couchbase.sdkdclient.cluster.NodeHost;
import com.couchbase.sdkdclient.context.HarnessError;
import com.couchbase.sdkdclient.context.HarnessException;
import com.couchbase.sdkdclient.logging.LogUtil;
import com.couchbase.sdkdclient.util.Retryer;
import org.slf4j.Logger;

public class ClusterConfigure {
    final private static Logger logger = LogUtil.getLogger(ClusterConfigure.class);
    private Configuration configParams;

    public ClusterConfigure(Configuration configParams){
        this.configParams=configParams;
    }

    static public abstract class RestRetryer extends Retryer<RestApiException> {
        RestRetryer(int seconds) {
            super(seconds * 1000, 500, RestApiException.class);
        }

        @Override
        protected void handleError(RestApiException caught) throws RestApiException {
            if (caught.getStatusLine().getStatusCode() >= 500) {
                call();
            } else if (caught.getStatusLine().getStatusCode() == 409) {
                logger.error("N1QL Index was not deleted from previous run");
                return;
            } else {
                throw HarnessException.create(HarnessError.CLUSTER, caught);
            }
        }
    }

    public void initializecluster(){
        configureCluster();
        if (!configParams.getinstallcouchbase()) {
            {
                if(!configParams.getinitializecluster()){
                    logger.warn("noinit specified. Not setting up cluster or creating buckets");
                    return;
                }
            }
        }

        // Ensure we can connect to the REST port
        try {
            new RestRetryer(defaults.RestTimeout) {
                @Override
                protected boolean tryOnce() throws RestApiException {
                    for (NodeHost nn : nodelist.getAll()) {
                        nn.getAdmin().getInfo();
                    }
                    return true;
                }
            }.call();
        } catch (RestApiException ex) {
            throw HarnessException.create(HarnessError.CLUSTER, ex);
        }
    }


    private void configureCluster(){
        
    }

}