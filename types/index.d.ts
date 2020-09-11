// Type definitions for www/batterynotification.js
// Project: [LIBRARY_URL_HERE] 
// Definitions by: [YOUR_NAME_HERE] <[YOUR_URL_HERE]> 
// Definitions: https://github.com/borisyankov/DefinitelyTyped
declare namespace BatteryNotification.prototype{
	// BatteryNotification.prototype._ensureBoolean.!ret
	type _ensureBooleanRet = ((result : any) => void);
}

/**
 * 
 */
declare interface BatteryNotification {
		
	/**
	 * 
	 */
	new ();
		
	/**
	 * 
	 * @param callback 
	 * @return  
	 */
	_ensureBoolean(callback : any): /* BatteryNotification.prototype._ensureBooleanRet */ any;
		
	/**
	 * Checks if the device is charging.
	 * Returns true if data roaming is enabled.
	 * 
	 * @param {Function} successCallback -  The callback which will be called when the operation is successful.
	 * This callback function is passed a single boolean parameter which is TRUE if device is charging.
	 * @param {Function} errorCallback -  The callback which will be called when the operation encounters an error.
	 *  This callback function is passed a single string parameter containing the error message.
	 * @param successCallback 
	 * @param errorCallback 
	 */
	isCharging(successCallback : Function, errorCallback : Function): void;
		
	/**
	 * 
	 * @param successCallback 
	 * @param errorCallback 
	 */
	getDataBatteryInfo(successCallback : any, errorCallback : any): void;
		
	/**
	 * 
	 * @param successCallback 
	 * @param errorCallback 
	 */
	startService(successCallback : any, errorCallback : any): void;
		
	/**
	 * 
	 * @param successCallback 
	 * @param errorCallback 
	 */
	stopService(successCallback : any, errorCallback : any): void;
		
	/**
	 * Callback for battery status
	 * 
	 * @param {Object} info            keys: level, isPlugged
	 * @param info 
	 */
	_status(info : any): void;
		
	/**
	 * Error callback for battery notification start
	 * @param e 
	 */
	_error(e : any): void;
		
	/**
	 * Event handlers for when callbacks get registered for the battery.
	 * Keep track of how many handlers we have so we can start and stop the native battery listener
	 * appropriately.
	 */
	onHasSubscribersChange(): void;
	
	/**
	 * Create new event handlers on the window (returns a channel instance)
	 */
	channels : {
	}
}

/**
 * 
 */
declare function handlers(): void;

/**
 * 
 */
export declare var batterynotification : BatteryNotification;
