package com.module.facerecognition.services;

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
public class FaceAntiSpoofService {

    private static final String MODEL_PATH =
            "src/main/resources/models/face_antispoof_minifasnet_v2se.onnx";

    private static final int INPUT_SIZE = 128;

    private final OrtEnvironment environment;
    private final OrtSession session;

    public FaceAntiSpoofService() throws OrtException {

        environment = OrtEnvironment.getEnvironment();

        OrtSession.SessionOptions options =
                new OrtSession.SessionOptions();

        session = environment.createSession(
                MODEL_PATH,
                options
        );

        options.close();

        System.out.println(
                "MiniFASNet anti-spoof model loaded successfully"
        );
    }

    public float[] predict(Mat face) throws OrtException {

        if (face == null || face.empty()) {
            throw new IllegalArgumentException(
                    "Face image is empty"
            );
        }

        Mat resized = new Mat();

        try {

            Imgproc.resize(
                    face,
                    resized,
                    new org.opencv.core.Size(
                            INPUT_SIZE,
                            INPUT_SIZE
                    )
            );

            Mat floatImage = new Mat();

            try {

                resized.convertTo(
                        floatImage,
                        CvType.CV_32FC3
                );

                /*
                 * OpenCV = BGR
                 * MiniFASNet = RGB
                 */
                Imgproc.cvtColor(
                        floatImage,
                        floatImage,
                        Imgproc.COLOR_BGR2RGB
                );

                float[] inputData =
                        new float[
                                3 * INPUT_SIZE * INPUT_SIZE
                                ];

                int channelSize =
                        INPUT_SIZE * INPUT_SIZE;

                for (int y = 0; y < INPUT_SIZE; y++) {

                    for (int x = 0; x < INPUT_SIZE; x++) {

                        double[] values =
                                floatImage.get(y, x);

                        /*
                         * RGB
                         */
                        float r = (float) values[0] / 255.0f;
                        float g = (float) values[1] / 255.0f;
                        float b = (float) values[2] / 255.0f;

                        int index =
                                y * INPUT_SIZE + x;

                        /*
                         * NCHW
                         */
                        inputData[index] = r;

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
                                            "input",
                                            inputTensor
                                    )
                            );

                    try {

                        float[][] output =
                                (float[][])
                                        result
                                                .get(0)
                                                .getValue();

                        return output[0];

                    } finally {

                        result.close();
                    }

                } finally {

                    inputTensor.close();
                }

            } finally {

                floatImage.release();
            }

        } finally {

            resized.release();
        }
    }
}