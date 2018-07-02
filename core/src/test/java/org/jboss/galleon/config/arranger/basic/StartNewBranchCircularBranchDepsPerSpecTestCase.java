/*
 * Copyright 2016-2018 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.galleon.config.arranger.basic;

import org.jboss.galleon.universe.galleon1.LegacyGalleon1Universe;
import org.jboss.galleon.universe.FeaturePackLocation.FPID;
import org.jboss.galleon.ProvisioningException;
import org.jboss.galleon.config.ConfigModel;
import org.jboss.galleon.config.FeatureConfig;
import org.jboss.galleon.config.FeaturePackConfig;
import org.jboss.galleon.creator.FeaturePackCreator;
import org.jboss.galleon.plugin.ProvisionedConfigHandler;
import org.jboss.galleon.runtime.ResolvedFeatureId;
import org.jboss.galleon.spec.FeatureAnnotation;
import org.jboss.galleon.spec.FeatureParameterSpec;
import org.jboss.galleon.spec.FeatureReferenceSpec;
import org.jboss.galleon.spec.FeatureSpec;
import org.jboss.galleon.state.ProvisionedFeaturePack;
import org.jboss.galleon.state.ProvisionedState;
import org.jboss.galleon.test.PmInstallFeaturePackTestBase;
import org.jboss.galleon.test.util.TestConfigHandlersProvisioningPlugin;
import org.jboss.galleon.test.util.TestProvisionedConfigHandler;
import org.jboss.galleon.xml.ProvisionedConfigBuilder;
import org.jboss.galleon.xml.ProvisionedFeatureBuilder;

/**
 *
 * @author Alexey Loubyansky
 */
public class StartNewBranchCircularBranchDepsPerSpecTestCase extends PmInstallFeaturePackTestBase {

    private static final FPID FP1_GAV = LegacyGalleon1Universe.newFPID("org.jboss.pm.test:fp1", "1", "1.0.0.Final");

    public static class ConfigHandler extends TestProvisionedConfigHandler {
        @Override
        protected boolean loggingEnabled() {
            return false;
        }
        @Override
        protected boolean branchesEnabled() {
            return true;
        }
        @Override
        protected String[] initEvents() throws Exception {
            return new String[] {
                    branchStartEvent(),
                    batchStartEvent(),
                    featurePackEvent(FP1_GAV),
                    specEvent("specA"),
                    featureEvent(ResolvedFeatureId.create(FP1_GAV, "specA", "a", "1")),
                    specEvent("specA1"),
                    featureEvent(ResolvedFeatureId.builder(FP1_GAV, "specA1").setParam("a", "1").setParam("a1", "2").build()),
                    specEvent("specA2"),
                    featureEvent(ResolvedFeatureId.builder(FP1_GAV, "specA2").setParam("a", "1").setParam("a1", "2").setParam("a2", "1").build()),
                    batchEndEvent(),
                    branchEndEvent(),

                    branchStartEvent(),
                    batchStartEvent(),
                    specEvent("specB"),
                    featureEvent(ResolvedFeatureId.builder(FP1_GAV, "specB").setParam("b", "1").build()),
                    specEvent("specB1"),
                    featureEvent(ResolvedFeatureId.builder(FP1_GAV, "specB1").setParam("b", "1").setParam("b1", "1").build()),
                    batchEndEvent(),
                    branchEndEvent(),

                    branchStartEvent(),
                    specEvent("specA1"),
                    featureEvent(ResolvedFeatureId.builder(FP1_GAV, "specA1").setParam("a", "1").setParam("a1", "1").build()),
                    branchEndEvent(),

                    branchStartEvent(),
                    specEvent("specA2"),
                    featureEvent(ResolvedFeatureId.builder(FP1_GAV, "specA2").setParam("a", "1").setParam("a1", "1").setParam("a2", "1").build()),
                    branchEndEvent(),
            };
        }
    }

