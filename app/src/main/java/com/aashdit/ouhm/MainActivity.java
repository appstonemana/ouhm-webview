package com.aashdit.ouhm;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.DownloadListener;
import android.webkit.GeolocationPermissions;
import android.webkit.SslErrorHandler;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    public static final int REQUEST_SELECT_FILE = 100;
    private static final String TAG = "MainActivity";
    private final static int FCR = 1;
    public static boolean flag = false;
    final private int REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS = 124;
    private final int PAGE_STARTED = 0x1;
    private final int PAGE_REDIRECTED = 0x2;
    public ValueCallback<Uri[]> uploadMessage;
    WebView webView;
    String baseUrl = "https://104.131.49.240:8443/ouhm/";
    private int webViewPreviousState;
    private Handler handler;
    private ValueCallback<Uri> mUploadMessage;
    private ValueCallback<Uri> mUM;
    private ValueCallback<Uri[]> mUMA;
    private String mCM;
//    String baseUrl = "http://209.97.136.18:8080/ouhm/";
    //        String baseUrl = "http://192.168.43.177:3030/ouhm/";
//    String baseUrl = "http://192.168.43.235:8050/ouhm/";
    private RelativeLayout rootView;
    private SwipeRefreshLayout swipeRefreshLayout;

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (Build.VERSION.SDK_INT >= 21) {
            Uri[] results = null;
            //Check if response is positive
            if (resultCode == Activity.RESULT_OK) {
                if (requestCode == FCR) {
                    if (null == mUMA) {
                        return;
                    }
                    if (intent == null) {
                        //Capture Photo if no image available
                        if (mCM != null) {
                            results = new Uri[]{Uri.parse(mCM)};
                        }
                    } else {
                        String dataString = intent.getDataString();
                        if (dataString != null) {
                            results = new Uri[]{Uri.parse(dataString)};
                        }
                    }
                }
            }
            mUMA.onReceiveValue(results);
            mUMA = null;
        } else {
            if (requestCode == FCR) {
                if (null == mUM) return;
                Uri result = intent == null || resultCode != RESULT_OK ? null : intent.getData();
                mUM.onReceiveValue(result);
                mUM = null;
            }
        }
        if (requestCode == REQUEST_SELECT_FILE) {

            if (null == mUploadMessage) return;

            Uri result = intent == null || resultCode != RESULT_OK ? null :
                    intent.getData();
            mUploadMessage.onReceiveValue(result);
            mUploadMessage = null;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webview);
        rootView = findViewById(R.id.rootView);
        swipeRefreshLayout = findViewById(R.id.refresh);

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
//                webView.loadUrl(baseUrl);

                webView.loadUrl("javascript:window.location.reload( true )");
                swipeRefreshLayout.setRefreshing(false);
            }
        });

        if (Build.VERSION.SDK_INT >= 23) {
            // Marshmallow+ Permission APIs
            askRuntimePermission();
        }
        if (0 != (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE)) {
            WebView.setWebContentsDebuggingEnabled(true);
        }

        webView.setInitialScale(1);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setUseWideViewPort(true);
        webView.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
        webView.setScrollbarFadingEnabled(false);

        webView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
//        webView.getSettings().setBuiltInZoomControls(true);
        webView.setWebViewClient(new GeoWebViewClient());
        // Below required for geolocation
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setGeolocationEnabled(true);
        webView.getSettings().setSupportMultipleWindows(true);
        webView.getSettings().setAllowFileAccess(true);
        webView.getSettings().setAllowContentAccess(true);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setUseWideViewPort(true);
        webView.getSettings().setPluginState(WebSettings.PluginState.ON);
        webView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        webView.setDrawingCacheEnabled(false);
        webView.setWebChromeClient(new GeoWebChromeClient());
        webView.getSettings().setAppCacheEnabled(true);
        webView.getSettings().setDatabaseEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);

        webView.getSettings().setGeolocationDatabasePath(getFilesDir().getPath());
        webView.loadUrl(baseUrl);
