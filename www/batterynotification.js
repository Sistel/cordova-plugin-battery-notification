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
 * This class contains information about the current battery status.
 * @constructor
 */
var cordova = require('cordova');
var exec = require('cordova/exec');

var Battery = function () {
    this._level = null;
    this._isPlugged = null;
    // Create new event handlers on the window (returns a channel instance)
    this.channels = {
        batterystatus: cordova.addWindowEventHandler('batterystatus')
    };
    for (var key in this.channels) {
        this.channels[key].onHasSubscribersChange = Battery.onHasSubscribersChange;
    }
};

function handlers() {
    return (
        battery.channels.batterystatus.numHandlers
    );
}

Battery.prototype._ensureBoolean = function (callback) {
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
Battery.prototype.isCharging = function (successCallback, errorCallback) {
    return exec(battery._ensureBoolean(successCallback),
        errorCallback,
        'Battery',
        'isCharging',
        []);
};

Battery.prototype.getDataBatteryInfo = function (successCallback, errorCallback) {
    return exec(successCallback,
        errorCallback,
        'Battery',
        'getDataBatteryInfo',
        []);
};

Battery.prototype.startService = function (successCallback, errorCallback) {
    return exec(successCallback,
        errorCallback,
        'Battery',
        'startService',
        []);
};

Battery.prototype.stopService = function (successCallback, errorCallback) {
    return exec(successCallback,
        errorCallback,
        'Battery',
        'stopService',
        []);
};

/**
 * Event handlers for when callbacks get registered for the battery.
 * Keep track of how many handlers we have so we can start and stop the native battery listener
 * appropriately.
 */
Battery.onHasSubscribersChange = function () {
    // If we just registered the first handler, make sure native listener is started.
    if (this.numHandlers === 1 && handlers() === 1) {
        exec(battery._status, battery._error, 'Battery', 'start', []);
    } else if (handlers() === 0) {
        exec(null, null, 'Battery', 'stop', []);
    }
};

/**
 * Callback for battery status
 *
 * @param {Object} info            keys: level, isPlugged
 */
Battery.prototype._status = function (info) {
    if (info) {
        if (battery._level !== info.level || battery._isPlugged !== info.isPlugged) {
            if (info.level === null && battery._level !== null) {
                return; // special case where callback is called because we stopped listening to the native side.
            }

            // Something changed. Fire batterystatus event
            cordova.fireWindowEvent('batterystatus', info);
            
            battery._level = info.level;
            battery._isPlugged = info.isPlugged;
        }
    }
};

/**
 * Error callback for battery start
 */
Battery.prototype._error = function (e) {
    console.log('Error initializing Battery Notification: ' + e);
};

var battery = new Battery();

module.exports = battery;
