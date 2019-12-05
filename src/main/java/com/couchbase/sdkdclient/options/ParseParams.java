package com.couchbase.sdkdclient.options;

import com.google.api.client.json.Json;
import com.google.gson.JsonObject;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;


public class ParseParams {
    String FileLocation = null;
    JSONObject paramList = null;
    public ParseParams(String FileLocation){
        this.FileLocation=FileLocation;
    }

    public void parse(){
        JSONParser jsonParser = new JSONParser();
        try
        {
            FileReader reader = new FileReader(FileLocation);
            Object obj = jsonParser.parse(reader);
            paramList = (JSONObject) obj;

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }


    public void printparams(){
        Iterator<Map.Entry> itr1 = paramList.entrySet().iterator();
        while(itr1.hasNext()){
            Map.Entry pair= itr1.next();
            System.out.println("Value for "+pair.getKey() + " : " + pair.getValue());
        }
    }
}
