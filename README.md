# LineSegmentationEvaluator
Line Segmentation Evaluator for the ICDAR2017 competition on Layout Analysis for Challenging Medieval Manuscripts

Minimal usage: `java -jar LineSegmentationEvaluator.jar -image_gt.png page_gt.xml page_to_evaluate.xml`

Parameters list: utility-name
```
 -igt,--imageGroundTruth <arg>   Ground Truth image at pixel-level (not the original image)
 -xgt,--xmlGroundTruth <arg>     Ground Truth XML
 -xp,--xmlPrediction <arg>       Prediction XML
 -overlap <arg>                  (Optional) Original image, to be overlapped with the results visualization
 -mt,--matchingThreshold <arg>   (Optional) Matching threshold for detected lines  
 -out,--outputPath <arg>         (Optional) Output path (relative to prediction input path)
 -csv                            (Optional) (Flag) Save the results to a CSV file
 ```
**Note:** this also output a human-friendly visualization of the results next to the `page_to_evaluate.xml` and, to enable deeper analysis, it can be overlapped to the original image if provided with the parameter `-overlap`.  

##Visualization of the results


![](b.png?=250x)

![Alt text](examples/a.png?raw=true)

hhh


![Alt text](examples/example_visualization_zoom.png?raw=true)

##Overlap of the results



(where green is true positive, red is false positive, blue false negative and yellow is mixed mistake).
