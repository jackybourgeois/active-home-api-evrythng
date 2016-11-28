package org.activehome.api.evrythng.test;

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
import org.activehome.com.Notif;
import org.activehome.context.data.DataPoint;
import org.activehome.test.ComponentTester;
import org.kevoree.annotation.ComponentType;
import org.kevoree.annotation.Input;
import org.kevoree.annotation.Param;

import java.util.Random;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author Jacky Bourgeois
 * @version %I%, %G%
 */
@ComponentType(description = "Mock up to test Evrythng API.")
public class TesterEvrythngAPI extends ComponentTester {

    @Param(defaultValue = "/active-home-api-evrythng")
    private String src;

    private ScheduledThreadPoolExecutor stpe = new ScheduledThreadPoolExecutor(1);
    private boolean testDone = false;
    private Random rand = new Random();

    private String[] fakeMetrics = {"kettle.power", "ev.soc", "kettle.temperature", "inverter.pv.DC1"};

    @Override
    public final void onInit() {
        super.onInit();
        startTS = getTic().getTS();
        stpe.scheduleAtFixedRate(this::sendRandomData, 0, 5, TimeUnit.SECONDS);
    }

    @Override
    protected String logHeaders() {
        return "";
    }

    private void sendRandomData() {
        for (String metric : fakeMetrics) {
            sendNotif(new Notif(getFullId(), getNode() + ".context", getCurrentTime(),
                    new DataPoint(metric, getCurrentTime(), rand.nextDouble() + "")));
        }
    }

    @Override
    protected final JsonObject prepareNextTest() {
        if (!testDone) {
            testDone = true;
            JsonObject timeProp = new JsonObject();
            timeProp.set("startDate", startDate);
            timeProp.set("zip", 1800);
            return timeProp;
        }
        return null;
    }

    @Input
    public final void getNotif(final String notifStr) {

    }

}