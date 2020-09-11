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

/* global Windows, WinJS, cordova */
exports.defineAutoTests = function () {
    var hasPowerManagerAPI =
        cordova.platformId === 'windows' && Windows && Windows.System && Windows.System.Power && Windows.System.Power.PowerManager;

    var batteryStatusUnsupported =
        (cordova.platformId === 'windows8' ||
            // We don't test battery status on Windows when there is no corresponding APIs available
            cordova.platformId === 'windows') &&
        !(hasPowerManagerAPI || WinJS.Utilities.isPhone);

    var onEvent;

    describe('Battery (navigator.battery)', function () {
        it('battery.spec.1 should exist', function () {
            if (batteryStatusUnsupported) {
                pending('Battery notification is not supported on windows store');
            }

            expect(navigator.battery).toBeDefined();
        });
    });

    
};

//* *****************************************************************************************
//* **************************************Manual Tests***************************************
//* *****************************************************************************************

exports.defineManualTests = function (contentEl, createActionButton) {
};
