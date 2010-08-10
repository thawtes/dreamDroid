/* © 2010 Stephan Reichholf <stephan at reichholf dot net>
 * 
 * Licensed under the Create-Commons Attribution-Noncommercial-Share Alike 3.0 Unported
 * http://creativecommons.org/licenses/by-nc-sa/3.0/
 */

package net.reichholf.dreamdroid.helpers;

import java.io.IOException;
import java.util.List;

import net.reichholf.dreamdroid.DreamDroid;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import android.util.Log;

/**
 * @author sreichholf
 * 
 */
public class SimpleHttpClient {
	private UsernamePasswordCredentials mCreds = null;
	private DefaultHttpClient mDhc;
	private HttpContext mContext;

	private String mPrefix;
	private String mHostname;
	private String mPort;
	private String mUser;
	private String mPass;
	private String mPageString;
	private String mErrorText;

	private boolean mLogin;
	private boolean mSsl;
	private boolean mError;

	/**
	 * @param sp SharedPreferences of the Base-Context
	 */
	private SimpleHttpClient() {
		BasicHttpParams params = new BasicHttpParams();
		params.setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 10000);

		mDhc = new DefaultHttpClient(getEasySSLClientConnectionManager(),
				params);

		mContext = new BasicHttpContext();
	}

	/**
	 * @return ThreadSafeClientConnManager Instance
	 */
	public ThreadSafeClientConnManager getEasySSLClientConnectionManager() {
		BasicHttpParams params = new BasicHttpParams();

		SchemeRegistry schemeRegistry = new SchemeRegistry();

		schemeRegistry.register(new Scheme("http", PlainSocketFactory
				.getSocketFactory(), 80));
		schemeRegistry.register(new Scheme("https", new EasySSLSocketFactory(),
				443));

		ThreadSafeClientConnManager cm = new ThreadSafeClientConnManager(
				params, schemeRegistry);

		return cm;
	}

	/**
	 * @param user Username for http-auth
	 * @param pass Password for http-auth
	 */
	public void setCredentials(String user, String pass) {
		mCreds = new UsernamePasswordCredentials(user, pass);
		mDhc.getCredentialsProvider().setCredentials(AuthScope.ANY, mCreds);
	}

	/**
	 * 
	 */
	public void unsetCrendentials() {
		mDhc.getCredentialsProvider().setCredentials(AuthScope.ANY, null);
	}

	/**
	 * @param uri
	 * @param parameters
	 * @return
	 */
	public boolean fetchPageContent(String uri, List<NameValuePair> parameters) {
		// Set login, ssl, port, host etc;
		applyConfig();

		mErrorText = null;
		mError = false;
		mPageString = "";
		if (!uri.startsWith("/")) {
			uri = "/".concat(uri);
		}

		String parms = URLEncodedUtils.format(parameters, HTTP.UTF_8);
		String url = mPrefix + mHostname + ":" + mPort + uri + parms;

		HttpGet get = new HttpGet(url);

		try {
			HttpResponse resp = mDhc.execute(get, mContext);
			StatusLine s = resp.getStatusLine();

			if (s.getStatusCode() == HttpStatus.SC_OK) {
				HttpEntity entity = resp.getEntity();
				if (entity != null) {
					byte[] tmp = EntityUtils.toByteArray(entity);

					mPageString = new String(tmp);
					return true;
				} else {
					mErrorText = "HttpEntity is null";
					mError = true;
				}
			} else {
				mErrorText = s.getStatusCode() + " - " + s.getReasonPhrase();
				mError = true;
				return false;
			}
		} catch (ClientProtocolException e) {
			Log.e(this.getClass().getSimpleName(), e.toString());

			mErrorText = e.toString();
			mError = true;
			return false;
		} catch (IOException e) {
			Log.e(this.getClass().getSimpleName(), e.toString());

			mErrorText = e.toString();
			mError = true;
			return false;
		}

		return false;
	}

	/**
	 * @return
	 */
	public String getPageContentString() {
		return this.mPageString;
	}

	/**
	 * @return
	 */
	public String getErrorText() {
		return mErrorText;
	}

	/**
	 * @return
	 */
	public boolean hasError() {
		return mError;
	}

	/**
	 * 
	 */
	public void applyConfig() {
//		mHostname = DreamDroid.SP.getString("host", "dm8000");
		mHostname = DreamDroid.PROFILE.getHost();
//		mPort = DreamDroid.SP.getString("port", "80");
		mPort = new Integer(DreamDroid.PROFILE.getPort()).toString();
//		mLogin = DreamDroid.SP.getBoolean("login", false);
		mLogin = DreamDroid.PROFILE.isLogin();
//		mSsl = DreamDroid.SP.getBoolean("ssl", false);
		mSsl = DreamDroid.PROFILE.isSsl();

		if (mSsl) {
			mPrefix = "https://";
		} else {
			mPrefix = "http://";
		}

		if (mLogin) {
//			mUser = DreamDroid.SP.getString("user", "root");
			mUser = DreamDroid.PROFILE.getUser();
//			mPass = DreamDroid.SP.getString("pass", "dreambox");
			mPass = DreamDroid.PROFILE.getPass();
			setCredentials(mUser, mPass);
		}
	}

	/**
	 * @param sp
	 * @return
	 */
	public static SimpleHttpClient getInstance() {
		return new SimpleHttpClient();
	}
}