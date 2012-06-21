package com.lowcountrysoftware.cocorahs;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created with IntelliJ IDEA.
 * CoCoComm is the class that deals with communicating with the CoCoRaHS web server (non-RESTful)
 * User: sjwoodr
 * Date: 6/21/12
 */
public class CoCoComm {
    private HttpContext localContext = new BasicHttpContext();
    private CookieStore cookieStore = new BasicCookieStore();
    private String viewState = "";
    private String stationId = "";
    private String stationName = "";

    CoCoComm() {
        localContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);
    }

    public String getStationName() {
        if(stationName.equals("")) {
            fetchStationId();
        }
        return stationName;
    }

    public String getStationId() {
        if(stationId.equals("")) {
            return fetchStationId();
        }
        else {
            return stationId;
        }
    }

    public void clearStation() {
        stationId = stationName = "";
    }

    private String getViewState() {
        String vs = "";

        try {
            BufferedReader in = null;
            HttpClient client = new DefaultHttpClient();
            HttpGet request = new HttpGet();
            request.setURI(new URI("http://www.cocorahs.org/Login.aspx"));
            HttpResponse response = client.execute(request, localContext);
            in = new BufferedReader
                    (new InputStreamReader(response.getEntity().getContent()));
            StringBuffer sb = new StringBuffer("");
            String line = "";
            String NL = System.getProperty("line.separator");
            while ((line = in.readLine()) != null) {
                sb.append(line + NL);
            }
            in.close();
            String page = sb.toString();
            vs = findPattern(page, "__VIEWSTATE\" value=\"(.*)\"", 1);
        } catch (Exception e) { CoCoRaHS.LOG("Exception occurred while fetching viewState: " + e.getMessage());}
        //LOG("__VIEWSTATE = " + vs);
        viewState = vs;
        return vs;
    }

    private String fetchStationId() {
        String vs = "";
        String id = "";
        String name = "";
        String date = "";
        String ampm = "";
        String time = "";

        try {
            BufferedReader in = null;
            HttpClient client = new DefaultHttpClient();
            HttpGet request = new HttpGet();
            request.setURI(new URI("http://www.cocorahs.org/Admin/MyDataEntry/DailyPrecipReport.aspx"));
            CoCoRaHS.LOG("Executing request " + request.getURI());
            HttpResponse response = client.execute(request, localContext);
            in = new BufferedReader
                    (new InputStreamReader(response.getEntity().getContent()));
            StringBuffer sb = new StringBuffer("");
            String line = "";
            String NL = System.getProperty("line.separator");
            while ((line = in.readLine()) != null) {
                sb.append(line + NL);
            }
            in.close();
            String page = sb.toString();
            vs = findPattern(page, "__VIEWSTATE\" value=\"(.*)\"", 1);
            if((vs != null) && (!vs.equals(""))) {
                viewState = vs;
            }
            id = findPattern(page, "frmReport_lblStationNumber\">(.*)</span", 1);
            name = findPattern(page, "frmReport_lblStationName\">(.*)</span", 1);
            date = findPattern(page, "value=\"(.*)\" id=\"frmReport_dcObsDate_t", 1);
            ampm = findPattern(page, "option selected=\"selected\" value=\"..\">(..)</option>", 1);
            time = findPattern(page, "input name=\"frmReport:tObsTime:txtTime\" type=\"text\" value=\"(.:..)\" maxlength", 1);
            CoCoRaHS.LOG("Date Observed: " + date + " " + time + " " + ampm);
        } catch (Exception e) { CoCoRaHS.LOG("Exception occurred while fetching viewState: " + e.getMessage());}
        stationId = id;
        stationName = name;
        return id;
    }

    private String findPattern(String src, String pattern, int groupNum){
        Pattern pp = Pattern.compile(pattern);
        Matcher m = pp.matcher(src);
        String result = "";
        try {
            while (m.find()) {
                if(groupNum <= m.groupCount()) {
                    result = m.group(groupNum);
                } else {
                    CoCoRaHS.LOG("findPattern::Group not found!");
                }
            }
        }
        catch (Exception e) {
            CoCoRaHS.LOG(e.getMessage());
        }
        return result;
    }

    public Boolean postLoginData(String url, String username, String password) {
        HttpClient httpclient = new DefaultHttpClient();
        Boolean login_ok = false;
        HttpPost httppost = new HttpPost(url);
        String viewState = "";

        CoCoRaHS.LOG("Fetching viewState...");
        viewState = getViewState();
        if(viewState.equals("")) {
            CoCoRaHS.LOG("Failed to fetch proper viewState for login page.");
            return false;
        }

        try {
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(5);
            nameValuePairs.add(new BasicNameValuePair("__VIEWSTATE", viewState));
            nameValuePairs.add(new BasicNameValuePair("txtUsername", username));
            nameValuePairs.add(new BasicNameValuePair("txtPassword", password));
            nameValuePairs.add(new BasicNameValuePair("cbSaveLogin","on"));
            nameValuePairs.add(new BasicNameValuePair("btnLogin", "Log+In"));
            httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs, HTTP.UTF_8));
            CoCoRaHS.LOG("Executing request " + httppost.getURI());
            HttpResponse response = httpclient.execute(httppost, localContext);

            String str = inputStreamToString(response.getEntity().getContent()).toString();
            String match = findPattern(str, "(DailyPrecipReport.aspx)", 1);
            if(match.contains("DailyPrecipReport.aspx")) {
                login_ok = true;
            }

        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return login_ok;
    }

    private StringBuilder inputStreamToString(InputStream is) {
        String line = "";
        StringBuilder total = new StringBuilder();
        BufferedReader rd = new BufferedReader(new InputStreamReader(is));
        try {
            while ((line = rd.readLine()) != null) {
                total.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
            CoCoRaHS.LOG("Exception in inputStreamToString(): " + e.getMessage());
        }
        return total;
    }
}
