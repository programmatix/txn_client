package com.couchbase.Tests.Transactions.Utils;

import com.couchbase.Constants.Strings;
import com.couchbase.Logging.LogUtil;
import com.couchbase.Tests.Transactions.BasicTests.simpleInsert;
import com.couchbase.Tests.Transactions.BasicTests.simplecommit;
import com.couchbase.Tests.Transactions.Hooks.failsbeforeCommit;
import com.couchbase.Tests.Transactions.transactionTests;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.Collection;
import com.couchbase.client.java.json.JsonObject;
import com.couchbase.grpc.protocol.TxnClient;
import org.slf4j.Logger;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class txnUtils {


    public static boolean verifyDocuments(List<String> keys, JsonObject docContent, boolean docExists,String hostname) {
        Logger logger = LogUtil.getLogger(transactionTests.class);

        boolean doc_Exists;
        try{
            Cluster cluster = Cluster.connect(hostname, Strings.ADMIN_USER, Strings.PASSWORD);
            Collection defaultCollection= cluster.bucket("default").defaultCollection();
            for (String key : keys) {
                if (docExists) {
                    JsonObject body = defaultCollection.get(key).contentAs(JsonObject.class);
                   if(docContent==null){
                       assertEquals(0, body.size());
                   }else{
                       assertEquals(docContent, body);
                   }
                } else {
                    doc_Exists = defaultCollection.exists(key).exists();
                    logger.debug("Checking  delete for Key: "+key+ " Does key exist: "+doc_Exists);
                    logger.debug("Content: "+defaultCollection.get(key).toString());
                    assertEquals(docExists, doc_Exists);
                }
            }
            return true;
        }catch(Exception e){
            logger.error("Exception during verification: "+e);
            return false;
        }
    }



}
