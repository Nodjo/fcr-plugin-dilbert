package nodjo.plugin.dilbert;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import nodjo.fcr.comics.IDailyComic;
import nodjo.fcr.utils.StreamUtils;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

public class Dilbert extends Service {

    private static final String TAG = Dilbert.class.getSimpleName();

    private static final String SERVER_URL = "http://www.dilbert.com";
    private static final String SERVER_URL_FOLDER = "/fast/";
    private static final String ONLINE_STORE_URL = "http://thedilbertstore.com/?utm_source=fastcomicreader";
    private static final String PARSING_PREFIX = "src=\"/dyn/str_strip/";
    private static final int PREFIX_QUOTE_POSITION = PARSING_PREFIX.indexOf('\"');
    private static final String PREFIX_WRONG_STRIP = "/dyn/str_strip/default";

    private static final String FIRST_STRIP_IDENTIFIER = "1989-04-16";
    private final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    private static final HttpParams HTTP_PARAMS = new BasicHttpParams();
    static {
        HttpConnectionParams.setConnectionTimeout(HTTP_PARAMS, 15000);
        HttpConnectionParams.setSoTimeout(HTTP_PARAMS, 30000);
    }
    private ThreadSafeClientConnManager mConnectionManager;

    @Override
    public IBinder onBind(Intent intent) {
        final SchemeRegistry registry = new SchemeRegistry();
        registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
        mConnectionManager = new ThreadSafeClientConnManager(HTTP_PARAMS, registry);
        return mBinder;
    }

    private final IDailyComic.Stub mBinder = new IDailyComic.Stub() {

        /**
         * Takes a long time !
         */
        @Override
        public String getStripUrl(long stripId) throws RemoteException {
            Log.v(TAG, "getStripUrl:" + stripId);
            String url = null;
            final String stripDate = SDF.format(new Date(stripId));
            HttpClient httpClient = new DefaultHttpClient(mConnectionManager, HTTP_PARAMS);
            HttpResponse response;
            HttpGet httpGet = new HttpGet(SERVER_URL + SERVER_URL_FOLDER + stripDate);
            try {
                response = httpClient.execute(httpGet);
                if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {

                    Log.d(TAG, "html size:" + response.getEntity().getContentLength() + " bytes");
                    final String strContent = StreamUtils.convertStreamToString(response.getEntity().getContent(), (int) response.getEntity()
                            .getContentLength(), stripDate);
                    int nIndex = strContent.indexOf(PARSING_PREFIX);
                    if (nIndex >= 0) {
                        String strSubOne = strContent.substring(nIndex + PREFIX_QUOTE_POSITION + 1);
                        strSubOne = strSubOne.replace("strip.print.gif", "strip.zoom.gif");
                        if (strSubOne != null && !strSubOne.startsWith(PREFIX_WRONG_STRIP)) {
                            int nLastIndex = strSubOne.indexOf('\"');
                            if (nLastIndex >= 0) {
                                url = SERVER_URL + strSubOne.substring(0, nLastIndex);
                            }
                        }
                    }
                } else {
                    Log.w(TAG, "Connection problem. Response status code:" + response.getStatusLine().getStatusCode());
                }
            } catch (ClientProtocolException e) {
                Log.w(TAG, "ClientProtocolException", e);
            } catch (IOException e) {
                Log.w(TAG, "IOException", e);
            }
            return url;
        }

        @Override
        public long getFirstDate() throws RemoteException {
            Log.v(TAG, "getFirstDate");
            try {
                return SDF.parse(FIRST_STRIP_IDENTIFIER).getTime();
            } catch (ParseException e) {
                Log.e(TAG, "Parsing exception", e);
                return 0;
            }
        }

        @Override
        public String getWebsiteUrl() throws RemoteException {
            Log.v(TAG, "getWebsiteUrl");
            return ONLINE_STORE_URL;
        }

    };

    @Override
    public boolean onUnbind(Intent intent) {
        Log.v(TAG, "onUnbind");
        mConnectionManager.shutdown();
        mConnectionManager = null;
        return super.onUnbind(intent);
    }
}
