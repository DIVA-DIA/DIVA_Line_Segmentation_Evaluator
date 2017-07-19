/*
 * Copyright (c) 2016 UniFR
 * University of Fribourg, Switzerland.
 */

package ch.unifr.experimenter;

import ch.unifr.experimenter.database.ImageLinePageDataset;
import ch.unifr.experimenter.evaluation.LineSegmentationEvaluator;
import ch.unifr.experimenter.evaluation.Results;
import org.apache.log4j.Logger;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

/**
 * Experimenter class of the Experimenter project
 *
 * @author Manuel Bouillon <manuel.bouillon@unifr.ch>
 * @date 16.08.16
 * @brief The experimenter main class
 */
public class HisDocLayoutComp {

    /**
     * Log4j logger
     */
    private static final Logger logger = Logger.getLogger(HisDocLayoutComp.class);

    /**
     * Experimenter function
     *
     * @param args no args required
     */
    public static void main(String[] args) {
        logger.trace(Thread.currentThread().getStackTrace()[1].getMethodName());

        if (args.length < 3) {
            logger.error("Usage: Evaluator image_gt.jpg page_gt.xml page_to_evaluate.xml [results.csv] [iu_matching_threshold] [take_comment_lines_boolean] [results.json]");

        } else {

            // Params
            BufferedImage image = null;
            List<Polygon> output = null;
            List<Polygon> truth = null;
            String outputPath = "";
            String jsonOutputPath = "";
            double threshold = 0.75;
            boolean comments = false;

            if (args.length > 3) {
                outputPath = args[3];
                logger.info("Output path is " + outputPath);
            }

            if (args.length > 4) {
                threshold = Double.parseDouble(args[4]);
                logger.info("IU matching threshold is " + (100 * threshold) + " %");
            }

            if (args.length > 5) {
                comments = Boolean.parseBoolean(args[5]);
                logger.info("Taking comments into account: " + comments);
            }

            if (args.length >= 6) {
                jsonOutputPath = args[6];
                logger.info("json output path is " + jsonOutputPath);
            }


            // Inputs
            logger.info("Loading image ground truth from " + args[0]);
            try {
                image = ImageIO.read(new File(args[0]));
            } catch (IOException e) {
                logger.error(e.getMessage());
            }

            logger.info("Loading page ground truth from " + args[1]);
            truth = ImageLinePageDataset.readOutputDataFromFile(args[1]);

            logger.info("Loading method output from " + args[2]);
            output = ImageLinePageDataset.readOutputDataFromFile(args[2]);


            // Evaluating...
            logger.info("Evaluating...");
            Results results = new LineSegmentationEvaluator().evaluate(output, truth, threshold);


            // Output
            if (!outputPath.isEmpty()) {
                logger.info("Writing results in " + outputPath);
                results.writeToCSV(outputPath);
            }

            if (!jsonOutputPath.isEmpty()) {
                logger.info("Writing json results in " + jsonOutputPath);
                results.writeToJson(jsonOutputPath, Paths.get(args[1]).getFileName().toString());
            }

        }
    }

}
