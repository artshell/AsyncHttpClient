package net.callumtaylor.asynchttp;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.HttpsURLConnection;

import net.callumtaylor.asynchttp.obj.ConnectionInfo;
import net.callumtaylor.asynchttp.response.AsyncHttpResponseHandler;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Build;

/**
 * The client class used for initiating HTTP requests using an AsyncTask. It
 * follows a RESTful paradigm for the connections with the 4 possible methods,
 * GET, POST, PUT and DELETE. <b>Depends on</b>
 * <ul>
 * <li>{@link AsyncHttpResponseHandler}</li>
 * <li>{@link HttpEntity}</li>
 * <li>{@link NameValuePair}</li>
 * <li>{@link ConnectionInfo}</li>
 * </ul>
 * <h1>Example GET</h1>
 *
 * <pre>
 * AsyncHttpClient client = new AsyncHttpClient(&quot;http://example.com&quot;);
 * List&lt;NameValuePair&gt; params = new ArrayList&lt;NameValuePair&gt;();
 * params.add(new BasicNameValuePair(&quot;key&quot;, &quot;value&quot;));
 *
 * List&lt;Header&gt; headers = new ArrayList&lt;Header&gt;();
 * headers.add(new BasicHeader(&quot;1&quot;, &quot;2&quot;));
 *
 * client.get(&quot;api/v1/&quot;, params, headers, new JsonResponseHandler()
 * {
 * 	&#064;Override public JsonElement onSuccess(byte[] response)
 * 	{
 * 		JsonElement result = super.onSuccess(response);
 * 		return null;
 * 	}
 * });
 * </pre>
 *
 * <h1>Example DELETE</h1>
 *
 * <pre>
 * AsyncHttpClient client = new AsyncHttpClient(&quot;http://example.com&quot;);
 * List&lt;NameValuePair&gt; params = new ArrayList&lt;NameValuePair&gt;();
 * params.add(new BasicNameValuePair(&quot;key&quot;, &quot;value&quot;));
 *
 * List&lt;Header&gt; headers = new ArrayList&lt;Header&gt;();
 * headers.add(new BasicHeader(&quot;1&quot;, &quot;2&quot;));
 *
 * client.delete(&quot;api/v1/&quot;, params, headers, new JsonResponseHandler()
 * {
 * 	&#064;Override public JsonElement onSuccess(byte[] response)
 * 	{
 * 		JsonElement result = super.onSuccess(response);
 * 		return null;
 * 	}
 * });
 * </pre>
 *
 * <h1>Example POST - Single Entity</h1>
 *
 * <pre>
 * AsyncHttpClient client = new AsyncHttpClient(&quot;http://example.com&quot;);
 * List&lt;NameValuePair&gt; params = new ArrayList&lt;NameValuePair&gt;();
 * params.add(new BasicNameValuePair(&quot;key&quot;, &quot;value&quot;));
 *
 * List&lt;Header&gt; headers = new ArrayList&lt;Header&gt;();
 * headers.add(new BasicHeader(&quot;1&quot;, &quot;2&quot;));
 *
 * JsonEntity data = new JsonEntity(&quot;{\&quot;key\&quot;:\&quot;value\&quot;}&quot;);
 * GzippedEntity entity = new GzippedEntity(data);
 *
 * client.post(&quot;api/v1/&quot;, params, entity, headers, new JsonResponseHandler()
 * {
 * 	&#064;Override public JsonElement onSuccess(byte[] response)
 * 	{
 * 		JsonElement result = super.onSuccess(response);
 * 		return null;
 * 	}
 * });
 * </pre>
 *
 * <h1>Example POST - Multiple Entity + file</h1>
 *
 * <pre>
 * AsyncHttpClient client = new AsyncHttpClient(&quot;http://example.com&quot;);
 * List&lt;NameValuePair&gt; params = new ArrayList&lt;NameValuePair&gt;();
 * params.add(new BasicNameValuePair(&quot;key&quot;, &quot;value&quot;));
 *
 * List&lt;Header&gt; headers = new ArrayList&lt;Header&gt;();
 * headers.add(new BasicHeader(&quot;1&quot;, &quot;2&quot;));
 *
 * MultiPartEntity entity = new MultiPartEntity();
 * FileEntity data1 = new FileEntity(new File(&quot;/IMG_6614.JPG&quot;), &quot;image/jpeg&quot;);
 * JsonEntity data2 = new JsonEntity(&quot;{\&quot;key\&quot;:\&quot;value\&quot;}&quot;);
 * entity.addFilePart(&quot;image1.jpg&quot;, data1);
 * entity.addPart(&quot;content1&quot;, data2);
 *
 * client.post(&quot;api/v1/&quot;, params, entity, headers, new JsonResponseHandler()
 * {
 * 	&#064;Override public JsonElement onSuccess(byte[] response)
 * 	{
 * 		JsonElement result = super.onSuccess(response);
 * 		return null;
 * 	}
 * });
 * </pre>
 *
 * <h1>Example PUT</h1>
 *
 * <pre>
 * AsyncHttpClient client = new AsyncHttpClient(&quot;http://example.com&quot;);
 * List&lt;NameValuePair&gt; params = new ArrayList&lt;NameValuePair&gt;();
 * params.add(new BasicNameValuePair(&quot;key&quot;, &quot;value&quot;));
 *
 * List&lt;Header&gt; headers = new ArrayList&lt;Header&gt;();
 * headers.add(new BasicHeader(&quot;1&quot;, &quot;2&quot;));
 *
 * JsonEntity data = new JsonEntity(&quot;{\&quot;key\&quot;:\&quot;value\&quot;}&quot;);
 * GzippedEntity entity = new GzippedEntity(data);
 *
 * client.post(&quot;api/v1/&quot;, params, entity, headers, new JsonResponseHandler()
 * {
 * 	&#064;Override public JsonElement onSuccess(byte[] response)
 * 	{
 * 		JsonElement result = super.onSuccess(response);
 * 		return null;
 * 	}
 * });
 * </pre>
 *
 * Because of the nature of REST, GET and DELETE requests behave in the same
 * way, POST and PUT requests also behave in the same way.
 *
 * @author Callum Taylor &lt;callumtaylor.net&gt; &#064;scruffyfox
 */
