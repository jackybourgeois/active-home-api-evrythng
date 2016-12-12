package org.activehome.api.evrythng;

/*
 * #%L
 * Active Home :: API :: EVRYTHNG
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2016 Active Home Project
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import com.eclipsesource.json.JsonObject;
import java.util.HashMap;

/**
 * @author Jacky Bourgeois
 */
public class Thng {

    private String id;
    private long createdAt;
    private long updatedAt;
    private String name;
    private String product;
    private HashMap<String, Object> properties;

    public Thng(final JsonObject json) {
        id = json.getString("id", "");
        createdAt = json.getLong("createdAt", 0);
        updatedAt = json.getLong("updatedAt", 0);
        name = json.getString("name", "");
        product = json.getString("product", "");
        properties = new HashMap<>();
        if (json.get("properties") != null) {
            JsonObject jsonProp = json.get("properties").asObject();
            for (String key : jsonProp.names()) {
                properties.put(key, jsonProp.get(key).asString());
            }
        }
    }

    public final String getId() {
        return id;
    }

    public final long getCreatedAt() {
        return createdAt;
    }

    public final long getUpdatedAt() {
        return updatedAt;
    }

    public final String getName() {
        return name;
    }

    public final String getProduct() {
        return product;
    }

    public final HashMap<String, Object> getProperties() {
        return properties;
    }

}
