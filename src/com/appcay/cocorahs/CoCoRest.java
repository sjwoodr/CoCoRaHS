package com.appcay.cocorahs;

import org.apache.http.client.protocol.ClientContext;
import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * CoCoRest is the class that communicates with the CoCoRaHS RESTful API (future)
 * User: stevewoodruff
 * Date: 6/21/12
 */

public class CoCoRest extends CoCoComm {

    private String getViewState() {
        // shouldn't need this with REST
        return null;
    }

    public ArrayList<CoCoRecord> getPrecipHistory(int maxdays) {
        ArrayList<CoCoRecord> history = new ArrayList<CoCoRecord>();
        return history;
    }

    public String fetchStationId() {
        // unsure if we'll need this or if it'll be in the json
        return null;
    }

    public Boolean postLoginData(String url, String username, String password) {
        // needs update for REST
        return false;
    }

    public Boolean postPrecipReport(String url, String date, String time, String rain, String regLocation, String flooding) {
        // needs update for REST
        return false;
    }

    CoCoRest() {
        localContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);
    }

    public String getStationName() {
        if(stationName.equals("")) {
            fetchStationId();
        }
        return stationName;
    }
}

