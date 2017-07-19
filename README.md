# LineSegmentationEvaluator
Line Segmentation Evaluator for the ICDAR2017 competition on Layout Analysis for Challenging Medieval Manuscripts

Usage: `java -jar LineSegmentationEvaluator.jar image_gt.jpg page_gt.xml page_to_evaluate.xml [results.csv] [iu_matching_threshold] [take_comment_lines_boolean]`

Note: now this also output a "evaluation image" next to the `page_to_evaluate.xml` (where green is true positive, red is false positive, blue false negative and black true negative).
