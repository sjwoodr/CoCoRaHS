package com.appcay.cocorahs;

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
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
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
    public String getObservedAmPm() { return observedAmPm; }

    public String getObservedDate() { return observedDate; }
    public String getObservedTime() { return observedTime; }
    public String getReportOkReason() { return report_ok_reason; }
    public void clearReportOkReason() { report_ok_reason = ""; }
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
            vs = vs.split(" ")[0].replaceAll("\"", "");
        } catch (Exception e) { CoCoRaHS.LOG("Exception occurred while fetching viewState: " + e.getMessage());}
        viewState = vs;
        return vs;
    }

    public ArrayList<CoCoRecord> getPrecipHistory(int maxdays) {
        ArrayList<CoCoRecord> history = new ArrayList<CoCoRecord>();

        try {
            HttpClient client = new DefaultHttpClient();
            HttpGet request = new HttpGet();
            request.setURI(new URI("http://www.cocorahs.org/Admin/MyDataEntry/ListDailyPrecipReports.aspx"));
            CoCoRaHS.LOG("Executing request " + request.getURI());
            HttpResponse response = client.execute(request, localContext);
            String page = inputStreamToString(response.getEntity().getContent()).toString();
            String vs = findPattern(page, "__VIEWSTATE\" value=\"(.*)\"", 1);
            vs = vs.split(" ")[0].replaceAll("\"", "");
            if((vs != null) && (!vs.equals(""))) {
                viewState = vs;
            }

            Document doc = Jsoup.parse(page);
            history.clear();
            for(int x=0; x< maxdays; x++) {
                // look for items that ar <tr> and class name ends with Item (GridItem, GridAltItem)
                Element gridItem = doc.select("tr[class$=Item]").select("tr").get(x);
                CoCoRecord historyItem = new CoCoRecord(gridItem.select("td").get(0).text(),
                        gridItem.select("td").get(1).text(), stationId, stationName,
                        gridItem.select("td").get(4).text(), gridItem.select("a").get(0).attr("href"));
                history.add(historyItem);
            }

        } catch (Exception e) { CoCoRaHS.LOG("Exception occurred while fetching history: " + e.toString() + " " + e.getMessage());}
        return history;
    }

    public String fetchStationId() {
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
            vs = vs.split(" ")[0].replaceAll("\"", "");
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

    public Boolean postPrecipReport(String url, String date, String time, String rain, String flooding,
                                    String notes,
                                    String new_snow, String new_snow_core, String total_snow, String total_snow_core) {
        HttpClient httpclient = new DefaultHttpClient();
        Boolean report_ok = false;
        HttpPost httppost = new HttpPost(url);
        String ampm = "AM";
        if(time.contains("PM")) {
            ampm = "PM";
        }
        time = time.replaceAll(" AM", "").replaceAll(" PM", "");
        String loc = "1";  // used to specify this, but no longer
        int d = flooding.indexOf(" ");
        if (d > 0) {
            flooding = flooding.substring(0, d);
        }
        if((notes == null) || notes.equals("")) {
            notes = "Submitted via CoCoRaHS Observer for Android";
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
            nameValuePairs.add(new BasicNameValuePair("frmReport:txtNotes", notes));
            nameValuePairs.add(new BasicNameValuePair("frmReport:prNewSnowAmount:tbPrecip", new_snow));
            nameValuePairs.add(new BasicNameValuePair("frmReport:prSnowCore:tbPrecip", new_snow_core));
            nameValuePairs.add(new BasicNameValuePair("frmReport:prTotalSnowDepth:tbPrecip", total_snow));
            nameValuePairs.add(new BasicNameValuePair("frmReport:prSWE:tbPrecip", total_snow_core));
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

            HashSet<String> attribs = new HashSet<String>();
            if(str.contains("to edit the existing report")) {
                report_ok = false;
                attribs.add("report_failed_dupe");
                CoCoRaHS.placedAgent.logCustomEvent("postPrecipReport", attribs);
                report_ok_reason = "A report for this date already exists.";
            }
            else if(str.contains("class='VAMValSummaryErrors'><li>")) {
                report_ok_reason = findPattern(str, "class='VAMValSummaryErrors'><li>(.*)</li>", 1);
                report_ok_reason = report_ok_reason.replaceAll("</li><li>", " ");
                CoCoRaHS.LOG("ERROR: " + report_ok_reason);
                attribs.add("report_failed_validation");
                CoCoRaHS.placedAgent.logCustomEvent("postPrecipReport", attribs);
            }
            else {
                String match = findPattern(str, "(ViewDailyPrecipReport.aspx)", 1);
                if(match.contains("ViewDailyPrecipReport.aspx")) {
                    report_ok = true;
                    attribs.add("report_ok");
                    CoCoRaHS.placedAgent.logCustomEvent("postPrecipReport", attribs);
                }
            }

            if(!report_ok) {
                CoCoRaHS.LOG(str);
            }

        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return report_ok;
    }

    protected StringBuilder inputStreamToString(InputStream is) {
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

    public Boolean isLoggedIn() {
        Boolean rc = true;
        if(viewState == null || viewState.equals("")) {
            rc = false;
        }
        return rc;
    }

    protected HttpContext localContext = new BasicHttpContext();
    protected CookieStore cookieStore = new BasicCookieStore();
    protected String viewState = "";
    protected String stationId = "";
    protected String stationName = "";
    protected String observedAmPm = "";
    protected String observedTime = "";
    protected String observedDate = "";
    protected String report_ok_reason = "";

    CoCoComm() {
        localContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);
    }

    public String getShortViewState() {
        return "" + viewState.length() + viewState.substring(0,8);
    }

    public String getStationName() {
        if(stationName.equals("")) {
            fetchStationId();
        }
        return stationName;
    }
}
