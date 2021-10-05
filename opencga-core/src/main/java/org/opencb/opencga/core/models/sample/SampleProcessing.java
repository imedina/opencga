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

package org.opencb.opencga.core.models.sample;

import java.util.Map;

public class SampleProcessing {

    private String product;
    private String preparationMethod;
    private String extractionMethod;
    private String labSampleId;
    private String quantity;
    private String date;
    private Map<String, Object> attributes;

    public SampleProcessing() {
    }

    public SampleProcessing(String product, String preparationMethod, String extractionMethod, String labSampleId, String quantity,
                            String date, Map<String, Object> attributes) {
        this.product = product;
        this.preparationMethod = preparationMethod;
        this.extractionMethod = extractionMethod;
        this.labSampleId = labSampleId;
        this.quantity = quantity;
        this.date = date;
        this.attributes = attributes;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("SampleProcessing{");
        sb.append("product='").append(product).append('\'');
        sb.append(", preparationMethod='").append(preparationMethod).append('\'');
        sb.append(", extractionMethod='").append(extractionMethod).append('\'');
        sb.append(", labSampleId='").append(labSampleId).append('\'');
        sb.append(", quantity='").append(quantity).append('\'');
        sb.append(", date='").append(date).append('\'');
        sb.append(", attributes=").append(attributes);
        sb.append('}');
        return sb.toString();
    }

    public String getProduct() {
        return product;
    }

    public SampleProcessing setProduct(String product) {
        this.product = product;
        return this;
    }

    public String getPreparationMethod() {
        return preparationMethod;
    }

    public SampleProcessing setPreparationMethod(String preparationMethod) {
        this.preparationMethod = preparationMethod;
        return this;
    }

    public String getExtractionMethod() {
        return extractionMethod;
    }

    public SampleProcessing setExtractionMethod(String extractionMethod) {
        this.extractionMethod = extractionMethod;
        return this;
    }

    public String getLabSampleId() {
        return labSampleId;
    }

    public SampleProcessing setLabSampleId(String labSampleId) {
        this.labSampleId = labSampleId;
        return this;
    }

    public String getQuantity() {
        return quantity;
    }

    public SampleProcessing setQuantity(String quantity) {
        this.quantity = quantity;
        return this;
    }

    public String getDate() {
        return date;
    }

    public SampleProcessing setDate(String date) {
        this.date = date;
        return this;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public SampleProcessing setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
        return this;
    }
}
