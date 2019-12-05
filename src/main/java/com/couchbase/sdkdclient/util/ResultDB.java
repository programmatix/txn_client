package com.couchbase.sdkdclient.util;

import java.util.HashMap;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.sdkdclient.logging.LogUtil;
import com.google.gson.Gson;
import org.slf4j.Logger;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class ResultDB {

    private HashMap<String,Object> sdkd_result = new HashMap<String,Object>();
    private Bucket bucket;
    static private Logger logger = LogUtil.getLogger(ResultDB.class);
    public ResultDB(){
    }

    public void connect_cb(Cluster cluster){
        try {
            bucket = cluster.bucket("situational");
        } catch (Exception ex) {
            logger.error(ex.toString());
        }
    }

    public void store_to_cb(){
        UUID id = UUID.randomUUID();
        Gson gson = new Gson();
        String json = gson.toJson(sdkd_result);
        bucket.defaultCollection().upsert(id.toString(), json);
        System.out.println("Document id:" + id.toString());
    }

    public HashMap<String, Object> getSdkdMap(){
        return sdkd_result;
    }

    public void close(){
    }
}
