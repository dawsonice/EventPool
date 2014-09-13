package me.dawson.eventpool;

import java.lang.reflect.Method;

public class ListenerHolder {
	private String event;
	private Method method;
	private Object listener;
	private boolean onMain;

	public ListenerHolder() {
		onMain = true;
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

	public boolean getOnMain() {
		return onMain;
	}

	public void setOnMain(boolean onMain) {
		this.onMain = onMain;
	}
}
