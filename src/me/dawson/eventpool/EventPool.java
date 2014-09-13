package me.dawson.eventpool;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

public class EventPool {
	public static final String TAG = "EventPool";

	private volatile static EventPool instance;

	private static EventPool getInstance() {
		synchronized (EventPool.class) {
			if (instance == null) {
				instance = new EventPool();
			}
		}
		return instance;
	}

	public static boolean sendEvent(Event event) {
		if (event == null || TextUtils.isEmpty(event.getName())) {
			return false;
		}
		return getInstance().realSendEvent(event);
	}

	public static boolean sendEvent(String name) {
		if (TextUtils.isEmpty(name)) {
			return false;
		}
		Event event = new Event(name);
		return getInstance().realSendEvent(event);
	}

	public static void quit() {
		if (instance == null) {
			return;
		}

		instance.realQuit();
		instance = null;
	}

	public static boolean attach(Object object) {
		return getInstance().realAttach(object);
	}

	public static boolean detach(Object object) {
		return getInstance().realDetach(object);
	}

	private Object eventLock;
	private Object poolLock;
	private HashMap<String, List<ListenerHolder>> mListeners;
	private LinkedList<Event> mEvents;
	private boolean dispatching;
	private Handler handler;
	private EventRunner runner;

	private EventPool() {
		eventLock = new Object();
		poolLock = new Object();
		mListeners = new HashMap<String, List<ListenerHolder>>();
		mEvents = new LinkedList<Event>();
		dispatching = true;
	}

	private boolean realAttach(Object target) {
		if (target == null) {
			return false;
		}

		Log.d(TAG, "realAttach " + target);

		Class<?> clazz = target.getClass();
		Method[] methods = clazz.getDeclaredMethods();
		for (int index = 0; index < methods.length; ++index) {
			Method method = methods[index];
			Class<EventFilter> filterClazz = EventFilter.class;

			if (!method.isAnnotationPresent(filterClazz)) {
				method = null;
				continue;
			}

			EventFilter eventFilter = method.getAnnotation(filterClazz);
			String[] events = eventFilter.events();

			if (events == null || events.length == 0 || method == null) {
				Log.d(TAG, "invalid event filter declaration!");
				continue;
			}

			if (!Modifier.isPublic(method.getModifiers())) {
				Log.w(TAG, "listener method should be public");
			}

			final Class<?>[] pTypes = method.getParameterTypes();
			if (pTypes == null || pTypes.length != 1) {
				Log.e(TAG, "invalid parameter list");
				continue;
			}

			Class<?> pClazz = pTypes[0];
			if (!Event.class.isAssignableFrom(pClazz)) {
				Log.e(TAG, "invalid parameter type");
				continue;
			}

			boolean isMain = eventFilter.isMain();

			synchronized (poolLock) {
				for (String event : events) {
					if (TextUtils.isEmpty(event)) {
						continue;
					}

					Log.d(TAG, "attach filter event " + event);
					ListenerHolder holder = new ListenerHolder();
					holder.setIsMain(isMain);
					holder.setEvent(event);
					holder.setListener(target);
					holder.setMethod(method);
					List<ListenerHolder> list = null;
					if (mListeners.containsKey(event)) {
						list = mListeners.get(event);
					} else {
						list = new ArrayList<ListenerHolder>();
						mListeners.put(event, list);
					}
					list.add(holder);
				}
			}
		}

		return true;
	}

	private boolean realDetach(Object target) {
		if (target == null) {
			return false;
		}

		Log.d(TAG, "realDetach " + target);

		synchronized (poolLock) {
			Iterator<Map.Entry<String, List<ListenerHolder>>> iter = mListeners
					.entrySet().iterator();
			while (iter.hasNext()) {
				Map.Entry<String, List<ListenerHolder>> entry = iter.next();
				List<ListenerHolder> list = entry.getValue();
				int listSz = list.size();
				for (int index = listSz - 1; index >= 0; --index) {
					ListenerHolder holder = list.get(index);
					Object object = holder.getListener();
					if (target.equals(object)) {
						list.remove(index);
					}
				}
				if (list.isEmpty()) {
					iter.remove();
				}
			}

			if (mListeners.isEmpty()) {
				realQuit();
			}
		}

		return true;
	}

	private boolean realSendEvent(Event event) {
		if (event == null) {
			return false;
		}

		String eventName = event.getName();
		if (TextUtils.isEmpty(eventName)) {
			return false;
		}

		Log.d(TAG, "realSendEvent " + eventName + " " + event.hashCode());

		synchronized (eventLock) {
			mEvents.addLast(event);
			eventLock.notify();
		}

		if (runner == null) {
			handler = new Handler(Looper.getMainLooper());
			runner = new EventRunner();
			runner.start();
		}

		return true;
	}

	private void realQuit() {
		Log.d(TAG, "realQuit");
		dispatching = false;

		if (runner != null) {
			runner.interrupt();
			runner = null;
			handler = null;
		}

		synchronized (eventLock) {
			mEvents.clear();
		}

		synchronized (poolLock) {
			mListeners.clear();
		}
	}

	private class EventRunner extends Thread {

		@Override
		public void run() {
			Log.d(TAG, "enter event runner");
			while (dispatching) {
				Event event = null;
				synchronized (eventLock) {
					if (mEvents.isEmpty()) {
						try {
							eventLock.wait();
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}

				synchronized (eventLock) {
					if (!mEvents.isEmpty()) {
						event = mEvents.removeFirst();
					}
				}

				if (event == null || !dispatching) {
					break;
				}

				runEvent(event);
			}
			Log.d(TAG, "exit event runner");
		}
	};

	private void runEvent(final Event event) {
		if (event == null) {
			return;
		}

		String eventName = event.getName();
		Log.d(TAG, "runEvent " + eventName + " " + event.hashCode());

		List<ListenerHolder> list = mListeners.get(eventName);
		if (list == null || list.isEmpty()) {
			Log.w(TAG, "no listener for " + eventName);
			return;
		}

		for (final ListenerHolder holder : list) {
			if (holder.getIsMain()) {
				handler.post(new Runnable() {
					public void run() {
						realRunEvent(holder, event);
					}
				});
			} else {
				realRunEvent(holder, event);
			}
		}
	}

	private void realRunEvent(ListenerHolder holder, Event event) {
		try {
			Method method = holder.getMethod();
			boolean oldAcc = method.isAccessible();
			if (!oldAcc) {
				method.setAccessible(true);
			}
			Object listener = holder.getListener();

			method.invoke(listener, event);

			if (method.isAccessible() != oldAcc) {
				method.setAccessible(oldAcc);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
