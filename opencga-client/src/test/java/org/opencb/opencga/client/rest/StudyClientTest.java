/*
 * Copyright 2015-2017 OpenCB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opencb.opencga.client.rest;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opencb.opencga.client.config.ClientConfiguration;
import org.opencb.opencga.client.exceptions.ClientException;
import org.opencb.opencga.client.rest.clients.StudyClient;
import org.opencb.opencga.core.models.study.Study;
import org.opencb.opencga.core.response.RestResponse;

import static org.junit.Assert.assertNotNull;

/**
 * Created by swaathi on 10/05/16.
 */
public class StudyClientTest {
    private OpenCGAClient openCGAClient;
    private StudyClient studyClient;
    private ClientConfiguration clientConfiguration;
    private static WSTestServer wsTestServer;

    public StudyClientTest() throws ClientException {
        try {
            clientConfiguration = ClientConfiguration.load(getClass().getResourceAsStream("/client-configuration.yml"));
//            clientConfiguration.getRest().setHost("http://localhost:8890/opencga/webservices/rest");
            openCGAClient = new OpenCGAClient("hgva", "hgva_cafeina", clientConfiguration);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @BeforeClass
    static public void initServer() throws Exception {
        wsTestServer = new WSTestServer();
        wsTestServer.initServer();

//        wsTestServer.setUp();
//        webTarget = serverTestUtils.getWebTarget();
//        sessionId = OpenCGAWSServer.catalogManager.login("user", CatalogManagerTest.PASSWORD, "localhost").first().getString("sessionId");
//        studyId = OpenCGAWSServer.catalogManager.getStudyId("user@1000G:phase1");
//        in1 = OpenCGAWSServer.catalogManager.createIndividual(studyId, "in1", "f1", -1, -1, null, null, sessionId).first().getId();
//        s1 = OpenCGAWSServer.catalogManager.createSample(studyId, "s1", "f1", "", null, null, sessionId).first().getId();
//        s2 = OpenCGAWSServer.catalogManager.createSample(studyId, "s2", "f1", "", null, null, sessionId).first().getId();
//        s3 = OpenCGAWSServer.catalogManager.createSample(studyId, "s3", "f1", "", null, null, sessionId).first().getId();
//        s4 = OpenCGAWSServer.catalogManager.createSample(studyId, "s4", "f1", "", null, null, sessionId).first().getId();
    }

    @AfterClass
    static public void shutdownServer() throws Exception {
        wsTestServer.shutdownServer();
    }

    @Before
    public void init() throws Exception {
//        clientConfiguration = ClientConfiguration.load(getClass().getResourceAsStream("/client-configuration-test.yml"));
//        clientConfiguration.getRest().setHost("http://localhost:8890/opencga/webservices/rest/");
//        openCGAClient = new OpenCGAClient("imedina", "pepe", clientConfiguration);
    }

    @Test
    public void get() throws Exception {
        studyClient = openCGAClient.getStudyClient();
        RestResponse<Study> info = studyClient.info("2", null);
        assertNotNull(info.firstResult());
    }
}
