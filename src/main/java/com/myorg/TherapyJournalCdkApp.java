package com.myorg;

import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.StackProps;

import java.util.Arrays;

public class TherapyJournalCdkApp {
    public static void main(final String[] args) {
        App app = new App();

        new TherapyJournalCdkStack(app, "TherapyJournalCdkStack", StackProps.builder()
                .build());

        app.synth();
    }
}

