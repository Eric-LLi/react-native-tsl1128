package com.tsl1128;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
//import android.media.MediaPlayer;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.uk.tsl.rfid.asciiprotocol.AsciiCommander;
import com.uk.tsl.rfid.asciiprotocol.commands.AlertCommand;
import com.uk.tsl.rfid.asciiprotocol.commands.BatteryStatusCommand;
import com.uk.tsl.rfid.asciiprotocol.commands.FactoryDefaultsCommand;
import com.uk.tsl.rfid.asciiprotocol.commands.FindTagCommand;
import com.uk.tsl.rfid.asciiprotocol.commands.InventoryCommand;
import com.uk.tsl.rfid.asciiprotocol.commands.SwitchActionCommand;
import com.uk.tsl.rfid.asciiprotocol.commands.WriteTransponderCommand;
import com.uk.tsl.rfid.asciiprotocol.device.ConnectionState;
import com.uk.tsl.rfid.asciiprotocol.device.IAsciiTransport;
import com.uk.tsl.rfid.asciiprotocol.device.ObservableReaderList;
import com.uk.tsl.rfid.asciiprotocol.device.Reader;
import com.uk.tsl.rfid.asciiprotocol.device.ReaderManager;
import com.uk.tsl.rfid.asciiprotocol.device.TransportType;
import com.uk.tsl.rfid.asciiprotocol.enumerations.Databank;
import com.uk.tsl.rfid.asciiprotocol.enumerations.QuerySelect;
import com.uk.tsl.rfid.asciiprotocol.enumerations.QuerySession;
import com.uk.tsl.rfid.asciiprotocol.enumerations.QueryTarget;
import com.uk.tsl.rfid.asciiprotocol.enumerations.SelectAction;
import com.uk.tsl.rfid.asciiprotocol.enumerations.SelectTarget;
import com.uk.tsl.rfid.asciiprotocol.enumerations.SwitchAction;
import com.uk.tsl.rfid.asciiprotocol.enumerations.SwitchState;
import com.uk.tsl.rfid.asciiprotocol.enumerations.TriState;
import com.uk.tsl.rfid.asciiprotocol.responders.ICommandResponseLifecycleDelegate;
import com.uk.tsl.rfid.asciiprotocol.responders.ISignalStrengthReceivedDelegate;
import com.uk.tsl.rfid.asciiprotocol.responders.ISwitchStateReceivedDelegate;
import com.uk.tsl.rfid.asciiprotocol.responders.ITransponderReceivedDelegate;
import com.uk.tsl.rfid.asciiprotocol.responders.LoggerResponder;
import com.uk.tsl.rfid.asciiprotocol.responders.SignalStrengthResponder;
import com.uk.tsl.rfid.asciiprotocol.responders.SwitchResponder;
import com.uk.tsl.rfid.asciiprotocol.responders.TransponderData;
import com.uk.tsl.utils.HexEncoding;
import com.uk.tsl.utils.Observable;

import java.util.ArrayList;

public class Tsl1128Module extends ReactContextBaseJavaModule implements LifecycleEventListener {

	private final ReactApplicationContext reactContext;
	// The Reader currently in use
	private static Reader mReader = null;
	private static Reader mLastUserDisconnectedReader = null;

	private static boolean mAnyTagSeen = false;

	private static boolean isSingleRead = false;

	private static ArrayList<String> cacheTags = new ArrayList<>();
	private static InventoryCommand mInventoryCommand = null;
	private static InventoryCommand mInventoryResponder = null;
	private static SwitchResponder mSwitchResponder = null;
	private static WriteTransponderCommand mWriteCommand = null;

	private static Boolean isLocateMode = false;
//	private FindTagCommand mFindTagCommand;

	// The responder to capture incoming RSSI responses
	private SignalStrengthResponder mSignalStrengthResponder;

	//Play Sound
//	private static MediaPlayer mp = null;
//	private static Thread soundThread = null;
//	private static boolean isPlaying = false;
	private static int soundRange = -1;

	private final SignalPercentageConverter mPercentageConverter = new SignalPercentageConverter();

