package milo.utils;

import org.brotli.dec.BrotliInputStream;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.ProtocolException;
import java.net.Proxy;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

public class HttpHelper {

	private static final Logger LOG = Logger.getLogger(HttpHelper.class.getName());
	private static final String[] agents = new String[]{
			"Mozilla/5.0 (Windows NT 6.3; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/96.0.4664.93 Safari/537.36",
			"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/96.0.4664.45 Safari/537.36 OPR/82.0.4227.23",
			"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/96.0.4664.45 Safari/537.36",
			"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/96.0.4664.93 Safari/537.36",
			"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/96.0.4664.55 Safari/537.36 Edg/96.0.1054.43",
			"Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/96.0.4664.45 Safari/537.36",
			"Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/96.0.4664.45 Safari/537.36 OPR/82.0.4227.23",
			"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_6) AppleWebKit/601.7.7 (KHTML, like Gecko) Version/9.1.2 Safari/601.7.7",
			"Mozilla/5.0 (Windows NT 6.3; Win64; x64; Trident/7.0; Touch; MALNJS; rv:11.0) like Gecko",
			// NOT WORKING:
//			"Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Safari/537.36",
//			"Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/135.0.0.0 Safari/537.36",
//			"Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/136.0.0.0 Safari/537.36",
//			"Mozilla/5.0 (X11; CrOS x86_64 14541.0.0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36",
//			"Mozilla/5.0 (X11; CrOS x86_64 14541.0.0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/136.0.0.0 Safari/537.36",
//			"Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36",
//			"Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/137.0.0.0 Safari/537.36",
//			"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36",
//			"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/139.0.0.0 Safari/537.36",
//			"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/135.0.0.0 Safari/537.36",
//			"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Safari/537.36",
//			"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36",
//			"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/136.0.0.0 Safari/537.36",
//			"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/137.0.0.0 Safari/537.36",
//			"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/136.0.0.0 Safari/537.36 Edg/136.0.0.0",
//			"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/137.0.0.0 Safari/537.36 Edg/137.0.0.0",
			// NOT WORKING ANYMORE:
//			"Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:76.0) Gecko/20100101 Firefox/76.0",
//			"Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:94.0) Gecko/20100101 Firefox/94.0",
//			"Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:94.0) Gecko/20100101 Firefox/94.0",
//			"Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:95.0) Gecko/20100101 Firefox/95.0",
//			"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_6) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.1.2 Safari/605.1.15",
//			"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.1 Safari/605.1.15",
//			"Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/81.0.4044.129 Safari/537.36",
//			"Mozilla/5.0 (Linux; Android 10; Mi A2 Lite) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/96.0.4664.92 Mobile Safari/537.36",
			// NEW:
			"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36",
			"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/136.0.0.0 Safari/537.36 Avast/136.0.0.0",
			"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Safari/537.36",
			"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/127.0.0.0 Safari/537.36",
			"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/109.0.0.0 Safari/537.36",
			"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36",
			"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36",
			"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/136.0.0.0 Safari/537.36 Edg/136.0.0.0",
			"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/135.0.0.0 Safari/537.36",
			"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/137.0.0.0 Safari/537.36 Edg/137.0.0.0",
			"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/136.0.0.0 Safari/537.36",
			"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/137.0.0.0 Safari/537.36"
	};

	public static PageResponse download(String url) throws IOException, MetaRefreshOccurred {
		return download(buildUrlConnection(url));
	}

	public static PageResponse download(
			String url, String cookies, int followRedirects, boolean allowErrorStream
	) throws IOException, MetaRefreshOccurred {
		HttpURLConnection httpURLConnection = buildUrlConnection(url);
		httpURLConnection.setRequestProperty("Cookie", cookies);
		return download(httpURLConnection, followRedirects, allowErrorStream);
	}

	public static PageResponse download(HttpURLConnection urlCon) throws IOException, MetaRefreshOccurred {
		return download(urlCon, 0, false);
	}

