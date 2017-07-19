/*
 * Copyright (c) 2016 UniFR
 * University of Fribourg, Switzerland.
 */

package ch.unifr.experimenter.evaluation;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.apache.log4j.Logger;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * Results class of the Experimenter project
 *
 * @author Manuel Bouillon <manuel.bouillon@unifr.ch>
 * @date 16.08.16
 * @brief Store results in a map.
 */
@SuppressWarnings("unused")
public class Results {

    /**
     * Keys for the different measures
     */
    public static final String FILENAME = "Results.filename.String";
    public static final String ACCURACY = "Results.Accuracy.Double";
    public static final String PRECISION = "Results.Precision.Double";
    public static final String RECALL = "Results.Recall.Double";
    public static final String FMEASURE = "Results.FMeasure.Double";
    public static final String TRUEPOSITIVE = "Results.TruePositive.double";
    public static final String FALSEPOSITIVE = "Results.FalsePositive.double";
    public static final String FALSENEGATIVE = "Results.FalseNegative.double";
    public static final String TRUENEGATIVE = "Results.TrueNegative.double";

    /**
     * Log4j logger
     */
    private static final Logger logger = Logger.getLogger(Results.class);

    /**
     * The map storing all the measures and their associated values
     */
    private Map<String, String> results = new HashMap<>();

    /**
     * Set/update the value associated with the key
     *
     * @param key   of the measure
     * @param value of the measure
     */
    @SuppressWarnings("WeakerAccess")
    public void put(String key, Object value) {
        logger.trace("put(" + key + ") = " + value);
        results.put(key, value.toString());
    }

    /**
     * Get the value associated with the key
     *
     * @param key the measure
     * @return the value of the measure
     */
    public String get(String key) {
        logger.trace("get(" + key + ") = " + results.get(key));
        return results.get(key);
    }

    /**
     * Get the measures set
     *
     * @return the measure set
     */
    public Set<String> keySet() {
        logger.trace("keyset (" + results.keySet().size() + " elements)");
        return results.keySet();
    }

    /**
     * Write results as CSV file
     *
     * @param outputPath the path for the CSV results file
     */
    public void writeToCSV(String outputPath) {
        ArrayList<String> lines = new ArrayList<>();

        String line1 = "";
        String line2 = "";
        for (String key: results.keySet()) {
            String k = key.substring(key.indexOf('.') + 1);
            k = k.substring(0, k.indexOf('.'));
            line1 += k + ",";
            line2 += results.get(key) + ",";
        }
        lines.add(line1);
        lines.add(line2);

        try {
            Files.write(Paths.get(outputPath), lines, Charset.forName("UTF-8"));
            logger.debug("wrote " + outputPath);
        } catch (IOException e) {
            logger.error(e.getMessage());
        }

    }

    public void writeToJson(String outputPath, String gtFilename){

        JsonObject result = new JsonObject();
        JsonArray output = new JsonArray();
        for(Map.Entry<String, String> entry : results.entrySet()){
            JsonObject content = new JsonObject();
            content.add("name", new JsonPrimitive(entry.getKey().split("\\.")[1]));
            content.add("value", new JsonPrimitive(Double.parseDouble(entry.getValue())));
            content.add("mime-type", new JsonPrimitive("text/plain"));
            JsonObject object = new JsonObject();
            object.add("number", content);
            output.add(object);

        }

        //gtFileName
        JsonObject gtFilenameContent = new JsonObject();
        gtFilenameContent.add("name", new JsonPrimitive("gtFilename"));
        gtFilenameContent.add("value", new JsonPrimitive(gtFilename));
        gtFilenameContent.add("mime-type", new JsonPrimitive("text/plain"));
        JsonObject gtFilenameObject = new JsonObject();
        gtFilenameObject.add("text", gtFilenameContent);
        output.add(gtFilenameObject);

        result.add("output", output);
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(outputPath, "UTF-8");
        } catch (FileNotFoundException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        writer.print(result);
        writer.close();
    }

}
