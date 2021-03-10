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

package org.opencb.opencga.catalog.stats.solr.converters;

import org.apache.commons.collections4.map.HashedMap;
import org.opencb.biodata.models.clinical.Disorder;
import org.opencb.biodata.models.clinical.Phenotype;
import org.opencb.commons.datastore.core.QueryParam;
import org.opencb.opencga.core.models.common.AnnotationSet;

import java.util.*;

/**
 * Created by wasim on 03/07/18.
 */
public class SolrConverterUtil {


    public static Map<String, Object> populateAnnotations(Map<String, Map<String, QueryParam.Type>> variableTypeMap,
                                                          List<AnnotationSet> annotationSets) {
        Map<String, Object> result = new HashedMap();
        if (annotationSets != null) {
            for (AnnotationSet annotationSet : annotationSets) {
                Map<String, QueryParam.Type> typeMap = variableTypeMap.get(annotationSet.getVariableSetId());

                for (String annotationKey : annotationSet.getAnnotations().keySet()) {
                    Object value = annotationSet.getAnnotations().get(annotationKey);
                    if (typeMap.containsKey(annotationKey)) {
                        result.put("annotations" + type(typeMap.get(annotationKey)) + annotationSet.getVariableSetId() + "."
                                + annotationKey, value);
                    } else {
                        // Dynamic annotation
                        String dynamicKey = annotationKey.substring(0, annotationKey.lastIndexOf(".") + 1) + "*";
                        if (!typeMap.containsKey(dynamicKey)) {
                            // TODO: This condition should be removed in 3.0. This is here because of a bug that happened annotating
                            // TODO: This should be completely fixed and removed in future releases
                            continue;
                        }
                        result.put("annotations" + type(typeMap.get(dynamicKey)) + annotationSet.getVariableSetId() + "." + annotationKey,
                                value);
                    }
                }
            }
        }
        return result;
    }

    public static List<String> populatePhenotypes(List<Phenotype> phenotypes) {
        Set<String> phenotypesIds = new HashSet<>();
        if (phenotypes != null) {
            for (Phenotype phenotype : phenotypes) {
                phenotypesIds.add(phenotype.getId());
                phenotypesIds.add(phenotype.getName());
            }
        }
        return new ArrayList(phenotypesIds);
    }

    public static List<String> populateDisorders(List<Disorder> disorders) {
        Set<String> disorderIds = new HashSet<>();
        if (disorders != null) {
            for (Disorder disorder : disorders) {
                disorderIds.add(disorder.getId());
                disorderIds.add(disorder.getName());
            }
        }
        return new ArrayList(disorderIds);
    }

    public static String type(QueryParam.Type type) {
        switch (type) {
            case TEXT:
                return "__s__";
            case TEXT_ARRAY:
                return "__sm__";
            case INTEGER:
            case LONG:
                return "__i__";
            case INTEGER_ARRAY:
            case LONG_ARRAY:
                return "__im__";
            case DECIMAL:
                return "__d__";
            case DECIMAL_ARRAY:
                return "__dm__";
            case BOOLEAN:
                return "__b__";
            case BOOLEAN_ARRAY:
                return "__bm__";
            default:
                return "__o__";
        }
    }

    /**
     * Parse internal OpenCGA ACLs.
     *
     * Given a List<Map<String, Object>> in the form [{member: user1, permissions: [VIEW, UPDATE]}, {member: user2, permissions: [DELETE]}],
     * return a Map<String, Set<String>> containing {user1: [VIEW, UPDATE], user2: [DELETE]}
     *
     * @param internalPermissions List containing the permissions for each member.
     * @return a map of permissions per member.
     */
    public static Map<String, Set<String>> parseInternalOpenCGAAcls(List<Map<String, Object>> internalPermissions) {
        if (internalPermissions == null) {
            return new HashMap<>();
        }
        Map<String, Set<String>> retPermissions = new HashMap<>(internalPermissions.size());

        internalPermissions.forEach(aclEntry ->
                retPermissions.put((String) aclEntry.get("member"), new HashSet<>((List<String>) aclEntry.get("permissions")))
        );

        return retPermissions;
    }

    /**
     * Get effective OpenCGA VIEW permissions.
     *
     * Given the map of permissions in the form {user1: [VIEW, UPDATE], user2: [DELETE]} for both the study and an entity, return the
     * effective read only permissions in the way [user1_VIEW, user2_NONE]
     *
     * @param studyPermissions Map of permissions.
     * @param entityPermissions Map of permissions.
     * @param entity Entity name.
     * @return a flattened list of effective VIEW permissions.
     */
    public static List<String> getEffectivePermissions(Map<String, Set<String>> studyPermissions,
                                                       Map<String, Set<String>> entityPermissions, String entity) {
        if (studyPermissions == null) {
            studyPermissions = new HashMap<>();
        }
        // entityPermissions are already fine, but we should increase that list with the ones contained in the studyPermissions if they are
        // not overrided by the entry permissions
        Map<String, Set<String>> additionalPermissions = new HashMap<>();
        studyPermissions.forEach((key, value) -> {
            if (!entityPermissions.containsKey(key)) {
                additionalPermissions.put(key, value);
            }
        });

        // FAMILY will need to become VIEW_FAMILIES, SAMPLE will be VIEW_SAMPLES
        String viewEntry = "VIEW_" + (entity.endsWith("Y") ? entity.replace("Y", "IE") : entity) + "S";
        String viewAnnotation = "VIEW_" + entity + "_ANNOTATIONS";

        List<String> permissions = new ArrayList<>((additionalPermissions.size() + entityPermissions.size()) * 3);
        addEffectivePermissions(entityPermissions, permissions, "VIEW", "VIEW_ANNOTATIONS");
        addEffectivePermissions(additionalPermissions, permissions, viewEntry, viewAnnotation);

        return permissions;
    }

    private static void addEffectivePermissions(Map<String, Set<String>> allPermissions, List<String> permissions, String viewPermission,
                                                String viewAnnotationPermission) {
        allPermissions.entrySet().forEach(aclEntry -> {
            List<String> currentPermissions = new ArrayList<>(2);
            if (aclEntry.getValue().contains(viewPermission)) {
                currentPermissions.add(aclEntry.getKey() + "__VIEW");
            }
            if (aclEntry.getValue().contains(viewAnnotationPermission)) {
                currentPermissions.add(aclEntry.getKey() + "__VIEW_ANNOTATIONS");
            }

            if (!currentPermissions.isEmpty()) {
                permissions.addAll(currentPermissions);
            } else {
                permissions.add(aclEntry.getKey() + "__NONE");
            }
        });
    }
}
