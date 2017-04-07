/**
 * 
 */
package main;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * 
 * Unofficial API for the Gardena Smart System
 * 
 * @author <a href="http://grunzwanzling.me">Maximilian von Gaisberg
 *         (Grunzwanzling)</a>
 *
 */
public class GardenaSmart {

	String token;
	String user_id;
	String location_id;
	String[] ids;
	JSONObject obj;
	JSONArray devices;
	HashMap<Integer, String> deviceList;
	public int number;

	/**
	 * 
	 * 
	 * Connect to the Gardena Smart System servers and get information about all
	 * the connected devices.
	 * 
	 * @param email
	 *            The Email address of the account
	 * @param password
	 *            The password of the account
	 * @throws IOException
	 * 
	 */
	public GardenaSmart(String email, String password) throws IOException {
		String param = "{\"sessions\": {\"email\": \"" + email + "\",\"password\": \"" + password + "\"}}",
				url = "https://sg-api.dss.husqvarnagroup.net/sg-1/sessions", charset = "UTF-8";
		URLConnection connection = new URL(url).openConnection();
		connection.setDoOutput(true); // Triggers POST.
		connection.setRequestProperty("Accept-Charset", charset);
		connection.setRequestProperty("Content-Type", "application/json;charset=" + charset);

		try (OutputStream output = connection.getOutputStream()) {
			output.write(param.getBytes(charset));
		}

		InputStream response = connection.getInputStream();
		java.util.Scanner s = new java.util.Scanner(response).useDelimiter("\\A");
		String result = !s.hasNext() ? "" : s.next();
		System.out.println(result);
		token = result.substring(22, 58);
		System.out.println(token);
		user_id = result.substring(71, 107);
		System.out.println(user_id);
		ids = getLocationID(token, user_id);
		for (String string : ids)
			System.out.println(string);

		System.out.println("\n\n");

		System.out.println(getDevice());

		deviceList = new HashMap<Integer, String>();

		devices = obj.getJSONArray("devices");
		for (int i = 0; i < devices.length(); i++) {
			JSONObject device = devices.getJSONObject(i);
			deviceList.put(new Integer(i), device.getString("id"));
			number = i;
		}
	}

	/**
	 * Get a <code>Properties</code> Object with all the information about one specific device.
	 * @param number The number from {@link GardenaSmart.getDevices}
	 * @return
	 */
	public Properties getDevice(int number) {
		return mapToProperties(jsonToMap(devices.getJSONObject(number), new HashMap<String, String>(), ""));

	}
/**
 * Get a HashMap with all the connected devices. It maps a number to the <code>deviceID</code>. Use the number to call {@link GardenaSmart.getDevice}
 * @return The <code>HashMap</code>
 */
	public HashMap getDevices() {
		return deviceList;
	}

	private static void parseArray(JSONArray array, HashMap map, String oldKey) {
		int length = array.length();

		for (int i = 0; i < length; i++) {
			Object ob = array.get(i);
			if (ob instanceof String) {
				map.put(oldKey + "." + i, ob);
			} else if (ob instanceof JSONObject) {
				jsonToMap((JSONObject) ob, map, oldKey + "." + i);
			} else if (ob instanceof JSONArray) {
				parseArray((JSONArray) ob, map, oldKey + "." + i);
			} else {
				System.out.println(oldKey + "." + i + " is missing! " + ob.getClass() + "\n" + ob);
			}
		}
	}

	private static HashMap jsonToMap(JSONObject jObject, HashMap map, String oldKey) {

		Iterator<?> keys = jObject.keys();

		while (keys.hasNext()) {
			String key = (String) keys.next();

			JSONObject ob = jObject.optJSONObject(key);
			String string = jObject.optString(key);
			JSONArray arr = jObject.optJSONArray(key);
			int in = jObject.optInt(key, 1337133);
			if (ob != null) {
				jsonToMap(ob, map, oldKey + "." + key);

			} else if (arr != null) {
				parseArray(arr, map, oldKey + "." + key);
			} else if (string != null) {
				map.put(oldKey + "." + key, string);
			} else if (in != 1337133) {
				map.put(oldKey + "." + key, in);
			} else {
				System.out.println(oldKey + "." + key + " is missing!");
			}
		}

		return map;
	}