	private final String LOG = "TSL";
	private final String READER_STATUS = "READER_STATUS";
	private final String TRIGGER_STATUS = "TRIGGER_STATUS";
	private final String WRITE_TAG_STATUS = "WRITE_TAG_STATUS";
	private final String TAG = "TAG";
	private final String LOCATE_TAG = "LOCATE_TAG";

	public Tsl1128Module(ReactApplicationContext reactContext) {
		super(reactContext);
		this.reactContext = reactContext;
		this.reactContext.addLifecycleEventListener(this);

//		mp = MediaPlayer.create(this.reactContext, R.raw.beeper);
	}

	private void sendEvent(String eventName, @Nullable WritableMap params) {
		this.reactContext
				.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
				.emit(eventName, params);
	}

	private void sendEvent(String eventName, String msg) {
		this.reactContext
				.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
				.emit(eventName, msg);
	}

	@Override
	public String getName() {
		return "Tsl1128";
	}


	@Override
	public void onHostResume() {
//		if (mReader != null) {
//			doSetEnabled(true);
//
//			// Register to receive notifications from the AsciiCommander
//			LocalBroadcastManager.getInstance(this.reactContext).registerReceiver(mCommanderMessageReceiver,
//					new IntentFilter(AsciiCommander.STATE_CHANGED_NOTIFICATION));
//
//			// Remember if the pause/resume was caused by ReaderManager - this will be cleared when ReaderManager.onResume() is called
//			boolean readerManagerDidCauseOnPause = ReaderManager.sharedInstance().didCauseOnPause();
//
//			// The ReaderManager needs to know about Activity lifecycle changes
//			ReaderManager.sharedInstance().onResume();
//
//			// The Activity may start with a reader already connected (perhaps by another App)
//			// Update the ReaderList which will add any unknown reader, firing events appropriately
//			ReaderManager.sharedInstance().updateList();
//
//			// Locate a Reader to use when necessary
//			AutoSelectReader(!readerManagerDidCauseOnPause);
//		}
	}

	@Override
	public void onHostPause() {
//		if (mReader != null) {
//			doSetEnabled(false);
//
//			// Register to receive notifications from the AsciiCommander
//			LocalBroadcastManager.getInstance(this.reactContext).unregisterReceiver(mCommanderMessageReceiver);
//
//			if (!ReaderManager.sharedInstance().didCauseOnPause() && mReader != null) {
//				mReader.disconnect();
//			}
//
//			ReaderManager.sharedInstance().onPause();
//		}
	}

	@Override
	public void onHostDestroy() {
		disconnect();
	}

	@ReactMethod
	public void isConnected(Promise promise) {
		Log.d(LOG, "isConnected");
		if (getCommander() != null) {
			promise.resolve(getCommander().isConnected());
		}

		promise.resolve(false);
	}

	@ReactMethod
	public void getDevices(Promise promise) {
		Log.d(LOG, "getDevices");
		try {
			if (getCommander() == null) {
				init();
			}

			WritableArray deviceList = Arguments.createArray();

			ArrayList<Reader> mReaders = doGetDevices();

			for (Reader reader : mReaders) {
				WritableMap map = Arguments.createMap();
				map.putString("name", reader.getDisplayName());
				map.putString("mac", reader.getDisplayInfoLine());
				deviceList.pushMap(map);
			}

			promise.resolve(deviceList);
		} catch (Exception err) {
			promise.reject(err);
		}
	}

	@ReactMethod
	public void connect(String mac, Promise promise) {
		Log.d(LOG, "connect");

		try {
			if (getCommander() != null && mReader != null) {
				disconnect();

				init();
			}

			ArrayList<Reader> mReaders = doGetDevices();
			for (Reader reader : mReaders) {
				if (reader.getDisplayInfoLine().equals(mac)) {
					mReader = reader;
					mReader.connect();
					getCommander().setReader(mReader);
				}
			}

			promise.resolve(mReader != null);
		} catch (Exception err) {
			promise.reject(err);
		}
	}

