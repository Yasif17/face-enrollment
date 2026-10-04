package com.module.facerecognition.ai;


import org.opencv.core.Rect2d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class FaceNms {

    private FaceNms() {
    }

    public static List<FaceDetection> apply(
            List<FaceDetection> detections,
            float iouThreshold) {

        // Highest confidence first
        detections.sort(
                Comparator.comparing(
                        FaceDetection::getConfidence
                ).reversed()
        );

        List<FaceDetection> selected =
                new ArrayList<>();

        boolean[] suppressed =
                new boolean[detections.size()];

        for (int i = 0; i < detections.size(); i++) {

            if (suppressed[i]) {
                continue;
            }

            FaceDetection current =
                    detections.get(i);

            selected.add(current);

            for (int j = i + 1; j < detections.size(); j++) {

                if (suppressed[j]) {
                    continue;
                }

                double iou =
                        calculateIoU(
                                current.getBoundingBox(),
                                detections.get(j).getBoundingBox()
                        );

                if (iou > iouThreshold) {
                    suppressed[j] = true;
                }
            }
        }

        return selected;
    }

    private static double calculateIoU(
            Rect2d a,
            Rect2d b) {

        double ax1 = a.x;
        double ay1 = a.y;
        double ax2 = a.x + a.width;
        double ay2 = a.y + a.height;

        double bx1 = b.x;
        double by1 = b.y;
        double bx2 = b.x + b.width;
        double by2 = b.y + b.height;

        double intersectionX1 =
                Math.max(ax1, bx1);

        double intersectionY1 =
                Math.max(ay1, by1);

        double intersectionX2 =
                Math.min(ax2, bx2);

        double intersectionY2 =
                Math.min(ay2, by2);

        double intersectionWidth =
                Math.max(
                        0,
                        intersectionX2 - intersectionX1
                );

        double intersectionHeight =
                Math.max(
                        0,
                        intersectionY2 - intersectionY1
                );

        double intersectionArea =
                intersectionWidth *
                        intersectionHeight;

        double areaA =
                a.width * a.height;

        double areaB =
                b.width * b.height;

        double unionArea =
                areaA + areaB - intersectionArea;

        if (unionArea <= 0) {
            return 0;
        }

        return intersectionArea / unionArea;
    }
}