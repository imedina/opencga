package org.opencb.opencga.app.migrations.v2_2_0.catalog.addRegistrationDate;

import com.mongodb.client.model.Projections;
import com.mongodb.client.model.UpdateOneModel;
import org.bson.Document;
import org.opencb.opencga.catalog.db.api.ProjectDBAdaptor;
import org.opencb.opencga.catalog.db.api.UserDBAdaptor;
import org.opencb.opencga.catalog.db.mongodb.MongoDBAdaptorFactory;
import org.opencb.opencga.catalog.migration.Migration;
import org.opencb.opencga.catalog.migration.MigrationTool;

import java.util.List;

import static com.mongodb.client.model.Filters.eq;

@Migration(id="add_registrationDate_to_project.internal", description = "Add registrationDate to Project #1804", version = "2.2.0",
        language = Migration.MigrationLanguage.JAVA, domain = Migration.MigrationDomain.CATALOG)
public class AddRegistrationDateToProjectInternal extends MigrationTool {

    @Override
    protected void run() throws Exception {

        migrateCollection(MongoDBAdaptorFactory.USER_COLLECTION,
                new Document()
                        .append("projects.internal", new Document("$exists", true))
                        .append("projects.internal.registrationDate", new Document("$exists", false)),
                Projections.include("_id", UserDBAdaptor.QueryParams.PROJECTS.key()),
                (user, bulk) -> {
                    // Get projects
                    List<Document> projects = user.getList(UserDBAdaptor.QueryParams.PROJECTS.key(), Document.class);

                    for (Document project : projects) {
                        String creationDate = project.getString(ProjectDBAdaptor.QueryParams.CREATION_DATE.key());
                        Document internal = project.get(ProjectDBAdaptor.QueryParams.INTERNAL.key(), Document.class);
                        internal.put("registrationDate", creationDate);
                    }

                    bulk.add(new UpdateOneModel<>(
                                    eq("_id", user.get("_id")),
                                    new Document("$set", new Document(UserDBAdaptor.QueryParams.PROJECTS.key(), projects))
                            )
                    );
                }
        );
    }
}
