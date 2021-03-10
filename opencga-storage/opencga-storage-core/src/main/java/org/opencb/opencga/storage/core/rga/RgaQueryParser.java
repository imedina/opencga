package org.opencb.opencga.storage.core.rga;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.opencb.biodata.models.variant.annotation.ConsequenceTypeMappings;
import org.opencb.commons.datastore.core.Query;
import org.opencb.commons.datastore.solr.FacetQueryParser;
import org.opencb.opencga.core.models.analysis.knockout.KnockoutVariant;
import org.opencb.opencga.storage.core.exceptions.RgaException;
import org.opencb.opencga.storage.core.variant.query.VariantQueryUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Pattern;

import static org.opencb.opencga.core.models.analysis.knockout.KnockoutVariant.KnockoutType.*;
import static org.opencb.opencga.storage.core.rga.RgaQueryParams.*;
import static org.opencb.opencga.storage.core.variant.query.VariantQueryUtils.printQuery;

public class RgaQueryParser {

    private static final Pattern FACET_RANGE_PATTERN = Pattern.compile("([_a-zA-Z]+)\\[([_a-zA-Z]+):([.a-zA-Z0-9]+)\\]:([.0-9]+)$");
    public static final String SEPARATOR = "__";

    protected static Logger logger = LoggerFactory.getLogger(RgaQueryParser.class);

    /**
     * Create a SolrQuery object from Query and QueryOptions.
     *
     * @param query         Query
     * @return              SolrQuery
     * @throws RgaException RgaException.
     */
    public SolrQuery parseQuery(Query query) throws RgaException {
        SolrQuery solrQuery = new SolrQuery();

        Query finalQuery = new Query(query);
        fixQuery(finalQuery);

        List<String> filterList = new ArrayList<>();
        parseStringValue(finalQuery, SAMPLE_ID, RgaDataModel.SAMPLE_ID, filterList);
        parseStringValue(finalQuery, INDIVIDUAL_ID, RgaDataModel.INDIVIDUAL_ID, filterList);
        parseStringValue(finalQuery, SEX, RgaDataModel.SEX, filterList);
        parseStringValue(finalQuery, PHENOTYPES, RgaDataModel.PHENOTYPES, filterList);
        parseStringValue(finalQuery, DISORDERS, RgaDataModel.DISORDERS, filterList);
        parseStringValue(finalQuery, CHROMOSOME, RgaDataModel.CHROMOSOME, filterList);
        parseStringValue(finalQuery, START, RgaDataModel.START, filterList);
        parseStringValue(finalQuery, END, RgaDataModel.END, filterList);
        parseStringValue(finalQuery, NUM_PARENTS, RgaDataModel.NUM_PARENTS, filterList);
        parseStringValue(finalQuery, GENE_ID, RgaDataModel.GENE_ID, filterList);
        parseStringValue(finalQuery, GENE_NAME, RgaDataModel.GENE_NAME, filterList);
        parseStringValue(finalQuery, TRANSCRIPT_ID, RgaDataModel.TRANSCRIPT_ID, filterList);
//        parseStringValue(finalQuery, TRANSCRIPT_BIOTYPE, RgaDataModel.TRANSCRIPT_BIOTYPE, filterList);
        parseStringValue(finalQuery, VARIANTS, RgaDataModel.VARIANTS, filterList);
        parseFilterValue(finalQuery, filterList);

        // Create Solr query, adding filter queries and fields to show
        solrQuery.setQuery("*:*");
        filterList.forEach(solrQuery::addFilterQuery);

        logger.debug("----------------------");
        logger.debug("query     : " + printQuery(finalQuery));
        logger.debug("solrQuery : " + solrQuery);
        return solrQuery;
    }

