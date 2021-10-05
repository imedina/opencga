package org.opencb.opencga.app.migrations.v2_2_0.catalog.addInternalLastModified;

import com.mongodb.client.model.Projections;
import com.mongodb.client.model.UpdateOneModel;
import org.bson.Document;
import org.opencb.opencga.catalog.migration.MigrationTool;

import static com.mongodb.client.model.Filters.eq;

public abstract class AddInternalLastModified extends MigrationTool {

    protected void addInternalModificationDate(String collection) {
        migrateCollection(collection,
                new Document("internal.lastModified", new Document("$exists", false)),
                Projections.include("_id", "modificationDate", "internal"),
                (doc, bulk) -> {
                    String modificationDate = doc.getString("modificationDate");

                    bulk.add(new UpdateOneModel<>(
                                    eq("_id", doc.get("_id")),
                                    new Document("$set", new Document("internal.lastModified", modificationDate))
                            )
                    );
                }
        );
    }

}
