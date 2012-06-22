package com.lowcountrysoftware.cocorahs;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.*;

import java.util.Calendar;
import java.util.GregorianCalendar;

public class CoCoRaHS extends Activity
{

    Context mContext = null;
    ProgressDialog progressDialog = null;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        mContext = this;

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

    public void TOAST(String msg) {
        Toast.makeText(mContext, msg, Toast.LENGTH_SHORT).show();
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

    class LoginTask extends AsyncTask<String, Void, Boolean> {
        private Exception exception;
        CoCoComm comm = new CoCoComm();

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
                TOAST("Login OK");
                setContentView(R.layout.report);
                TextView tvId = (TextView) findViewById(R.id.txtStationId);
                if(tvId != null) {
                    tvId.setText(comm.getStationId());
                }
                TextView tvName = (TextView) findViewById(R.id.txtStationName);
                if(tvName != null) {
                    tvName.setText(comm.getStationName());
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

}