//        webView.setDownloadListener(new DownloadListener() {
//            @Override
//            public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {
//                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
//                request.setTitle(URLUtil.guessFileName(url, contentDisposition, mimetype));
//                request.setDescription("Downloading file...");
//                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
//                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, URLUtil.guessFileName(url, contentDisposition, mimetype));
//                DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
//                dm.enqueue(request);
//                Toast.makeText(getApplicationContext(), "Downloading...", Toast.LENGTH_SHORT).show();
//                registerReceiver(onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
//            }
//            BroadcastReceiver onComplete = new BroadcastReceiver() {
//                @Override
//                public void onReceive(Context context, Intent intent) {
//                    Toast.makeText(getApplicationContext(), "Downloading Complete", Toast.LENGTH_SHORT).show();
//                }
//            };
//        });
        webView.setDownloadListener(new DownloadListener() {
            public void onDownloadStart(String url, String userAgent,
                                        String contentDisposition, String mimeType,
                                        long contentLength) {


//                String fileName,cookie;
//                try {
//                    fileName = contentDisposition.replace("inline; filename=", "");
//                    fileName = fileName.replaceAll("\"", "");
//                    downloadFileAsync(url, fileName);
//                }catch (Exception e){
//
//                }


                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                startActivity(i);


//                DownloadManager.Request request = new DownloadManager.Request(
//                        Uri.parse(url));
//                request.setMimeType(mimeType);
//                String cookies = CookieManager.getInstance().getCookie(url);
//                request.addRequestHeader("cookie", cookies);
//                request.addRequestHeader("User-Agent", userAgent);
//                request.addRequestHeader("mimetype", mimeType);
//                request.setDescription("Downloading file...");
//                request.setTitle(URLUtil.guessFileName(url, contentDisposition,
//                        mimeType));
//                request.allowScanningByMediaScanner();
//                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
//                request.setDestinationInExternalPublicDir(
//                        Environment.DIRECTORY_DOWNLOADS, URLUtil.guessFileName(
//                                url, contentDisposition, mimeType));
//                DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
//                dm.enqueue(request);
//                Toast.makeText(getApplicationContext(), "Downloading File",
//                        Toast.LENGTH_LONG).show();

                //workaround
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//                    Intent i = new Intent(Intent.ACTION_VIEW);
//                    i.setData(Uri.parse(url));
//                    startActivity(i);
//                }

            }
        });


//        webView.setDownloadListener(new DownloadListener() {
//
//            @Override
//            public void onDownloadStart(String url, String userAgent,
//                                        String contentDisposition, String mimetype,
//                                        long contentLength) {
//                DownloadManager.Request request = new DownloadManager.Request(
//                        Uri.parse(url));
//
//                request.allowScanningByMediaScanner();
//                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED); //Notify client once download is completed!
//                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "file");
//                DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
//                dm.enqueue(request);
//                Toast.makeText(getApplicationContext(), "Downloading File", //To notify the Client that the file is being downloaded
//                        Toast.LENGTH_LONG).show();
//
//            }
//        });

