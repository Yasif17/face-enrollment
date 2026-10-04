package com.module.facerecognition.ai;


import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.springframework.stereotype.Component;

import java.nio.FloatBuffer;
import java.util.Collections;

@Component
public class ArcFaceEmbedder {

    private static final String MODEL_PATH =
            "src/main/resources/models/w600k_r50.onnx";

    private static final int INPUT_SIZE = 112;

    private final OrtEnvironment environment;
    private final OrtSession session;

    public ArcFaceEmbedder() throws OrtException {

        environment =
                OrtEnvironment.getEnvironment();

        OrtSession.SessionOptions options =
                new OrtSession.SessionOptions();

        session =
                environment.createSession(
                        MODEL_PATH,
                        options
                );

        options.close();

        System.out.println(
                "ArcFace model loaded successfully"
        );
    }

    public float[] generateEmbedding(Mat alignedFace)
            throws OrtException {

        if (alignedFace.empty()) {
            throw new IllegalArgumentException(
                    "Aligned face is empty"
            );
        }

        if (alignedFace.cols() != INPUT_SIZE ||
                alignedFace.rows() != INPUT_SIZE) {

            throw new IllegalArgumentException(
                    "Expected 112x112 face image"
            );
        }

        /*
         * Convert image to FLOAT32.
         */
        Mat floatImage = new Mat();

        alignedFace.convertTo(
                floatImage,
                CvType.CV_32FC3
        );

        try {

            /*
             * OpenCV image is BGR.
             *
             * ArcFace expects RGB.
             */
            Imgproc.cvtColor(
                    floatImage,
                    floatImage,
                    Imgproc.COLOR_BGR2RGB
            );

            /*
             * Create NCHW tensor:
             *
             * [1, 3, 112, 112]
             */
            float[] inputData =
                    new float[
                            1
                                    * 3
                                    * INPUT_SIZE
                                    * INPUT_SIZE
                            ];

            int channelSize =
                    INPUT_SIZE * INPUT_SIZE;

            float[] pixel =
                    new float[3];

            for (int y = 0; y < INPUT_SIZE; y++) {

                for (int x = 0; x < INPUT_SIZE; x++) {

                    double[] values =
                            floatImage.get(y, x);

                    /*
                     * ArcFace normalization:
                     *
                     * (pixel - 127.5) / 127.5
                     */
                    float r =
                            (float)
                                    ((values[0] - 127.5)
                                            / 127.5);

                    float g =
                            (float)
                                    ((values[1] - 127.5)
                                            / 127.5);

                    float b =
                            (float)
                                    ((values[2] - 127.5)
                                            / 127.5);

                    int index =
                            y * INPUT_SIZE + x;

                    /*
                     * NCHW
                     */
                    inputData[index] =
                            r;

                    inputData[
                            channelSize + index
                            ] = g;

                    inputData[
                            2 * channelSize + index
                            ] = b;
                }
            }

            long[] shape = {
                    1,
                    3,
                    INPUT_SIZE,
                    INPUT_SIZE
            };

            FloatBuffer buffer =
                    FloatBuffer.wrap(inputData);

            OnnxTensor inputTensor =
                    OnnxTensor.createTensor(
                            environment,
                            buffer,
                            shape
                    );

            try {

                OrtSession.Result result =
                        session.run(
                                Collections.singletonMap(
                                        "input.1",
                                        inputTensor
                                )
                        );

                try {

                    float[][] output =
                            (float[][])
                                    result
                                            .get(0)
                                            .getValue();

                    float[] embedding =
                            output[0];

                    /*
                     * L2 normalize embedding.
                     */
                    return normalize(embedding);

                } finally {
                    result.close();
                }

            } finally {
                inputTensor.close();
            }

        } finally {
            floatImage.release();
        }
    }

    private float[] normalize(
            float[] embedding) {

        double sum = 0.0;

        for (float value : embedding) {
            sum += value * value;
        }

        double norm =
                Math.sqrt(sum);

        if (norm == 0.0) {
            throw new IllegalStateException(
                    "Embedding norm is zero"
            );
        }

        float[] normalized =
                new float[embedding.length];

        for (int i = 0;
             i < embedding.length;
             i++) {

            normalized[i] =
                    (float)
                            (embedding[i] / norm);
        }

        return normalized;
    }
}