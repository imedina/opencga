/*
 * Copyright 2015-2020 OpenCB
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

package org.opencb.opencga.master.monitor.daemons;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opencb.commons.datastore.core.Query;
import org.opencb.commons.datastore.core.QueryOptions;
import org.opencb.opencga.analysis.alignment.qc.AlignmentQcAnalysis;
import org.opencb.opencga.analysis.variant.operations.VariantAnnotationIndexOperationTool;
import org.opencb.opencga.analysis.variant.operations.VariantIndexOperationTool;
import org.opencb.opencga.catalog.db.api.FileDBAdaptor;
import org.opencb.opencga.catalog.exceptions.CatalogException;
import org.opencb.opencga.catalog.managers.AbstractManagerTest;
import org.opencb.opencga.catalog.managers.FileManager;
import org.opencb.opencga.catalog.utils.ParamUtils;
import org.opencb.opencga.core.api.ParamConstants;
import org.opencb.opencga.core.common.JacksonUtils;
import org.opencb.opencga.core.common.TimeUtils;
import org.opencb.opencga.core.models.common.Enums;
import org.opencb.opencga.core.models.file.FileContent;
import org.opencb.opencga.core.models.file.FileLinkParams;
import org.opencb.opencga.core.models.job.*;
import org.opencb.opencga.core.models.study.GroupUpdateParams;
import org.opencb.opencga.core.models.study.StudyNotification;
import org.opencb.opencga.core.models.study.StudyUpdateParams;
import org.opencb.opencga.core.response.OpenCGAResult;
import org.opencb.opencga.core.tools.result.ExecutionResultManager;
import org.opencb.opencga.core.tools.result.JobResult;
import org.opencb.opencga.core.tools.result.Status;
import org.opencb.opencga.master.monitor.executors.BatchExecutor;
import org.opencb.opencga.master.monitor.models.PrivateJobUpdateParams;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ExecutionDaemonTest extends AbstractManagerTest {

    private ExecutionDaemon executionDaemon;
    private JobDaemon jobDaemon;
    private DummyBatchExecutor executor;

    @Override
    @Before
    public void setUp() throws IOException, CatalogException {
        super.setUp();

        String expiringToken = this.catalogManager.getUserManager().loginAsAdmin("admin").getToken();
        String nonExpiringToken = this.catalogManager.getUserManager().getNonExpiringToken("opencga", expiringToken);
        catalogManager.getConfiguration().getAnalysis().getExecution().getMaxConcurrentJobs().put(VariantIndexOperationTool.ID, 1);

        jobDaemon = new JobDaemon(1000, nonExpiringToken, catalogManager, "/tmp");
        executionDaemon = new ExecutionDaemon(1000, nonExpiringToken, catalogManager);
        executor = new DummyBatchExecutor();
        jobDaemon.batchExecutor = executor;
    }

    @Test
    public void testBuildCli() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("key", "value");
        params.put("camelCaseKey", "value");
        params.put("flag", "");
        params.put("boolean", "true");
        params.put("outdir", "/tmp/folder");
        params.put("paramWithSpaces", "This could be a description");
        params.put("paramWithSingleQuotes", "This could 'be' a description");
        params.put("paramWithDoubleQuotes", "This could \"be\" a description");

        Map<String, String> dynamic1 = new LinkedHashMap<>();
        dynamic1.put("dynamic", "It's true");
        dynamic1.put("param with spaces", "Fuc*!");

        params.put("dynamicParam1", dynamic1);
        params.put("dynamicNested2", dynamic1);
        String cli = JobDaemon.buildCli("opencga-internal.sh", "variant index-run", params);
        assertEquals("opencga-internal.sh variant index-run "
                + "--key value "
                + "--camel-case-key value "
                + "--flag  "
                + "--boolean true "
                + "--outdir '/tmp/folder' "
                + "--param-with-spaces 'This could be a description' "
                + "--param-with-single-quotes 'This could '\"'\"'be'\"'\"' a description' "
                + "--param-with-double-quotes 'This could \"be\" a description' "
                + "--dynamic-param1 dynamic='It'\"'\"'s true' "
                + "--dynamic-param1 'param with spaces'='Fuc*!' "
                + "--dynamic-nested2 dynamic='It'\"'\"'s true' "
                + "--dynamic-nested2 'param with spaces'='Fuc*!'", cli);
    }

    @Test
    public void testCreateDefaultOutDir() throws Exception {
        HashMap<String, Object> params = new HashMap<>();
        String jobId = catalogManager.getJobManager().submit(studyFqn, "files-delete", Enums.Priority.MEDIUM, params, token).first().getId();

        jobDaemon.checkPendingJobs();

        URI uri = getJob(jobId).getOutDir().getUri();
        Assert.assertTrue(Files.exists(Paths.get(uri)));
        assertTrue(uri.getPath().startsWith(catalogManager.getConfiguration().getJobDir()) && uri.getPath().endsWith(jobId + "/"));
    }

    @Test
    public void testWebhookNotification() throws Exception {
        catalogManager.getStudyManager().update(studyFqn, new StudyUpdateParams()
                .setNotification(new StudyNotification(new URL("https://ptsv2.com/t/dgogf-1581523512/post"))), null, token);

        HashMap<String, Object> params = new HashMap<>();
        String jobId = catalogManager.getJobManager().submit(studyFqn, "files-delete", Enums.Priority.MEDIUM, params, token).first().getId();

        jobDaemon.checkPendingJobs();
        // We sleep because there must be a thread sending notifying to the webhook url.
        Thread.sleep(1500);

        Job job = getJob(jobId);
        assertEquals(1, job.getInternal().getEvents().size());
        assertTrue(job.getInternal().getWebhook().getStatus().containsKey("QUEUED"));
        assertEquals(JobInternalWebhook.Status.ERROR, job.getInternal().getWebhook().getStatus().get("QUEUED"));
    }

    @Test
    public void testCreateOutDir() throws Exception {
        HashMap<String, Object> params = new HashMap<>();
        params.put("outdir", "outputDir/");
        String jobId = catalogManager.getJobManager().submit(studyFqn, "files-delete", Enums.Priority.MEDIUM, params, token).first().getId();

        jobDaemon.checkPendingJobs();

        URI uri = getJob(jobId).getOutDir().getUri();
        Assert.assertTrue(Files.exists(Paths.get(uri)));
        assertTrue(!uri.getPath().startsWith("/tmp/opencga/jobs/") && uri.getPath().endsWith("outputDir/"));
    }

    @Test
    public void testUseEmptyDirectory() throws Exception {
        // Create empty directory that is registered in OpenCGA
        org.opencb.opencga.core.models.file.File directory = catalogManager.getFileManager().createFolder(studyFqn, "outputDir/",
                true, "", QueryOptions.empty(), token).first();
        catalogManager.getIoManagerFactory().get(directory.getUri()).createDirectory(directory.getUri(), true);

        HashMap<String, Object> params = new HashMap<>();
        params.put("outdir", "outputDir/");
        String jobId = catalogManager.getJobManager().submit(studyFqn, "files-delete", Enums.Priority.MEDIUM, params,
                token).first().getId();

        jobDaemon.checkPendingJobs();

        URI uri = getJob(jobId).getOutDir().getUri();
        Assert.assertTrue(Files.exists(Paths.get(uri)));
        assertTrue(!uri.getPath().startsWith("/tmp/opencga/jobs/") && uri.getPath().endsWith("outputDir/"));
    }

    @Test
    public void testNotEmptyOutDir() throws Exception {
        HashMap<String, Object> params = new HashMap<>();
        params.put("outdir", "data/");
        String jobId = catalogManager.getJobManager().submit(studyFqn, "files-delete", Enums.Priority.MEDIUM, params, token).first().getId();

        jobDaemon.checkPendingJobs();

        OpenCGAResult<Job> jobOpenCGAResult = catalogManager.getJobManager().get(studyFqn, jobId, QueryOptions.empty(), token);
        assertEquals(1, jobOpenCGAResult.getNumResults());
        assertEquals(Enums.ExecutionStatus.ABORTED, jobOpenCGAResult.first().getInternal().getStatus().getName());
        assertTrue(jobOpenCGAResult.first().getInternal().getStatus().getDescription().contains("not an empty directory"));
    }

    @Test
    public void testProjectScopeTask() throws Exception {
        // User 2 to admins group in study1 but not in study2
        catalogManager.getStudyManager().updateGroup(studyFqn, "@admins", ParamUtils.BasicUpdateAction.ADD,
                new GroupUpdateParams(Collections.singletonList("user2")), token);

        // User 3 to admins group in both study1 and study2
        catalogManager.getStudyManager().updateGroup(studyFqn, "@admins", ParamUtils.BasicUpdateAction.ADD,
                new GroupUpdateParams(Collections.singletonList("user3")), token);
        catalogManager.getStudyManager().updateGroup(studyFqn2, "@admins", ParamUtils.BasicUpdateAction.ADD,
                new GroupUpdateParams(Collections.singletonList("user3")), token);

        HashMap<String, Object> params = new HashMap<>();
        String jobId = catalogManager.getJobManager().submit(studyFqn, VariantAnnotationIndexOperationTool.ID, Enums.Priority.MEDIUM,
                params, token).first().getId();
        String jobId2 = catalogManager.getJobManager().submit(studyFqn, VariantAnnotationIndexOperationTool.ID, Enums.Priority.MEDIUM,
                params, sessionIdUser2).first().getId();
        String jobId3 = catalogManager.getJobManager().submit(studyFqn, VariantAnnotationIndexOperationTool.ID, Enums.Priority.MEDIUM,
                params, sessionIdUser3).first().getId();

        jobDaemon.checkPendingJobs();

        // Job sent by the owner
        OpenCGAResult<Job> jobOpenCGAResult = catalogManager.getJobManager().get(studyFqn, jobId, QueryOptions.empty(), token);
        assertEquals(1, jobOpenCGAResult.getNumResults());
        assertEquals(Enums.ExecutionStatus.QUEUED, jobOpenCGAResult.first().getInternal().getStatus().getName());

        // Job sent by user2 (admin from study1 but not from study2)
        jobOpenCGAResult = catalogManager.getJobManager().get(studyFqn, jobId2, QueryOptions.empty(), token);
        assertEquals(1, jobOpenCGAResult.getNumResults());
        assertEquals(Enums.ExecutionStatus.ABORTED, jobOpenCGAResult.first().getInternal().getStatus().getName());
        assertTrue(jobOpenCGAResult.first().getInternal().getStatus().getDescription()
                .contains("can only be executed by the project owners or admins"));

        // Job sent by user3 (admin from study1 and study2)
        jobOpenCGAResult = catalogManager.getJobManager().get(studyFqn, jobId3, QueryOptions.empty(), token);
        assertEquals(1, jobOpenCGAResult.getNumResults());
        assertEquals(Enums.ExecutionStatus.QUEUED, jobOpenCGAResult.first().getInternal().getStatus().getName());

        jobOpenCGAResult = catalogManager.getJobManager().search(studyFqn2, new Query(), QueryOptions.empty(), token);
        assertEquals(0, jobOpenCGAResult.getNumResults());

        jobOpenCGAResult = catalogManager.getJobManager().search(studyFqn2, new Query(),
                new QueryOptions(ParamConstants.OTHER_STUDIES_FLAG, true), token);
        assertEquals(2, jobOpenCGAResult.getNumResults());
    }

    @Test
    public void testDependsOnJobs() throws Exception {
        HashMap<String, Object> params = new HashMap<>();
        String job1 = catalogManager.getJobManager().submit(studyFqn, "files-delete", Enums.Priority.MEDIUM, params, token).first().getId();
        String job2 = catalogManager.getJobManager().submit(studyFqn, "files-delete", Enums.Priority.MEDIUM, params, null, null,
                Collections.singletonList(job1), null, token).first().getId();

        jobDaemon.checkPendingJobs();

        OpenCGAResult<Job> jobOpenCGAResult = catalogManager.getJobManager().get(studyFqn, job1, QueryOptions.empty(), token);
        assertEquals(1, jobOpenCGAResult.getNumResults());
        assertEquals(Enums.ExecutionStatus.QUEUED, jobOpenCGAResult.first().getInternal().getStatus().getName());

        jobOpenCGAResult = catalogManager.getJobManager().get(studyFqn, job2, QueryOptions.empty(), token);
        assertEquals(1, jobOpenCGAResult.getNumResults());
        assertEquals(Enums.ExecutionStatus.PENDING, jobOpenCGAResult.first().getInternal().getStatus().getName());

        // Set the status of job1 to ERROR
        catalogManager.getJobManager().update(studyFqn, job1, new PrivateJobUpdateParams()
                .setInternal(new JobInternal(new Enums.ExecutionStatus(Enums.ExecutionStatus.ERROR))), QueryOptions.empty(), token);

        // The job that depended on job1 should be ABORTED because job1 execution "failed"
        jobDaemon.checkPendingJobs();
        jobOpenCGAResult = catalogManager.getJobManager().get(studyFqn, job2, QueryOptions.empty(), token);
        assertEquals(1, jobOpenCGAResult.getNumResults());
        assertEquals(Enums.ExecutionStatus.ABORTED, jobOpenCGAResult.first().getInternal().getStatus().getName());
        assertTrue(jobOpenCGAResult.first().getInternal().getStatus().getDescription().contains("depended on did not finish successfully"));

        // Set status of job1 to DONE to simulate it finished successfully
        catalogManager.getJobManager().update(studyFqn, job1, new PrivateJobUpdateParams()
                .setInternal(new JobInternal(new Enums.ExecutionStatus(Enums.ExecutionStatus.DONE))), QueryOptions.empty(), token);

        // And create a new job to simulate a normal successfully dependency
        String job3 = catalogManager.getJobManager().submit(studyFqn, "files-delete", Enums.Priority.MEDIUM, params, null, null,
                Collections.singletonList(job1), null, token).first().getId();
        jobDaemon.checkPendingJobs();

        jobOpenCGAResult = catalogManager.getJobManager().get(studyFqn, job3, QueryOptions.empty(), token);
        assertEquals(1, jobOpenCGAResult.getNumResults());
        assertEquals(Enums.ExecutionStatus.QUEUED, jobOpenCGAResult.first().getInternal().getStatus().getName());
    }

    @Test
    public void testDependsOnMultiStudy() throws Exception {
        HashMap<String, Object> params = new HashMap<>();
        Job firstJob = catalogManager.getJobManager().submit(studyFqn, "files-delete", Enums.Priority.MEDIUM, params, token).first();
        Job job = catalogManager.getJobManager().submit(studyFqn2, "files-delete", Enums.Priority.MEDIUM, params, null, null,
                Collections.singletonList(firstJob.getUuid()), null, token).first();
        assertEquals(1, job.getDependsOn().size());
        assertEquals(firstJob.getId(), job.getDependsOn().get(0).getId());
        assertEquals(firstJob.getUuid(), job.getDependsOn().get(0).getUuid());

        job = catalogManager.getJobManager().get(studyFqn2, job.getId(), QueryOptions.empty(), token).first();
        assertEquals(1, job.getDependsOn().size());
        assertEquals(firstJob.getId(), job.getDependsOn().get(0).getId());
        assertEquals(firstJob.getUuid(), job.getDependsOn().get(0).getUuid());
    }

    @Test
    public void testRunJob() throws Exception {
        HashMap<String, Object> params = new HashMap<>();
        params.put(ExecutionDaemon.OUTDIR_PARAM, "outDir");
        org.opencb.opencga.core.models.file.File inputFile = catalogManager.getFileManager().get(studyFqn, testFile1, null, token).first();
        params.put("myFile", inputFile.getPath());
        Job job = catalogManager.getJobManager().submit(studyFqn, "variant-index", Enums.Priority.MEDIUM, params, token).first();
        String jobId = job.getId();

        jobDaemon.checkJobs();

        String[] cli = getJob(jobId).getCommandLine().split(" ");
        int i = Arrays.asList(cli).indexOf("--my-file");
        assertEquals("'" + inputFile.getPath() + "'", cli[i + 1]);
        assertEquals(1, getJob(jobId).getInput().size());
        assertEquals(inputFile.getPath(), getJob(jobId).getInput().get(0).getPath());
        assertEquals(Enums.ExecutionStatus.QUEUED, getJob(jobId).getInternal().getStatus().getName());
        executor.jobStatus.put(jobId, Enums.ExecutionStatus.RUNNING);

        jobDaemon.checkJobs();

        assertEquals(Enums.ExecutionStatus.RUNNING, getJob(jobId).getInternal().getStatus().getName());
        createAnalysisResult(jobId, "myTest", ar -> ar.setStatus(new Status(Status.Type.DONE, null, TimeUtils.getDate())));
        executor.jobStatus.put(jobId, Enums.ExecutionStatus.READY);

        jobDaemon.checkJobs();

        assertEquals(Enums.ExecutionStatus.DONE, getJob(jobId).getInternal().getStatus().getName());
    }

    @Test
    public void testCheckLogs() throws Exception {
        HashMap<String, Object> params = new HashMap<>();
        params.put(ExecutionDaemon.OUTDIR_PARAM, "outDir");
        org.opencb.opencga.core.models.file.File inputFile = catalogManager.getFileManager().get(studyFqn, testFile1, null, token).first();
        params.put("myFile", inputFile.getPath());
        Job job = catalogManager.getJobManager().submit(studyFqn, "variant-index", Enums.Priority.MEDIUM, params, token).first();
        String jobId = job.getId();

        jobDaemon.checkJobs();

        String[] cli = getJob(jobId).getCommandLine().split(" ");
        int i = Arrays.asList(cli).indexOf("--my-file");
        assertEquals("'" + inputFile.getPath() + "'", cli[i + 1]);
        assertEquals(1, getJob(jobId).getInput().size());
        assertEquals(inputFile.getPath(), getJob(jobId).getInput().get(0).getPath());
        assertEquals(Enums.ExecutionStatus.QUEUED, getJob(jobId).getInternal().getStatus().getName());
        executor.jobStatus.put(jobId, Enums.ExecutionStatus.RUNNING);

        job = catalogManager.getJobManager().get(studyFqn, jobId, null, token).first();

        jobDaemon.checkJobs();
        assertEquals(Enums.ExecutionStatus.RUNNING, getJob(jobId).getInternal().getStatus().getName());

        InputStream inputStream = new ByteArrayInputStream("my log content\nlast line".getBytes(StandardCharsets.UTF_8));
        catalogManager.getIoManagerFactory().getDefault().copy(inputStream,
                Paths.get(job.getOutDir().getUri()).resolve(job.getId() + ".log").toUri());

        OpenCGAResult<FileContent> fileContentResult = catalogManager.getJobManager().log(studyFqn, jobId, 0, 1, "stdout", true, token);
        assertEquals("last line", fileContentResult.first().getContent());

        fileContentResult = catalogManager.getJobManager().log(studyFqn, jobId, 0, 1, "stdout", false, token);
        assertEquals("my log content\n", fileContentResult.first().getContent());
    }

    @Test
    public void testRegisterFilesSuccessfully() throws Exception {
        HashMap<String, Object> params = new HashMap<>();
//        params.put(ExecutionDaemon.OUTDIR_PARAM, "outDir");
        org.opencb.opencga.core.models.file.File inputFile = catalogManager.getFileManager().get(studyFqn, testFile1, null, token).first();
        params.put("myFile", inputFile.getPath());
        Job job = catalogManager.getJobManager().submit(studyFqn, "variant-index", Enums.Priority.MEDIUM, params, token).first();
        String jobId = job.getId();

        jobDaemon.checkJobs();

        String[] cli = getJob(jobId).getCommandLine().split(" ");
        int i = Arrays.asList(cli).indexOf("--my-file");
        assertEquals("'" + inputFile.getPath() + "'", cli[i + 1]);
        assertEquals(1, getJob(jobId).getInput().size());
        assertEquals(inputFile.getPath(), getJob(jobId).getInput().get(0).getPath());
        assertEquals(Enums.ExecutionStatus.QUEUED, getJob(jobId).getInternal().getStatus().getName());
        executor.jobStatus.put(jobId, Enums.ExecutionStatus.RUNNING);

        jobDaemon.checkJobs();

        assertEquals(Enums.ExecutionStatus.RUNNING, getJob(jobId).getInternal().getStatus().getName());
        createAnalysisResult(jobId, "myTest", ar -> ar.setStatus(new Status(Status.Type.DONE, null, TimeUtils.getDate())));
        executor.jobStatus.put(jobId, Enums.ExecutionStatus.READY);

        job = catalogManager.getJobManager().get(studyFqn, job.getId(), QueryOptions.empty(), token).first();
        Files.createFile(Paths.get(job.getOutDir().getUri()).resolve("file1.txt"));
        Files.createFile(Paths.get(job.getOutDir().getUri()).resolve("file2.txt"));
        Files.createDirectory(Paths.get(job.getOutDir().getUri()).resolve("A"));
        Files.createFile(Paths.get(job.getOutDir().getUri()).resolve("A/file3.txt"));

        Files.createFile(Paths.get(job.getOutDir().getUri()).resolve(job.getId() + ".log"));
        Files.createFile(Paths.get(job.getOutDir().getUri()).resolve(job.getId() + ".err"));

        jobDaemon.checkJobs();

        assertEquals(Enums.ExecutionStatus.DONE, getJob(jobId).getInternal().getStatus().getName());

        job = catalogManager.getJobManager().get(studyFqn, job.getId(), QueryOptions.empty(), token).first();

        String outDir = job.getOutDir().getPath();

        assertEquals(4, job.getOutput().size());
        for (org.opencb.opencga.core.models.file.File file : job.getOutput()) {
            assertTrue(Arrays.asList(outDir + "file1.txt", outDir + "file2.txt", outDir + "A/", outDir + "A/file3.txt")
                    .contains(file.getPath()));
        }
        assertEquals(0, job.getOutput().stream().filter(f -> f.getName().endsWith(ExecutionResultManager.FILE_EXTENSION))
                .collect(Collectors.toList()).size());

        assertEquals(job.getId() + ".log", job.getStdout().getName());
        assertEquals(job.getId() + ".err", job.getStderr().getName());

        // Check jobId is properly populated
        OpenCGAResult<org.opencb.opencga.core.models.file.File> files = catalogManager.getFileManager().search(studyFqn,
                new Query(FileDBAdaptor.QueryParams.JOB_ID.key(), job.getId()), FileManager.INCLUDE_FILE_URI_PATH, token);
        assertEquals(7, files.getNumResults());
        for (org.opencb.opencga.core.models.file.File file : files.getResults()) {
            assertTrue(Arrays.asList(outDir, outDir + "file1.txt", outDir + "file2.txt", outDir + "A/", outDir + "A/file3.txt",
                    outDir + "" + job.getId() + ".log", outDir + "" + job.getId() + ".err").contains(file.getPath()));
        }

        files = catalogManager.getFileManager().count(studyFqn, new Query(FileDBAdaptor.QueryParams.JOB_ID.key(), ""), token);
        assertEquals(10, files.getNumMatches());
        files = catalogManager.getFileManager().count(studyFqn, new Query(FileDBAdaptor.QueryParams.JOB_ID.key(), "NonE"), token);
        assertEquals(10, files.getNumMatches());
    }

    @Test
    public void testRunJobFail() throws Exception {
        HashMap<String, Object> params = new HashMap<>();
        params.put(ExecutionDaemon.OUTDIR_PARAM, "outputDir/");
        Job job = catalogManager.getJobManager().submit(studyFqn, "variant-index", Enums.Priority.MEDIUM, params, token).first();
        String jobId = job.getId();

        jobDaemon.checkJobs();

        assertEquals(Enums.ExecutionStatus.QUEUED, getJob(jobId).getInternal().getStatus().getName());
        executor.jobStatus.put(jobId, Enums.ExecutionStatus.RUNNING);

        jobDaemon.checkJobs();

        assertEquals(Enums.ExecutionStatus.RUNNING, getJob(jobId).getInternal().getStatus().getName());
        createAnalysisResult(jobId, "myTest", ar -> ar.setStatus(new Status(Status.Type.ERROR, null, TimeUtils.getDate())));
        executor.jobStatus.put(jobId, Enums.ExecutionStatus.ERROR);

        jobDaemon.checkJobs();

        assertEquals(Enums.ExecutionStatus.ERROR, getJob(jobId).getInternal().getStatus().getName());
        assertEquals("Job could not finish successfully", getJob(jobId).getInternal().getStatus().getDescription());
    }

    @Test
    public void testRunJobFailMissingExecutionResult() throws Exception {
        HashMap<String, Object> params = new HashMap<>();
        params.put(ExecutionDaemon.OUTDIR_PARAM, "outputDir/");
        Job job = catalogManager.getJobManager().submit(studyFqn, "variant-index", Enums.Priority.MEDIUM, params, token).first();
        String jobId = job.getId();

        jobDaemon.checkJobs();

        assertEquals(Enums.ExecutionStatus.QUEUED, getJob(jobId).getInternal().getStatus().getName());
        executor.jobStatus.put(jobId, Enums.ExecutionStatus.RUNNING);

        jobDaemon.checkJobs();

        assertEquals(Enums.ExecutionStatus.RUNNING, getJob(jobId).getInternal().getStatus().getName());
        executor.jobStatus.put(jobId, Enums.ExecutionStatus.READY);

        jobDaemon.checkJobs();

        assertEquals(Enums.ExecutionStatus.ERROR, getJob(jobId).getInternal().getStatus().getName());
        assertEquals("Job could not finish successfully. Missing execution result", getJob(jobId).getInternal().getStatus().getDescription());
    }

    @Test
    public void fastQCAnalysisPipelineTest() throws IOException, CatalogException, URISyntaxException {
        String bamUri = getClass().getResource("/biofiles/HG00096.chrom20.small.bam").toURI().toString();
        catalogManager.getFileManager().link(studyFqn, new FileLinkParams().setUri(bamUri).setPath("."), false, token);

        // Load and create the pipeline
        Pipeline pipeline = Pipeline.load(getClass().getResource("/pipelines/alignment-qc-2.yml").openStream());
        catalogManager.getPipelineManager().create(studyFqn, pipeline, QueryOptions.empty(), token);

        // Submit an alignment-qc execution
        Map<String, Object> params = new HashMap<>();
        params.put("bamFile", "HG00096.chrom20.small.bam");
        params.put("bedFile", "HG00096.chrom20.small.bam");
        params.put("dictFile", "HG00096.chrom20.small.bam");
        OpenCGAResult<Execution> executionResult = catalogManager.getExecutionManager().submitPipeline(studyFqn,
                AlignmentQcAnalysis.ID, Enums.Priority.MEDIUM, params, "", "", null, null, token);

        executionDaemon.checkPendingExecutions();

        executionResult = catalogManager.getExecutionManager().get(studyFqn, executionResult.first().getId(), QueryOptions.empty(), token);
        assertEquals(1, executionResult.getNumResults());
        assertEquals(Enums.ExecutionStatus.PENDING, executionResult.first().getInternal().getStatus().getName());
        assertEquals(3, executionResult.first().getJobs().size());
        for (Job job : executionResult.first().getJobs()) {
            assertEquals(Enums.ExecutionStatus.PENDING, job.getInternal().getStatus().getName());
        }
//        assertEquals(1, executionResult.first().getJobs().get(3).getDependsOn().size());
//        assertEquals(executionResult.first().getJobs().get(0).getId(),
//                executionResult.first().getJobs().get(3).getDependsOn().get(0).getId());
    }

    @Test
    public void executionToolTest() throws CatalogException, URISyntaxException {
        String bamUri = getClass().getResource("/biofiles/HG00096.chrom20.small.bam").toURI().toString();
        catalogManager.getFileManager().link(studyFqn, new FileLinkParams().setUri(bamUri).setPath("."), false, token);

        // Submit an alignment-qc execution
        Map<String, Object> params = new HashMap<>();
        params.put("bamFile", "HG00096.chrom20.small.bam");
        params.put("bedFile", "HG00096.chrom20.small.bam");
        params.put("dictFile", "HG00096.chrom20.small.bam");
        OpenCGAResult<Execution> executionResult = catalogManager.getExecutionManager().submitTool(studyFqn,
                AlignmentQcAnalysis.ID, Enums.Priority.MEDIUM, params, "", "", null, null, token);

        executionResult = catalogManager.getExecutionManager().get(studyFqn, executionResult.first().getId(), QueryOptions.empty(), token);
        assertEquals(1, executionResult.getNumResults());
        assertEquals(Enums.ExecutionStatus.PROCESSED, executionResult.first().getInternal().getStatus().getName());
        assertEquals(1, executionResult.first().getJobs().size());
        for (Job job : executionResult.first().getJobs()) {
            assertEquals(Enums.ExecutionStatus.PENDING, job.getInternal().getStatus().getName());
        }
    }

    private Job getJob(String jobId) throws CatalogException {
        return catalogManager.getJobManager().get(studyFqn, jobId, new QueryOptions(), token).first();
    }

    private void createAnalysisResult(String jobId, String analysisId, Consumer<JobResult> c) throws CatalogException, IOException {
        JobResult ar = new JobResult();
        c.accept(ar);
        File resultFile = Paths.get(getJob(jobId).getOutDir().getUri()).resolve(analysisId + ExecutionResultManager.FILE_EXTENSION).toFile();
        JacksonUtils.getDefaultObjectMapper().writeValue(resultFile, ar);
    }

    private static class DummyBatchExecutor implements BatchExecutor {

        public Map<String, String> jobStatus = new HashMap<>();

        @Override
        public void execute(String jobId, String queue, String commandLine, Path stdout, Path stderr) throws Exception {
            System.out.println("Executing job " + jobId + " --- " + commandLine);
            jobStatus.put(jobId, Enums.ExecutionStatus.QUEUED);
        }

        @Override
        public String getStatus(String jobId) {
            return jobStatus.getOrDefault(jobId, Enums.ExecutionStatus.UNKNOWN);
        }

        @Override
        public boolean isExecutorAlive() {
            return true;
        }

        @Override
        public boolean stop(String jobId) throws Exception {
            return false;
        }

        @Override
        public boolean resume(String jobId) throws Exception {
            return false;
        }

        @Override
        public boolean kill(String jobId) throws Exception {
            return false;
        }
    }
}
