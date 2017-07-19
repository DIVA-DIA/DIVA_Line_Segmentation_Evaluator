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
 * Find all possible matching between Ground Truth (GT) polygons and
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
     * Evaluate output data with respect to ground truth
     * (STANDARD VERSION)
     *
     * @param methodOutput the polygons output by the method to evaluate
     * @param groundTruth  the ground truth polygons
     * @return Results object
     */
    public Results evaluate(List<Polygon> methodOutput, List<Polygon> groundTruth, double threshold) {
        return evaluate(methodOutput, groundTruth, null, null, threshold);
    }

    /**
     * Evaluate output data with respect to ground truth
     * (HISDOC-LAYOUT-COMP VERSION)
     * >> TAKES INTO ACCOUNT THE PIXEL LEVEL GROUND TRUTH <<
     *
     * @param methodOutput     the polygons output by the method to evaluate
     * @param groundTruth      the ground truth polygons
     * @param groundTruthImage the pixel level ground truth (null if none)
     * @param mainTextArea
     * @return Results object
     */
    public Results evaluate(List<Polygon> methodOutput, List<Polygon> groundTruth, BufferedImage groundTruthImage, Rectangle mainTextArea, double threshold) {
        logger.trace(Thread.currentThread().getStackTrace()[1].getMethodName());

        // Match overlapping polygons
        Map<Polygon, Polygon> matching = getMatchingPolygons(methodOutput, groundTruth);
        logger.debug("matching.size " + matching.size());

        // Line count
        int nbLinesCorrects = 0;

        // Pixels counts
        int matchingSum = 0;
        int missedSum = 0;
        int falseSum = 0;
        int outputSum = 0;
        int truthSum = 0;

        boolean isInPmo;
        boolean isInPgt;

        // for every polygon MO (from Method Output)
        for (Polygon pmo : matching.keySet()) {

            // get the matched polygon GT (from Ground truth)
            Polygon pgt = matching.get(pmo);

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

                    // FOR THE HISDOC-LAYOUT-COMP: IGNORE BOUNDARIES AND BACKGROUND
                    if (groundTruthImage != null) {
                        // ignore boundary pixels
                        int boundary = (groundTruthImage.getRGB(i, j) >> 23) & 0x1;
                        if (boundary == 1) {
                            continue;
                        }
                        // ignore background pixels
                        int background = (groundTruthImage.getRGB(i, j) >> 0) & 0x1;
                        if (background == 1) {
                            continue;
                        }
                        // ignore if out of main text area
                        if (mainTextArea != null && !mainTextArea.contains(i, j)) {
                            continue;
                        }
                    }
                    // END FOR THE HISDOC-LAYOUT-COMP

                    isInPmo = pmo.contains(i, j);
                    isInPgt = pgt.contains(i, j);

                    // check if match
                    if (isInPmo && isInPgt) {
                        matchingPixels++;
                    }

                    // check if missed
                    if (!isInPmo && isInPgt) {
                        missedPixels++;
                    }

                    // check if wrongly detected
                    if (isInPmo && !isInPgt) {
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

            // Take matching into account if IU > 75%
            double iu = 0;
            double union = matchingPixels + missedPixels + falsePixels;
            if (union > 0) {
                iu = matchingPixels / union;
            }
            logger.trace("IU = " + iu);
            if (iu > threshold) {
                nbLinesCorrects++;

                matchingSum += matchingPixels;
                missedSum += missedPixels;
                falseSum += falsePixels;
                outputSum += outputPixels;
                truthSum += truthPixels;
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
        double lineRecall = Double.isNaN(nbLinesCorrects / (double) groundTruth.size()) ? 0 : nbLinesCorrects / (double) groundTruth.size();
        double linePrecision = Double.isNaN(nbLinesCorrects / (double) methodOutput.size()) ? 0 : nbLinesCorrects / (double) methodOutput.size();
        int lineUnion = Double.isNaN(groundTruth.size() + methodOutput.size() - nbLinesCorrects) ? 0 : groundTruth.size() + methodOutput.size() - nbLinesCorrects;
        double lineIU = Double.isNaN(nbLinesCorrects / (double) lineUnion) ? 0 : nbLinesCorrects / (double) lineUnion;

        // Pixel scores
        double precision = Double.isNaN(matchingSum / (double) outputSum) ? 0 : matchingSum / (double) outputSum;
        double recall = Double.isNaN(matchingSum / (double) truthSum) ? 0 : matchingSum / (double) truthSum;
        double truePositive = Double.isNaN(matchingSum / (double) truthSum) ? 0 : matchingSum / (double) truthSum;
        double falsePositive = Double.isNaN(falseSum / (double) outputSum) ? 0 : falseSum / (double) outputSum;
        double falseNegative = Double.isNaN(missedSum / (double) truthSum) ? 0 : missedSum / (double) truthSum;
        int unionSum = Double.isNaN(matchingSum + missedSum + falseSum) ? 0 : matchingSum + missedSum + falseSum;
        double avgPixelIU = Double.isNaN(matchingSum / (double) unionSum) ? 0 : matchingSum / (double) unionSum;

        // Logging
        logger.debug("line IU = " + lineIU);
        logger.debug("line precision = " + linePrecision);
        logger.debug("line recall = " + lineRecall);
        logger.debug("pixel IU = " + avgPixelIU);
        logger.debug("pixel precision = " + precision);
        logger.debug("pixel recall = " + recall);
//        logger.debug("True Negative = " + (pageSum - outputSum) / (double)(pageSum - truthSum));

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
     * @return the matching polygons
     */
    private Map<Polygon, Polygon> getMatchingPolygons(List<Polygon> methodOutput, List<Polygon> groundTruth) {
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
                logger.debug("matching failed…");
            }
        }

        logger.debug("found " + matching.size() + " matches");
        logger.trace(matching.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(matching)));
        return matching;
    }

}
