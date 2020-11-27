import { NativeModules, NativeEventEmitter } from 'react-native';

const { Tsl1128 } = NativeModules;

const events = {};

const eventEmitter = new NativeEventEmitter(Tsl1128);

Tsl1128.on = (event, handler) => {
	const eventListener = eventEmitter.addListener(event, handler);

	events[event] = eventListener;
};

Tsl1128.off = (event) => {
	if (events.hasOwnProperty(event)) {
		const eventListener = events[event];

		eventListener.remove();

		delete events[event];
	}
};

export default Tsl1128;