//        webView.setDownloadListener(new DownloadListener() {
//            public void onDownloadStart(String url, String userAgent,
//                                        String contentDisposition, String mimetype,
//                                        long contentLength) {
//                DownloadManager.Request request = new DownloadManager.Request(
//                        Uri.parse(url));
//                request.allowScanningByMediaScanner();
//                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
//                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "ouhm");
//                DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
//                dm.enqueue(request);
//
//            }
//        });

    }

    /**
     * Check if there is any connectivity
     *
     * @return is Device Connected
     */
    public boolean isConnected() {

        ConnectivityManager cm = (ConnectivityManager)
                this.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (null != cm) {
            NetworkInfo info = cm.getActiveNetworkInfo();
            return (info != null && info.isConnected());
        }

        return false;

    }

    // Create an image file
    private File createImageFile() throws IOException {
        @SuppressLint("SimpleDateFormat") String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "img_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS: {
                Map<String, Integer> perms = new HashMap<String, Integer>();
                // Initial
                perms.put(Manifest.permission.ACCESS_FINE_LOCATION, PackageManager.PERMISSION_GRANTED);
                perms.put(Manifest.permission.READ_EXTERNAL_STORAGE, PackageManager.PERMISSION_GRANTED);
                perms.put(Manifest.permission.WRITE_EXTERNAL_STORAGE, PackageManager.PERMISSION_GRANTED);
                perms.put(Manifest.permission.CAMERA, PackageManager.PERMISSION_GRANTED);


                // Fill with results
                for (int i = 0; i < permissions.length; i++)
                    perms.put(permissions[i], grantResults[i]);

                // Check for ACCESS_FINE_LOCATION
                if (perms.get(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED


                ) {
                    // All Permissions Granted

                    // Permission Denied
                    Toast.makeText(MainActivity.this, "All Permission GRANTED !! Thank You :)", Toast.LENGTH_SHORT)
                            .show();

                } else {
                    // Permission Denied
                    Toast.makeText(MainActivity.this, "One or More Permissions are DENIED Exiting App :(", Toast.LENGTH_SHORT)
                            .show();

                    finish();
                }
            }
            break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK) && this.webView.canGoBack()) {
            this.webView.goBack();
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void askRuntimePermission() {
        List<String> permissionsNeeded = new ArrayList<String>();

        final List<String> permissionsList = new ArrayList<String>();
        if (!addPermission(permissionsList, Manifest.permission.ACCESS_FINE_LOCATION)) {
            permissionsNeeded.add("Show Location");
        }
        if (!addPermission(permissionsList, Manifest.permission.READ_EXTERNAL_STORAGE)) {
            permissionsNeeded.add("Show Read External Storage");
        }
        if (!addPermission(permissionsList, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            permissionsNeeded.add("Show Write External Storage");
        }
        if (!addPermission(permissionsList, Manifest.permission.CAMERA)) {
            permissionsNeeded.add("Show Camera");
        }

        if (permissionsList.size() > 0) {
            if (permissionsNeeded.size() > 0) {

                // Need Rationale
                String message = "App need access to " + permissionsNeeded.get(0);

                for (int i = 1; i < permissionsNeeded.size(); i++)
                    message = message + ", " + permissionsNeeded.get(i);

                showMessageOKCancel(message,
                        new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                requestPermissions(permissionsList.toArray(new String[permissionsList.size()]),
                                        REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS);
                            }
                        });
                return;
            }
            requestPermissions(permissionsList.toArray(new String[permissionsList.size()]),
                    REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS);
            return;
        }

        Toast.makeText(MainActivity.this, "No new Permission Required- Launching App .You are Awesome!!", Toast.LENGTH_SHORT)
                .show();
    }

    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(MainActivity.this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }

    @TargetApi(Build.VERSION_CODES.M)
    private boolean addPermission(List<String> permissionsList, String permission) {

        if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
            permissionsList.add(permission);
            // Check for Rationale Option
            if (!shouldShowRequestPermissionRationale(permission))
                return false;
        }
        return true;
    }

    //For Android 5.0+
    public boolean onShowFileChooser(
            android.webkit.WebView webView, ValueCallback<Uri[]> filePathCallback,
            WebChromeClient.FileChooserParams fileChooserParams) {
        if (mUMA != null) {
            mUMA.onReceiveValue(null);
        }
        mUMA = filePathCallback;
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
                takePictureIntent.putExtra("PhotoPath", mCM);
            } catch (IOException ex) {
                Log.e(TAG, "Image file creation failed", ex);
            }
            if (photoFile != null) {
                mCM = "file:" + photoFile.getAbsolutePath();
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
            } else {
                takePictureIntent = null;
            }
        }
        Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
        contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
        contentSelectionIntent.setType("*/*");
        Intent[] intentArray;
        if (takePictureIntent != null) {
            intentArray = new Intent[]{takePictureIntent};
        } else {
            intentArray = new Intent[0];
        }

        Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
        chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
        chooserIntent.putExtra(Intent.EXTRA_TITLE, "Image Chooser");
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);
        startActivityForResult(chooserIntent, FCR);
        return true;
    }

    /**
     * File name from URL
     *
     * @param url
     * @return
     */
    public String getFileName(String url) {
        String filenameWithoutExtension = "";
        filenameWithoutExtension = String.valueOf(System.currentTimeMillis());
        return filenameWithoutExtension;
    }

    /**
     * WebChromeClient subclass handles UI-related calls
     * Note: think chrome as in decoration, not the Chrome browser
     */
    public class GeoWebChromeClient extends android.webkit.WebChromeClient {


        //For Android 5.0+
        public boolean onShowFileChooser(
                android.webkit.WebView webView, ValueCallback<Uri[]> filePathCallback,
                WebChromeClient.FileChooserParams fileChooserParams) {
            if (mUMA != null) {
                mUMA.onReceiveValue(null);
            }
            mUMA = filePathCallback;
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                File photoFile = null;
                try {
                    photoFile = createImageFile();
                    takePictureIntent.putExtra("PhotoPath", mCM);
                } catch (IOException ex) {
                    Log.e(TAG, "Image file creation failed", ex);
                }
                if (photoFile != null) {
                    mCM = "file:" + photoFile.getAbsolutePath();
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
                } else {
                    takePictureIntent = null;
                }
            }
            Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
            contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
            contentSelectionIntent.setType("*/*");
            Intent[] intentArray;
            if (takePictureIntent != null) {
                intentArray = new Intent[]{takePictureIntent};
            } else {
                intentArray = new Intent[0];
            }

            Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
            chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
            chooserIntent.putExtra(Intent.EXTRA_TITLE, "Image Chooser");
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);
            startActivityForResult(chooserIntent, FCR);
            return true;
        }


        @Override
        public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
            android.webkit.WebView newWebView = new android.webkit.WebView(MainActivity.this);
            newWebView.getSettings().setJavaScriptEnabled(true);
            newWebView.getSettings().setSupportZoom(false);
            newWebView.getSettings().setBuiltInZoomControls(true);
            newWebView.getSettings().setPluginState(WebSettings.PluginState.ON);
            newWebView.getSettings().setSupportMultipleWindows(true);
