/*
 * Copyright (c) 2016 UniFR
 * University of Fribourg, Switzerland.
 */

package ch.unifr.experimenter.database;

import org.apache.log4j.Logger;
import org.jdom2.*;
import org.jdom2.input.SAXBuilder;

import java.awt.Polygon;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * ImageLinePageDataset class of the Experimenter project
 *
 * @author Manuel Bouillon <manuel.bouillon@unifr.ch>
 * @date 14.12.16
 * @brief Load data files in PAGE format
 */
public class ImageLinePageDataset {

    /**
     * Log4j logger
     */
    private static final Logger logger = Logger.getLogger(ImageLinePageDataset.class);

    /**
     * ImageLineDataset constructor
     */
    public ImageLinePageDataset() {
        logger.trace(Thread.currentThread().getStackTrace()[1].getMethodName());
    }

    /**
     * ImageLineDataset constructor
     *
     * @param path the path of the folder containing the dataset content
     */
    public ImageLinePageDataset(String path) {
        logger.trace(Thread.currentThread().getStackTrace()[1].getMethodName());
    }

    /**
     * ImageLineDataset constructor
     *
     * @param path the path of the folder containing the dataset content
     * @param tag  the tag of truth files
     */
    public ImageLinePageDataset(String path, String tag) {
        logger.trace(Thread.currentThread().getStackTrace()[1].getMethodName());
    }

    /**
     * Convert an XML document into a list of polygon
     *
     * @param xmlDocument the XML ground truth
     * @return a list of polygon
     */
    public static List<Polygon> getLinePolygonsFromXml(Document xmlDocument, boolean processComments) {
        logger.trace(Thread.currentThread().getStackTrace()[1].getMethodName());
        if (xmlDocument == null) {
            logger.error("cannot extract polygons from null xml document");
            return null;
        }

        List<Polygon> groundTruth = new ArrayList<>();

//          // Old ground truth format
//        Element root = xmlDocument.getRootElement();
//        List<Element> textRegions = root.getChild("Page").getChildren("TextRegion");
//
//        for (Element line : textRegions) {
//            if (line.getAttribute("type").getValue().equals("textline")
//                    || processComment && line.getAttribute("type").getValue().equals("comment")) {
//
//                Polygon polygon = new Polygon();
//                List<Element> coords = line.getChild("Coords").getChildren("Point");
//
//                // Get the list of point
//                for (int j = 0; j < coords.size(); j++) {
//                    Element point = coords.get(j);
//                    try {
//                        int x = point.getAttribute("x").getIntValue();
//                        int y = point.getAttribute("y").getIntValue();
//                        polygon.addPoint(x, y);
//                    } catch (DataConversionException e) {
//                        logger.info("ERROR: cannot convert point coordinates");
//                        e.printStackTrace();
//                    }
//                }
//
//                // add the polygon
//                groundTruth.add(polygon);
//            }
//        }

        // PAGE ground truth format
        Element root = xmlDocument.getRootElement();
        Namespace namespace = root.getNamespace();
        List<Element> textRegions = root.getChild("Page", namespace).getChildren("TextRegion", namespace);

        // for every text regions
        for (Element region : textRegions) {
            if (processComments || region.getAttribute("id").getValue().equals("region_textline")) {

                // get all the textlines
                List<Element> lines = region.getChildren("TextLine", namespace);

                // for each line
                for (Element line : lines) {
                    Polygon polygon = new Polygon();

                    // get coordinates string and split by spaces
                    String coords = line.getChild("Coords", namespace).getAttributeValue("points");
                    String coordsArray[] = coords.split("\\s+");

                    // for each 'x,y' string
                    for(String s : coordsArray) {
                        String xy[] = s.split(",");
                        int x = Integer.parseInt(xy[0]);
                        int y = Integer.parseInt(xy[1]);
                        polygon.addPoint(x, y);
                    }

                    // add the polygon
                    groundTruth.add(polygon);
                }
            }
        }

        logger.debug("found " + groundTruth.size() + " polygons in ground truth");
        return groundTruth;
    }

    /**
     * Load output data
     *
     * @param path the output data path
     * @param processComments take comments into account
     * @return the output data object
     */
    public static List<Polygon> readOutputDataFromFile(String path, boolean processComments) {
        logger.trace(Thread.currentThread().getStackTrace()[1].getMethodName());

        // Getting XML doc from PAGE ground truth
        Document xmlDocument = null;
        try {
            xmlDocument = new SAXBuilder().build(new File(path));
        } catch (JDOMException | IOException e) {
            logger.error("cannot open ground truth");
        }

        List<Polygon> lines = getLinePolygonsFromXml(xmlDocument, processComments);

        logger.trace(lines.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(lines)));
        return lines;
    }

    /**
     * Write output data
     *
     * @param data     the data to write
     * @param dataPath the path
     */
    public static void writeOutputDataToFile(List<Polygon> data, String dataPath) {
        logger.trace(Thread.currentThread().getStackTrace()[1].getMethodName());
        //TODO write xml document…
        logger.error("writeOutputDataToFile: unimplemented method…");
    }

    /**
     * Load output data
     *
     * @param path the output data path
     * @param processComments take comments into account
     * @return the output data object
     */
    public List<Polygon> readOutputData(String path, boolean processComments) {
        return readOutputDataFromFile(path, processComments);
    }

    /**
     * Write output data
     *
     * @param data     the data to write
     * @param dataPath the path
     */
    public void writeOutputData(List<Polygon> data, String dataPath) {
        writeOutputDataToFile(data, dataPath);
    }

    /**
     * Get the page polygon from the XML document
     *
     * @param xmlDocument the XML ground truth
     * @return the polygon of the page
     */
    private Polygon getPagePolygonFromXml(Document xmlDocument) {
        logger.trace(Thread.currentThread().getStackTrace()[1].getMethodName());

        Polygon page = null;

        Element root = xmlDocument.getRootElement();
        List<Element> textRegions = root.getChild("Page").getChildren("TextRegion");

        for (Element line : textRegions) {
            if (line.getAttribute("type").getValue().equals("page")) {

                if (page != null) {
                    logger.error("ERROR: more than one page polygon…");
                }
                page = new Polygon();
                List<Element> coords = line.getChild("Coords").getChildren("Point");

                // Get the list of point
                for (int j = 0; j < coords.size(); j++) {
                    Element point = coords.get(j);
                    try {
                        int x = point.getAttribute("x").getIntValue();
                        int y = point.getAttribute("y").getIntValue();
                        page.addPoint(x, y);
                    } catch (DataConversionException e) {
                        logger.info("ERROR: cannot convert point coordinates");
                        e.printStackTrace();
                    }
                }
            }
        }

        logger.trace(page.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(page)));
        return page;
    }

}