	@ReactMethod
	public void disconnect() {
		Log.d(LOG, "disconnect");

		if (mReader != null && getCommander() != null) {
			doSetEnabled(false);

			// Remove observers for changes
			ReaderManager.sharedInstance().getReaderList().readerAddedEvent().removeObserver(mAddedObserver);
			ReaderManager.sharedInstance().getReaderList().readerUpdatedEvent().removeObserver(mUpdatedObserver);
			ReaderManager.sharedInstance().getReaderList().readerRemovedEvent().removeObserver(mRemovedObserver);

			LocalBroadcastManager.getInstance(this.reactContext).unregisterReceiver(mCommanderMessageReceiver);

			mReader.disconnect();

			mReader = null;

			cacheTags = new ArrayList<>();
		}

//		promise.resolve(true);
	}

	@ReactMethod
	public void setSingleRead(boolean state) {
		Log.d(LOG, "setSingleRead");

		isSingleRead = state;
//		promise.resolve(true);
	}

	@ReactMethod
	public void clear() {
		Log.d(LOG, "clear");
		cacheTags = new ArrayList<>();
//		promise.resolve(true);
	}

	@ReactMethod
	public void getDeviceDetails(Promise promise) {
		Log.d(LOG, "getDeviceDetails");
		try {
			if (getCommander() != null && getCommander().isConnected()) {
				BatteryStatusCommand bCommand = BatteryStatusCommand.synchronousCommand();
				getCommander().executeCommand(bCommand);
				int batteryLevel = bCommand.getBatteryLevel();

				WritableMap map = Arguments.createMap();

				map.putString("name", mReader.getDisplayName());
				map.putString("mac", mReader.getDisplayInfoLine());
				map.putInt("power", batteryLevel);

				promise.resolve(map);
			}

			promise.resolve(null);
		} catch (Exception err) {
			promise.reject(err);
		}
	}

	@ReactMethod
	public void setPower(int power, Promise promise) {
		Log.d(LOG, "setPower");
		try {
			if (getCommander() != null && getCommander().isConnected()) {
				mInventoryCommand.setOutputPower(power);
				mInventoryCommand.setTakeNoAction(TriState.YES);
				getCommander().executeCommand(mInventoryCommand);
			}

			promise.resolve(true);
		} catch (Exception err) {
			promise.reject(err);
		}
	}

	@ReactMethod
	public void setEnabled(boolean state, Promise promise) {
		Log.d(LOG, "setEnabled");
		try {
			doSetEnabled(state);

			promise.resolve(true);
		} catch (Exception err) {
			promise.reject(err);
		}
	}

	@ReactMethod
	public void programTag(String oldTag, String newTag, Promise promise) {
		Log.d(LOG, "programTag");
		try {
			boolean result = false;
			String errMsg = "";
			if (getCommander() != null && getCommander().isConnected()) {
				InitProgramTag();

				byte[] data;
				data = HexEncoding.stringToBytes(newTag);
				mWriteCommand.setData(data);
				mWriteCommand.setLength(data.length / 2);
				mWriteCommand.setSelectData(oldTag);
				mWriteCommand.setSelectLength(oldTag.length() * 4);
				getCommander().executeCommand(mWriteCommand);

				if (!mWriteCommand.isSuccessful()) {
					errMsg = String.format(
							"%s failed!\nError code: %s\n",
							mWriteCommand.getClass().getSimpleName(), mWriteCommand.getErrorCode());
				} else {
					result = true;
				}
			}

			WritableMap map = Arguments.createMap();
			map.putBoolean("status", result);
			map.putString("error", errMsg);
			sendEvent(WRITE_TAG_STATUS, map);

			promise.resolve(result);
		} catch (Exception err) {
			promise.reject(err);
		}
	}

