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

import com.eclipsesource.json.JsonObject;
import com.evrythng.java.wrapper.ApiManager;
import com.evrythng.thng.resource.model.store.Thng;
import org.activehome.api.API;
import org.activehome.com.Notif;
import org.activehome.com.Request;
import org.activehome.com.ShowIfErrorCallback;
import org.activehome.com.Status;
import org.activehome.context.data.DataPoint;
import org.kevoree.annotation.Input;
import org.kevoree.annotation.Param;
import org.kevoree.annotation.Start;
import org.pcollections.PVector;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @author Jacky Bourgeois
 * @version %I%, %G%
 */
public class EvrythngAPI extends API {

    @Param(defaultValue = "Active Home API for the Evrythng platform.")
    private String description;
    @Param(defaultValue = "/active-home-evrythng")
    private String src;
    @Param(optional = false)
    private String apiKey;

    private ApiManager apiManager;
    private Map<String, Thng> localThngMap;

    @Start
    public void start() {
        super.start();
        apiManager = new ApiManager(apiKey);
        localThngMap = new HashMap<>();
    }

    @Override
    public void sendOutside(String s) {

    }

    @Override
    public final void onStartTime() {
        subscribeToContext();
    }


    /**
     * Subscribe to the context to publish on EVRYTHNG
     */
    private void subscribeToContext() {
        Request request = new Request(this.getFullId(),
                this.getNode() + ".context",
                this.getCurrentTime(),
                "subscribe",
                new Object[]{new String[]{"*"}, this.getFullId()});
        sendRequest(request, new ShowIfErrorCallback());
    }

    /**
     * @param notifStr The received notification from the context as string
     */
    @Input
    public final void getNotif(final String notifStr) {
        Notif notif = new Notif(JsonObject.readFrom(notifStr));
        if (notif.getDest().compareTo(getFullId()) == 0) {
            if (notif.getContent() instanceof DataPoint) {
                updateThng((DataPoint) notif.getContent());
            } else if (notif.getContent() instanceof DataPoint[]) {
                updateThng((DataPoint[]) notif.getContent());
            }
        }
    }

    private void updateThng(DataPoint dp) {
        String thngName = dp.getMetricId().substring(dp.getMetricId().indexOf("."));
        if (!localThngMap.containsKey(thngName)) {
            refreshLocalThngMap();
            if (!localThngMap.containsKey(thngName)) {
                localThngMap.put(thngName, createThng(thngName));
            }
        }
        apiManager.thngService().propertyUpdater(localThngMap.get(thngName).getId(),
                dp.getMetricId().substring(dp.getMetricId().indexOf(".")),
                dp.getValue());

    }

    public void updateThng(DataPoint[] dpArray) {
        for (DataPoint dp : dpArray) {
            updateThng(dp);
        }
    }

    private Thng createThng(String name) {
        Thng newThng = new Thng();
        newThng.setName(name);
        return apiManager.thngService().thngCreator(newThng).execute();
    }

    /**
     * Check EVRYTHNG for the list of all Thng and update the local map.
     */
    private void refreshLocalThngMap() {
        Iterator<PVector<Thng>> allMyThngs = apiManager
                .thngService().iterator().perPage(10).execute();
        while (allMyThngs.hasNext()) {
            PVector<Thng> page = allMyThngs.next();
            for (Thng thng : page) {
                localThngMap.put(thng.getName(), thng);
            }
        }
    }

}