public class AsyncHttpClient
{
	private ClientExecutorTask executorTask;
	private Uri requestUri;
	private long requestTimeout = 0L;

	private static final String USER_AGENT;
	static
	{
		USER_AGENT = getDefaultUserAgent();
	}

	public enum RequestMode
	{
		/**
		 * Gets data from the server as String
		 */
		GET("GET"),
		/**
		 * Posts to a server
		 */
		POST("POST"),
		/**
		 * Puts data to the server (equivilant to POST with relevant headers)
		 */
		PUT("PUT"),
		/**
		 * Deletes data from the server (equivilant to GET with relevant
		 * headers)
		 */
		DELETE("DELETE");

		private String canonicalStr = "";
		private RequestMode(String canonicalStr)
		{
			this.canonicalStr = canonicalStr;
		}

		public String getCanonical()
		{
			return this.canonicalStr;
		}
	}

	/**
	 * Creates a new client using a base Url without a timeout
	 * @param baseUrl The base connection url
	 */
	public AsyncHttpClient(String baseUrl)
	{
		this(baseUrl, 0);
	}

	/**
	 * Creates a new client using a base Uri without a timeout
	 * @param baseUrl The base connection uri
	 */
	public AsyncHttpClient(Uri baseUri)
	{
		this(baseUri, 0);
	}

	/**
	 * Creates a new client using a base Url with a timeout in MS
	 * @param baseUrl The base connection url
	 * @param timeout The timeout in MS
	 */
	public AsyncHttpClient(String baseUrl, long timeout)
	{
		this(Uri.parse(baseUrl), timeout);
	}

	/**
	 * Creates a new client using a base Uri with a timeout in MS
	 * @param baseUrl The base connection uri
	 * @param timeout The timeout in MS
	 */
	public AsyncHttpClient(Uri baseUri, long timeout)
	{
		requestUri = baseUri;
		requestTimeout = timeout;
	}

	/**
	 * Performs a GET request on the baseUri
	 * @param response The response handler for the request
	 */
	public void get(AsyncHttpResponseHandler response)
	{
		get("", null, null, response);
	}