	@ReactMethod
	public void enableLocateTag(Boolean isEnable, @Nullable String mTargetTagEpc, Promise promise) {
		Log.d(LOG, "enableLocateTag");
		try {
			if (isEnable && mTargetTagEpc != null) {
				isLocateMode = true;

				InitFindTag();

				// Configure the switch actions
				SwitchActionCommand switchActionCommand = SwitchActionCommand.synchronousCommand();
				switchActionCommand.setResetParameters(TriState.YES);
				switchActionCommand.setAsynchronousReportingEnabled(TriState.YES);

				// Configure the single press switch action for the appropriate command
				switchActionCommand.setSinglePressAction(SwitchAction.FIND_TAG);
				// Lower the repeat delay to maximise the response rate
				switchActionCommand.setSinglePressRepeatDelay(10);

				getCommander().executeCommand(switchActionCommand);

				mInventoryCommand = InventoryCommand.synchronousCommand();
				mInventoryCommand.setResetParameters(TriState.YES);
				mInventoryCommand.setTakeNoAction(TriState.YES);

				mInventoryCommand.setIncludeTransponderRssi(TriState.YES);

				mInventoryCommand.setQuerySession(QuerySession.SESSION_0);
				mInventoryCommand.setQueryTarget(QueryTarget.TARGET_B);

				mInventoryCommand.setInventoryOnly(TriState.NO);

				mInventoryCommand.setSelectData(mTargetTagEpc);
				mInventoryCommand.setSelectOffset(0x20);
				mInventoryCommand.setSelectLength(mTargetTagEpc.length() * 4);
				mInventoryCommand.setSelectAction(SelectAction.DEASSERT_SET_B_NOT_ASSERT_SET_A);
				mInventoryCommand.setSelectTarget(SelectTarget.SESSION_0);

				mInventoryCommand.setUseAlert(TriState.NO);

				getCommander().executeCommand(mInventoryCommand);
				Boolean succeeded = mInventoryCommand.isSuccessful();

				Log.d("STATUS", String.valueOf(succeeded));

			} else {
				isLocateMode = false;

				// Configure the switch actions
				SwitchActionCommand switchActionCommand = SwitchActionCommand.synchronousCommand();
				switchActionCommand.setResetParameters(TriState.YES);
				switchActionCommand.setAsynchronousReportingEnabled(TriState.YES);

				// Configure the single press switch action for the appropriate command
				switchActionCommand.setSinglePressAction(SwitchAction.INVENTORY);
				// Lower the repeat delay to maximise the response rate
				switchActionCommand.setSinglePressRepeatDelay(1);

				getCommander().executeCommand(switchActionCommand);

				InitInventory();
			}

		} catch (Exception err) {
			promise.reject(err);
		}
	}

	@ReactMethod
	public void locateTag(String mTargetTagEpc, Promise promise) {
		Log.d(LOG, "locateTag");
		try {
			// Configure the switch actions
//			SwitchActionCommand switchActionCommand = SwitchActionCommand.synchronousCommand();
//			switchActionCommand.setResetParameters(TriState.YES);
//			switchActionCommand.setAsynchronousReportingEnabled(TriState.YES);
//
//			// Configure the single press switch action for the appropriate command
//			switchActionCommand.setSinglePressAction(SwitchAction.FIND_TAG);
//			// Lower the repeat delay to maximise the response rate
//			switchActionCommand.setSinglePressRepeatDelay(10);
//
//			getCommander().executeCommand(switchActionCommand);
//
//			mFindTagCommand = FindTagCommand.synchronousCommand();
//			mFindTagCommand.setResetParameters(TriState.YES);
//
//			mFindTagCommand.setSelectData(mTargetTagEpc);
//			mFindTagCommand.setSelectLength(mTargetTagEpc.length() * 4);
//			mFindTagCommand.setSelectOffset(0x20);
//
//			mFindTagCommand.setTakeNoAction(TriState.YES);
//			getCommander().executeCommand(mFindTagCommand);

		} catch (Exception err) {
			promise.reject(err);
		}
	}

	private void doDisconnect() {
		//
	}

