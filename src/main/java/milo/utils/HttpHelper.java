package milo.utils;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.ProtocolException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

public class HttpHelper {

	private static final String[] agents = new String[]{
			"Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 5.1; .NET CLR 2.0.50727)",
//		"Mozilla/5.0 (iPhone; CPU iPhone OS 6_0 like Mac OS X) AppleWebKit/536.26 (KHTML, like Gecko) Version/6.0 Mobile/10A5376e
			"Mozilla/5.0 (Windows NT 6.2; WOW64) AppleWebKit/537.31 (KHTML, like Gecko) Chrome/26.0.1410.64 Safari/537.31",
			"Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 5.1; Trident/4.0)",
//		"Mozilla/5.0 (Linux; U; Android 4.1.2; sk-sk; PMP7280C3G Build/JZO54K) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 MobilSafari/534.30"
			"Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 5.1; InfoPath.1; .NET CLR 2.0.50727; .NET CLR 1.1.4322; MS-RTC LM",
			"Mozilla/5.0 (Windows NT 6.3; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Maxthon/4.4.1.5000 Chrome/30.0.1599.101 Safari/537.36",
			"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_10_2) AppleWebKit/600.3.5 (KHTML, like Gecko) Version/8.0.2 Safari/600.3.5",
			"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_5) AppleWebKit/537.78.2 (KHTML, like Gecko) Version/6.1.6 Safari/537.78.2",
			"Mozilla/5.0 (Macintosh; Intel Mac OS X 10.7; rv:33.0) Gecko/20100101 Firefox/33.0",
			"Mozilla/5.0 (Windows NT 6.1; WOW64; rv:33.0) Gecko/20100101 Firefox/33.0",
			"Mozilla/5.0 (Windows NT 6.3; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Maxthon/4.4.1.3000 Chrome/30.0.1599.101 Safari/537.36",
			"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/39.0.2171.71 Safari/537.36",
			"Mozilla/5.0 (Windows NT 6.1; WOW64; Trident/7.0; rv:11.0) like Gecko",
			"Mozilla/5.0 (Windows NT 6.3; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/39.0.2171.71 Safari/537.36",
			"Opera/9.80 (Windows NT 6.1; WOW64) Presto/2.12.388 Version/12.17",
			"Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/39.0.2171.71 Safari/537.36",
			"Mozilla/5.0 (Windows NT 6.2; WOW64) AppleWebKit/537.1 (KHTML, like Gecko) Maxthon/4.1.3.2000 Chrome/26.0.1410.43 Safari/537.1",
			"Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/31.0.1650.57 Safari/537.36",
			"Mozilla/5.0 (Windows NT 5.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/30.0.1599.101 Safari/537.36",
			"Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.0; Trident/5.0)",
			"Mozilla/5.0 (compatible; MSIE 10.0; Windows NT 6.1; Trident/6.0)",
			"Mozilla/5.0 (Windows NT 6.3; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/31.0.1650.57 Safari/537.36",
			"Mozilla/5.0 (Windows NT 6.3; WOW64; rv:25.0) Gecko/20100101 Firefox/25.0",
			"Mozilla/5.0 (Windows NT 6.3; WOW64; rv:31.0) Gecko/20100101 Firefox/31.0",
			"Mozilla/5.0 (Windows NT 6.3; WOW64; Trident/7.0; Touch; MALNJS; rv:11.0) like Gecko",
			"Mozilla/5.0 (Windows NT 6.3; Win64; x64; Trident/7.0; Touch; MALNJS; rv:11.0) like Gecko",
			"Mozilla/5.0 (Windows NT 6.3; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/31.0.1650.57 Safari/537.36 OPR/18.0.1284.49",
			"Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/38.0.2125.101 Safari/537.36"
	};

	public static String download(String url) throws IOException, MetaRefreshOccurred {
		return download(buildUrlConnection(url));
	}

	public static String download(URLConnection urlCon) throws IOException, MetaRefreshOccurred {

		urlCon.connect();

		StringBuffer tmp = new StringBuffer();
		InputStream inputStream = ((urlCon.getContentEncoding() != null) && urlCon.getContentEncoding().equals("gzip"))
				? new GZIPInputStream(urlCon.getInputStream()) : urlCon.getInputStream();
		String charset = urlCon.getContentType();

		if (charset == null) {
			charset = "utf-8";
		} else {
			String[] parts = charset.split("harset=");
			charset = (parts.length > 1) ? parts[1].trim() : "utf-8";
		}

		try (BufferedReader in = new BufferedReader(new InputStreamReader(inputStream, Charset.forName(charset)))) {
			String line;
			while ((line = in.readLine()) != null) {
				tmp.append(line);
			}
			in.close();
		}

		if (urlCon instanceof HttpURLConnection) {
			((HttpURLConnection) urlCon).disconnect();
		}
		String result = String.valueOf(tmp);
		String metaRefreshUrl = findMetaRefreshUrl(result);
		if (metaRefreshUrl != null && !metaRefreshUrl.isEmpty()) {
			if (metaRefreshUrl.startsWith("/")) {
				metaRefreshUrl = urlCon.getURL().getProtocol() + "://" + urlCon.getURL().getHost() + metaRefreshUrl;
			}
			throw new MetaRefreshOccurred(metaRefreshUrl);
		}
		return result;
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

	public static URLConnection buildUrlConnection(String urlString, String ipAddress, int port) throws IOException {
		CookieHandler.setDefault(new CookieManager(null, CookiePolicy.ACCEPT_ALL));
		URL url = new URL(fixUrl(urlString).replaceAll("\\P{Print}", ""));
		Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(ipAddress, port));
		URLConnection uc = url.openConnection(proxy);
		setDefaultParams(uc);
		return uc;
	}

	public static URLConnection buildUrlConnection(String urlString) throws IOException {
		CookieHandler.setDefault(new CookieManager(null, CookiePolicy.ACCEPT_ALL));
		String fixedUrlString = fixUrl(urlString).replaceAll("\\P{Print}", "");
		URL url = new URL(fixedUrlString);
		URLConnection uc = url.openConnection();
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

	private static void setDefaultParams(URLConnection uc) throws ProtocolException {
		uc.setConnectTimeout(10000);
		uc.setReadTimeout(50000);
		uc.setAllowUserInteraction(false);
		if (uc instanceof HttpURLConnection) {
			((HttpURLConnection) uc).setInstanceFollowRedirects(true);
			((HttpURLConnection) uc).setRequestMethod("GET");
		}
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
		} catch (Exception e) {
		}
	}

}