	private static Properties mapToProperties(HashMap m) {

		Properties props = new Properties();

		for (int i = 0; i < 10; i++) {
			for (int e = 0; e < 10; e++) {
				if (m.containsKey(".abilities." + i + ".properties." + e + ".name")) {
					String value = (String) m.get(".abilities." + i + ".properties." + e + ".value");
					if (m.containsKey(".abilities." + i + ".properties." + e + ".unit")) {
						value += " " + m.get(".abilities." + i + ".properties." + e + ".unit");
						m.remove(".abilities." + i + ".properties." + e + ".unit");
					}
					props.setProperty((String) m.get(".abilities." + i + ".properties." + e + ".name"), value);
					m.remove(".abilities." + i + ".properties." + e + ".name");
					m.remove(".abilities." + i + ".properties." + e + ".value");
					m.remove(".abilities." + i + ".properties." + e + ".id");
					m.remove(".abilities." + i + ".properties." + e + ".writeable");

					if (m.containsKey(".abilities." + i + ".properties." + e + ".timestamp"))
						m.remove(".abilities." + i + ".properties." + e + ".timestamp");

					for (int f = 0; f < 50; f++)
						if (m.containsKey(".abilities." + i + ".properties." + e + ".supported_values." + f))
							m.remove(".abilities." + i + ".properties." + e + ".supported_values." + f);

				}
			}
		}

		for (int i = 0; i < 30; i++)
			if (m.containsKey(".status_report_history." + i + ".message")) {
				props.setProperty(".status_report_history." + i + ".message",
						(String) m.get(".status_report_history." + i + ".message"));
				props.setProperty(".status_report_history." + i + ".level",
						(String) m.get(".status_report_history." + i + ".level"));
				props.setProperty(".status_report_history." + i + ".raw_message",
						(String) m.get(".status_report_history." + i + ".raw_message"));
				props.setProperty(".status_report_history." + i + ".source",
						(String) m.get(".status_report_history." + i + ".source"));
				props.setProperty(".status_report_history." + i + ".timestamp",
						(String) m.get(".status_report_history." + i + ".timestamp"));
				m.remove(".status_report_history." + i + ".message");
				m.remove(".status_report_history." + i + ".level");
				m.remove(".status_report_history." + i + ".raw_message");
				m.remove(".status_report_history." + i + ".source");
				m.remove(".status_report_history." + i + ".timestamp");
			}

		for (int i = 0; i < 10; i++)
			if (m.containsKey(".settings." + i + ".name")) {
				props.setProperty((String) m.get(".settings." + i + ".name"),
						(String) m.get(".settings." + i + ".value"));
				m.remove(".settings." + i + ".name");
				m.remove(".settings." + i + ".value");
				m.remove(".settings." + i + ".id");
			}

		for (int i = 0; i < 30; i++)
			if (m.containsKey(".scheduled_events." + i + ".id")) {
				props.setProperty(".scheduled_events." + i + ".start_at",
						(String) m.get(".scheduled_events." + i + ".start_at"));
				props.setProperty(".scheduled_events." + i + ".end_at",
						(String) m.get(".scheduled_events." + i + ".end_at"));
				props.setProperty(".scheduled_events." + i + ".type",
						(String) m.get(".scheduled_events." + i + ".type"));
				props.setProperty(".scheduled_events." + i + ".recurrence.type",
						(String) m.get(".scheduled_events." + i + ".recurrence.type"));
				props.setProperty(".scheduled_events." + i + ".weekday",
						(String) m.get(".scheduled_events." + i + ".weekday"));
			}

		if (m.containsKey(".name")) {
			props.setProperty("name", (String) m.get(".name"));
			m.remove(".name");
		}
		if (m.containsKey(".configuration_synchronized")) {
			props.setProperty("configuration_synchronized", (String) m.get(".configuration_synchronized"));
			m.remove(".configuration_synchronized");
		}
		if (m.containsKey(".description")) {
			props.setProperty("description", (String) m.get(".description"));
			m.remove(".description");
		}
		if (m.containsKey(".category")) {
			props.setProperty("category", (String) m.get(".category"));
			m.remove(".category");
		}
		if (m.containsKey(".configuration_synchronized_v2.value")) {
			props.setProperty("configuration_synchronized_v2.value",
					(String) m.get(".configuration_synchronized_v2.value"));
			m.remove(".configuration_synchronized_v2.value");
		}
		return props;
	}

	public String[] getLocationID(String token, String user_id) throws MalformedURLException, IOException {
		String url = "https://sg-api.dss.husqvarnagroup.net/sg-1/locations/?user_id=" + user_id, charset = "UTF-8";
		URLConnection connection = new URL(url).openConnection();
		connection.setDoOutput(true); // Triggers POST.
		// connection.setRequestProperty("Accept-Charset", charset);
		connection.setRequestProperty("Content-Type", "application/json");
		connection.setRequestProperty("X-Session", token);

		connection.connect();

		InputStream response = connection.getInputStream();
		java.util.Scanner s = new java.util.Scanner(response).useDelimiter("\\A");
		String result = s.hasNext() ? s.next() : "";
		System.out.println(result);
		location_id = result.substring(21, 57);
		result = result.substring(result.indexOf("\"devices\":") + 12, result.indexOf("],\"zones\"") - 1);
		return result.split("\",\"");
	}

	private String getDevice() throws MalformedURLException, IOException {
		String url = "https://sg-api.dss.husqvarnagroup.net/sg-1/devices?locationId=" + location_id;
		URLConnection connection = new URL(url).openConnection();
		connection.setDoOutput(true); // Triggers POST.
		// connection.setRequestProperty("Accept-Charset", charset);
		connection.setRequestProperty("Content-Type", "application/json");
		connection.setRequestProperty("X-Session", token);

		connection.connect();

		InputStream response = connection.getInputStream();
		java.util.Scanner s = new java.util.Scanner(response).useDelimiter("\\A");
		String result = !s.hasNext() ? "" : s.next();

		// result = result.substring(12, result.length()-2);

		obj = new JSONObject(result);

		return result;
	}

	public static void main(String[] args) throws IOException {
		GardenaSmart gardena = new GardenaSmart("alexander@gaisberg-helfenberg.de", "beilstein");
		gardena.getDevice(1).list(System.out);
	}

}
