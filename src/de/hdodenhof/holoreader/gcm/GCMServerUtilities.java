package de.hdodenhof.holoreader.gcm;

import java.net.URI;
import java.util.HashMap;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreConnectionPNames;

import com.google.gson.Gson;

import de.hdodenhof.holoreader.Config;

public class GCMServerUtilities {

    @SuppressWarnings("unused")
    private static final String TAG = GCMServerUtilities.class.getName();

    public static boolean registerOnServer(String eMail, String regId, String uuid) {
        try {
            HashMap<String, String> entityMap = new HashMap<String, String>();
            entityMap.put("eMail", eMail);
            entityMap.put("regId", regId);
            entityMap.put("uuid", uuid);
            entityMap.put("device", android.os.Build.MODEL);
            String entity = new Gson().toJson(entityMap);

            HttpPut request = new HttpPut();
            request.setURI(new URI(Config.API_BASEURL + "register"));
            request.setHeader("Content-type", "application/json");
            request.setEntity(new StringEntity(entity));

            HttpClient client = new DefaultHttpClient();
            client.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 15000);
            client.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, 15000);

            HttpResponse response = client.execute(request);
            StatusLine responseStatus = response.getStatusLine();

            int statusCode = responseStatus != null ? responseStatus.getStatusCode() : 0;

            return statusCode == 204;

        } catch (Exception e) {
            return false;
        }
    }
}
