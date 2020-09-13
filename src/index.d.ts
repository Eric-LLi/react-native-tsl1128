export enum READER_EVENTS {
	TAG = 'TAG',
	TAGS = 'TAGS',
	HANDLE_ERROR = 'HANDLE_ERROR',
	BARCODE = 'BARCODE',
	LOCATE_TAG = 'LOCATE_TAG',
	WRITE_TAG = 'WRITE_TAG',
	TRIGGER_STATUS = 'TRIGGER_STATUS',
	READER_STATUS = 'READER_STATUS',
}

export type DevicesTypes = {
	//
};

type Callbacks<T> = {
	//
};

export declare function on<T extends READER_EVENTS, U>(event: T, callback: Callbacks<T>): void;

export declare function off(event: READER_EVENTS): void;

export declare function connect(): Promise<boolean>;

export declare function disconnect(): Promise<boolean>;

export declare function isConnected(): Promise<boolean>;

export declare function clear(): void;

export declare function getDevices(): Promise<Array<DevicesTypes>>;

export declare function getBatteryPower(): Promise<string>;

export declare function setBatteryPower(power: string): Promise<void>;

export declare function programTag(tag: string, targetTag: string): Promise<boolean>;

export declare function locateTag(tag: string): Promise<void>;