//            newWebView.getSettings().setBuiltInZoomControls(true);

            newWebView.getSettings().setLoadWithOverviewMode(true);
            newWebView.getSettings().setUseWideViewPort(true);

            newWebView.getSettings().setAllowFileAccess(true);
            newWebView.getSettings().setAllowContentAccess(true);

            newWebView.getSettings().setDefaultZoom(WebSettings.ZoomDensity.FAR);

            view.addView(newWebView);
            android.webkit.WebView.WebViewTransport transport = (android.webkit.WebView.WebViewTransport) resultMsg.obj;
            transport.setWebView(newWebView);
            resultMsg.sendToTarget();

            newWebView.setWebViewClient(new WebViewClient() {
                @Override
                public boolean shouldOverrideUrlLoading(android.webkit.WebView view, String url) {
                    if (url.startsWith("tel:") || url.startsWith("sms:")) {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                        return true;
                    } else {
                        view.loadUrl(url);
                        return true;
                    }
                }

                @Override
                public void onPageFinished(android.webkit.WebView view, String url) {
                    webView.scrollTo(0, 0);
                    super.onPageFinished(view, url);
                }


                // For Lollipop 5.0+ Devices
                public boolean onShowFileChooser(WebView mWebView, ValueCallback<Uri[]> filePathCallback, WebChromeClient.FileChooserParams fileChooserParams) {
                    if (uploadMessage != null) {
                        uploadMessage.onReceiveValue(null);
                        uploadMessage = null;
                    }

                    uploadMessage = filePathCallback;

                    Intent intent = null;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        intent = fileChooserParams.createIntent();
                    }
                    try {
                        startActivityForResult(intent, REQUEST_SELECT_FILE);
                    } catch (ActivityNotFoundException e) {
                        uploadMessage = null;
                        Toast.makeText(getApplicationContext(), "Cannot Open File Chooser", Toast.LENGTH_LONG).show();
                        return false;
                    }
                    return true;
                }

            });

            newWebView.setWebChromeClient(this);
            return true;
        }

        @Override
        public void onGeolocationPermissionsShowPrompt(final String origin,
                                                       final GeolocationPermissions.Callback callback) {
            // Always grant permission since the app itself requires location
            // permission and the user has therefore already granted it
            callback.invoke(origin, true, false);

            //            final boolean remember = false;
            //            AlertDialog.Builder builder = new AlertDialog.Builder(WebViewActivity.this);
            //            builder.setTitle("Locations");
            //            builder.setMessage("Would like to use your Current Location ")
            //                    .setCancelable(true).setPositiveButton("Allow", new DialogInterface.OnClickListener() {
            //                public void onClick(DialogInterface dialog, int id) {
            //                    // origin, allow, remember
            //                    callback.invoke(origin, true, remember);
            //                }
            //            }).setNegativeButton("Don't Allow", new DialogInterface.OnClickListener() {
            //                public void onClick(DialogInterface dialog, int id) {
            //                    // origin, allow, remember
            //                    callback.invoke(origin, false, remember);
            //                }
            //            });
            //            AlertDialog alert = builder.create();
            //            alert.show();
        }


        @Override
        public void onCloseWindow(android.webkit.WebView window) {
            webView.removeAllViews();
            webView.removeView(window);
            super.onCloseWindow(window);
        }

    }

    /**
     * WebViewClient subclass loads all hyperlinks in the existing WebView
     */
    public class GeoWebViewClient extends WebViewClient {
        Dialog loadingDialog = new Dialog(MainActivity.this);

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            // When user clicks a hyperlink, load in the existing WebView
            view.loadUrl(url);
            return true;
        }

        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
