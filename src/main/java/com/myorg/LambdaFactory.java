package com.myorg;

import software.amazon.awscdk.Duration;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.Runtime;
import software.constructs.Construct;

import java.util.Map;

public class LambdaFactory {

    public static Function createJavaLambda(Construct scope, String id, String handlerMethod, Map<String, String> envVars) {
        return Function.Builder.create(scope, id)
                .runtime(Runtime.JAVA_17)
                .handler(handlerMethod)
                .code(Code.fromAsset("target/therapy_journal_cdk-0.1.jar"))
                .environment(envVars)
                .memorySize(1024)
                .timeout(Duration.seconds(30))
                .build();
    }
}
