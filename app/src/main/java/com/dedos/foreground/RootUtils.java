package com.dedos.foreground;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;

public class RootUtils {


    public static boolean executeRootCommand(String command) {
        try {
            // Ejecutar comando su para ejecutar el comando root
            Process process = Runtime.getRuntime().exec(new String[]{"su", "-c", command});
            process.waitFor();
            return true;
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }
}
