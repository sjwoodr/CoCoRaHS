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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
    private String observedAmPm = "";
    private String observedTime = "";
    private String observedDate = "";
    private String report_ok_reason = "";
    private String report_ok_callback = "";

    CoCoComm() {
        localContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);
    }

    public String getStationName() {
        if(stationName.equals("")) {
            fetchStationId();
        }
        return stationName;
    }

    public String getObservedAmPm() { return observedAmPm; }
    public String getObservedDate() { return observedDate; }
    public String getObservedTime() { return observedTime; }
    public String getReportOkReason() { return report_ok_reason; }

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

    private String getPrecipHistory() {
        String vs = "";

        try {
            BufferedReader in = null;
            HttpClient client = new DefaultHttpClient();
            HttpGet request = new HttpGet();
            request.setURI(new URI("http://www.cocorahs.org/Admin/MyDataEntry/ListDailyPrecipReports.aspx"));
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
        } catch (Exception e) { CoCoRaHS.LOG("Exception occurred while fetching history: " + e.getMessage());}
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
            date = findPattern(page, "value=\"(.*)\" id=\"frmReport_dcObsDate\"", 1);
            ampm = findPattern(page, "option selected=\"selected\" value=\"..\">(..)</option>", 1);
            time = findPattern(page, "input name=\"frmReport:tObsTime:txtTime\" type=\"text\" value=\"(.:..)\" maxlength", 1);
            CoCoRaHS.LOG("Date Observed: " + date + " " + time + " " + ampm);
        } catch (Exception e) { CoCoRaHS.LOG("Exception occurred while fetching viewState: " + e.getMessage());}
        stationId = id;
        stationName = name;
        observedTime = time;
        observedDate = date;
        observedAmPm = ampm;
        return id;
    }

    public static String findPattern(String src, String pattern, int groupNum){
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
            HashSet<String> attribs = new HashSet<String>();
            if(match.contains("DailyPrecipReport.aspx")) {
                login_ok = true;
                attribs.add("login_ok");
                CoCoRaHS.placedAgent.logCustomEvent("postLoginData", attribs);
            } else {
                attribs.add("login_failed");
                CoCoRaHS.placedAgent.logCustomEvent("postLoginData", attribs);
            }

        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return login_ok;
    }

    public Boolean postPrecipReport(String url, String date, String time, String rain, String regLocation, String flooding) {
        HttpClient httpclient = new DefaultHttpClient();
        Boolean report_ok = false;
        HttpPost httppost = new HttpPost(url);
        String loc = "1";
        String ampm = "AM";
        if(time.contains("PM")) {
            ampm = "PM";
        }
        time = time.replaceAll(" AM", "").replaceAll(" PM", "");
        if(! regLocation.equals("Registered Location")) {
            loc = "0";
        }
        int d = flooding.indexOf(" ");
        if (d > 0) {
            flooding = flooding.substring(0, d);
        }
        String[] dateParts = date.split("/");
        String funkyDate = dateParts[2] + "-" + dateParts[0] + "-" + dateParts[1] + "-0-0-0-0";

        try {
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(22);
            nameValuePairs.add(new BasicNameValuePair("__EVENTTARGET", ""));
            nameValuePairs.add(new BasicNameValuePair("__EVENTARGUMENT", ""));
            nameValuePairs.add(new BasicNameValuePair("VAM_Group", ""));
            nameValuePairs.add(new BasicNameValuePair("__VIEWSTATE", viewState));
            nameValuePairs.add(new BasicNameValuePair("frmReport:btnSubmitTop", "Submit Data"));
            nameValuePairs.add(new BasicNameValuePair("frmReport:dcObsDate", date));
            nameValuePairs.add(new BasicNameValuePair("frmReport_dcObsDate_p", funkyDate));
            nameValuePairs.add(new BasicNameValuePair("frmReport:tObsTime:txtTime", time));
            nameValuePairs.add(new BasicNameValuePair("frmReport:tObsTime:ddlAmPm", ampm));
            nameValuePairs.add(new BasicNameValuePair("frmReport:prTotalPrecip:tbPrecip", rain));
            nameValuePairs.add(new BasicNameValuePair("frmReport:rblTakenAtRegisteredLocation", loc));
            nameValuePairs.add(new BasicNameValuePair("frmReport:txtNotes", "Submitted via CoCoRaHS Observer for Android"));
            nameValuePairs.add(new BasicNameValuePair("frmReport:prNewSnowAmount:tbPrecip", "NA"));
            nameValuePairs.add(new BasicNameValuePair("frmReport:prSnowCore:tbPrecip", "NA"));
            nameValuePairs.add(new BasicNameValuePair("frmReport:prTotalSnowDepth:tbPrecip", "NA"));
            nameValuePairs.add(new BasicNameValuePair("frmReport:prSWE:tbPrecip", "NA"));
            nameValuePairs.add(new BasicNameValuePair("frmReport:tbPrecipBegan", ""));
            nameValuePairs.add(new BasicNameValuePair("frmReport:tbPrecipEnded", ""));
            nameValuePairs.add(new BasicNameValuePair("frmReport:tbHeavyPrecipBegan", ""));
            nameValuePairs.add(new BasicNameValuePair("frmReport:tbHeavyPrecipMinLasted", ""));
            nameValuePairs.add(new BasicNameValuePair("frmReport:ddlPrecipTimeAccuracy", ""));
            nameValuePairs.add(new BasicNameValuePair("frmReport:ddlFlooding", flooding));

            httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs, HTTP.UTF_8));
            CoCoRaHS.LOG("Executing request " + httppost.getURI());
            HttpResponse response = httpclient.execute(httppost, localContext);

            String str = inputStreamToString(response.getEntity().getContent()).toString();
            CoCoRaHS.LOG("Debug: " + str);

            HashSet<String> attribs = new HashSet<String>();
            if(str.contains("to edit the existing report")) {
                report_ok = false;
                attribs.add("report_failed_dupe");
                CoCoRaHS.placedAgent.logCustomEvent("postPrecipReport", attribs);
                report_ok_reason = "A report for this date already exists.";
                report_ok_callback = "";        //TODO: set this to the url given in the response
            }
            else if(str.contains("class='VAMValSummaryErrors'><li>Total Precipitation is required.</li>")) {
                report_ok_reason = findPattern(str, "class='VAMValSummaryErrors'><li>(.*)</li>", 1);
                CoCoRaHS.LOG("ERROR: " + report_ok_reason);
            }
            else {
                String match = findPattern(str, "(ViewDailyPrecipReport.aspx)", 1);
                if(match.contains("ViewDailyPrecipReport.aspx")) {
                    report_ok = true;
                    attribs.add("report_ok");
                    CoCoRaHS.placedAgent.logCustomEvent("postPrecipReport", attribs);
                }
            }

        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return report_ok;
    }

    private StringBuilder inputStreamToString(InputStream is) {
        String line = "";
        StringBuilder total = new StringBuilder();
        BufferedReader rd = new BufferedReader(new InputStreamReader(is));
        try {
            while ((line = rd.readLine()) != null) {
                CoCoRaHS.LOG("XXX: " + line);
                total.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
            CoCoRaHS.LOG("Exception in inputStreamToString(): " + e.getMessage());
        }
        return total;
    }
}
