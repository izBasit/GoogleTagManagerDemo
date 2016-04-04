/*
 * Copyright 2016 Basit Parkar.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 *  @date 4/4/16 9:57 PM
 *  @modified 2/6/16 9:19 AM
 */

package me.iz.mobility.googletagmanagerdemo;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.tagmanager.Container;
import com.google.android.gms.tagmanager.Container.FunctionCallMacroCallback;
import com.google.android.gms.tagmanager.Container.FunctionCallTagCallback;
import com.google.android.gms.tagmanager.ContainerHolder;
import com.google.android.gms.tagmanager.TagManager;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Displays simple splash screen while GTM container is loading. Once the container is loaded,
 * launches the {@link MainActivity}.
 */
public class SplashScreenActivity extends Activity {
    private static final long TIMEOUT_FOR_CONTAINER_OPEN_MILLISECONDS = 2000;
    private static final String CONTAINER_ID = "GTM-TVNB99";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splashscreen);

        TagManager tagManager = TagManager.getInstance(this);

        // Modify the log level of the logger to print out not only
        // warning and error messages, but also verbose, debug, info messages.
        tagManager.setVerboseLoggingEnabled(true);

        PendingResult<ContainerHolder> pending =
                tagManager.loadContainerPreferNonDefault(CONTAINER_ID,
                R.raw.gtm_tvnb99);

        // The onResult method will be called as soon as one of the following happens:
        //     1. a saved container is loaded
        //     2. if there is no saved container, a network container is loaded
        //     3. the 2-second timeout occurs
        pending.setResultCallback(new ResultCallback<ContainerHolder>() {
            @Override
            public void onResult(ContainerHolder containerHolder) {
                ContainerHolderSingleton.setContainerHolder(containerHolder);
                Container container = containerHolder.getContainer();
                if (!containerHolder.getStatus().isSuccess()) {
                    Log.e("CuteAnimals", "failure loading container");
                    displayErrorToUser(R.string.load_error);
                    return;
                }
                ContainerHolderSingleton.setContainerHolder(containerHolder);
                ContainerLoadedCallback.registerCallbacksForContainer(container);
                containerHolder.setContainerAvailableListener(new ContainerLoadedCallback());
                startMainActivity();
            }
        }, TIMEOUT_FOR_CONTAINER_OPEN_MILLISECONDS, TimeUnit.MILLISECONDS);
    }

    /**
     * Looks up the externalized string resource and displays it in a pop-up dialog box.
     *
     * @param stringKey
     */
    private void displayErrorToUser(int stringKey) {
        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle("Error");
        alertDialog.setMessage(getResources().getString(stringKey));
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE,
                "OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
        });
        alertDialog.show();
    }

    private void startMainActivity() {
        Intent intent = new Intent(SplashScreenActivity.this, MainActivity.class);
        startActivity(intent);
    }

    private static class ContainerLoadedCallback implements ContainerHolder.ContainerAvailableListener {
        @Override
        public void onContainerAvailable(ContainerHolder containerHolder, String containerVersion) {
            // We load each container when it becomes available.
            Container container = containerHolder.getContainer();
            registerCallbacksForContainer(container);
        }

        public static void registerCallbacksForContainer(Container container) {
            // Register two custom function call macros to the container.
            container.registerFunctionCallMacroCallback("increment", new CustomMacroCallback());
            container.registerFunctionCallMacroCallback("mod", new CustomMacroCallback());
            // Register a custom function call tag to the container.
            container.registerFunctionCallTagCallback("custom_tag", new CustomTagCallback());
        }
    }

    private static class CustomMacroCallback implements FunctionCallMacroCallback {
        private int numCalls;

        @Override
        public Object getValue(String name, Map<String, Object> parameters) {
            if ("increment".equals(name)) {
                return ++numCalls;
            } else if ("mod".equals(name)) {
                return (Long) parameters.get("key1") % Integer.valueOf((String) parameters.get("key2"));
            } else {
                throw new IllegalArgumentException("Custom macro name: " + name + " is not supported.");
            }
        }
    }

    private static class CustomTagCallback implements FunctionCallTagCallback {
        @Override
        public void execute(String tagName, Map<String, Object> parameters) {
            // The code for firing this custom tag.
            Log.i("CuteAnimals", "Custom function call tag :" + tagName + " is fired.");
        }
    }
}