	public static PageResponse download(
			HttpURLConnection urlCon, int followAnyRedirects, boolean allowErrorStream
	) throws IOException, MetaRefreshOccurred {
		InputStream urlInputStream = null;
		InputStream inputStream = null;
		try {
			urlCon.connect();
			int responseCode = urlCon.getResponseCode();
			String encoding = urlCon.getContentEncoding() == null ? "" : urlCon.getContentEncoding();

			urlInputStream = responseCode >= 400 && allowErrorStream ? urlCon.getErrorStream() : urlCon.getInputStream();

			switch (encoding) {
				case "gzip":
					inputStream = new GZIPInputStream(urlInputStream);
					break;
				case "br":
					inputStream = new BrotliInputStream(urlInputStream);
					break;
				default:
					inputStream = urlInputStream;
			}

			String charset = urlCon.getContentType();
			if (charset == null) {
				charset = "utf-8";
			} else {
				String[] parts = charset.split("harset=");
				charset = (parts.length > 1) ? parts[1].trim() : "utf-8";
			}
			charset = charset.replaceAll("\"", "");

			StringBuffer tmp = new StringBuffer();
			try (BufferedReader in = new BufferedReader(new InputStreamReader(inputStream, Charset.forName(charset)))) {
				String line;
				while ((line = in.readLine()) != null) {
					tmp.append(line);
				}
				in.close();
			}

			Map<String, List<String>> headerFields;
			try {
				headerFields = new HashMap<>(urlCon.getHeaderFields());
			} catch (Exception ex) {
				LOG.warning("caught urlCon.getHeaderFields() " + ex.getMessage());
				headerFields = new HashMap<>();
			}
			String cookies = extractCookiesFromConnection(urlCon);

			urlCon.disconnect();

			String result = String.valueOf(tmp);

			String metaRefreshUrl = findMetaRefreshUrl(result);
			if (metaRefreshUrl != null && !metaRefreshUrl.isEmpty()) {
				if (metaRefreshUrl.startsWith("/")) {
					metaRefreshUrl = urlCon.getURL().getProtocol() + "://" + urlCon.getURL().getHost() + metaRefreshUrl;
				}
				throw new MetaRefreshOccurred(metaRefreshUrl);
			}

			// allow redirect from http to https
			if (responseCode == 301 || responseCode == 302) {
				String newUrlString = urlCon.getHeaderField("Location");
				if (followAnyRedirects > 0 || newUrlString.replace("https", "http").equals(urlCon.getURL().toString())) {
					return download(newUrlString, cookies, followAnyRedirects - 1, allowErrorStream);
				}
			}

			headerFields.put("$cookies", Collections.singletonList(cookies));
			return new PageResponse(headerFields, result);
		} finally {
			if (inputStream != null) {
				inputStream.close();
			}
			if (urlInputStream != null) {
				urlInputStream.close();
			}
			if (urlCon != null) {
				urlCon.disconnect();
			}
		}
	}

