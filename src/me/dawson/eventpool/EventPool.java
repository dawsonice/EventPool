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

	public static EventPool getInstance() {
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
		return getInstance().doSendEvent(event);
	}

	public static boolean sendEvent(String name) {
		if (TextUtils.isEmpty(name)) {
			return false;
		}
		Event event = new Event(name);
		return getInstance().doSendEvent(event);
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
		dispatching = false;
		handler = new Handler(Looper.getMainLooper());
		runner = new EventRunner();
	}

	public boolean attach(Object target) {
		if (target == null) {
			return false;
		}

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

			synchronized (poolLock) {
				for (String event : events) {
					if (TextUtils.isEmpty(event)) {
						continue;
					}

					Log.d(TAG, "attach filter event " + event);
					ListenerHolder holder = new ListenerHolder();
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

		Log.d(TAG, "attach " + target);
		return true;
	}

	public boolean detach(Object target) {
		if (target == null) {
			return false;
		}

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
		}

		Log.d(TAG, "detach " + target);

		return true;
	}

	private boolean doSendEvent(Event event) {
		if (event == null || TextUtils.isEmpty(event.getName())) {
			return false;
		}

		synchronized (eventLock) {
			mEvents.addLast(event);
		}

		dispatchEvent();
		return true;
	}

	private void dispatchEvent() {
		if (dispatching) {
			return;
		}

		Event event = null;
		synchronized (eventLock) {
			if (!mEvents.isEmpty()) {
				event = mEvents.removeFirst();
			}
		}

		if (event == null) {
			return;
		}

		runner.setEvent(event);
		handler.post(runner);
	}

	class EventRunner implements Runnable {
		private Event event = null;

		public void setEvent(Event e) {
			this.event = e;
		}

		@Override
		public void run() {
			dispatching = true;
			runEvent();
			dispatching = false;
			dispatchEvent();
		}

		private void runEvent() {
			if (event == null) {
				return;
			}
			String eventName = event.getName();
			List<ListenerHolder> list = mListeners.get(eventName);
			if (list == null || list.isEmpty()) {
				Log.w(TAG, "no listener for " + eventName);
				return;
			}

			for (ListenerHolder holder : list) {
				Method method = holder.getMethod();
				boolean oldAcc = method.isAccessible();
				if (!oldAcc) {
					method.setAccessible(true);
				}
				Object listener = holder.getListener();

				try {
					method.invoke(listener, event);
				} catch (Exception e) {
					e.printStackTrace();
				}

				if (method.isAccessible() != oldAcc) {
					method.setAccessible(oldAcc);
				}
			}
		}
	};
}
