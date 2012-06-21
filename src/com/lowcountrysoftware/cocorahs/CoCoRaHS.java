package com.lowcountrysoftware.cocorahs;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;

public class CoCoRaHS extends Activity
{

    Context mContext = null;

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
                    new LoginTask().execute("http://www.cocorahs.org/Login.aspx", username, password);
                }
            });
        }
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
        Toast.makeText(mContext, msg, Toast.LENGTH_LONG).show();
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

        protected Boolean doInBackground(String... args) {
            CoCoComm comm = new CoCoComm();
            try {
                String url = args[0];
                String username = args[1];
                String password = args[2];
                if(comm.postLoginData(url, username, password)) {
                    LOG("Login OK.");
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
            if(result) {
                TOAST("Login OK");
            }
            else {
                showOKAlertMsg("Whoops!", "Login Failed.  Check your username and password then try again.", false);
            }
        }
    }

}
