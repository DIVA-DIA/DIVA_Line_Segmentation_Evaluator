/*
 * Copyright (c) 2016 UniFR
 * University of Fribourg, Switzerland.
 */

package ch.unifr.experimenter.database;

import org.apache.log4j.Logger;
import org.jdom2.*;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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
     * TASKTAG should be "Coords" for line segmentation
     * and "Baseline" for baseline extraction
     */
    protected static String TASKTAG = "Coords";

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
    public static List<Polygon> getPolygonFromXml(Document xmlDocument) {
        logger.trace(Thread.currentThread().getStackTrace()[1].getMethodName());
        if (xmlDocument == null) {
            logger.error("cannot extract polygons from null xml document");
            return null;
        }

        List<Polygon> polygons = new ArrayList<>();

        Element root = xmlDocument.getRootElement();
        Namespace namespace = root.getNamespace();
        Element page = root.getChild("Page", namespace);
        List<Element> textRegions = page.getChildren("TextRegion", namespace);

//        // Parsing structure of Kai Page Xml…
//        for (Element line : textRegions) {
//            if (line.getAttribute("type").getValue().equals("textline")) {
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
//                polygons.add(polygon);
//            }
//        }

        // Parsing structure of standard Page Xml…
        for (Element region : textRegions) {

            // Find the text region corresponding to main text area
            if (region.getAttribute("id").getValue().equals("region_textline")) {

                // Get all the text lines
                List<Element> textLines = region.getChildren("TextLine", namespace);
                for (Element line : textLines) {

                    Polygon polygon = new Polygon();

                    // Get the list of point and split it
                    String coordString = line.getChild(TASKTAG, namespace).getAttributeValue("points");
                    String[] coords = coordString.split(" ");

                    // For each point
                    for (int j = 0; j < coords.length; j++) {

                        // Split x and y
                        String[] c = coords[j].split(",");
                        int x = Integer.parseInt(c[0]);
                        int y = Integer.parseInt(c[1]);

                        // Add point
                        polygon.addPoint(x, y);
                    }

                    // add the polygon
                    polygons.add(polygon);
                }
            }
        }

        logger.debug("found " + polygons.size() + " polygons in XML document");
        return polygons;
    }

    /**
     * Load output data
     *
     * @param path the output data path
     * @return the output data object
     */
    public static List<Polygon> readOutputDataFromFile(String path) {
        logger.trace(Thread.currentThread().getStackTrace()[1].getMethodName());

        // Getting XML doc from PAGE ground truth
        Document xmlDocument = null;
        try {
            xmlDocument = new SAXBuilder().build(new File(path));
        } catch (JDOMException | IOException e) {
            logger.error("cannot open file: " + path);
            if (logger.isDebugEnabled()) {
                e.printStackTrace();
            }
        }

        List<Polygon> lines = getPolygonFromXml(xmlDocument);

        String classname = (lines == null) ? "null" : lines.getClass().getName();
        logger.trace(classname + "@" + Integer.toHexString(System.identityHashCode(lines)));
        return lines;
    }

    /**
     * Write output data
     *
     * @param data       the data to write
     * @param outputPath the path
     */
    public static void writeOutputDataToFile(List<Polygon> data, String outputPath) {
        logger.trace(Thread.currentThread().getStackTrace()[1].getMethodName());

        // Getting XML doc with PAGE ground truth from polygons
        Document xmlDocument = buildXmlFromPolygons(data, outputPath);

        // Write file
        try {
            File directory = new File(outputPath).getParentFile();
            if (directory != null && !directory.exists()) {
                directory.mkdirs();
            }

            FileWriter writer = new FileWriter(outputPath);
            XMLOutputter outputter = new XMLOutputter();
            outputter.setFormat(Format.getPrettyFormat());
            outputter.output(xmlDocument, writer);
            writer.close(); // close writer

            logger.debug("wrote output to: " + outputPath);
        } catch (IOException io) {
            logger.error(io.getMessage());
        }
    }

    /**
     * Build XML document in PAGE format from polygons
     *
     * @param data the list of polygons
     * @return the XML document in PAGE format
     */
    private static Document buildXmlFromPolygons(List<Polygon> data, String outputPath) {
        logger.trace(Thread.currentThread().getStackTrace()[1].getMethodName());

        // information
        String creator = "The Experimenter";
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS");
        String created = dateFormat.format(new Date());
        String imageWidth = "0";
        String imageHeight = "0";
        String imageFilename;

        //ugly way to get the original image name //FIXME
        imageFilename = outputPath.substring(outputPath.lastIndexOf('/') + 1);
        imageFilename = imageFilename.substring(0, imageFilename.lastIndexOf('.')); //remove extension
        if (imageFilename.lastIndexOf('.') > -1) {
            imageFilename = imageFilename.substring(0, imageFilename.lastIndexOf('.')); //remove method name
        }
        imageFilename += ".jpg";

        //ugly way to get the original image path //FIXME
        String inputPath = outputPath;
        if (inputPath.lastIndexOf('/') > -1) {
            inputPath = inputPath.substring(0, inputPath.lastIndexOf('/')); //remove filename
            inputPath = inputPath.substring(0, inputPath.lastIndexOf('/')); //remove method directory
            inputPath = inputPath.substring(0, inputPath.lastIndexOf('/')); //remove output directory
        }
        inputPath += "/" + imageFilename;

        try {
            BufferedImage image = ImageIO.read(new File(inputPath));
            imageWidth = "" + image.getHeight(); // image appears to be loaded rotated…
            imageHeight = "" + image.getWidth(); //TODO look for better fix!
        } catch (IOException e) {
            logger.warn(e.getMessage() + ", image dimensions will not be set");
        }

        // XML root
        Element root = new Element("PcGts");
        Namespace namespace = Namespace.getNamespace("http://schema.primaresearch.org/PAGE/gts/pagecontent/2013-07-15");
        root.setNamespace(namespace);
        root.addNamespaceDeclaration(Namespace.getNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance"));
        root.setAttribute("schemaLocation", "http://schema.primaresearch.org/PAGE/gts/pagecontent/2013-07-15 http://schema.primaresearch.org/PAGE/gts/pagecontent/2013-07-15/pagecontent.xsd", Namespace.getNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance"));
        Document document = new Document(root);

        // metadata
        Element metadata = new Element("Metadata");
        root.addContent(metadata);
        metadata.setNamespace(namespace);
        metadata.addContent(new Element("Creator").setText(creator).setNamespace(namespace));
        metadata.addContent(new Element("Created").setText(created).setNamespace(namespace));
        metadata.addContent(new Element("LastChange").setText(created).setNamespace(namespace));

        // page
        Element page = new Element("Page");
        root.addContent(page);
        page.setNamespace(namespace);
        page.setAttribute("imageWidth", imageHeight);
        page.setAttribute("imageHeight", imageWidth);
        page.setAttribute("imageFilename", imageFilename);

        // text region
        Element textRegion = new Element("TextRegion");
        page.addContent(textRegion);
        textRegion.setNamespace(namespace);
        textRegion.setAttribute("id", "region_textline");
        textRegion.setAttribute("custom", "0");

        Element coordsRegion = new Element("Coords");
        textRegion.addContent(coordsRegion);
        coordsRegion.setNamespace(namespace);
        int xmin = Integer.MAX_VALUE;
        int xmax = Integer.MIN_VALUE;
        int ymin = Integer.MAX_VALUE;
        int ymax = Integer.MIN_VALUE;

        Element textEquivRegion = new Element("TextEquiv", namespace);
        textRegion.addContent(textEquivRegion);
        textEquivRegion.addContent(new Element("Unicode", namespace));

        // for every polygon
        for (int i = 0; i < data.size(); i++) {
            Polygon polygon = data.get(i);

            // build the coordinate list
            String coordPolygon = "";
            for (int j = 0; j < polygon.npoints; j++) {
                coordPolygon += polygon.xpoints[j] + "," + polygon.ypoints[j] + " ";
            }
            coordPolygon = coordPolygon.substring(0, coordPolygon.length() - 1);

            // update textRegion position
            Rectangle bounds = polygon.getBounds();
            xmin = Math.min(xmin, (int) bounds.getMinX());
            xmax = Math.max(xmax, (int) bounds.getMaxX());
            ymin = Math.min(ymin, (int) bounds.getMinY());
            ymax = Math.max(ymax, (int) bounds.getMaxY());

            // add text line to xml
            Element textLine = new Element("TextLine");
            textRegion.addContent(textLine);
            textLine.setNamespace(namespace);
            textLine.setAttribute("id", "textline_" + i);
            textLine.setAttribute("custom", "0");

            // add coords to text line
            Element coordsLine = new Element(TASKTAG);
            textLine.addContent(coordsLine);
            coordsLine.setNamespace(namespace);
            coordsLine.setAttribute("points", coordPolygon);

//            Element baseline = new Element("Baseline");
//            textLine.addContent(baseline);
//            coordsLine.setAttribute(new Attribute("points", "?,? ?,?"));

            // add things for compliance
            Element textEquivLine = new Element("TextEquiv");
            textLine.addContent(textEquivLine);
            textEquivLine.setNamespace(namespace);
            textEquivLine.addContent(new Element("Unicode").setNamespace(namespace));
        }

        // set the textRegion position
        String coordRegion = xmin + "," + ymin + " ";
        coordRegion += xmax + "," + ymin + " ";
        coordRegion += xmax + "," + ymax + " ";
        coordRegion += xmin + "," + ymax + " ";
        coordsRegion.setAttribute("points", coordRegion);

        return document;
    }

    /**
     * Load output data
     *
     * @param path the output data path
     * @return the output data object
     */
    public List<Polygon> readOutputData(String path) {
        return readOutputDataFromFile(path);
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
    @Deprecated
    private Polygon getPagePolygonFromXml(Document xmlDocument) { //TODO fuse with method above
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
                List<Element> coords = line.getChild(TASKTAG).getChildren("Point");

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

    public String getDefaultTruthExt() {
        return "xml";
    }

}

