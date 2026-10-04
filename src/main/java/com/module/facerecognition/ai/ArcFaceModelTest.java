package com.module.facerecognition.ai;

import ai.onnxruntime.*;

public class ArcFaceModelTest {

    public static void main(String[] args) throws Exception {

        OrtEnvironment environment =
                OrtEnvironment.getEnvironment();

        OrtSession.SessionOptions options =
                new OrtSession.SessionOptions();

        OrtSession session =
                environment.createSession(
                        "src/main/resources/models/w600k_r50.onnx",
                        options
                );

        System.out.println("ARCFACE MODEL LOADED");
        System.out.println("=================================");

        session.getInputInfo().forEach(
                (name, info) -> {

                    System.out.println(
                            "INPUT : "
                                    + name
                                    + " -> "
                                    + info
                    );
                }
        );

        session.getOutputInfo().forEach(
                (name, info) -> {

                    System.out.println(
                            "OUTPUT: "
                                    + name
                                    + " -> "
                                    + info
                    );
                }
        );

        session.close();
        options.close();
        environment.close();
    }
}
