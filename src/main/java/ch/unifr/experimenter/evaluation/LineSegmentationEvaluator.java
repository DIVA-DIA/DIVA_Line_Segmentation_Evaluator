/*
 * Copyright (c) 2016 UniFR
 * University of Fribourg, Switzerland.
 */

package ch.unifr.experimenter.evaluation;

import org.apache.log4j.Logger;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * LineSegmentationEvaluator class of the Experimenter project
 *
 * @author Manuel Bouillon <manuel.bouillon@unifr.ch>
 * @date 26.09.16
 * @brief Line segmentation evaluator
 * Find all possible matching between Grount Truth (GT) polygons and
 * Method Output (MO) polygons and compute the matching possibilities areas.
 * Final matching is done starting from the biggest matching possibility area
 * and continuing until all polygons are match or until no matching possibility remains.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class LineSegmentationEvaluator {

    /**
     * Keys for the different measures
     */
    public static final String LINES_NB_PROPOSED = "LineSegmentation.NbLinesProposed.int";
    public static final String LINES_NB_CORRECT = "LineSegmentation.NbLinesCorrect.int";
    public static final String LINES_NB_TRUTH = "LineSegmentation.NbLinesTruth.int";
    public static final String LINES_NB_RECALL = "LineSegmentation.LinesRecall.Double";
    public static final String LINES_NB_PRECISION = "LineSegmentation.LinesPrecision.Double";
    public static final String LINES_NB_IU = "LineSegmentation.LinesIU.double";
    public static final String LINES_PIXEL_IU = "LineSegmentation.PixelIU.double";

    /**
     * Log4j logger
     */
    protected static final Logger logger = Logger.getLogger(LineSegmentationEvaluator.class);

    /**
     * Evaluation image
     */
    private BufferedImage evalImage = null;

    /**
     * Evaluate output data with respect to ground truth
     *
     * @param groundTruthImage         the ground truth groundTruthImage
     * @param methodOutput  the polygons output by the method to evaluate
     * @param groundTruth   the ground truth polygons
     * @param threshold     the IU threshold for line matching
     * @return Results object
     */
    public Results evaluate(BufferedImage groundTruthImage, List<Polygon> groundTruth, List<Polygon> methodOutput, double threshold, boolean comments) {
        logger.trace(Thread.currentThread().getStackTrace()[1].getMethodName());
        return evaluate(groundTruthImage, groundTruth, methodOutput, threshold, comments, null);
    }

    /**
     * Evaluate output data with respect to ground truth
     *
     * @param groundTruthImage         the ground truth groundTruthImage
     * @param methodOutput  the polygons output by the method to evaluate
     * @param groundTruth   the ground truth polygons
     * @param threshold     the IU threshold for line matching
     * @param mainTextArea  the area of the main text
     * @return Results object
     */
    public Results evaluate(BufferedImage groundTruthImage, List<Polygon> groundTruth, List<Polygon> methodOutput, double threshold, boolean comments, Rectangle mainTextArea) {
        logger.trace(Thread.currentThread().getStackTrace()[1].getMethodName());

        // Match overlapping polygons
        Map<Polygon, Polygon> matching = getMatchingPolygons(methodOutput, groundTruth, comments);
        logger.debug("matching.size " + matching.size());

        // Init evaluation image
        evalImage = new BufferedImage(groundTruthImage.getWidth(), groundTruthImage.getHeight(), BufferedImage.TYPE_INT_RGB);

        // Line count
        int nbLinesCorrects = 0;

        // Pixels counts
        int matchingSum = 0;
        int missedSum = 0;
        int falseSum = 0;
        int outputSum = 0;
        int truthSum = 0;

        // for every polygon MO (from Method Output)
        for (Polygon pmo : matching.keySet()) {

            // get the matched polygon GT (from Ground truth)
            Polygon pgt = matching.get(pmo);
            logger.trace("evaluation matching " + pgt + " * " + pmo);

            // Pixels counts
            int matchingPixels = 0;
            int missedPixels = 0;
            int falsePixels = 0;
            int outputPixels = 0;
            int truthPixels = 0;

            // find the bounding box of both polygons
            Rectangle rmo = pmo.getBounds();
            Rectangle rgt = pgt.getBounds();
            int xmin = (int) Math.min(rmo.getMinX(), rgt.getMinX());
            int xmax = (int) Math.max(rmo.getMaxX(), rgt.getMaxX());
            int ymin = (int) Math.min(rmo.getMinY(), rgt.getMinY());
            int ymax = (int) Math.max(rmo.getMaxY(), rgt.getMaxY());

            // for every pixel of the bounding box
            for(int i = xmin; i <= xmax; i++) {
                for(int j = ymin; j < ymax; j++) {

                    // ignore boundary pixels
                    int boundary = (groundTruthImage.getRGB(i,j) >> 23) & 0x1;
                    if (boundary == 1) {
                        continue;
                    }

                    // ignore background pixels
                    int background = (groundTruthImage.getRGB(i,j) >> 0) & 0x1;
                    if (background == 1) {
                        continue;
                    }

                    // ignore if out of main text area
                    if (mainTextArea != null && !mainTextArea.contains(i, j)) {
                        continue;
                    }

                    boolean isInPmo = pmo.contains(i, j);
                    boolean isInPgt = pgt.contains(i, j);


                    if (isInPmo && isInPgt) { // check if match
                        matchingPixels++;
                    } else if (!isInPmo && isInPgt) {  // check if missed
                        missedPixels++;
                    } else if (isInPmo && !isInPgt) { // check if wrongly detected
                        falsePixels++;
                    }

                    // compute pmo size
                    if (isInPmo) {
                        outputPixels++;
                    }

                    // compute pgt size
                    if (isInPgt) {
                        truthPixels++;
                    }
                }
            }

            // Take matching into account if IU > threshold
            double iu = 0;
            double union = matchingPixels + missedPixels + falsePixels;
            if (union > 0) {
                iu = matchingPixels / union;
            }
            logger.trace("IU = " + iu);

            if (iu > threshold) {
                logger.trace("line detected");
                nbLinesCorrects++;

                matchingSum += matchingPixels;
                missedSum += missedPixels;
                falseSum += falsePixels;
                outputSum += outputPixels;
                truthSum += truthPixels;

                // update visualization image
                // for every pixel of the bounding box
                for(int i = xmin; i <= xmax; i++) {
                    for(int j = ymin; j < ymax; j++) {

                        // ignore background pixels
                        int background = (groundTruthImage.getRGB(i,j) >> 0) & 0x1;
                        if (background == 1) {
                            continue;
                        }

                        // ignore boundary pixels
                        int boundary = (groundTruthImage.getRGB(i,j) >> 23) & 0x1;
                        if (boundary == 1) {
                            evalImage.setRGB(i, j, Color.gray.getRGB());
                            continue;
                        }

                        boolean isInPmo = pmo.contains(i, j);
                        boolean isInPgt = pgt.contains(i, j);

                        if (isInPmo && isInPgt) { // check if match
                            evalImage.setRGB(i, j, Color.green.getRGB());
                        } else if (!isInPmo && isInPgt) { // check if missed
                            evalImage.setRGB(i, j, Color.blue.getRGB());
                        } else if (isInPmo && !isInPgt) { // check if wrongly detected
                            evalImage.setRGB(i, j, Color.red.getRGB());
                        }
                    }
                }
            } else {
                logger.debug("line skipped, IU below threshold: " + iu);
            }
        }

        // Debug printing
        logger.trace("truthSum = " + truthSum);
        logger.trace("outputSum = " + outputSum);

        logger.trace("matchingSum = " + matchingSum);
        logger.trace("falseSum = " + falseSum);
        logger.trace("missedSum = " + missedSum);

        logger.trace("matchingSum + missedSum = " + (matchingSum + missedSum));
        logger.trace("matchingSum + falseSum = " + (matchingSum + falseSum));

        // Line scores
        double lineRecall = nbLinesCorrects / (double) groundTruth.size();
        double linePrecision = nbLinesCorrects / (double) methodOutput.size();
        int lineUnion = groundTruth.size() + methodOutput.size() - nbLinesCorrects;
        double lineIU = nbLinesCorrects / (double) lineUnion;

        // Pixel scores
        double precision = matchingSum / (double) outputSum;
        double recall = matchingSum / (double) truthSum;
        double truePositive = matchingSum / (double) truthSum;
        double falsePositive = falseSum / (double) outputSum;
        double falseNegative = missedSum / (double) truthSum;
        int unionSum = matchingSum + missedSum + falseSum;
        double avgPixelIU = matchingSum / (double) unionSum;

        // Logging
        logger.info("line IU = " + lineIU);
        logger.trace("intersection = " + matchingSum);
        logger.trace("union = " + unionSum);
        logger.info("pixel IU = " + avgPixelIU);
        logger.info("Precision = " + precision);
        logger.info("Recall = " + recall);
        logger.debug("True Positive = " + truePositive);
        logger.debug("False Positive = " + falsePositive);
        logger.debug("False Negative = " + falseNegative);
//        logger.info("True Negative = " + (pageSum - outputSum) / (double)(pageSum - truthSum));

        // Storing line results
        Results results = new Results();
        results.put(LINES_NB_TRUTH, groundTruth.size());
        results.put(LINES_NB_PROPOSED, methodOutput.size());
        results.put(LINES_NB_CORRECT, nbLinesCorrects);
        results.put(LINES_NB_RECALL, lineRecall);
        results.put(LINES_NB_PRECISION, linePrecision);
        results.put(LINES_NB_IU, lineIU);

        // Storing pixel results
        results.put(LINES_PIXEL_IU, avgPixelIU);
        results.put(Results.PRECISION, precision);
        results.put(Results.RECALL, recall);
        results.put(Results.TRUEPOSITIVE, truePositive);
        results.put(Results.FALSEPOSITIVE, falsePositive);
        results.put(Results.FALSENEGATIVE, falseNegative);
//        results.put(Results.TRUENEGATIVE, (pageSum - outputSum) / (double)(pageSum - truthSum));

        logger.trace(results.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(results)));
        return results;
    }

    /**
     * Find the matching polygons between the methodOutput and the groundTruth
     *
     * @param methodOutput polygons given by the method
     * @param groundTruth  polygons in the ground truth
     * @param comments
     * @return the matching polygons
     */
    private Map<Polygon, Polygon> getMatchingPolygons(List<Polygon> methodOutput, List<Polygon> groundTruth, boolean comments) {
        logger.trace(Thread.currentThread().getStackTrace()[1].getMethodName());

        Map<Polygon, Polygon> matching = new HashMap<>();

        // Find all intersecting polygons
        Map<Polygon, Map<Polygon, Double>> possibilities = new HashMap<>();
        int possibilitiesCount = 0;

        // for every GT polygon
        for (Polygon pgt : groundTruth) {
            logger.trace("matching possibility for GT: " + pgt);

            // for every MO polygon
            //noinspection Convert2streamapi
            for (Polygon pmo : methodOutput) {

                // skip if no overlap
                if (!pgt.getBounds().intersects(pmo.getBounds())) {
                    logger.trace("no matching possibility of " + pgt + " with MO: " + pmo);
                    continue;
                }

                // create map if first possibility with pgt
                if (!possibilities.containsKey(pgt)) {
                    possibilities.put(pgt, new HashMap<>());
                }

                // find bounding box
                Rectangle rmo = pmo.getBounds();
                Rectangle rgt = pgt.getBounds();
                int xmin = (int) Math.min(rmo.getMinX(), rgt.getMinX());
                int xmax = (int) Math.max(rmo.getMaxX(), rgt.getMaxX());
                int ymin = (int) Math.min(rmo.getMinY(), rgt.getMinY());
                int ymax = (int) Math.max(rmo.getMaxY(), rgt.getMaxY());

                // compute intersection area
                int intersectingPixels = 0;
                for(int i = xmin; i <= xmax; i++) {
                    for(int j = ymin; j < ymax; j++) {
                        if (rmo.contains(i, j) && rgt.contains(i, j)) {
                            intersectingPixels++;
                        }
                    }
                }

                // add the matching possibility
                possibilities.get(pgt).put(pmo, (double)intersectingPixels);
                possibilitiesCount++;
                logger.trace("matching possibility: " + pgt + " * " + pmo + " = " + intersectingPixels);
            }
        }
        logger.debug(possibilitiesCount + " possibilities");

        // For 'possibilities' times
        for (int i = possibilities.size(); i > 0; i--) {

            // Select best matching first
            // (biggest area of all matching for all polygons)
            double maxArea = 0;
            Polygon polygonGT = null;
            Polygon polygonMO = null;

            // for each gt polygon
            for (Polygon pgt : possibilities.keySet()) {

                // find best matching mo pgt
                for (Polygon pmo : possibilities.get(pgt).keySet()) {

                    if (!matching.containsValue(pgt)
                            && !matching.containsKey(pmo)
                            && possibilities.get(pgt).get(pmo) > maxArea) {
                        maxArea = possibilities.get(pgt).get(pmo);
                        polygonGT = pgt;
                        polygonMO = pmo;
                    }
                }
            }

            // Add matching polygons
            if ((polygonGT != null) && (polygonMO != null)) {
                logger.debug("match " + maxArea / 1000);
                matching.put(polygonMO, polygonGT);
            } else {
                logger.warn("matching failed…");
            }
        }

        // check if no polygon is matched twice
        if (matching.keySet().size() != matching.size()
                || matching.values().size() != matching.size()) {
            logger.warn("ERROR: some polygons are matched twice…");
        }

        logger.info("found " + matching.size() + " matches");
        logger.trace(matching.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(matching)));
        return matching;
    }

    /**
     * Get the evaluation image
     * @return eval image
     */
    public BufferedImage getEvalImage() {
        return evalImage;
    }

}