    @Override
    protected void createFeaturePacks(FeaturePackCreator creator) throws ProvisioningException {
        creator
        .newFeaturePack(FP1_GAV)

            .addSpec(FeatureSpec.builder("specA")
                    .addAnnotation(FeatureAnnotation.parentChildrenBranch(true))
                    .addParam(FeatureParameterSpec.createId("a"))
                    .build())
            .addSpec(FeatureSpec.builder("specA1")
                    .addFeatureRef(FeatureReferenceSpec.create("specA"))
                    .addFeatureRef(FeatureReferenceSpec.create("specB1", true))
                    .addParam(FeatureParameterSpec.createId("a"))
                    .addParam(FeatureParameterSpec.createId("a1"))
                    .addParam(FeatureParameterSpec.create("b", true))
                    .addParam(FeatureParameterSpec.create("b1", true))
                    .build())
            .addSpec(FeatureSpec.builder("specA2")
                    .addFeatureRef(FeatureReferenceSpec.create("specA1"))
                    .addParam(FeatureParameterSpec.createId("a"))
                    .addParam(FeatureParameterSpec.createId("a1"))
                    .addParam(FeatureParameterSpec.createId("a2"))
                    .build())

            .addSpec(FeatureSpec.builder("specB")
                    .addAnnotation(FeatureAnnotation.parentChildrenBranch(true))
                    .addFeatureRef(FeatureReferenceSpec.create("specA", true))
                    .addParam(FeatureParameterSpec.createId("b"))
                    .addParam(FeatureParameterSpec.create("a", true))
                    .build())
            .addSpec(FeatureSpec.builder("specB1")
                    .addFeatureRef(FeatureReferenceSpec.create("specB"))
                    .addParam(FeatureParameterSpec.createId("b"))
                    .addParam(FeatureParameterSpec.createId("b1"))
                    .build())

            .addConfig(ConfigModel.builder()
                    .setProperty(ConfigModel.BRANCH_PER_SPEC, "true")
                    .addFeature(new FeatureConfig("specA2").setParam("a", "1").setParam("a1", "1").setParam("a2", "1"))
                    .addFeature(new FeatureConfig("specA2").setParam("a", "1").setParam("a1", "2").setParam("a2", "1"))

                    .addFeature(new FeatureConfig("specB").setParam("b", "1").setParam("a", "1"))
                    .addFeature(new FeatureConfig("specA").setParam("a", "1"))

                    .addFeature(new FeatureConfig("specA1").setParam("a", "1").setParam("a1", "1").setParam("b", "1").setParam("b1", "1"))
                    .addFeature(new FeatureConfig("specB1").setParam("b", "1").setParam("b1", "1"))

                    .addFeature(new FeatureConfig("specA1").setParam("a", "1").setParam("a1", "2"))

                    .build())
            .addPlugin(TestConfigHandlersProvisioningPlugin.class)
            .addService(ProvisionedConfigHandler.class, ConfigHandler.class)
            .getCreator()
        .install();
    }

    @Override
    protected FeaturePackConfig featurePackConfig() {
        return FeaturePackConfig.forLocation(FP1_GAV.getLocation());
    }

    @Override
    protected ProvisionedState provisionedState() throws ProvisioningException {
        return ProvisionedState.builder()
                .addFeaturePack(ProvisionedFeaturePack.builder(FP1_GAV)
                        .build())
                .addConfig(ProvisionedConfigBuilder.builder()
                        .setProperty(ConfigModel.BRANCH_PER_SPEC, "true")

                        .addFeature(ProvisionedFeatureBuilder.builder(ResolvedFeatureId.create(FP1_GAV, "specA", "a", "1")).build())
                        .addFeature(ProvisionedFeatureBuilder.builder(ResolvedFeatureId.builder(FP1_GAV, "specA1").setParam("a", "1").setParam("a1", "2").build())
                                .build())
                        .addFeature(ProvisionedFeatureBuilder.builder(ResolvedFeatureId.builder(FP1_GAV, "specA2").setParam("a", "1").setParam("a1", "2").setParam("a2", "1").build())
                                .build())

                        .addFeature(ProvisionedFeatureBuilder.builder(ResolvedFeatureId.builder(FP1_GAV, "specB").setParam("b", "1").build())
                                .setConfigParam("a", "1")
                                .build())
                        .addFeature(ProvisionedFeatureBuilder.builder(ResolvedFeatureId.builder(FP1_GAV, "specB1").setParam("b", "1").setParam("b1", "1").build())
                                .build())

                        .addFeature(ProvisionedFeatureBuilder.builder(ResolvedFeatureId.builder(FP1_GAV, "specA1").setParam("a", "1").setParam("a1", "1").build())
                                .setConfigParam("b", "1")
                                .setConfigParam("b1", "1")
                                .build())

                        .addFeature(ProvisionedFeatureBuilder.builder(ResolvedFeatureId.builder(FP1_GAV, "specA2").setParam("a", "1").setParam("a1", "1").setParam("a2", "1").build())
                                .build()))

                .build();
    }
}