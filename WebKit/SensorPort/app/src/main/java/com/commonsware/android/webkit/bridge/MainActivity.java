/***
  Copyright (c) 2013-2017 CommonsWare, LLC
  Licensed under the Apache License, Version 2.0 (the "License"); you may not
  use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0. Unless required
  by applicable law or agreed to in writing, software distributed under the
  License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
  OF ANY KIND, either express or implied. See the License for the specific
  language governing permissions and limitations under the License.
  
  Covered in detail in the book _The Busy Coder's Guide to Android Development_
    https://commonsware.com/Android
 */

package com.commonsware.android.webkit.bridge;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebMessage;
import android.webkit.WebMessagePort;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import java.util.Locale;

public class MainActivity extends Activity implements SensorEventListener {
  private static final String URL="file:///android_asset/index.html";
  private SensorManager mgr;
  private Sensor light;
  private WebView wv;
  private final JSInterface jsInterface=new JSInterface();
  private WebMessagePort port;

  @SuppressLint({"AddJavascriptInterface", "SetJavaScriptEnabled"})
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);

    mgr=(SensorManager)getSystemService(Context.SENSOR_SERVICE);
    light=mgr.getDefaultSensor(Sensor.TYPE_LIGHT);

    wv=(WebView)findViewById(R.id.webkit);
    wv.getSettings().setJavaScriptEnabled(true);
    wv.addJavascriptInterface(jsInterface, "LIGHT_SENSOR");

    if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.M) {
      wv.setWebViewClient(new WebViewClient() {
        @Override
        public void onPageFinished(WebView view, String url) {
          initPort();
        }
      });
    }

    wv.loadUrl(URL);
  }

  @Override
  protected void onStart() {
    super.onStart();

    mgr.registerListener(this, light, SensorManager.SENSOR_DELAY_UI);
  }

  @Override
  protected void onStop() {
    mgr.unregisterListener(this);

    super.onStop();
  }

  @Override
  public void onSensorChanged(SensorEvent sensorEvent) {
    float lux=sensorEvent.values[0];

    jsInterface.updateLux(lux);

    String js=String.format(Locale.US, "update_lux(%f)", lux);

    if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.M) {
      // postLux();
    }
    else if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.KITKAT) {
      wv.evaluateJavascript(js, null);
    }
    else {
      wv.loadUrl("javascript:"+js);
    }
  }

  @Override
  public void onAccuracyChanged(Sensor sensor, int i) {
    // unused
  }

  @TargetApi(Build.VERSION_CODES.M)
  private void initPort() {
    final WebMessagePort[] channel=wv.createWebMessageChannel();

    port=channel[0];
    port.setWebMessageCallback(new WebMessagePort.WebMessageCallback() {
      @Override
      public void onMessage(WebMessagePort port, WebMessage message) {
        postLux();
      }
    });

    wv.postWebMessage(new WebMessage("", new WebMessagePort[]{channel[1]}),
          Uri.EMPTY);
  }

  @TargetApi(Build.VERSION_CODES.M)
  private void postLux() {
    port.postMessage(new WebMessage(jsInterface.getLux()));
  }

  private static class JSInterface {
    float lux=0.0f;

    private void updateLux(float lux) {
      this.lux=lux;
    }

    @JavascriptInterface
    public String getLux() {
      return(String.format(Locale.US, "{\"lux\": %f}", lux));
    }
  }
}