/*
 * Copyright (c) 2016 UniFR
 * University of Fribourg, Switzerland.
 */

package ch.unifr.experimenter.evaluation;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Results class of the Experimenter project
 *
 * @author Manuel Bouillon <manuel.bouillon@unifr.ch>
 * @author Michele Alberti <michele.alberti@unifr.ch>
 * @date 25.07.2017
 * @brief Store results in a map.
 */
@SuppressWarnings({"WeakerAccess"})
public class Results {

    /**
     * Log4j logger
     */
    private static final Logger logger = Logger.getLogger(Results.class);
    /**
     * The map storing all the measures and their associated values
     */
    private Map<String, String> results = new HashMap<>();
    /**
     * Keys for the different measures
     */
    public static final String FILENAME = "LineSegmentation.filename.String";

    public static final String LINES_NB_TRUTH = "LineSegmentation.NbLinesTruth.int";
    public static final String LINES_NB_PROPOSED = "LineSegmentation.NbLinesProposed.int";
    public static final String LINES_NB_CORRECT = "LineSegmentation.NbLinesCorrect.int";

    public static final String LINES_IU = "LineSegmentation.LinesIU.double";
    public static final String LINES_FMEASURE = "LineSegmentation.LinesFMeasure.double";
    public static final String LINES_RECALL = "LineSegmentation.LinesRecall.Double";
    public static final String LINES_PRECISION = "LineSegmentation.LinesPrecision.Double";

    public static final String PIXEL_IU = "LineSegmentation.PixelIU.double";
    public static final String PIXEL_FMEASURE = "LineSegmentation.PixelFMeasure.Double";
    public static final String PIXEL_PRECISION = "LineSegmentation.PixelPrecision.Double";
    public static final String PIXEL_RECALL = "LineSegmentation.PixelRecall.Double";

    /**
     * Set/update the value associated with the key
     *
     * @param key   of the measure
     * @param value of the measure
     */
    public void put(String key, Object value) {
        logger.trace("put(" + key + ") = " + value);
        results.put(key, value.toString());
    }

    /**
     * Write results as CSV file. If the file already exists it appends a new line only
     * @param outputPath the path for the CSV results file
     */
    public void writeToCSV(String outputPath) {
        //TODO append if file exist and fix print order
        ArrayList<String> lines = new ArrayList<>();

        StringBuilder s = new StringBuilder();
        String line2 = "";
        for (String key: results.keySet()) {
            // Take the middle part of the key, which is the metric name
            s.append(key.split("\\.")[1]).append(",");
            line2 += results.get(key) + ",";
        }
        lines.add(s.toString());
        lines.add(line2);

        try {
           Files.write(Paths.get(outputPath), line2.getBytes(), StandardOpenOption.APPEND);
            logger.debug("wrote " + outputPath);
        } catch (IOException e) {
            logger.error(e.getMessage());
        }

    }
}
