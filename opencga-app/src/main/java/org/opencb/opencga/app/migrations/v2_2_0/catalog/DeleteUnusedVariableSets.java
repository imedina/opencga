package org.opencb.opencga.app.migrations.v2_2_0.catalog;

import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Projections;
import org.bson.Document;
import org.opencb.opencga.catalog.db.mongodb.MongoDBAdaptorFactory;
import org.opencb.opencga.catalog.db.mongodb.converters.StudyConverter;
import org.opencb.opencga.catalog.exceptions.CatalogException;
import org.opencb.opencga.catalog.migration.Migration;
import org.opencb.opencga.catalog.migration.MigrationTool;
import org.opencb.opencga.core.models.study.Study;

import java.util.ArrayList;
import java.util.List;

@Migration(id = "delete_unused_variablesets",
        description = "Delete unused VariableSets, #1859", version = "2.2.0",
        language = Migration.MigrationLanguage.JAVA,
        domain = Migration.MigrationDomain.CATALOG,
        patch = 2,
        date = 20211210)
public class DeleteUnusedVariableSets extends MigrationTool {

    @Override
    protected void run() throws Exception {
        StudyConverter converter = new StudyConverter();
        MongoCursor<Document> studyIterator = getMongoCollection(MongoDBAdaptorFactory.STUDY_COLLECTION)
                .find(new Document())
                .projection(Projections.include("fqn", "variableSets")).iterator();

        List<Study> studyList = new ArrayList<>();
        while (studyIterator.hasNext()) {
            Document studyDoc = studyIterator.next();
            Study study = converter.convertToDataModelType(studyDoc);
            studyList.add(study);
        }

        for (Study study : studyList) {
            logger.info("Deleting VariableSets from study '{}'", study.getFqn());
            delete(study.getFqn(), "opencga_alignment_samtools_flagstat");
            delete(study.getFqn(), "opencga_alignment_stats");
            delete(study.getFqn(), "opencga_sample_qc");
            delete(study.getFqn(), "opencga_file_variant_stats");
        }
    }

    private void delete(String studyFqn, String variableSetId) {
        try {
            catalogManager.getStudyManager().deleteVariableSet(studyFqn, variableSetId, true, token);
        } catch (CatalogException e) {
            logger.warn(e.getMessage());
        }
    }

}
