package com.couchbase.Tests.Transactions.Utils;

import com.couchbase.Constants.Strings;
import com.couchbase.Logging.LogUtil;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.Collection;
import com.couchbase.client.java.json.JsonObject;
import org.slf4j.Logger;

import java.util.List;

import static org.junit.Assert.*;

public class txnUtils {
    static Logger logger = LogUtil.getLogger(txnUtils.class);

    public static void verifyDocuments(List<String> keys, JsonObject docContent,boolean docExists,String hostname) {
        try{
            Cluster cluster = Cluster.connect(hostname, Strings.ADMIN_USER, Strings.PASSWORD);
            Collection defaultCollection= cluster.bucket("default").defaultCollection();
            for (String key : keys) {
                JsonObject body=null;
                try{
                    body = defaultCollection.get(key).contentAs(JsonObject.class);
                }catch(Exception e){
                    if(!docExists){
                        logger.debug("Inside docExists false");
                        assertTrue(e.getClass().getName().contains("DocumentNotFoundException"));
                    }
                }

                if(docExists){
                   logger.debug("Inside docExists true");
                    if(docContent==null){
                        assertEquals(0, body.size());
                    }else{
                        assertEquals(docContent, body);
                    }
                }else{
                    assertNull(body);
                }
            }
        }catch(Exception e){
            logger.error("Exception during verification: "+e);
        }
    }
}
