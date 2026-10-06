package com.module.facerecognition.ai;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.springframework.stereotype.Component;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class ScrfdFaceDetector {

    private final OrtEnvironment environment;
    private final OrtSession session;

    private static final int INPUT_WIDTH = 640;
    private static final int INPUT_HEIGHT = 640;

    private static final float SCORE_THRESHOLD = 0.50f;
    private static final float NMS_THRESHOLD = 0.40f;

    /*
     * SCRFD uses these three feature-map strides.
     */
    private static final int[] STRIDES = {
            8, 16, 32
    };

    public ScrfdFaceDetector() throws OrtException {

        environment = OrtEnvironment.getEnvironment();

        OrtSession.SessionOptions options =
                new OrtSession.SessionOptions();

        session = environment.createSession(
                "src/main/resources/models/det_500m.onnx",
                options
        );

        System.out.println("=================================");
        System.out.println("SCRFD MODEL LOADED");
        System.out.println("=================================");
    }

    private void clampDetection(
            FaceDetection detection,
            int imageWidth,
            int imageHeight) {

        Rect2d box =
                detection.getBoundingBox();

        double x1 =
                Math.max(0, box.x);

        double y1 =
                Math.max(0, box.y);

        double x2 =
                Math.min(
                        imageWidth,
                        box.x + box.width
                );

        double y2 =
                Math.min(
                        imageHeight,
                        box.y + box.height
                );

        box.x = x1;
        box.y = y1;
        box.width = Math.max(0, x2 - x1);
        box.height = Math.max(0, y2 - y1);

        for (Point point :
                detection.getLandmarks()) {

            point.x =
                    Math.max(
                            0,
                            Math.min(
                                    imageWidth,
                                    point.x
                            )
                    );

            point.y =
                    Math.max(
                            0,
                            Math.min(
                                    imageHeight,
                                    point.y
                            )
                    );
        }
    }

    private LetterboxResult letterbox(Mat image) {

        double scale = Math.min(
                (double) INPUT_WIDTH / image.cols(),
                (double) INPUT_HEIGHT / image.rows()
        );

        int newWidth =
                (int) Math.round(image.cols() * scale);

        int newHeight =
                (int) Math.round(image.rows() * scale);

        Mat resized = new Mat();

        Imgproc.resize(
                image,
                resized,
                new Size(newWidth, newHeight)
        );

        int padX =
                (INPUT_WIDTH - newWidth) / 2;

        int padY =
                (INPUT_HEIGHT - newHeight) / 2;

        Mat output =
                new Mat(
                        INPUT_HEIGHT,
                        INPUT_WIDTH,
                        image.type(),
                        new Scalar(114, 114, 114)
                );

        resized.copyTo(
                output.submat(
                        padY,
                        padY + newHeight,
                        padX,
                        padX + newWidth
                )
        );

        resized.release();

        return new LetterboxResult(
                output,
                scale,
                padX,
                padY
        );
    }

    public List<FaceDetection> detect(Mat image) throws OrtException {

        // ---------------------------------------------
        // 1. Letterbox image
        // ---------------------------------------------

        LetterboxResult letterbox = letterbox(image);

        Mat resized = letterbox.getImage();

        double scale = letterbox.getScale();
        double padX = letterbox.getPadX();
        double padY = letterbox.getPadY();

        try {

            // ---------------------------------------------
            // 2. Convert BGR image to NCHW float array
            // ---------------------------------------------

            int channelSize =
                    INPUT_WIDTH * INPUT_HEIGHT;

            float[] inputData =
                    new float[3 * channelSize];

            /*
             * resized should be CV_8UC3 because the original
             * image is decoded using IMREAD_COLOR.
             *
             * Instead of calling resized.get(y, x) 409,600 times,
             * read the complete image into one byte array.
             */
            byte[] pixels =
                    new byte[3 * channelSize];

            resized.get(
                    0,
                    0,
                    pixels
            );

            /*
             * OpenCV stores the image as:
             *
             * B G R | B G R | B G R ...
             *
             * We convert it to:
             *
             * B channel
             * G channel
             * R channel
             *
             * which matches your existing NCHW layout.
             */
            for (int i = 0; i < channelSize; i++) {

                int pixelIndex = i * 3;

                float b =
                        (pixels[pixelIndex] & 0xFF);

                float g =
                        (pixels[pixelIndex + 1] & 0xFF);

                float r =
                        (pixels[pixelIndex + 2] & 0xFF);

                inputData[i] =
                        (b - 127.5f) / 128.0f;

                inputData[channelSize + i] =
                        (g - 127.5f) / 128.0f;

                inputData[2 * channelSize + i] =
                        (r - 127.5f) / 128.0f;
            }

            // ---------------------------------------------
            // 3. Create ONNX input tensor
            // ---------------------------------------------

            long[] shape = {
                    1,
                    3,
                    INPUT_HEIGHT,
                    INPUT_WIDTH
            };

            FloatBuffer buffer =
                    FloatBuffer.wrap(inputData);

            try (OnnxTensor inputTensor =
                         OnnxTensor.createTensor(
                                 environment,
                                 buffer,
                                 shape
                         )) {

                // ---------------------------------------------
                // 4. Run model
                // ---------------------------------------------

                Map<String, OnnxTensor> inputs =
                        Map.of(
                                "input.1",
                                inputTensor
                        );

                try (OrtSession.Result result =
                             session.run(inputs)) {

                    // ---------------------------------------------
                    // 5. Decode model outputs
                    // ---------------------------------------------

                    List<FaceDetection> detections =
                            decodeResults(result);

                    // ---------------------------------------------
                    // 6. Convert coordinates back
                    // ---------------------------------------------

                    for (FaceDetection detection : detections) {

                        Rect2d box =
                                detection.getBoundingBox();

                        box.x =
                                (box.x - padX) / scale;

                        box.y =
                                (box.y - padY) / scale;

                        box.width =
                                box.width / scale;

                        box.height =
                                box.height / scale;

                        for (Point point :
                                detection.getLandmarks()) {

                            point.x =
                                    (point.x - padX) / scale;

                            point.y =
                                    (point.y - padY) / scale;
                        }

                        clampDetection(
                                detection,
                                image.cols(),
                                image.rows()
                        );
                    }

                    // ---------------------------------------------
                    // 7. NMS
                    // ---------------------------------------------

                    return FaceNms.apply(
                            detections,
                            NMS_THRESHOLD
                    );
                }
            }

        } finally {

            // Release 640x640 letterboxed Mat
            resized.release();
        }
    }

    private List<FaceDetection> decodeResults(
            OrtSession.Result result)
            throws OrtException {

        List<FaceDetection> detections =
                new ArrayList<>();

        /*
         * Output order from your model:
         *
         * 0 = scores stride 8
         * 1 = scores stride 16
         * 2 = scores stride 32
         *
         * 3 = boxes stride 8
         * 4 = boxes stride 16
         * 5 = boxes stride 32
         *
         * 6 = landmarks stride 8
         * 7 = landmarks stride 16
         * 8 = landmarks stride 32
         */

        for (int scale = 0; scale < 3; scale++) {

            float[][] scores =
                    (float[][])
                            result
                                    .get(scale)
                                    .getValue();

            float[][] boxes =
                    (float[][])
                            result
                                    .get(scale + 3)
                                    .getValue();

            float[][] landmarks =
                    (float[][])
                            result
                                    .get(scale + 6)
                                    .getValue();

            int stride =
                    STRIDES[scale];

            decodeScale(
                    scores,
                    boxes,
                    landmarks,
                    stride,
                    detections
            );
        }

        return detections;
    }



    private void decodeScale(
            float[][] scores,
            float[][] boxes,
            float[][] landmarks,
            int stride,
            List<FaceDetection> detections) {

        int featureWidth =
                INPUT_WIDTH / stride;

        /*
         * Your model has TWO anchors
         * per feature-map location.
         */
        int anchorsPerLocation = 2;

        for (int i = 0; i < scores.length; i++) {

            float score =
                    scores[i][0];

            if (score < SCORE_THRESHOLD) {
                continue;
            }

            /*
             * Convert flattened anchor index
             * into feature-map position.
             */
            int location =
                    i / anchorsPerLocation;

//            int anchorIndex =
//                    i % anchorsPerLocation;

            int gridX =
                    location % featureWidth;

            int gridY =
                    location / featureWidth;

            /*
             * Center of the anchor.
             */
            float anchorX =
                    gridX * stride;

            float anchorY =
                    gridY * stride;

            /*
             * SCRFD box decoding.
             */
            float x1 =
                    anchorX -
                            boxes[i][0] * stride;

            float y1 =
                    anchorY -
                            boxes[i][1] * stride;

            float x2 =
                    anchorX +
                            boxes[i][2] * stride;

            float y2 =
                    anchorY +
                            boxes[i][3] * stride;

            /*
             * Prevent invalid boxes.
             */
            if (x2 <= x1 || y2 <= y1) {
                continue;
            }

            Point[] points =
                    new Point[5];

            for (int j = 0; j < 5; j++) {

                float lx =
                        anchorX +
                                landmarks[i][j * 2]
                                        * stride;

                float ly =
                        anchorY +
                                landmarks[i][j * 2 + 1]
                                        * stride;

                points[j] =
                        new Point(lx, ly);
            }

            detections.add(
                    new FaceDetection(
                            new Rect2d(
                                    x1,
                                    y1,
                                    x2 - x1,
                                    y2 - y1
                            ),
                            score,
                            points
                    )
            );
        }
    }
}