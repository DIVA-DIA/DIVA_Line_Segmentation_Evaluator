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
import java.awt.Polygon;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * HisDocLayoutComp class of the ICDAR 2017 competition
 *
 * @author Manuel Bouillon <manuel.bouillon@unifr.ch>
 * @date 16.08.16
 * @brief The line segmentation evaluator main class
 */
public class HisDocLayoutComp {

    /**
     * Log4j logger
     */
    private static final Logger logger = Logger.getLogger(HisDocLayoutComp.class);

    /**
     * Experimenter function
     * @param args no args required
     */
    public static void main(String[] args) {
        logger.trace(Thread.currentThread().getStackTrace()[1].getMethodName());

        if (args.length < 3) {
            logger.error("Usage: Evaluator image_gt.jpg page_gt.xml page_to_evaluate.xml [results.csv] [iu_matching_threshold] [take_comment_lines_boolean]"); // add evaluation image filename
        } else {

            // Params
            BufferedImage image = null;
            List<Polygon> output = null;
            List<Polygon> truth = null;
            String outputPath = "";
            double threshold = 0.75;
            boolean comments = false;

            if (args.length > 3) {
                outputPath = args[3];
                logger.info("Output path is " + outputPath);
            }

            if (args.length > 4) {
                threshold = Double.parseDouble(args[4]);
                logger.info("IU matching threshold is " + (100*threshold) + " %");
            }

            if (args.length > 5) {
                comments = Boolean.parseBoolean(args[5]);
                logger.info("Taking comments into account: " + comments);
            }


            // Inputs
            logger.info("Loading image ground truth from " + args[0]);
            try {
                image = ImageIO.read(new File(args[0]));
            } catch (IOException e) {
                logger.error(e.getMessage());
            }

            logger.info("Loading page ground truth from " + args[1]);
            truth = ImageLinePageDataset.readOutputDataFromFile(args[1], comments);

            logger.info("Loading method output from " + args[2]);
            output = ImageLinePageDataset.readOutputDataFromFile(args[2], comments);


            // Evaluating...
            logger.info("Evaluating...");
            LineSegmentationEvaluator evaluator = new LineSegmentationEvaluator();
            Results results = evaluator.evaluate(image, truth, output, threshold, comments);


            // Write evaluation image
            String evalImagePath = outputPath.substring(0, outputPath.lastIndexOf('.'));
            evalImagePath += ".visualization";
            evalImagePath += args[0].substring(args[0].lastIndexOf('.'));
            try {
                ImageIO.write(evaluator.getEvalImage(), evalImagePath.substring(evalImagePath.lastIndexOf('.') + 1), new File(evalImagePath));
                logger.info("Writing evaluation image in " + evalImagePath);
            } catch (IOException e) {
                logger.error(e);
            }

            // Output
            if (!outputPath.isEmpty()) {
                logger.info("Writing results in " + outputPath);
                results.writeToCSV(outputPath);
            }

        }
    }

}
