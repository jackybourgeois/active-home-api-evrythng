package org.activehome.api.evrythng;

/*
 * #%L
 * Active Home :: IO :: EVRYTHNG
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

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import org.activehome.api.API;
import org.activehome.com.Notif;
import org.activehome.com.Request;
import org.activehome.com.ShowIfErrorCallback;
import org.activehome.context.data.DataPoint;
import org.kevoree.annotation.ComponentType;
import org.kevoree.annotation.Input;
import org.kevoree.annotation.Param;
import org.kevoree.annotation.Start;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Jacky Bourgeois
 */
@ComponentType(version = 1, description = "Active Home API for"
        + " the Evrythng platform.")
public class EvrythngAPI extends API {

    /**
     * Where to find the sources (for the Active Home store).
     */
    @Param(defaultValue = "/active-home-evrythng")
    private String src;
    @Param(optional = false)
    private String apiKey;
    @Param(optional = false)
    private String userName;
    @Param(optional = false)
    private String pass;

    public static final String EVRYTHNG_URL = "https://api.evrythng.com";

    private EvrythngUser user;
    private Map<String, Thng> localThngMap;

    @Start
    public final void start() {
        super.start();
        localThngMap = new HashMap<>();
    }

    @Override
    public void sendOutside(final String s) {

    }

    @Override
    public final void onInit() {
        super.onInit();
        try {
            user = loginIn(userName, pass, apiKey);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public final void onStartTime() {
        super.onStartTime();
        subscribeToContext();
    }

    /**
     * Subscribe to the context to publish on EVRYTHNG.
     */
    private void subscribeToContext() {
        Request request = new Request(this.getFullId(),
                this.getNode() + ".context",
                this.getCurrentTime(),
                "subscribe",
                new Object[]{new String[]{"*"}, this.getFullId()});
        logInfo(request.toString());
        sendRequest(request, new ShowIfErrorCallback());
    }

    /**
     * @param notifStr The received notification from the context as string
     */
    @Input
    public final void getNotif(final String notifStr) {
        logInfo(notifStr);
        Notif notif = new Notif(JsonObject.readFrom(notifStr));
        if (notif.getDest().compareTo(getFullId()) == 0
                && user != null) {
            try {
                if (notif.getContent() instanceof DataPoint) {
                    updateThng((DataPoint) notif.getContent(), user);
                } else if (notif.getContent() instanceof DataPoint[]) {
                    updateThng((DataPoint[]) notif.getContent(), user);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Send new properties to EVRYTHNG.
     */
    private void updateThng(final DataPoint dp,
                            final EvrythngUser evrythngUser) throws Exception {
        String thngName = dp.getMetricId()
                .substring(0, dp.getMetricId().indexOf("."));
        if (!localThngMap.containsKey(thngName)) {
            refreshLocalThngMap(evrythngUser);
            if (!localThngMap.containsKey(thngName)) {
                localThngMap.put(thngName, createThng(thngName, evrythngUser));
            }
        }

        JsonObject entry = new JsonObject();
        String propStr = dp.getMetricId().substring(
                dp.getMetricId().indexOf(".") + 1);
        entry.add("key", propStr.replaceAll(".", "--"));
        entry.add("value", dp.getValue());
        entry.add("timestamp", dp.getTS());

        JsonArray propArray = new JsonArray();
        propArray.add(entry);

        String url = EVRYTHNG_URL + "/thngs/"
                + localThngMap.get(thngName).getId() + "/properties";
        HelperHttp.send(url, "PUT",
                evrythngUser.getKey(), propArray.toString());
    }

    private void updateThng(final DataPoint[] dpArray,
                            final EvrythngUser evrythngUser) throws Exception {
        for (DataPoint dp : dpArray) {
            updateThng(dp, evrythngUser);
        }
    }

    private Thng createThng(final String name,
                            final EvrythngUser evrythngUser) throws Exception {
        JsonObject thngJson = new JsonObject();
        thngJson.add("name", name);
        String result = HelperHttp.send(EVRYTHNG_URL + "/thngs",
                "POST", evrythngUser.getKey(), thngJson.toString());
        return new Thng(JsonObject.readFrom(result));
    }

    public final JsonObject createUser(final String firstName,
                                 final String lastName,
                                 final String email,
                                 final String password,
                                 final String key) throws Exception {
        JsonObject userJson = new JsonObject();
        userJson.add("firstName", firstName);
        userJson.add("lastName", lastName);
        userJson.add("email", email);
        userJson.add("password", password);
        String result = HelperHttp.send(EVRYTHNG_URL + "/auth/evrythng/users",
                "POST", key, userJson.toString());
        return JsonObject.readFrom(result);
    }

    public final void activateUser(final String evrythngUser,
                             final String activationCode,
                             final String key) throws Exception {
        String url = EVRYTHNG_URL + "/auth/evrythng/users/"
                + evrythngUser + "/validate";
        JsonObject activeJson = new JsonObject();
        activeJson.add("activationCode", activationCode);
        String result = HelperHttp.send(url,
                "POST", key, activeJson.toString());
    }

    public final EvrythngUser loginIn(final String email,
                                final String password,
                                final String key) throws Exception {
        JsonObject credentialJson = new JsonObject();
        credentialJson.add("email", email);
        credentialJson.add("password", password);
        String result = HelperHttp.send(EVRYTHNG_URL + "/auth/evrythng",
                "POST", key, credentialJson.toString());
        JsonObject resultJson = JsonObject.readFrom(result);
        return new EvrythngUser(resultJson.get("evrythngUser").asString(),
                resultJson.get("evrythngApiKey").asString());
    }

    /**
     * Check EVRYTHNG for the list of all Thng and update the local map.
     */
    private void refreshLocalThngMap(final EvrythngUser evrythngUser)
            throws Exception {
        String result = HelperHttp.send(EVRYTHNG_URL + "/thngs",
                "GET", evrythngUser.getKey(), null);
        JsonArray jsonArray = JsonArray.readFrom(result);
        for (JsonValue json : jsonArray) {
            Thng thng = new Thng((JsonObject) json);
            localThngMap.put(thng.getName(), thng);
        }
    }

}