	/**
	 * Performs a GET request on the baseUri
	 * @param path The path extended from the baseUri
	 * @param response The response handler for the request
	 */
	public void get(String path, AsyncHttpResponseHandler response)
	{
		get(path, null, null, response);
	}

	/**
	 * Performs a GET request on the baseUri
	 * @param headers The request headers for the connection
	 * @param response The response handler for the request
	 */
	public void get(List<Header> headers, AsyncHttpResponseHandler response)
	{
		get("", null, headers, response);
	}

	/**
	 * Performs a GET request on the baseUri
	 * @param params The Query params to append to the baseUri
	 * @param headers The request headers for the connection
	 * @param response The response handler for the request
	 */
	public void get(List<NameValuePair> params, List<Header> headers, AsyncHttpResponseHandler response)
	{
		get("", params, headers, response);
	}

	/**
	 * Performs a GET request on the baseUri
	 * @param path The path extended from the baseUri
	 * @param headers The request headers for the connection
	 * @param response The response handler for the request
	 */
	public void get(String path, List<NameValuePair> params, AsyncHttpResponseHandler response)
	{
		get(path, params, null, response);
	}

	/**
	 * Performs a GET request on the baseUri
	 * @param path The path extended from the baseUri
	 * @param params The Query params to append to the baseUri
	 * @param headers The request headers for the connection
	 * @param response The response handler for the request
	 */
	public void get(String path, List<NameValuePair> params, List<Header> headers, AsyncHttpResponseHandler response)
	{
		requestUri = Uri.withAppendedPath(requestUri, path);
		requestUri = appendParams(requestUri, params);
		executeTask(RequestMode.GET, requestUri, headers, null, response);
	}

	/**
	 * Performs a DELETE request on the baseUri
	 * @param response The response handler for the request
	 */
	public void delete(AsyncHttpResponseHandler response)
	{
		delete("", null, null, response);
	}

	/**
	 * Performs a DELETE request on the baseUri
	 * @param path The path extended from the baseUri
	 * @param response The response handler for the request
	 */
	public void delete(String path, AsyncHttpResponseHandler response)
	{
		delete(path, null, null, response);
	}

	/**
	 * Performs a DELETE request on the baseUri
	 * @param headers The request headers for the connection
	 * @param response The response handler for the request
	 */
	public void delete(List<Header> headers, AsyncHttpResponseHandler response)
	{
		delete("", null, headers, response);
	}

	/**
	 * Performs a DELETE request on the baseUri
	 * @param params The Query params to append to the baseUri
	 * @param headers The request headers for the connection
	 * @param response The response handler for the request
	 */
	public void delete(List<NameValuePair> params, List<Header> headers, AsyncHttpResponseHandler response)
	{
		delete("", params, headers, response);
	}

	/**
	 * Performs a DELETE request on the baseUri
	 * @param path The path extended from the baseUri
	 * @param headers The request headers for the connection
	 * @param response The response handler for the request
	 */
	public void delete(String path, List<NameValuePair> params, AsyncHttpResponseHandler response)
	{
		delete(path, params, null, response);
	}

	/**
	 * Performs a DELETE request on the baseUri
	 * @param path The path extended from the baseUri
	 * @param params The Query params to append to the baseUri
	 * @param headers The request headers for the connection
	 * @param response The response handler for the request
	 */
	public void delete(String path, List<NameValuePair> params, List<Header> headers, AsyncHttpResponseHandler response)
	{
		requestUri = Uri.withAppendedPath(requestUri, path);
		requestUri = appendParams(requestUri, params);
		executeTask(RequestMode.DELETE, requestUri, headers, null, response);
	}

	/**
	 * Performs a POST request on the baseUri
	 * @param response The response handler for the request
	 */
	public void post(AsyncHttpResponseHandler response)
	{
		post("", null, null, null, response);
	}

	/**
	 * Performs a POST request on the baseUr
	 * @param path The path extended from the baseUri
	 * @param response The response handler for the request
	 */
	public void post(String path, AsyncHttpResponseHandler response)
	{
		post(path, null, null, null, response);
	}


