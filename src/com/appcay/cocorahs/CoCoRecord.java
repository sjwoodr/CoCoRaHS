package com.appcay.cocorahs;

/**
 * Created with IntelliJ IDEA.
 * Precipitation History Record
 * User: sjwoodr
 * Date: 6/23/12
 */
public class CoCoRecord {
    String date;
    String time;
    String stationId;
    String stationName;
    String totalPrecip;
    String editLink;

    CoCoRecord(String date, String time, String stationId, String stationName, String totalPrecip, String editLink) {
        this.date = date;
        this.time = time;
        this.stationId = stationId;
        this.stationName = stationName;
        this.totalPrecip = totalPrecip;
        this.editLink = editLink;
    }

    public String getDate() { return date; }
    public String getTime() { return time; }
    public String getStationId() { return stationId; }
    public String getStationName() { return stationName; }
    public String getTotalPrecip() { return totalPrecip; }
    public String getEditLink() { return editLink; }

    public void setDate(String s) { date = s; }
    public void setTime(String s) { time = s; }
    public void setStationId(String s) { stationId = s; }
    public void setStationName(String s) { stationName = s; }
    public void setTotalPrecip(String s) { totalPrecip = s; }
    public void setEditLink(String s) { editLink = s; }
}