	private void init() {
		// Ensure the shared instance of AsciiCommander exists
		AsciiCommander.createSharedInstance(this.reactContext);

		AsciiCommander commander = getCommander();

		// Ensure that all existing responders are removed
		commander.clearResponders();

		//Logger
		commander.addResponder(new LoggerResponder());

		// Add responder to enable the synchronous commands
		commander.addSynchronousResponder();

		// Configure the ReaderManager when necessary
		ReaderManager.create(this.reactContext);

		// Add observers for changes
		ReaderManager.sharedInstance().getReaderList().readerAddedEvent().addObserver(mAddedObserver);
		ReaderManager.sharedInstance().getReaderList().readerUpdatedEvent().addObserver(mUpdatedObserver);
		ReaderManager.sharedInstance().getReaderList().readerRemovedEvent().addObserver(mRemovedObserver);

		// Register to receive notifications from the AsciiCommander
		LocalBroadcastManager.getInstance(this.reactContext).registerReceiver(mCommanderMessageReceiver,
				new IntentFilter(AsciiCommander.STATE_CHANGED_NOTIFICATION));
	}

	// ReaderList Observers
	private Observable.Observer<Reader> mAddedObserver = new Observable.Observer<Reader>() {
		@Override
		public void update(Observable<? extends Reader> observable, Reader reader) {
			// Log.e("mAddedObserver", "mAddedObserver");
			// See if this newly added Reader should be used
			// AutoSelectReader(true);
		}
	};

	private Observable.Observer<Reader> mUpdatedObserver = new Observable.Observer<Reader>() {
		@Override
		public void update(Observable<? extends Reader> observable, Reader reader) {
			// Is this a change to the last actively disconnected reader
			if (reader == mLastUserDisconnectedReader) {
				// Things have changed since it was actively disconnected so
				// treat it as new
				mLastUserDisconnectedReader = null;
			}

			// Was the current Reader disconnected i.e. the connected transport went away or disconnected
			if (reader == mReader && !reader.isConnected()) {
				// No longer using this reader
				mReader = null;

				// Stop using the old Reader
				getCommander().setReader(mReader);
			} else {
				// See if this updated Reader should be used
				// e.g. the Reader's USB transport connected
				AutoSelectReader(true);
			}
		}
	};

	private Observable.Observer<Reader> mRemovedObserver = new Observable.Observer<Reader>() {
		@Override
		public void update(Observable<? extends Reader> observable, Reader reader) {
			// Is this a change to the last actively disconnected reader
			if (reader == mLastUserDisconnectedReader) {
				// Things have changed since it was actively disconnected so
				// treat it as new
				mLastUserDisconnectedReader = null;
			}

			// Was the current Reader removed
			if (reader == mReader) {
				mReader = null;

				// Stop using the old Reader
				getCommander().setReader(mReader);
			}
		}
	};

