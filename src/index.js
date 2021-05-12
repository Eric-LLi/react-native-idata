// main index.js

import { NativeModules, NativeEventEmitter } from 'react-native';

const { Idata } = NativeModules;

const events = {};

const eventEmitter = new NativeEventEmitter(Idata);

Idata.on = (event, handler) => {
	const eventListener = eventEmitter.addListener(event, handler);

	events[event] =  events[event] ? [...events[event], eventListener]: [eventListener];
};

Idata.off = (event) => {
	if (Object.hasOwnProperty.call(events, event)) {
		const eventListener = events[event].shift();

		if(eventListener) eventListener.remove();
	}
};

Idata.removeAll = (event) => {
	if (Object.hasOwnProperty.call(events, event)) {
		eventEmitter.removeAllListeners(event);

		events[event] = [];
	}
}

export default Idata;
