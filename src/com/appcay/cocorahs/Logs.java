package com.appcay.cocorahs;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import android.content.Context;
import android.content.Intent;

public class Logs {	
	static String TAG = "CoCoRaHS";

	public Logs() { }
	
	public ArrayList<String> getLogs() {
		ArrayList<String> listOfLogLines = null;
        try {
	        Process mLogcatProc = null;
	        BufferedReader reader = null;
	        mLogcatProc = Runtime.getRuntime().exec(
	            new String[] {"logcat", "-v", "time", "-d", "AndroidRuntime:E CoCoRaHS:V *:S" });
	        reader = new BufferedReader(new InputStreamReader (mLogcatProc.getInputStream()));
	        String line;
	        listOfLogLines = new ArrayList<String>();
	        while ((line = reader.readLine()) != null)
	        {
	            listOfLogLines.add(line);
	        }
        } catch (Exception e) {
        	CoCoRaHS.LOG("Error: Can't get system logs: "  + e.getMessage());
        }
        return listOfLogLines;
    }
	
	public void sendLogs(Context c, String emailaddr, String subject, String emailtext) {		
		Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);	                               
	    emailIntent.setType("plain/text");	                                 
	    emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{ emailaddr});	                               
	    emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, subject);	                               
	    emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, emailtext);	                       
	    c.startActivity(Intent.createChooser(emailIntent, "Send mail..."));	 
	}
}
