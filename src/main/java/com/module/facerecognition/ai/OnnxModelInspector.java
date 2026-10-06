package com.module.facerecognition.ai;

import ai.onnxruntime.NodeInfo;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;

import java.util.Map;

public class OnnxModelInspector {

    public static void main(String[] args) throws OrtException {

        String modelPath =
                "src/main/resources/models/face_antispoof_minifasnet_v2se.onnx";

        OrtEnvironment environment =
                OrtEnvironment.getEnvironment();

        OrtSession.SessionOptions options =
                new OrtSession.SessionOptions();

        OrtSession session =
                environment.createSession(
                        modelPath,
                        options
                );

        System.out.println("=================================");
        System.out.println("MODEL: det_500m.onnx");
        System.out.println("=================================");

        System.out.println("\nINPUTS:");

        for (Map.Entry<String, NodeInfo> entry :
                session.getInputInfo().entrySet()) {

            System.out.println(
                    "Name: " + entry.getKey()
            );

            System.out.println(
                    "Info: " + entry.getValue().getInfo()
            );

            System.out.println();
        }

        System.out.println("\nOUTPUTS:");

        for (Map.Entry<String, NodeInfo> entry :
                session.getOutputInfo().entrySet()) {

            System.out.println(
                    "Name: " + entry.getKey()
            );

            System.out.println(
                    "Info: " + entry.getValue().getInfo()
            );

            System.out.println();
        }

        session.close();
        options.close();

        System.out.println("=================================");
        System.out.println("Inspection complete");
        System.out.println("=================================");
    }
}