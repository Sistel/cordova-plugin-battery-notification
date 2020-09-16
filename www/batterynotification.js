/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */

/**
 * This class contains information about the current battery notification.
 * @constructor
 */
var cordova = require('cordova');
var exec = require('cordova/exec');

var BatteryNotification = function () {
    this._level = null;
    this._isPlugged = null;
    // Create new event handlers on the window (returns a channel instance)
    this.channels = {
        batterystatus: cordova.addWindowEventHandler('batterystatus')
    };
    for (var key in this.channels) {
        this.channels[key].onHasSubscribersChange = BatteryNotification.onHasSubscribersChange;
    }
};

function handlers() {
    return (
        batterynotification.channels.batterystatus.numHandlers
    );
}

BatteryNotification.prototype._ensureBoolean = function (callback) {
    return function (result) {
        callback(!!result);
    }
};

/**
     * Checks if the device is charging.
     * Returns true if data roaming is enabled.
     *
     * @param {Function} successCallback -  The callback which will be called when the operation is successful.
     * This callback function is passed a single boolean parameter which is TRUE if device is charging.
     * @param {Function} errorCallback -  The callback which will be called when the operation encounters an error.
     *  This callback function is passed a single string parameter containing the error message.
     */
BatteryNotification.prototype.isCharging = function (successCallback, errorCallback) {
    return exec(BatteryNotification._ensureBoolean(successCallback),
        errorCallback,
        'BatteryNotification',
        'isCharging',
        []);
};

BatteryNotification.prototype.getDataBatteryInfo = function (successCallback, errorCallback) {
    return exec(successCallback,
        errorCallback,
        'BatteryNotification',
        'getDataBatteryInfo',
        []);
};

BatteryNotification.prototype.startService = function (minLevel, message, successCallback, errorCallback) {
    return exec(successCallback,
        errorCallback,
        'BatteryNotification',
        'startService',
        [minLevel, message]);
};

BatteryNotification.prototype.stopService = function (successCallback, errorCallback) {
    return exec(successCallback,
        errorCallback,
        'BatteryNotification',
        'stopService',
        []);
};

/**
 * Event handlers for when callbacks get registered for the battery.
 * Keep track of how many handlers we have so we can start and stop the native battery listener
 * appropriately.
 */
BatteryNotification.onHasSubscribersChange = function () {
    // If we just registered the first handler, make sure native listener is started.
    if (this.numHandlers === 1 && handlers() === 1) {
        exec(batterynotification._status, batterynotification._error, 'BatteryNotification', 'start', []);
    } else if (handlers() === 0) {
        exec(null, null, 'BatteryNotification', 'stop', []);
    }
};

/**
 * Callback for battery status
 *
 * @param {Object} info            keys: level, isPlugged
 */
BatteryNotification.prototype._status = function (info) {
    if (info) {
        if (batterynotification._level !== info.level || batterynotification._isPlugged !== info.isPlugged) {
            if (info.level === null && batterynotification._level !== null) {
                return; // special case where callback is called because we stopped listening to the native side.
            }

            // Something changed. Fire batterystatus event
            cordova.fireWindowEvent('batterystatus', info);

            batterynotification._level = info.level;
            batterynotification._isPlugged = info.isPlugged;
        }
    }
};

/**
 * Error callback for battery notification start
 */
BatteryNotification.prototype._error = function (e) {
    console.log('Error initializing Battery Notification: ' + e);
};

var batterynotification = new BatteryNotification();

module.exports = batterynotification;