	/**
	 * Performs a POST request on the baseUri
	 * @param path The path extended from the baseUri
	 * @param params The Query params to append to the baseUri
	 * @param response The response handler for the request
	 */
	public void post(List<NameValuePair> params, AsyncHttpResponseHandler response)
	{
		post("", params, null, null, response);
	}

	/**
	 * Performs a POST request on the baseUri
	 * @param params The Query params to append to the baseUri
	 * @param headers The request headers for the connection
	 * @param response The response handler for the request
	 */
	public void post(List<NameValuePair> params, List<Header> headers, AsyncHttpResponseHandler response)
	{
		post("", params, null, headers, response);
	}

	/**
	 * Performs a POST request on the baseUri
	 * @param postData The post data entity to post to the server
	 * @param response The response handler for the request
	 */
	public void post(HttpEntity postData, AsyncHttpResponseHandler response)
	{
		post("", null, postData, null, response);
	}

	/**
	 * Performs a POST request on the baseUri
	 * @param postData The post data entity to post to the server
	 * @param headers The request headers for the connection
	 * @param response The response handler for the request
	 */
	public void post(HttpEntity postData, List<Header> headers, AsyncHttpResponseHandler response)
	{
		post("", null, postData, headers, response);
	}

	/**
	 * Performs a POST request on the baseUri
	 * @param params The Query params to append to the baseUri
	 * @param postData The post data entity to post to the server
	 * @param response The response handler for the request
	 */
	public void post(List<NameValuePair> params, HttpEntity postData, AsyncHttpResponseHandler response)
	{
		post("", params, postData, null, response);
	}

	/**
	 * Performs a POST request on the baseUri
	 * @param path The path extended from the baseUri
	 * @param params The Query params to append to the baseUri
	 * @param response The response handler for the request
	 */
	public void post(String path, List<NameValuePair> params, AsyncHttpResponseHandler response)
	{
		post(path, params, null, null, response);
	}

	/**
	 * Performs a POST request on the baseUri
	 * @param path The path extended from the baseUri
	 * @param params The Query params to append to the baseUri
	 * @param headers The request headers for the connection
	 * @param response The response handler for the request
	 */
	public void post(String path, List<NameValuePair> params, List<Header> headers, AsyncHttpResponseHandler response)
	{
		post(path, params, null, headers, response);
	}

	/**
	 * Performs a POST request on the baseUri
	 * @param path The path extended from the baseUri
	 * @param postData The post data entity to post to the server
	 * @param response The response handler for the request
	 */
	public void post(String path, HttpEntity postData, AsyncHttpResponseHandler response)
	{
		post(path, null, postData, null, response);
	}

	/**
	 * Performs a POST request on the baseUri
	 * @param path The path extended from the baseUri
	 * @param postData The post data entity to post to the server
	 * @param headers The request headers for the connection
	 * @param response The response handler for the request
	 */
	public void post(String path, HttpEntity postData, List<Header> headers, AsyncHttpResponseHandler response)
	{
		post(path, null, postData, headers, response);
	}

	/**
	 * Performs a POST request on the baseUri
	 * @param path The path extended from the baseUri
	 * @param params The Query params to append to the baseUri
	 * @param postData The post data entity to post to the server
	 * @param response The response handler for the request
	 */
	public void post(String path, List<NameValuePair> params, HttpEntity postData, AsyncHttpResponseHandler response)
	{
		post(path, params, postData, null, response);
	}

	/**
	 * Performs a POST request on the baseUri
	 * @param path The path extended from the baseUri
	 * @param params The Query params to append to the baseUri
	 * @param postData The post data entity to post to the server
	 * @param headers The request headers for the connection
	 * @param response The response handler for the request
	 */
	public void post(String path, List<NameValuePair> params, HttpEntity postData, List<Header> headers, AsyncHttpResponseHandler response)
	{
		requestUri = Uri.withAppendedPath(requestUri, path);
		requestUri = appendParams(requestUri, params);
		executeTask(RequestMode.POST, requestUri, headers, postData, response);
	}

	/**
	 * Performs a PUT request on the baseUr
	 * @param response The response handler for the request
	 */
	public void put(AsyncHttpResponseHandler response)
	{
		put("", null, null, null, response);
	}

