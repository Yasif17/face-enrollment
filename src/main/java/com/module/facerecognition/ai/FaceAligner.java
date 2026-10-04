package com.module.facerecognition.ai;


import org.opencv.calib3d.Calib3d;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.springframework.stereotype.Component;

@Component
public class FaceAligner {

    private static final int OUTPUT_SIZE = 112;

    private static final Point[] REFERENCE_POINTS = {
            new Point(38.2946, 51.6963),
            new Point(73.5318, 51.5014),
            new Point(56.0252, 71.7366),
            new Point(41.5493, 92.3655),
            new Point(70.7299, 92.2041)
    };

    public Mat align(
            Mat image,
            Point[] landmarks) {

        if (landmarks == null || landmarks.length != 5) {
            throw new IllegalArgumentException(
                    "Exactly 5 landmarks are required"
            );
        }

        MatOfPoint2f source =
                new MatOfPoint2f(landmarks);

        MatOfPoint2f destination =
                new MatOfPoint2f(REFERENCE_POINTS);

        Mat transform =
                Calib3d.estimateAffinePartial2D(
                        source,
                        destination
                );

        if (transform.empty()) {
            source.release();
            destination.release();

            throw new IllegalStateException(
                    "Could not calculate face alignment transform"
            );
        }

        Mat aligned = new Mat();

        Imgproc.warpAffine(
                image,
                aligned,
                transform,
                new Size(
                        OUTPUT_SIZE,
                        OUTPUT_SIZE
                ),
                Imgproc.INTER_LINEAR
        );

        source.release();
        destination.release();
        transform.release();

        return aligned;
    }
}