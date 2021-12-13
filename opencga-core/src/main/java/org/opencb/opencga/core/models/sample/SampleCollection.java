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

import org.opencb.biodata.models.core.OntologyTermAnnotation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SampleCollection {

    private List<OntologyTermAnnotation> from;
    private String type;
    private String quantity;
    private String method;
    private String date;
    private Map<String, Object> attributes;

    public SampleCollection() {
    }

    public SampleCollection(List<OntologyTermAnnotation> from, String type, String quantity, String method, String date,
                            Map<String, Object> attributes) {
        this.from = from;
        this.type = type;
        this.quantity = quantity;
        this.method = method;
        this.date = date;
        this.attributes = attributes;
    }

    public static SampleCollection init() {
        return new SampleCollection(new ArrayList<>(), "", "", "", "", new HashMap<>());
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("SampleCollection{");
        sb.append("from=").append(from);
        sb.append(", type='").append(type).append('\'');
        sb.append(", quantity='").append(quantity).append('\'');
        sb.append(", method='").append(method).append('\'');
        sb.append(", date='").append(date).append('\'');
        sb.append(", attributes=").append(attributes);
        sb.append('}');
        return sb.toString();
    }

    public List<OntologyTermAnnotation> getFrom() {
        return from;
    }

    public SampleCollection setFrom(List<OntologyTermAnnotation> from) {
        this.from = from;
        return this;
    }

    public String getType() {
        return type;
    }

    public SampleCollection setType(String type) {
        this.type = type;
        return this;
    }

    public String getQuantity() {
        return quantity;
    }

    public SampleCollection setQuantity(String quantity) {
        this.quantity = quantity;
        return this;
    }

    public String getMethod() {
        return method;
    }

    public SampleCollection setMethod(String method) {
        this.method = method;
        return this;
    }

    public String getDate() {
        return date;
    }

    public SampleCollection setDate(String date) {
        this.date = date;
        return this;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public SampleCollection setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
        return this;
    }
}
