package com.module.facerecognition.services;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import com.module.facerecognition.ai.FaceDetection;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.Map;

/** Runs the OCEC open/closed eye classifier on eye crops around SCRFD landmarks. */
@Service
public class EyeStateModelService {

    private static final String MODEL_PATH =
            "src/main/resources/models/ocec_p.onnx";
    private static final int INPUT_WIDTH = 40;
    private static final int INPUT_HEIGHT = 24;

    private final OrtEnvironment environment;
    private final OrtSession session;
    private final String inputName;
    private final String outputName;

    public EyeStateModelService() throws OrtException {
        environment = OrtEnvironment.getEnvironment();
        OrtSession.SessionOptions options = new OrtSession.SessionOptions();
        try {
            session = environment.createSession(MODEL_PATH, options);
        } finally {
            options.close();
        }
        inputName = session.getInputInfo().keySet().iterator().next();
        outputName = session.getOutputInfo().keySet().iterator().next();
        System.out.println("OCEC eye-state ONNX model loaded successfully");
    }

    public EyeProbabilities classify(
            MultipartFile image,
            FaceDetection face) throws IOException, OrtException {

        Point[] landmarks = face.getLandmarks();
        if (landmarks == null || landmarks.length < 2) {
            throw new IllegalArgumentException("Eye landmarks are unavailable");
        }

        MatOfByte encoded = new MatOfByte(image.getBytes());
        Mat frame;
        try {
            frame = Imgcodecs.imdecode(encoded, Imgcodecs.IMREAD_COLOR);
        } finally {
            encoded.release();
        }
        if (frame.empty()) {
            frame.release();
            throw new IOException("Could not decode eye-state frame");
        }

        try {
            double eyeDistance = Math.hypot(
                    landmarks[1].x - landmarks[0].x,
                    landmarks[1].y - landmarks[0].y
            );
            if (eyeDistance < 8) {
                throw new IllegalArgumentException("Face is too small for eye-state detection");
            }

            double cropWidth = eyeDistance * 0.58;
            double cropHeight = eyeDistance * 0.34;

            float leftOpen = classifyEye(
                    frame,
                    landmarks[0],
                    cropWidth,
                    cropHeight
            );
            float rightOpen = classifyEye(
                    frame,
                    landmarks[1],
                    cropWidth,
                    cropHeight
            );
            return new EyeProbabilities(leftOpen, rightOpen);
        } finally {
            frame.release();
        }
    }

    private float classifyEye(
            Mat frame,
            Point center,
            double cropWidth,
            double cropHeight) throws OrtException {

        int x1 = Math.max(0, (int) Math.floor(center.x - cropWidth / 2));
        int y1 = Math.max(0, (int) Math.floor(center.y - cropHeight / 2));
        int x2 = Math.min(frame.cols(), (int) Math.ceil(center.x + cropWidth / 2));
        int y2 = Math.min(frame.rows(), (int) Math.ceil(center.y + cropHeight / 2));
        if (x2 <= x1 || y2 <= y1) {
            throw new IllegalArgumentException("Eye crop falls outside the camera frame");
        }

        Mat eye = new Mat(frame, new Rect(x1, y1, x2 - x1, y2 - y1));
        Mat resized = new Mat();
        Mat rgb = new Mat();
        Mat floatRgb = new Mat();
        try {
            Imgproc.resize(eye, resized, new Size(INPUT_WIDTH, INPUT_HEIGHT));
            Imgproc.cvtColor(resized, rgb, Imgproc.COLOR_BGR2RGB);
            rgb.convertTo(floatRgb, CvType.CV_32FC3, 1.0 / 255.0);

            float[] nchw = new float[3 * INPUT_WIDTH * INPUT_HEIGHT];
            int channelSize = INPUT_WIDTH * INPUT_HEIGHT;
            for (int y = 0; y < INPUT_HEIGHT; y++) {
                for (int x = 0; x < INPUT_WIDTH; x++) {
                    double[] pixel = floatRgb.get(y, x);
                    int index = y * INPUT_WIDTH + x;
                    nchw[index] = (float) pixel[0];
                    nchw[channelSize + index] = (float) pixel[1];
                    nchw[2 * channelSize + index] = (float) pixel[2];
                }
            }

            try (OnnxTensor input = OnnxTensor.createTensor(
                    environment,
                    FloatBuffer.wrap(nchw),
                    new long[]{1, 3, INPUT_HEIGHT, INPUT_WIDTH}
            ); OrtSession.Result result = session.run(Map.of(inputName, input))) {
                Object output = result.get(outputName).orElseThrow().getValue();
                if (output instanceof float[] values && values.length > 0) {
                    return clampProbability(values[0]);
                }
                if (output instanceof float[][] values && values.length > 0 && values[0].length > 0) {
                    return clampProbability(values[0][0]);
                }
                throw new IllegalStateException("Unexpected OCEC output shape");
            }
        } finally {
            eye.release();
            resized.release();
            rgb.release();
            floatRgb.release();
        }
    }

    private float clampProbability(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }

    public record EyeProbabilities(float leftOpen, float rightOpen) {
    }
}