    private void fixQuery(Query query) {
        if (query.containsKey(CONSEQUENCE_TYPE.key())) {
            // Convert CONSEQUENCE TYPES to full SO Terms so they can be successfully processed
            List<String> orConsequenceTypeList = query.getAsStringList(CONSEQUENCE_TYPE.key(), ",");
            List<String> andConsequenceTypeList = query.getAsStringList(CONSEQUENCE_TYPE.key(), ";");

            List<String> consequenceTypeList;
            String separator;
            if (orConsequenceTypeList.size() >= andConsequenceTypeList.size()) {
                consequenceTypeList = orConsequenceTypeList;
                separator = ",";
            } else {
                consequenceTypeList = andConsequenceTypeList;
                separator = ";";
            }

            List<String> result = new ArrayList<>(consequenceTypeList.size());
            for (String ct : consequenceTypeList) {
                if (ct.startsWith("SO:")) {
                    result.add(ct);
                } else {
                    result.add(ConsequenceTypeMappings.getSoAccessionString(ct));
                }
            }

            query.put(CONSEQUENCE_TYPE.key(), StringUtils.join(result, separator));
        }
    }

    private void parseFilterValue(Query query, List<String> filterList) throws RgaException {
        List<String> knockoutValues = query.getAsStringList(KNOCKOUT.key());
        String filterValue = query.getString(FILTER.key());
        List<String> ctValues = query.getAsStringList(CONSEQUENCE_TYPE.key());
        List<String> popFreqValues = query.getAsStringList(POPULATION_FREQUENCY.key(), ";");

        int count = 0;
        count += knockoutValues.isEmpty() ? 0 : 1;
        count += StringUtils.isEmpty(filterValue) ? 0 : 1;
        count += ctValues.isEmpty() ? 0 : 1;
        count += popFreqValues.isEmpty() ? 0 : 1;

        if (count == 1) {
            // Simple filter
            parseStringValue(query, KNOCKOUT, RgaDataModel.KNOCKOUT_TYPES, filterList);
            parseStringValue(query, FILTER, RgaDataModel.FILTERS, filterList);
            parseStringValue(query, CONSEQUENCE_TYPE, RgaDataModel.CONSEQUENCE_TYPES, filterList);

            if (!popFreqValues.isEmpty()) {
                List<List<String>> encodedPopFreqs = RgaUtils.parsePopulationFrequencyQuery(popFreqValues);

                List<String> popFreqList = new ArrayList<>(encodedPopFreqs.size());
                for (List<String> encodedPopFreq : encodedPopFreqs) {
                    parseStringValue(encodedPopFreq, "", popFreqList, "||");
                }
                // TODO: The pop freq key is dynamic
                parseStringValue(popFreqList, RgaDataModel.POPULATION_FREQUENCIES, filterList, "&&");
            }
        } else if (count > 1) {
            if (knockoutValues.size() == 1 && KnockoutVariant.KnockoutType.COMP_HET.name().equals(knockoutValues.get(0).toUpperCase())) {
                // COMP_HET complex filter
                buildCompHetComplexQueryFilter(filterList, filterValue, ctValues, popFreqValues);
            } else {
                // Complex filter
                buildComplexQueryFilter(filterList, knockoutValues, filterValue, ctValues, popFreqValues);
            }
        }
    }

    private void buildCompHetComplexQueryFilter(List<String> filterList, String filterValue, List<String> ctList, List<String> popFreqList)
            throws RgaException {
        String koValue = RgaUtils.encode(COMP_HET.name());

        // Filter
        List<String> filterValues;
        if (StringUtils.isEmpty(filterValue)) {
            filterValues = Arrays.asList("PASS", "NOT_PASS");
        } else {
            filterValues = Collections.singletonList(filterValue);
        }
        filterValues = RgaUtils.parseFilterQuery(filterValues);

        // CT
        List<String> ctValues;
        if (!ctList.isEmpty()) {
            ctValues = new ArrayList<>(ctList.size());
            for (String ctValue : ctList) {
                String encodedValue = String.valueOf(VariantQueryUtils.parseConsequenceType(ctValue));
                ctValues.add(encodedValue);
            }
        } else {
            ctValues = Collections.emptyList();
        }

        // Pop. freq
        List<List<String>> popFreqQueryList = RgaUtils.parsePopulationFrequencyQuery(popFreqList);

        buildComplexQuery(Collections.singletonList(koValue), generateSortedCombinations(filterValues),
                generateSortedCombinations(ctValues), popFreqQueryList, filterList);

    }

