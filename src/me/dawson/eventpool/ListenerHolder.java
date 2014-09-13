package me.dawson.eventpool;

import java.lang.reflect.Method;

public class ListenerHolder {

	// event name to receive
	private String event;

	// corresponding method name
	private Method method;

	// listener object stub
	private Object listener;

	// if run on main thread
	private boolean isMain;

	public ListenerHolder() {
		isMain = true;
	}

	public String getEvent() {
		return event;
	}

	public void setEvent(String event) {
		this.event = event;
	}

	public Method getMethod() {
		return method;
	}

	public void setMethod(Method method) {
		this.method = method;
	}

	public Object getListener() {
		return listener;
	}

	public void setListener(Object listener) {
		this.listener = listener;
	}

	public boolean getIsMain() {
		return isMain;
	}

	public void setIsMain(boolean isMain) {
		this.isMain = isMain;
	}
}