	/**
	 * Performs a PUT request on the baseUr
	 * @param path The path extended from the baseUri
	 * @param response The response handler for the request
	 */
	public void put(String path, AsyncHttpResponseHandler response)
	{
		put(path, null, null, null, response);
	}

	/**
	 * Performs a PUT request on the baseUri
	 * @param params The Query params to append to the baseUri
	 * @param response The response handler for the request
	 */
	public void put(List<NameValuePair> params, AsyncHttpResponseHandler response)
	{
		put("", params, null, null, response);
	}

	/**
	 * Performs a PUT request on the baseUri
	 * @param params The Query params to append to the baseUri
	 * @param headers The request headers for the connection
	 * @param response The response handler for the request
	 */
	public void put(List<NameValuePair> params, List<Header> headers, AsyncHttpResponseHandler response)
	{
		put("", params, null, headers, response);
	}

	/**
	 * Performs a PUT request on the baseUri
	 * @param postData The post data entity to post to the server
	 * @param response The response handler for the request
	 */
	public void put(HttpEntity postData, AsyncHttpResponseHandler response)
	{
		put("", null, postData, null, response);
	}

	/**
	 * Performs a PUT request on the baseUri
	 * @param postData The post data entity to post to the server
	 * @param headers The request headers for the connection
	 * @param response The response handler for the request
	 */
	public void put(HttpEntity postData, List<Header> headers, AsyncHttpResponseHandler response)
	{
		put("", null, postData, headers, response);
	}

	/**
	 * Performs a PUT request on the baseUri
	 * @param params The Query params to append to the baseUri
	 * @param postData The post data entity to post to the server
	 * @param response The response handler for the request
	 */
	public void put(List<NameValuePair> params, HttpEntity postData, AsyncHttpResponseHandler response)
	{
		put("", params, postData, null, response);
	}

	/**
	 * Performs a PUT request on the baseUri
	 * @param path The path extended from the baseUri
	 * @param params The Query params to append to the baseUri
	 * @param response The response handler for the request
	 */
	public void put(String path, List<NameValuePair> params, AsyncHttpResponseHandler response)
	{
		put(path, params, null, null, response);
	}

	/**
	 * Performs a PUT request on the baseUri
	 * @param path The path extended from the baseUri
	 * @param params The Query params to append to the baseUri
	 * @param headers The request headers for the connection
	 * @param response The response handler for the request
	 */
	public void put(String path, List<NameValuePair> params, List<Header> headers, AsyncHttpResponseHandler response)
	{
		put(path, params, null, headers, response);
	}

	/**
	 * Performs a PUT request on the baseUri
	 * @param path The path extended from the baseUri
	 * @param postData The post data entity to post to the server
	 * @param response The response handler for the request
	 */
	public void put(String path, HttpEntity postData, AsyncHttpResponseHandler response)
	{
		put(path, null, postData, null, response);
	}

	/**
	 * Performs a PUT request on the baseUri
	 * @param path The path extended from the baseUri
	 * @param postData The post data entity to post to the server
	 * @param headers The request headers for the connection
	 * @param response The response handler for the request
	 */
	public void put(String path, HttpEntity postData, List<Header> headers, AsyncHttpResponseHandler response)
	{
		put(path, null, postData, headers, response);
	}

	/**
	 * Performs a PUT request on the baseUri
	 * @param path The path extended from the baseUri
	 * @param params The Query params to append to the baseUri
	 * @param postData The post data entity to post to the server
	 * @param response The response handler for the request
	 */
	public void put(String path, List<NameValuePair> params, HttpEntity postData, AsyncHttpResponseHandler response)
	{
		put(path, params, postData, null, response);
	}

	/**
	 * Performs a PUT request on the baseUri
	 * @param path The path extended from the baseUri
	 * @param params The Query params to append to the baseUri
	 * @param postData The post data entity to post to the server
	 * @param headers The request headers for the connection
	 * @param response The response handler for the request
	 */
	public void put(String path, List<NameValuePair> params, HttpEntity postData, List<Header> headers, AsyncHttpResponseHandler response)
	{
		requestUri = Uri.withAppendedPath(requestUri, path);
		requestUri = appendParams(requestUri, params);
		executeTask(RequestMode.PUT, requestUri, headers, postData, response);
	}