	public static String findMetaRefreshUrl(String content) {
		content = content.replaceAll("(?i)<noscript.*?>.*?</noscript>", "");
		String pattern = "<meta[^>]*?http-equiv=['\"]refresh['\"][^>]*?url=['\"]([^>]*?)['\"][^>]*>";
		Matcher urlMatcher = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE)
				.matcher(content);
		while (urlMatcher.find()) {
			if (urlMatcher.group(1) != null && !urlMatcher.group(1).isEmpty()) {
				return urlMatcher.group(1);
			}
		}
		return null;
	}

	public static HttpURLConnection buildUrlConnection(String urlString, String ipAddress, int port) throws IOException {
//		CookieHandler.setDefault(new CookieManager(null, CookiePolicy.ACCEPT_ALL));
		URL url = new URL(fixUrl(urlString).replaceAll("\\P{Print}", ""));
		Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(ipAddress, port));
		HttpURLConnection uc = (HttpURLConnection) url.openConnection(proxy);
		setDefaultParams(uc);
		return uc;
	}

	public static HttpURLConnection buildUrlConnection(String urlString) throws IOException {
//		CookieHandler.setDefault(new CookieManager(null, CookiePolicy.ACCEPT_ALL));
		String fixedUrlString = fixUrl(urlString).replaceAll("\\P{Print}", "");
		URL url = new URL(fixedUrlString);
		HttpURLConnection uc = (HttpURLConnection) url.openConnection();
		setDefaultParams(uc);
		return uc;
	}

	public static String fixUrl(String url) {
		if (url.startsWith("//")) {
			url = "http:" + url;
		}
		if (!url.startsWith("http")) {
			url = "http://" + url;
		}
		// fix fucked up urls e.g. 'http://bla.bl/../xyz/' issue
		return url.replaceFirst("^([a-z]+://[^/]+/)\\.\\./(.*)$", "$1$2");
	}

	public static String varcharUrl(String url) {
		if (url.length() >= 255) {
			url = url.substring(0, 254);
		}
		return url;
	}

	public static boolean isUrl(String url) {
		return url != null && url.matches("^(([a-zA-Z]+:)?//)?[^/]+\\.[a-zA-Z]+.*$");
	}

	public static String parseFirstAndSecondClassDomain(String url) {
		String newUrl = url.replaceFirst("^[a-zA-Z]+://([^\\.]+\\.)*([^/\\.]+)\\.(com?\\.)([a-zA-Z]+).*$", "$2.$3$4");
		if (newUrl != url) {
			return newUrl;
		}
		return url.replaceFirst("^[a-zA-Z]+://([^\\.]+\\.)*([^/\\.]+)\\.([a-zA-Z]+).*$", "$2.$3");
	}

	private static void setDefaultParams(HttpURLConnection uc) throws ProtocolException {
		uc.setConnectTimeout(10000);
		uc.setReadTimeout(50000);
		uc.setAllowUserInteraction(false);
		uc.setInstanceFollowRedirects(true);
		uc.setRequestMethod("GET");
		uc.addRequestProperty("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
		uc.addRequestProperty("accept-encoding", "gzip,deflate,sdch,br");
		uc.addRequestProperty("accept-language", "en-US,en;q=0.8,cs;q=0.4,sk;q=0.2");
		uc.addRequestProperty("User-Agent", getRandomAgent());
	}

	public static String getRandomAgent() {
		return agents[new Random().nextInt(agents.length)];
	}

	public static class MetaRefreshOccurred extends Exception {
		private String refreshUrl;

		public MetaRefreshOccurred(String refreshUrl) {
			this.refreshUrl = refreshUrl;
		}

		public String getRefreshUrl() {
			return refreshUrl;
		}
	}

	public static void trustAllSslCertificates() {
		TrustManager[] trustAllCerts = new TrustManager[]{
				new X509TrustManager() {
					public java.security.cert.X509Certificate[] getAcceptedIssuers() {
						return null;
					}
					public void checkClientTrusted(
							java.security.cert.X509Certificate[] certs, String authType) {
					}
					public void checkServerTrusted(
							java.security.cert.X509Certificate[] certs, String authType) {
					}
				}
		};

		try {
			SSLContext sc = SSLContext.getInstance("SSL");
			sc.init(null, trustAllCerts, new java.security.SecureRandom());
			HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
			HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
		} catch (Exception e) {
		}
	}

	public static String extractCookiesFromConnection(HttpURLConnection urlCon) {
		StringBuilder cookies = new StringBuilder();
		try {
			Map<String, List<String>> headerFields = urlCon.getHeaderFields();
			List<String> setCookie = (headerFields != null && headerFields.containsKey("Set-Cookie") ?
					headerFields.get("Set-Cookie") : Collections.emptyList());
			setCookie = setCookie.stream().filter(cookieItem -> cookieItem != null && !cookieItem.isEmpty())
					.collect(Collectors.toList());

			for (String cookieString : setCookie) {
				List<HttpCookie> cookieList = HttpCookie.parse(cookieString);
				for (HttpCookie cookie : cookieList) {
					if (cookies.length() > 0) {
						cookies.append("; ");
					}
					cookies.append(cookie.toString());
				}
			}
		} catch (Exception ex) {
			LOG.warning("caught cookie parsing exception " + ex.getMessage());
		}
		return cookies.toString();
	}

	public static class PageResponse {
		private String content;
		private Map<String, List<String>> headers;

		public PageResponse() {
		}

		public PageResponse(Map<String, List<String>> headerFields, String content) {
			setHeaders(headerFields);
			setContent(content);
		}

		public String getContent() {
			return content;
		}

		public void setContent(String content) {
			this.content = content;
		}

		public Map<String, List<String>> getHeaders() {
			return headers;
		}

		public void setHeaders(Map<String, List<String>> headers) {
			this.headers = headers;
		}
	}

}