    private void buildComplexQueryFilter(List<String> filterList, List<String> knockoutList, String filterValue, List<String> ctList,
                                         List<String> popFreqList) throws RgaException {
        // KT
        List<String> koValues;
        if (knockoutList.isEmpty()) {
            koValues = Arrays.asList(COMP_HET.name(), DELETION_OVERLAP.name(), HET_ALT.name(), HOM_ALT.name());
        } else {
            koValues = knockoutList;
        }
        koValues = RgaUtils.parseKnockoutTypeQuery(koValues);

        // Filter
        List<String> filterValues;
        if (StringUtils.isEmpty(filterValue)) {
            filterValues = Arrays.asList("PASS", "NOT_PASS");
        } else {
            filterValues = Collections.singletonList(filterValue);
        }
        filterValues = RgaUtils.parseFilterQuery(filterValues);

        // CT
        List<String> ctValues;
        if (!ctList.isEmpty()) {
            ctValues = new ArrayList<>(ctList.size());
            for (String ctValue : ctList) {
                String encodedValue = String.valueOf(VariantQueryUtils.parseConsequenceType(ctValue));
                ctValues.add(encodedValue);
            }
        } else {
            ctValues = Collections.emptyList();
        }

        // Pop. freq
        List<List<String>> popFreqQueryList = RgaUtils.parsePopulationFrequencyQuery(popFreqList);

        buildComplexQuery(koValues, filterValues, ctValues, popFreqQueryList, filterList);
    }

    private void buildComplexQuery(List<String> koValues, List<String> filterValues, List<String> ctValues,
                                   List<List<String>> popFreqQueryList, List<String> filterList) {
        if (ctValues.isEmpty() && popFreqQueryList.isEmpty()) {
            // KT + FILTER
            List<String> orFilterList = new LinkedList<>();
            for (String koValue : koValues) {
                for (String filterVal : filterValues) {
                    orFilterList.add(koValue + SEPARATOR + filterVal);
                }
            }
            parseStringValue(orFilterList, RgaDataModel.COMPOUND_FILTERS, filterList, "||");
        } else if (!ctValues.isEmpty() && !popFreqQueryList.isEmpty()) {
            // KT + FILTER + CT + POP_FREQ
            List<String> andQueryList = new ArrayList<>(popFreqQueryList.size());
            for (List<String> tmpPopFreqList : popFreqQueryList) {
                List<String> orQueryList = new LinkedList<>();
                for (String popFreq : tmpPopFreqList) {
                    for (String koValue : koValues) {
                        for (String filterVal : filterValues) {
                            for (String ctValue : ctValues) {
                                orQueryList.add(koValue + SEPARATOR + filterVal + SEPARATOR + ctValue + SEPARATOR + popFreq);
                            }
                        }
                    }
                }
                parseStringValue(orQueryList, "", andQueryList, "||");
            }
            parseStringValue(andQueryList, RgaDataModel.COMPOUND_FILTERS, filterList, "&&");
        } else if (!ctValues.isEmpty()) {
            // KT + FILTER + CT
            List<String> orFilterList = new LinkedList<>();
            for (String koValue : koValues) {
                for (String filterVal : filterValues) {
                    for (String ctValue : ctValues) {
                        orFilterList.add(koValue + SEPARATOR + filterVal + SEPARATOR + ctValue);
                    }
                }
            }
            parseStringValue(orFilterList, RgaDataModel.COMPOUND_FILTERS, filterList, "||");
        } else { // POP_FREQ not empty
            // KT + FILTER + POP_FREQ
            List<String> andQueryList = new ArrayList<>(popFreqQueryList.size());
            for (List<String> tmpPopFreqList : popFreqQueryList) {
                List<String> orQueryList = new LinkedList<>();
                for (String popFreq : tmpPopFreqList) {
                    for (String koValue : koValues) {
                        for (String filterVal : filterValues) {
                            orQueryList.add(koValue + SEPARATOR + filterVal + SEPARATOR + popFreq);
                        }
                    }
                }
                parseStringValue(orQueryList, "", andQueryList, "||");
            }
            parseStringValue(andQueryList, RgaDataModel.COMPOUND_FILTERS, filterList, "&&");
        }
    }

