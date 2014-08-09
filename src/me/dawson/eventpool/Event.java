package me.dawson.eventpool;

import java.util.HashMap;
import java.util.Map;

public class Event {
	public static final String TAG = "Event";

	private String name;
	private Map<String, Object> mMap;

	public Event(String name) {
		this.name = name;
		this.mMap = new HashMap<String, Object>();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void put(String key, Object object) {
		mMap.put(key, object);
	}

	public String getString(String key) {
		Object obj = mMap.get(key);
		if (obj instanceof String) {
			return (String) obj;
		}
		return null;
	}

	public int getInt(String key) {
		Object obj = mMap.get(key);
		if (obj instanceof Integer) {
			return (Integer) obj;
		}
		return 0;
	}
}
