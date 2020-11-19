package com.nhandz.meetclone;

import android.util.Log;

import java.net.URISyntaxException;

import io.socket.client.IO;
import io.socket.client.Socket;

public class clientConfig {
    private static String TAG= clientConfig.class.getSimpleName();
    public static String NodeServer="https://ggmeet.herokuapp.com";
    public static Socket mSocket;


}
