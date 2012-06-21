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
import com.lowcountrysoftware.cocorahs.R;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import java.io.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    public void LOG(String msg) {
        if(msg != null) {
            Log.v("CoCoRaHS", msg);
        }
    }

    public void TOAST(String msg) {
        Toast.makeText(mContext, msg, Toast.LENGTH_LONG).show();
    }

    public String getViewState(HttpContext localContext) {
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
        } catch (Exception e) { LOG("Exception occurred while fetching viewState: " + e.getMessage());}
        LOG("__VIEWSTATE = " + vs);
        return vs;
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
                    LOG("findPattern::Group not found!");
                }
            }
        }
        catch (Exception e) {
            LOG(e.getMessage());
        }
        return result;
    }

    public Boolean postLoginData(String url, String username, String password) {
        HttpClient httpclient = new DefaultHttpClient();
        HttpContext localContext = new BasicHttpContext();
        CookieStore cookieStore = new BasicCookieStore();
        localContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);
        Boolean login_ok = false;
        HttpPost httppost = new HttpPost(url);
        String viewState = "";

        LOG("Fetching viewState...");
        viewState = getViewState(localContext);
        if(viewState.equals("")) {
            LOG("Failed to fetch proper viewState for login page.");
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
            LOG("Executing request " + httppost.getURI());
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
            LOG("Exception in inputStreamToString(): " + e.getMessage());
        }
        return total;
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
            try {
                String url = args[0];
                String username = args[1];
                String password = args[2];
                if(postLoginData(url, username, password)) {
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