	// Handle the messages broadcast from the AsciiCommander
	//
	private BroadcastReceiver mCommanderMessageReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {

			String connectionStateMsg = intent.getStringExtra(AsciiCommander.REASON_KEY);

			if (getCommander().getConnectionState().equals(ConnectionState.CONNECTED)) {
				resetDevice();
				InitInventory();
				InitTrigger();
				SetBuzzer(false);
				updateConfiguration();

				WritableMap map = Arguments.createMap();
				map.putBoolean("status", true);
				map.putString("name", mReader.getDisplayName());
				map.putString("mac", mReader.getDisplayInfoLine());
				sendEvent(READER_STATUS, map);
			} else if (getCommander().getConnectionState().equals(ConnectionState.DISCONNECTED)) {
				WritableMap map = Arguments.createMap();
				map.putBoolean("status", false);
				sendEvent(READER_STATUS, map);
			}
		}
	};


	private void doSetEnabled(boolean state) {
		if (getCommander() != null) {
			// Update the commander for state changes
			if (state) {
				// Listener for transponders
				getCommander().addResponder(mInventoryResponder);

				//Listener for trigger state
//			getCommander().addResponder(mSwitchResponder);

				// Listener for barcodes
				// getCommander().addResponder(mBarcodeResponder);
			} else {
				// Stop listening for transponders
				getCommander().removeResponder(mInventoryResponder);

				//Listener for trigger state
//			getCommander().removeResponder(mSwitchResponder);

				// Listener for barcodes
				// getCommander().removeResponder(mBarcodeResponder);
			}
		}
	}

	private ArrayList<Reader> doGetDevices() {
		if (getCommander() == null) {
			init();
		}

		ReaderManager.sharedInstance().updateList();
		ArrayList<Reader> mReaders = ReaderManager.sharedInstance().getReaderList().list();

		return mReaders;
	}

	private void SetBuzzer(boolean value) {
		if (getCommander() != null && mReader.isConnected()) {
			AlertCommand aCommand = AlertCommand.synchronousCommand();
			aCommand.setEnableBuzzer(value ? TriState.YES : TriState.NO);
			aCommand.setEnableVibrator(value ? TriState.YES : TriState.NO);
			getCommander().executeCommand(aCommand);
		}
	}

	private void updateConfiguration() {
		if (getCommander() != null && mReader.isConnected()) {
			mInventoryCommand.setTakeNoAction(TriState.YES);
			getCommander().executeCommand(mInventoryCommand);
		}
	}

	private void AutoSelectReader(boolean attemptReconnect) {
		ObservableReaderList readerList = ReaderManager.sharedInstance().getReaderList();
		Reader usbReader = null;
		if (readerList.list().size() >= 1) {
			// Currently only support a single USB connected device so we can safely take the
			// first CONNECTED reader if there is one
			for (Reader reader : readerList.list()) {
				if (reader.hasTransportOfType(TransportType.USB)) {
					usbReader = reader;
					break;
				}
			}
		}

		if (mReader == null) {
			if (usbReader != null && usbReader != mLastUserDisconnectedReader) {
				// Use the Reader found, if any
				mReader = usbReader;
				getCommander().setReader(mReader);
			}
		} else {
			// If already connected to a Reader by anything other than USB then
			// switch to the USB Reader
			IAsciiTransport activeTransport = mReader.getActiveTransport();
			if (activeTransport != null && activeTransport.type() != TransportType.USB && usbReader != null) {
				mReader.disconnect();

				mReader = usbReader;

				// Use the Reader found, if any
				getCommander().setReader(mReader);
			}
		}

		// Reconnect to the chosen Reader
		if (mReader != null
				&& !mReader.isConnecting()
				&& (mReader.getActiveTransport() == null || mReader.getActiveTransport().connectionStatus().value() == ConnectionState.DISCONNECTED)) {
			// Attempt to reconnect on the last used transport unless the ReaderManager is cause of OnPause (USB device connecting)
			if (attemptReconnect) {
				if (mReader.allowMultipleTransports() || mReader.getLastTransportType() == null) {
					// Reader allows multiple transports or has not yet been connected so connect to it over any available transport
					mReader.connect();
				} else {
					// Reader supports only a single active transport so connect to it over the transport that was last in use
					mReader.connect(mReader.getLastTransportType());
				}
			}
		}
	}

	private void InitTrigger() {
		//Trigger
		mSwitchResponder = new SwitchResponder();
		mSwitchResponder.setSwitchStateReceivedDelegate(mSwitchDelegate);
		getCommander().addResponder(mSwitchResponder);

		// Configure the switch actions
		SwitchActionCommand switchActionCommand = SwitchActionCommand.synchronousCommand();
		switchActionCommand.setResetParameters(TriState.YES);
		// Enable asynchronous switch state reporting
		switchActionCommand.setAsynchronousReportingEnabled(TriState.YES);

		getCommander().executeCommand(switchActionCommand);
	}

	private void InitProgramTag() {
		mWriteCommand = WriteTransponderCommand.synchronousCommand();

		mWriteCommand.setResetParameters(TriState.YES);

		mWriteCommand.setSelectOffset(0x20);
		mWriteCommand.setBank(Databank.ELECTRONIC_PRODUCT_CODE);
		mWriteCommand.setOffset(2);

		mWriteCommand.setSelectAction(SelectAction.DEASSERT_SET_B_NOT_ASSERT_SET_A);
		mWriteCommand.setSelectTarget(SelectTarget.SESSION_2);

		mWriteCommand.setQuerySelect(QuerySelect.ALL);
		mWriteCommand.setQuerySession(QuerySession.SESSION_2);
		mWriteCommand.setQueryTarget(QueryTarget.TARGET_B);

		mWriteCommand.setTransponderReceivedDelegate(mProgramTagDelegate);
	}

	private void InitInventory() {
		// Initiate tags array for saving scanned tags, and prevent duplicate tags.
		cacheTags = new ArrayList<>();

		// This is the command that will be used to perform configuration changes and
		// inventories
		mInventoryCommand = new InventoryCommand();
		mInventoryCommand.setResetParameters(TriState.YES);

		// Configure the type of inventory
		mInventoryCommand.setIncludeTransponderRssi(TriState.YES);
		mInventoryCommand.setIncludeChecksum(TriState.YES);
		mInventoryCommand.setIncludePC(TriState.YES);
		mInventoryCommand.setIncludeDateTime(TriState.YES);

		// Use an InventoryCommand as a responder to capture all incoming inventory
		// responses
		mInventoryResponder = new InventoryCommand();
		// Also capture the responses that were not from App commands
		mInventoryResponder.setCaptureNonLibraryResponses(true);

		// Notify when each transponder is seen
		mInventoryResponder.setTransponderReceivedDelegate(mInventoryDelegate);

		mInventoryResponder.setResponseLifecycleDelegate(new ICommandResponseLifecycleDelegate() {
			@Override
			public void responseEnded() {
				if (!mAnyTagSeen && mInventoryCommand.getTakeNoAction() != TriState.YES) {
					Log.i("No transponders seen", "No transponders seen");
					if (isLocateMode) {
						soundRange = -1;
						WritableMap map = Arguments.createMap();
						map.putInt("distance", 0);
						sendEvent(LOCATE_TAG, map);
					}
				}
				mInventoryCommand.setTakeNoAction(TriState.NO);
			}

			@Override
			public void responseBegan() {
				mAnyTagSeen = false;
			}
		});
	}

	private void InitFindTag() {
//		mFindTagCommand = FindTagCommand.synchronousCommand();
		mSignalStrengthResponder = new SignalStrengthResponder();

		mSignalStrengthResponder.setRawSignalStrengthReceivedDelegate(mFindTagDelegate);
		mSignalStrengthResponder.setPercentageSignalStrengthReceivedDelegate(mFindTagPercentageDelegate);

		getCommander().addResponder(mSignalStrengthResponder);
//		mFindTagCommand.setResetParameters(TriState.YES);
//		mFindTagCommand.setTakeNoAction(TriState.YES);
//		getCommander().executeCommand(mFindTagCommand);
	}

	//Inventory Delegate Handler
	private final ITransponderReceivedDelegate mInventoryDelegate =
			new ITransponderReceivedDelegate() {
				int mTagsSeen = 0;

				@Override
				public void transponderReceived(TransponderData transponder, boolean moreAvailable) {
					mTagsSeen++;

					//Inventory received tags
					mAnyTagSeen = true;
					String EPC = transponder.getEpc();
					int rssi = transponder.getRssi();

					if (isSingleRead) {
						if (rssi > -60) {
							sendEvent(TAG, EPC);
						}
					} else {
						if (addTagToList(EPC)) {
							sendEvent(TAG, EPC);
						}
					}
				}
			};

	//Trigger Handler
	private final ISwitchStateReceivedDelegate mSwitchDelegate = new ISwitchStateReceivedDelegate() {
		@Override
		public void switchStateReceived(SwitchState state) {
			// Use the alert command to indicate the type of asynchronous switch press
			// No vibration just vary the tone & duration
			if (SwitchState.OFF.equals(state)) {
				if (isLocateMode) {
					if (mSignalStrengthResponder.getRawSignalStrengthReceivedDelegate() != null) {
						mSignalStrengthResponder.getRawSignalStrengthReceivedDelegate().signalStrengthReceived(0);
					}
					if (mSignalStrengthResponder.getPercentageSignalStrengthReceivedDelegate() != null) {
						mSignalStrengthResponder.getPercentageSignalStrengthReceivedDelegate().signalStrengthReceived(0);
					}

					WritableMap map = Arguments.createMap();
					map.putInt("distance", 0);
					sendEvent(LOCATE_TAG, map);
				} else {
					WritableMap map = Arguments.createMap();
					map.putBoolean("status", false);
					sendEvent(TRIGGER_STATUS, map);
				}

			} else {
				WritableMap map = Arguments.createMap();
				map.putBoolean("status", true);
				sendEvent(TRIGGER_STATUS, map);
			}
		}
	};

	//Program tag Delegate Handler
	private final ITransponderReceivedDelegate mProgramTagDelegate =
			new ITransponderReceivedDelegate() {
				@Override
				public void transponderReceived(TransponderData transponderData, boolean b) {
					String eaMsg = transponderData.getAccessErrorCode() == null ? "" : transponderData.getAccessErrorCode().getDescription() + " (EA)";
					String ebMsg = transponderData.getBackscatterErrorCode() == null ? "" : transponderData.getBackscatterErrorCode().getDescription() + " (EB)";
					String errorMsg = eaMsg + ebMsg;
					if (errorMsg.length() > 0) {
//						WritableMap map = Arguments.createMap();
//						map.putBoolean("status", false);
//						map.putString("error", errorMsg);
//						sendEvent(WRITE_TAG_STATUS, map);
					} else {
//						WritableMap map = Arguments.createMap();
//						map.putBoolean("status", true);
//						map.putString("error", null);
//						sendEvent(WRITE_TAG_STATUS, map);
					}
				}
			};

	//Find Tag Delegate Handler
	private final ISignalStrengthReceivedDelegate mFindTagDelegate =
			new ISignalStrengthReceivedDelegate() {
				@Override
				public void signalStrengthReceived(Integer level) {
					int levelNum = mPercentageConverter.asPercentage(level);

//					PlaySound(levelNum);
					Log.d("distance", String.valueOf(levelNum));

					WritableMap map = Arguments.createMap();
					map.putInt("distance", levelNum);
					sendEvent(LOCATE_TAG, map);
				}
			};

	private final ISignalStrengthReceivedDelegate mFindTagPercentageDelegate =
			new ISignalStrengthReceivedDelegate() {
				@Override
				public void signalStrengthReceived(Integer level) {
					int percentage = mPercentageConverter.asPercentage(level);
//					WritableMap map = Arguments.createMap();
//					map.putInt("distance", percentage);
//					sendEvent(LOCATE_TAG, map);
				}
			};

	private void resetDevice() {
		if (getCommander().isConnected()) {
			FactoryDefaultsCommand fdCommand = new FactoryDefaultsCommand();
			fdCommand.setResetParameters(TriState.YES);
			getCommander().executeCommand(fdCommand);
		}
	}

	private boolean addTagToList(String strEPC) {
		if (strEPC != null) {
			if (!checkIsExisted(strEPC)) {
				cacheTags.add(strEPC);
				return true;
			}
		}
		return false;
	}

	private boolean checkIsExisted(String strEPC) {
		for (int i = 0; i < cacheTags.size(); i++) {
			String tag = cacheTags.get(i);
			if (strEPC != null && strEPC.equals(tag)) {
				return true;
			}
		}
		return false;
	}

	private AsciiCommander getCommander() {
		return AsciiCommander.sharedInstance();
	}

//	private void PlaySound(long value) {
//		if (value > 0 && value <= 30) {
//			soundRange = 1000;
//		} else if (value > 31 && value <= 75) {
//			soundRange = 600;
//		} else {
//			soundRange = 100;
//		}
//
//		if (soundThread == null) {
//			soundThread = new Thread(new Runnable() {
//				@Override
//				public void run() {
//					while (isPlaying) {
//						if (soundRange > 0) {
//							Log.e("LOOP", soundRange + "");
//							try {
//								Thread.sleep(soundRange);
//							} catch (InterruptedException e) {
//								e.getMessage();
//							}
//							mp.start();
//						}
//
//					}
//				}
//			});
//			soundThread.start();
//		}
//	}
}