    public static List<String> generateSortedCombinations(List<String> list) {
        Set<String> results = new HashSet<>();
        for (String term1 : list) {
            for (String term2 : list) {
                if (StringUtils.compare(term1, term2) <= 0) {
                    results.add(term1 + SEPARATOR + term2);
                } else {
                    results.add(term2 + SEPARATOR + term1);
                }
            }
        }
        return new ArrayList<>(results);
    }

    private void parseStringValue(Query query, RgaQueryParams queryParam, String storageKey, List<String> filterList) {
        parseStringValue(query, queryParam, storageKey, filterList, "||");
    }

    private void parseStringValue(Query query, RgaQueryParams queryParam, String storageKey, List<String> filterList, String opSeparator) {
        if (StringUtils.isNotEmpty(query.getString(queryParam.key()))) {
            List<String> escapedValues = escapeValues(query.getAsStringList(queryParam.key()));
            parseStringValue(escapedValues, storageKey, filterList, opSeparator);
        }
    }

    private void parseStringValue(List<String> values, String storageKey, List<String> filterList, String opSeparator) {
        String separator = " " + opSeparator + " ";

        if (!values.isEmpty()) {
            String value = values.size() == 1 ? values.get(0) : "( " + StringUtils.join(values, separator) + " )";
            if (StringUtils.isNotEmpty(storageKey)) {
                filterList.add(storageKey + ":" + value);
            } else {
                filterList.add(value);
            }
        }
    }

    private List<String> escapeValues(List<String> values) {
        List<String> result = new ArrayList<>(values.size());
        for (String value : values) {
            result.add(value.replace(":", "\\:"));
        }
        return result;
    }

    public String parseFacet(String facetQuery) {
        StringBuilder sb = new StringBuilder();
        String[] facets = facetQuery.split(FacetQueryParser.FACET_SEPARATOR);

        for (int i = 0; i < facets.length; i++) {
            if (i > 0) {
                sb.append(FacetQueryParser.FACET_SEPARATOR);
            }
            String[] nestedFacets = facets[i].split(FacetQueryParser.NESTED_FACET_SEPARATOR);
            for (int j = 0; j < nestedFacets.length; j++) {
                if (j > 0) {
                    sb.append(FacetQueryParser.NESTED_FACET_SEPARATOR);
                }
                String[] nestedSubfacets = nestedFacets[j].split(FacetQueryParser.NESTED_SUBFACET_SEPARATOR);
                for (int k = 0; k < nestedSubfacets.length; k++) {
                    if (k > 0) {
                        sb.append(FacetQueryParser.NESTED_SUBFACET_SEPARATOR);
                    }
                    // Convert to Solr schema fields, if necessary
                    sb.append(toSolrSchemaFields(nestedSubfacets[k]));
                }
            }
        }

        return sb.toString();
    }

    private String toSolrSchemaFields(String facet) {
//        if (facet.contains(CHROM_DENSITY)) {
//            return parseChromDensity(facet);
//        } else if (facet.contains(ANNOT_FUNCTIONAL_SCORE.key())) {
//            return parseFacet(facet, ANNOT_FUNCTIONAL_SCORE.key());
//        } else if (facet.contains(ANNOT_CONSERVATION.key())) {
//            return parseFacet(facet, ANNOT_CONSERVATION.key());
//        } else if (facet.contains(ANNOT_PROTEIN_SUBSTITUTION.key())) {
//            return parseFacet(facet, ANNOT_PROTEIN_SUBSTITUTION.key());
//        } else if (facet.contains(ANNOT_POPULATION_ALTERNATE_FREQUENCY.key())) {
//            return parseFacetWithStudy(facet, "popFreq");
//        } else if (facet.contains(STATS_ALT.key())) {
//            return parseFacetWithStudy(facet, "altStats");
//        } else if (facet.contains(SCORE.key())) {
//            return parseFacetWithStudy(facet, SCORE.key());
//        } else {
            return facet;
//        }
    }

}