	/**
	 * Creates a user agent string for the device
	 * @return
	 */
	public static String getDefaultUserAgent()
	{
		StringBuilder result = new StringBuilder(64);
		result.append("Dalvik/");
		result.append(System.getProperty("java.vm.version"));
		result.append(" (Linux; U; Android ");

		String version = Build.VERSION.RELEASE;
		result.append(version.length() > 0 ? version : "1.0");

		// add the model for the release build
		if ("REL".equals(Build.VERSION.CODENAME))
		{
			String model = Build.MODEL;
			if (model.length() > 0)
			{
				result.append("; ");
				result.append(model);
			}
		}

		String id = Build.ID;
		if (id.length() > 0)
		{
			result.append(" Build/");
			result.append(id);
		}

		result.append(")");
		return result.toString();
	}

	/**
	 * Appends a list of KV params on to the end of a URI
	 * @param uri The URI to append to
	 * @param params The params to append
	 * @return The new URI
	 */
	public static Uri appendParams(Uri uri, List<NameValuePair> params)
	{
		try
		{
			Uri.Builder builder = uri.buildUpon();
			for (NameValuePair p : params)
			{
				builder.appendQueryParameter(p.getName(), p.getValue());
			}

			return builder.build();
		}
		catch (Exception e){}

		return uri;
	}

	/**
	 * Gets the actual response code from a HttpURLConnection
	 * @param conn The connection to use
	 * @return The response code or -1
	 */
	private static int getResponseCode(HttpURLConnection conn)
	{
		try
		{
			return conn.getResponseCode();
		}
		catch (Exception e)
		{
			if (e.getMessage().toLowerCase().contains("authentication"))
			{
				return 401;
			}

			if (e instanceof FileNotFoundException)
			{
				return 404;
			}
		}

		return -1;
	}

	private void executeTask(RequestMode mode, Uri uri, List<Header> headers, HttpEntity sendData, AsyncHttpResponseHandler response)
	{
		if (executorTask != null || (executorTask != null && executorTask.getStatus() == Status.RUNNING))
		{
			executorTask.cancel(true);
		}
		else
		{
			executorTask = new ClientExecutorTask(mode, uri, headers, sendData, response);
		}

		executorTask.execute();
	}

	private class ClientExecutorTask extends AsyncTask<Void, Void, byte[]>
	{
		private static final int BUFFER_SIZE = 8192;

		private AsyncHttpResponseHandler response;
		private Uri requestUri;
		private List<Header> requestHeaders;
		private HttpEntity postData;
		private RequestMode requestMode;

		public ClientExecutorTask(RequestMode mode, Uri request, List<Header> headers, HttpEntity postData, AsyncHttpResponseHandler response)
		{
			this.response = response;
			this.requestUri = request;
			this.requestHeaders = headers;
			this.postData = postData;
			this.requestMode = mode;
		}

		@Override protected void onPreExecute()
		{
			super.onPreExecute();
			if (this.response != null)
			{
				this.response.getConnectionInfo().connectionTime = System.currentTimeMillis();
				this.response.getConnectionInfo().requestMethod = requestMode;
				this.response.onSend();
			}
		}

