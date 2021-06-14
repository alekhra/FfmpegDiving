package com.dfki.ffmpeg;

import android.content.Context;
import android.util.JsonReader;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;

public class Utils {
    static String getJsonFromAssets(Context context, String fileName) {
        String jsonString;
        JsonReader reader;
        try {
            InputStream is = context.getAssets().open(fileName);

            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();

            jsonString = new String(buffer, "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        return jsonString;
    }

    static JSONObject getDataAtFrame(String jsonFileString, int frame){
        JSONObject objj = null;
        JSONObject  pose = null;
        try {
            objj = new JSONObject(jsonFileString);
            JSONObject info = (JSONObject) objj.get("info");
            JSONArray data = (JSONArray) info.get("data");
            pose = (JSONObject) data.get(frame);

        } catch (
                JSONException e) {
            e.printStackTrace();
        }
        return pose;
    }

    static JSONObject getposeDataAtFrame(String jsonFileString, int frame){
        //JSONObject objj = null;
        //JSONObject  pose = null;
        JSONObject  newpose = null;
        try {
            //objj = new JSONObject(jsonFileString);
            // JSONObject info = (JSONObject) objj.get("info");
            //JSONArray data = (JSONArray) info.get("data");
            //JSONObject pose = (JSONObject) data.get(frame);
            JSONObject pose = getDataAtFrame(jsonFileString,frame);
            newpose = (JSONObject) pose.get("pose");
        } catch (
                JSONException e) {
            e.printStackTrace();
        }
        return newpose;
    }

}
