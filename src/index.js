import { NativeModules, NativeEventEmitter } from 'react-native';

const { Tsl1128 } = NativeModules;

const events = {};

const eventEmitter = new NativeEventEmitter(Tsl1128);

Tsl1128.on = (event, handler) => {
	const eventListener = eventEmitter.addListener(event, handler);

	events[event] =  events[event] ? [...events[event], eventListener]: [eventListener];
};

Tsl1128.off = (event) => {
	if (Object.hasOwnProperty.call(events, event)) {
		const eventListener = events[event].shift();

		if(eventListener) eventListener.remove();
	}
};

Tsl1128.removeAll = (event) => {
	if (Object.hasOwnProperty.call(events, event)) {
		eventEmitter.removeAllListeners(event);

		events[event] = [];
	}
}

export default Tsl1128;