//            super.onReceivedSslError(view, handler, error);
//            handler.proceed();

            final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            String message = "SSL Certificate error.";
            switch (error.getPrimaryError()) {
                case SslError.SSL_UNTRUSTED:
                    message = "The certificate authority is not trusted.";
                    break;
                case SslError.SSL_EXPIRED:
                    message = "The certificate has expired.";
                    break;
                case SslError.SSL_IDMISMATCH:
                    message = "The certificate Hostname mismatch.";
                    break;
                case SslError.SSL_NOTYETVALID:
                    message = "The certificate is not yet valid.";
                    break;
            }
            message += " Do you want to continue anyway?";

            builder.setTitle("SSL Certificate Error");
            builder.setMessage(message);
            builder.setPositiveButton("continue", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        webView.getSettings()
                                .setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
                    }
                    handler.proceed();
                }
            });
            builder.setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        webView.getSettings()
                                .setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
                    }
                    handler.cancel();
                }
            });
            final AlertDialog dialog = builder.create();
            dialog.show();

        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            webViewPreviousState = PAGE_STARTED;


            if (url.startsWith("https://demo.b2biz.co.in")) {
                loadingDialog.dismiss();
            } else {

                if (loadingDialog == null || !loadingDialog.isShowing())
                    loadingDialog = ProgressDialog.show(MainActivity.this, "",
                            "Loading Please Wait", true, true,
                            new DialogInterface.OnCancelListener() {

                                @Override
                                public void onCancel(DialogInterface dialog) {
                                    // do something
                                }
                            });

                loadingDialog.setCancelable(false);
            }
        }


        @RequiresApi(api = Build.VERSION_CODES.M)
        @Override
        public void onReceivedError(WebView view, WebResourceRequest request,
                                    WebResourceError error) {


            if (isConnected()) {
                final Snackbar snackBar = Snackbar.make(rootView, "onReceivedError : " + error.getDescription(), Snackbar.LENGTH_INDEFINITE);
                snackBar.setAction("Reload", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        webView.loadUrl("javascript:window.location.reload( true )");
                    }
                });
                snackBar.show();
            } else {
                final Snackbar snackBar = Snackbar.make(rootView, "No Internet Connection ", Snackbar.LENGTH_INDEFINITE);
                snackBar.setAction("Enable Data", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        startActivityForResult(new Intent(Settings.ACTION_WIRELESS_SETTINGS), 0);
                        webView.loadUrl("javascript:window.location.reload( true )");
                        snackBar.dismiss();
                    }
                });
                snackBar.show();
            }

            super.onReceivedError(view, request, error);

        }

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void onReceivedHttpError(WebView view,
                                        WebResourceRequest request, WebResourceResponse errorResponse) {

            if (isConnected()) {
                final Snackbar snackBar = Snackbar.make(rootView, "HttpError : " + errorResponse.getReasonPhrase(), Snackbar.LENGTH_INDEFINITE);

                snackBar.setAction("Reload", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        webView.loadUrl("javascript:window.location.reload( true )");
                    }
                });
                snackBar.show();
            } else {
                final Snackbar snackBar = Snackbar.make(rootView, "No Internet Connection ", Snackbar.LENGTH_INDEFINITE);
                snackBar.setAction("Enable Data", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        startActivityForResult(new Intent(Settings.ACTION_WIRELESS_SETTINGS), 0);
                        webView.loadUrl("javascript:window.location.reload( true )");
                        snackBar.dismiss();
                    }
                });
                snackBar.show();
            }
            super.onReceivedHttpError(view, request, errorResponse);
        }

        @Override
        public void onPageFinished(WebView view, String url) {

            if (url.contains("#") && flag == false) {
                webView.loadUrl(url);
                flag = true;
            } else {
                flag = false;
            }

            if (webViewPreviousState == PAGE_STARTED) {

                if (null != loadingDialog) {
                    loadingDialog.dismiss();
                    loadingDialog = null;
                }

            }
        }

    }

    private class HelloWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            // This line right here is what you're missing.
            // Use the url provided in the method.  It will match the member URL!
            view.loadUrl(url);
            return true;
        }

        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
//            super.onReceivedSslError(view, handler, error);
//            handler.proceed();

            final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            String message = "SSL Certificate error.";
            switch (error.getPrimaryError()) {
                case SslError.SSL_UNTRUSTED:
                    message = "The certificate authority is not trusted.";
                    break;
                case SslError.SSL_EXPIRED:
                    message = "The certificate has expired.";
                    break;
                case SslError.SSL_IDMISMATCH:
                    message = "The certificate Hostname mismatch.";
                    break;
                case SslError.SSL_NOTYETVALID:
                    message = "The certificate is not yet valid.";
                    break;
            }
            message += " Do you want to continue anyway?";

            builder.setTitle("SSL Certificate Error");
            builder.setMessage(message);
            builder.setPositiveButton("continue", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        webView.getSettings()
                                .setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
                    }
                    handler.proceed();
                }
            });
            builder.setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        webView.getSettings()
                                .setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
                    }
                    handler.cancel();
                }
            });
            final AlertDialog dialog = builder.create();
            dialog.show();

        }
    }

}