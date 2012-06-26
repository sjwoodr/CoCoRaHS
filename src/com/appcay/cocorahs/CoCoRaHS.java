package com.appcay.cocorahs;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.test.suitebuilder.TestSuiteBuilder;
import android.util.Log;
import android.view.*;
import android.widget.*;
import com.placed.client.android.PlacedAgent;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;

public class CoCoRaHS extends Activity
{

    Context mContext = null;
    ProgressDialog progressDialog = null;
    CoCoComm comm = new CoCoComm();
    static PlacedAgent placedAgent = null;
    Menu cocoMenu = null;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.main);
        mContext = this;
        placedAgent = PlacedAgent.getInstance(mContext, "c6ff9337c4f9");
        handleButtons();
        placedAgent.logPageView("Login Screen");
    }

    @Override
    public void onStart() {
        super.onStart();
        placedAgent.logStartSession();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        placedAgent.logEndSession();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        cocoMenu = menu;
        getMenuInflater().inflate(R.menu.option_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        switch (item.getItemId()) {
            case R.id.history:
                if(comm.isLoggedIn()) {
                    showHistory();
                }
                else {
                    showOKAlertMsg("Whoops!", "You must log in before viewing history.", false);
                }
                return true;
        }
        return false;
    }

    private void handleButtons() {
        CheckBox cbSaveLogin = (CheckBox) findViewById(R.id.cbSaveLogin);
        if(cbSaveLogin != null) {
            String username = getPreference("username", mContext);
            String password = getPreference("password", mContext);
            EditText etUser = (EditText) findViewById(R.id.etUsername);
            if(etUser != null) {
                etUser.setText(username);
            }
            EditText etPass = (EditText) findViewById(R.id.etPassword);
            if(etPass != null) {
                etPass.setText(password);
            }
            if(username.length() > 0 && password.length() > 0) {
                cbSaveLogin.setChecked(true);
            }
            else {
                cbSaveLogin.setChecked(false);
            }
        }

        Button btnSubmitPrecip = (Button) findViewById(R.id.btnSubmit);
        if(btnSubmitPrecip != null) {
            btnSubmitPrecip.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    EditText etObTime = (EditText) findViewById(R.id.etObTime);
                    EditText etObDate = (EditText) findViewById(R.id.etObDate);
                    EditText etObRain = (EditText) findViewById(R.id.etObRain);
                    Spinner spnLoc = (Spinner) findViewById(R.id.spnLoc);
                    Spinner spnFlood = (Spinner) findViewById(R.id.spnFlood);
                    if(etObTime != null && etObDate != null && etObRain != null && spnLoc != null && spnFlood != null) {
                        progressDialog = ProgressDialog.show(CoCoRaHS.this, "", "Submitting...", true);
                        new PrecipTask().execute("http://www.cocorahs.org/Admin/MyDataEntry/DailyPrecipReport.aspx",
                                etObDate.getText().toString().trim(), etObTime.getText().toString().trim(),
                                etObRain.getText().toString().trim(),
                                spnLoc.getSelectedItem().toString(), spnFlood.getSelectedItem().toString());
                    }
                }
            });
        }

        Button btnLogin = (Button) findViewById(R.id.btnLogin);
        if(btnLogin != null) {
            btnLogin.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    String username = "", password = "";
                    EditText etUser = (EditText) findViewById(R.id.etUsername);
                    if(etUser != null) {
                        username = etUser.getText().toString().toLowerCase().trim();
                    }
                    EditText etPass = (EditText) findViewById(R.id.etPassword);
                    if(etPass != null) {
                        password = etPass.getText().toString().trim();
                    }
                    CheckBox cbSaveLogin = (CheckBox) findViewById(R.id.cbSaveLogin);
                    if(cbSaveLogin != null) {
                        if(cbSaveLogin.isChecked()) {
                            LOG("Saving user/pass for future login...");
                            savePreference("username", username, mContext);
                            savePreference("password", password, mContext);
                        }
                        else {
                            LOG("Clearing saved user/pass...");
                            savePreference("username", "", mContext);
                            savePreference("password", "", mContext);
                        }
                    }
                    progressDialog = ProgressDialog.show(CoCoRaHS.this, "", "Authenticating...", true);
                    new LoginTask().execute("http://www.cocorahs.org/Login.aspx", username, password);
                }
            });
        }
    }

    protected Dialog onCreateDialog(int id) {
        Dialog dialog = null;
        switch (id) {
            default:
                dialog = null;
        }
        return dialog;
    }

    public static String savePreference(String key, String value, Context c) {
        String value2 = value.replaceAll("[^\\d\\w\\.@]", "").replaceAll("\000", "");
        SharedPreferences mySharedPrefs = c.getSharedPreferences("CoCoRaHS-Prefs",
                Activity.MODE_WORLD_WRITEABLE);
        SharedPreferences.Editor editor = mySharedPrefs.edit();
        editor.putString(key, value2);
        editor.commit();
        return value2;  // in case we want to chain it to another function
    }

    public static String getPreference(String key, Context c) {
        SharedPreferences mySharedPrefs = c.getSharedPreferences("CoCoRaHS-Prefs",
                Activity.MODE_WORLD_WRITEABLE);
        return mySharedPrefs.getString(key, "");
    }

    public static void LOG(String msg) {
        if(msg != null) {
            Log.v("CoCoRaHS", msg);
        }
    }

    void showOKAlertMsg(String title, String msg, final Boolean xfinish) {
        LOG(title + ": " + msg);
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setNeutralButton("OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        if(xfinish) { finish(); }
                    }
                });

        dialogBuilder.setTitle(title);
        dialogBuilder.setMessage(msg);
        try {
            dialogBuilder.show();
        } catch (Exception e) {
            LOG("OOPS: I can't show a showOKAlertMsg dialog: " + e.getMessage());
        }
    }

    private void showHistory() {
        setContentView(R.layout.history);
        TextView txtStationId = (TextView) findViewById(R.id.txtStationId);
        TextView txtStationName = (TextView) findViewById(R.id.txtStationName);
        if(txtStationId != null && txtStationName != null) {
            txtStationId.setText(comm.getStationId());
            txtStationName.setText(comm.getStationName());
        }
        progressDialog = ProgressDialog.show(CoCoRaHS.this, "", "Fetching History...", true);
        new HistoryTask().execute("");
    }

    private void addTableRow(String date, String time, String rain, String editLink, Boolean isHeader) {
        final TableLayout table = (TableLayout) findViewById(R.id.history_table);
        final TableRow tr = (TableRow) getLayoutInflater().inflate(R.layout.table_row_item, null);

        // Fill out our cells
        TextView tv1 = (TextView) tr.findViewById(R.id.cell_date);
        tv1.setText(date);
        TextView tv2 = (TextView) tr.findViewById(R.id.cell_time);
        tv2.setText(time);
        TextView tv3 = (TextView) tr.findViewById(R.id.cell_rain);
        tv3.setText(rain);
        TextView tv4 = (TextView) tr.findViewById(R.id.cell_editLink);
        tv4.setText(editLink);
        if(isHeader) {
            tv1.setTypeface(null, Typeface.BOLD);
            tv2.setTypeface(null, Typeface.BOLD);
            tv3.setTypeface(null, Typeface.BOLD);
            tv4.setTypeface(null, Typeface.BOLD);
            tr.setFocusableInTouchMode(false);
        }
        tr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TextView tv1 = (TextView) v.findViewById(R.id.cell_date);
                TextView tv2 = (TextView) v.findViewById(R.id.cell_time);
                TextView tv3 = (TextView) v.findViewById(R.id.cell_rain);
                TextView tv4 = (TextView) v.findViewById(R.id.cell_editLink);
                CoCoRaHS.LOG("Clicked row: " + tv1.getText() + " " + tv2.getText() +
                    " " + tv3.getText() + " " + tv4.getText());
            }
        });
        table.addView(tr);

        // Draw separator
        TextView tv5 = new TextView(this);
        tv5.setBackgroundColor(Color.parseColor("#80808080"));
        tv5.setHeight(2);
        table.addView(tv5);

        // If you use context menu it should be registered for each table row
        registerForContextMenu(tr);
    }

    class LoginTask extends AsyncTask<String, Void, Boolean> {
        private Exception exception;

        protected Boolean doInBackground(String... args) {

            try {
                String url = args[0];
                String username = args[1];
                String password = args[2];
                comm.clearStation();
                if(comm.postLoginData(url, username, password)) {
                    LOG("Login OK.");
                    LOG("Station ID: " + comm.getStationId());
                    LOG("Station Name: " + comm.getStationName());
                    return true;
                }
                else {
                    LOG("Login Failed.");
                    return false;
                }
            } catch (Exception e) {
                this.exception = e;
                return null;
            }
        }

        protected void onPostExecute(Boolean result) {
            try {
                progressDialog.dismiss();
            } catch (Exception e) {}
            if(result) {
                setContentView(R.layout.report);
                placedAgent.logPageView("Precip Report");
                handleButtons();
                TextView tvId = (TextView) findViewById(R.id.txtStationId);
                if(tvId != null) {
                    tvId.setText(comm.getStationId());
                }
                TextView tvName = (TextView) findViewById(R.id.txtStationName);
                if(tvName != null) {
                    tvName.setText(comm.getStationName());
                }

                EditText etObTime = (EditText) findViewById(R.id.etObTime);
                EditText etObDate = (EditText) findViewById(R.id.etObDate);
                if(etObTime != null && etObDate != null) {
                    etObDate.setText(comm.getObservedDate());
                    etObTime.setText(comm.getObservedTime() + " " + comm.getObservedAmPm());
                }

                Spinner spnLoc = (Spinner) findViewById(R.id.spnLoc);
                ArrayAdapter<CharSequence> levelsAdapter = null;
                levelsAdapter = ArrayAdapter.createFromResource(mContext, R.array.yesno , android.R.layout.simple_spinner_item);
                levelsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spnLoc.setAdapter(levelsAdapter);
                Spinner spnFlood = (Spinner) findViewById(R.id.spnFlood);
                ArrayAdapter<CharSequence> floodlevelsAdapter = null;
                floodlevelsAdapter = ArrayAdapter.createFromResource(mContext, R.array.flooding , android.R.layout.simple_spinner_item);
                floodlevelsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spnFlood.setAdapter(floodlevelsAdapter);
                EditText etDate = (EditText) findViewById(R.id.etObDate);
                if(etDate != null) {
                    etDate.setText(comm.getObservedDate());
                }
                EditText etTime = (EditText) findViewById(R.id.etObTime);
                if(etTime != null) {
                    etTime.setText(comm.getObservedTime() + " " + comm.getObservedAmPm());
                }
            }
            else {
                showOKAlertMsg("Whoops!", "Login Failed.  Check your username and password then try again.", false);
            }
        }
    }

    class PrecipTask extends AsyncTask<String, Void, Boolean> {
        private Exception exception;

        protected Boolean doInBackground(String... args) {

            try {
                String url = args[0];
                String date = args[1];
                String time = args[2];
                String rain = args[3];
                String loc = args[4];
                String flooding = args[5];
                comm.clearStation();
                if(comm.postPrecipReport(url,date,time,rain,loc,flooding)) {
                    LOG("Precip Report Sent OK.");
                    return true;
                }
                else {
                    LOG("Failed to send precip report.");
                    return false;
                }
            } catch (Exception e) {
                this.exception = e;
                return null;
            }
        }

        protected void onPostExecute(Boolean result) {
            try {
                progressDialog.dismiss();
            } catch (Exception e) {}
            if(result == null) {
                 showOKAlertMsg("Whoops!", "An error occurred: Make sure all fields are entered correctly.  Exception: " + exception.getMessage(), false);
            }
            else if(result) {
                showOKAlertMsg("Success!", "Thank you for submitting your daily precipation report.", false);
                showHistory();
            }
            else {
                if(! comm.getReportOkReason().equals("")) {
                    showOKAlertMsg("Whoops!", "Failed to send the precipitation report to CoCoRaHS: " + comm.getReportOkReason(), false);
                }
                else {
                    showOKAlertMsg("Whoops!", "Failed to send the precipitation report to CoCoRaHS: Try again.", false);
                }
            }
        }
    }

    class HistoryTask extends AsyncTask<String, Void, Boolean> {
        private Exception exception;
        ArrayList<CoCoRecord> history = null;

        protected Boolean doInBackground(String... args) {
            comm.clearReportOkReason();
            try {
                history = comm.getPrecipHistory(7);
            } catch (Exception e) {
                this.exception = e;
                return null;
            }
            return true;
        }

        protected void onPostExecute(Boolean result) {
            try {
                progressDialog.dismiss();
            } catch (Exception e) {}
            if(result == null) {
                showOKAlertMsg("Whoops!", "An error occurred: " + exception.getMessage(), false);
            }
            else if(result) {
                addTableRow("DATE", "TIME", "PRECIP", "", true);
                for(CoCoRecord item : history) {
                    addTableRow(item.getDate(), item.getTime(), "" + item.getTotalPrecip() + "\"", item.getEditLink(), false);
                }
            }
            else {
                if(! comm.getReportOkReason().equals("")) {
                    showOKAlertMsg("Whoops!", "Failed to fetch CoCoRaHS history: " + comm.getReportOkReason(), false);
                }
                else {
                    showOKAlertMsg("Whoops!", "Failed to fetch CoCoRaHS history: Try again.", false);
                }
            }
        }
    }

}
