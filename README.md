###EventPool

===

An easy and flexiable event bus for Android.

No need to declare receivers ikplements.

Multiple event handler functions supported.

===

Filter events for function

```
@EventFilter(events = { "progress", "started" })
private void onEvent(Event e) {
	// handle events here
}
```

Attach filter for events

```
EventPool.getInstance().attach(this);
```

Detach filter for events;

```
EventPool.getInstance().detach(this);
```

===

Any further question?

[Email](mailto:coder.kiss@gmail.com) me please!