		@Override protected byte[] doInBackground(Void... params)
		{
			try
			{
				URL url = new URL(requestUri.toString());

				if (Build.VERSION.SDK_INT < Build.VERSION_CODES.FROYO)
				{
					System.setProperty("http.keepAlive", "false");
				}

				HttpURLConnection conn;
				if (url.getHost().equalsIgnoreCase("https"))
				{
					conn = (HttpsURLConnection)url.openConnection();
				}
				else
				{
					conn = (HttpURLConnection)url.openConnection();
				}

				conn.setRequestProperty("User-Agent", USER_AGENT);
				conn.setRequestProperty("Connection", "close");
				conn.setConnectTimeout((int)requestTimeout);
				conn.setFollowRedirects(true);
				conn.setUseCaches(false);

				if (requestMode == RequestMode.GET)
				{
					conn.setRequestMethod("GET");
					conn.setDoInput(true);
				}
				else if (requestMode == RequestMode.POST)
				{
					conn.setRequestMethod("POST");
					conn.setDoInput(true);
					conn.setDoOutput(true);
				}
				else if (requestMode == RequestMode.PUT)
				{
					conn.setRequestMethod("PUT");
					conn.setDoInput(true);
					conn.setDoOutput(true);
				}
				else if (requestMode == RequestMode.DELETE)
				{
					conn.setRequestMethod("DELETE");
					conn.setDoInput(true);
				}

				if (postData != null)
				{
					conn.setRequestProperty(postData.getContentType().getName(), postData.getContentType().getValue());
				}

				if (requestHeaders != null)
				{
					for (Header header : requestHeaders)
					{
						conn.setRequestProperty(header.getName(), header.getValue());
					}
				}

				conn.connect();

				if (requestMode == RequestMode.POST || requestMode == RequestMode.PUT)
				{
					long contentLength = postData.getContentLength();

					if (this.response != null)
					{
						this.response.getConnectionInfo().connectionLength = contentLength;
					}

					BufferedInputStream content = new BufferedInputStream(postData.getContent(), BUFFER_SIZE);
					BufferedOutputStream wr = new BufferedOutputStream(conn.getOutputStream(), BUFFER_SIZE);

					byte[] buffer = new byte[BUFFER_SIZE];
					int writeCount = 0;
					int len = 0;

					while ((len = content.read(buffer)) != -1)
					{
						if (this.response != null)
						{
							this.response.onPublishedUploadProgress(buffer, writeCount, contentLength);
							this.response.onPublishedUploadProgress(buffer, writeCount, contentLength, len);
						}

						wr.write(buffer, 0, len);
						writeCount += len;
					}

					wr.flush();
					wr.close();
				}

				// Get the response
				InputStream i;
				int responseCode = getResponseCode(conn);

				if ((responseCode / 100) == 2)
				{
					i = conn.getInputStream();
				}
				else
				{
					i = conn.getErrorStream();
				}

				if ("gzip".equals(conn.getContentEncoding()))
				{
					i = new GZIPInputStream(new BufferedInputStream(i, BUFFER_SIZE));
				}
				else
				{
					i = new BufferedInputStream(i, BUFFER_SIZE);
				}

				if (this.response != null)
				{
					this.response.getConnectionInfo().responseCode = conn.getResponseCode();
				}

				InputStream is = new BufferedInputStream(i, BUFFER_SIZE);
				ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
				byte[] buffer = new byte[BUFFER_SIZE];

				int len = 0;
				int readCount = 0;
				while ((len = is.read(buffer)) > -1)
				{
					if (this.response != null)
					{
						this.response.onPublishedDownloadProgress(buffer, readCount, conn.getContentLength());
						this.response.onPublishedDownloadProgress(buffer, readCount, conn.getContentLength(), len);
					}

					byteBuffer.write(buffer, 0, len);
					readCount += len;
				}

				if (this.response != null)
				{
					this.response.getConnectionInfo().responseLength = readCount;
				}

				byte[] responseContent = byteBuffer.toByteArray();
				if (this.response != null)
				{
					// we fake the content length, because it can be -1
					this.response.onPublishedDownloadProgress(responseContent, readCount, readCount);
					this.response.onPublishedDownloadProgress(responseContent, readCount, readCount, readCount);
				}

				is.close();
				i.close();
				byteBuffer.close();
				conn.disconnect();

				if (this.response != null)
				{
					this.response.getConnectionInfo().responseCode = responseCode;
					if (this.response.getConnectionInfo().responseCode / 100 == 2)
					{
						this.response.onSuccess(responseContent);
					}
					else
					{
						this.response.onFailure(responseContent);
					}
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}

			return null;
		}

		@Override protected void onPostExecute(byte[] result)
		{
			super.onPostExecute(result);

			if (this.response != null)
			{
				this.response.getConnectionInfo().responseTime = System.currentTimeMillis();
				this.response.beforeCallback();

				this.response.beforeFinish();
				this.response.onFinish();
			}
		}
	}
}