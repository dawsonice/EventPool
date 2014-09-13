###EventPool

===

An easy and flexiable event bus for Android.

No need to declare receivers implements.

Multiple event handler functions supported.

===

Filter events for listener's receive functions,

More than one filters can be defined in each class.

```
receive "started" & "finished" events on main thread

@EventFilter(events = { "started", "finished" })
private void onStatus(Event e) {
	// handle status events here
	String eventName = e.getName();
	tvStatus.setText(eventName);
}

receive "progress" event on worker thread

@EventFilter(events = { "progress"}, isMain = false)
private void onProgress(Event e) {
	// handle progress event here
	int progress = e.getInt("progress");
	Log.d(TAG, "onProgress " + progress);
}

```

Attach listener for events

```
EventPool.attach(this);
```

Detach listener for events;

```
EventPool.detach(this);
```

Send envets to pool

```
Event event = new Event("event_name");
event.put("progress", 64);
EventPool.sendEvent(event);
```

Quit event pool,

event pool can quit by user or automaticly by detach all listeners

```
EventPool.quit()
```


===

Any further question?

[Email](mailto:coder.kiss@gmail.com) me please!

