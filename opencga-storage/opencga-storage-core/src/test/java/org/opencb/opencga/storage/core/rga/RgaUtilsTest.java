package org.opencb.opencga.storage.core.rga;

import org.opencb.biodata.models.clinical.Disorder;
import org.opencb.biodata.models.clinical.Phenotype;
import org.opencb.biodata.models.pedigree.IndividualProperty;
import org.opencb.biodata.models.variant.avro.ClinicalSignificance;
import org.opencb.biodata.models.variant.avro.PopulationFrequency;
import org.opencb.biodata.models.variant.avro.SequenceOntologyTerm;
import org.opencb.biodata.models.variant.avro.VariantType;
import org.opencb.opencga.core.models.analysis.knockout.KnockoutByIndividual;
import org.opencb.opencga.core.models.analysis.knockout.KnockoutTranscript;
import org.opencb.opencga.core.models.analysis.knockout.KnockoutVariant;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RgaUtilsTest {

    public static KnockoutByIndividual createKnockoutByIndividual(int count) {
        KnockoutByIndividual knockoutByIndividual = new KnockoutByIndividual();
        knockoutByIndividual.setId("id" + count);
        knockoutByIndividual.setSampleId("sample" + count);
        knockoutByIndividual.setSex(IndividualProperty.Sex.MALE);
        knockoutByIndividual.setDisorders(Collections.singletonList(new Disorder().setId("disorderId" + count).setName("disorderName" + count)));
        knockoutByIndividual.setPhenotypes(Collections.singletonList(new Phenotype().setId("phenotypeId" + count).setName("phenotypeName" + count)));

        List<KnockoutByIndividual.KnockoutGene> knockoutGeneList = new ArrayList<>(2);
        KnockoutByIndividual.KnockoutGene knockoutGene = new KnockoutByIndividual.KnockoutGene();
        knockoutGene.setId("geneId" + count);
        knockoutGene.setName("geneName" + count);

        List<KnockoutTranscript> knockoutTranscriptList = new ArrayList<>(2);
        knockoutTranscriptList.add(createTranscript(1));
        knockoutTranscriptList.add(createTranscript(2));
        knockoutGene.setTranscripts(knockoutTranscriptList);
        knockoutGeneList.add(knockoutGene);

        knockoutGene = new KnockoutByIndividual.KnockoutGene();
        knockoutGene.setId("geneId" + (count + 10));
        knockoutGene.setName("geneName" + (count + 10));

        knockoutTranscriptList = new ArrayList<>(2);
        knockoutTranscriptList.add(createTranscript(count));
        knockoutTranscriptList.add(createTranscript(count + 10));
        knockoutGene.setTranscripts(knockoutTranscriptList);
        knockoutGeneList.add(knockoutGene);

        knockoutByIndividual.setGenes(knockoutGeneList);
        return knockoutByIndividual;
    }

    public static KnockoutTranscript createTranscript(int count) {
        List<ClinicalSignificance> clinicalSignificance = Collections.emptyList();

        KnockoutTranscript knockoutTranscript = new KnockoutTranscript("transcriptId" + count);
        knockoutTranscript.setBiotype("biotype" + count);

        List<KnockoutVariant> knockoutVariantList = new ArrayList<>(2);
        List<PopulationFrequency> populationFrequencyList = new ArrayList<>(3);
        populationFrequencyList.add(new PopulationFrequency(RgaUtils.GNOMAD_GENOMES_STUDY, "ALL", "", "", 1f, 0.3f, 1f, 0.4f, 0.2f));
        populationFrequencyList.add(new PopulationFrequency(RgaUtils.THOUSAND_GENOMES_STUDY, "ALL", "", "", 1f, 0.04f, 1f, 0.04f, 0.02f));
        populationFrequencyList.add(new PopulationFrequency("otherStudy", "ALL", "", "", 1f, 0.04f, 1f, 0.004f, 0.2f));

        List<SequenceOntologyTerm> sequenceOntologyTermList = new ArrayList<>(3);
        sequenceOntologyTermList.add(new SequenceOntologyTerm("SO:0002019", "start_retained_variant"));
        sequenceOntologyTermList.add(new SequenceOntologyTerm("SO:0001891", "regulatory_region_amplification"));
        sequenceOntologyTermList.add(new SequenceOntologyTerm("SO:0000685", "DNAseI_hypersensitive_site"));

        knockoutVariantList.add(new KnockoutVariant("variant" + count, VariantType.SNV, "Genotype", 10, "PASS", "10", KnockoutVariant.KnockoutType.COMP_HET,
                sequenceOntologyTermList, populationFrequencyList, clinicalSignificance));

        populationFrequencyList = new ArrayList<>(3);
        populationFrequencyList.add(new PopulationFrequency(RgaUtils.GNOMAD_GENOMES_STUDY, "ALL", "", "", 1f, 0.2f, 1f, 0.04f, 0.02f));
        populationFrequencyList.add(new PopulationFrequency(RgaUtils.THOUSAND_GENOMES_STUDY, "ALL", "", "", 1f, 0.01f, 1f, 0.01f, 0.01f));
        populationFrequencyList.add(new PopulationFrequency("otherStudy", "ALL", "", "", 1f, 0.04f, 1f, 0.004f, 0.2f));
        knockoutVariantList.add(new KnockoutVariant("variant" + (count + 1), VariantType.SNV, "Genotype", 2, "NOT_PASS", "1",
                KnockoutVariant.KnockoutType.COMP_HET, sequenceOntologyTermList, populationFrequencyList, clinicalSignificance));
        knockoutTranscript.setVariants(knockoutVariantList);
        return knockoutTranscript;
    }

}
