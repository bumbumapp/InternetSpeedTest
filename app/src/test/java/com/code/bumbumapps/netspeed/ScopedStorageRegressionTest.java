package com.code.bumbumapps.netspeed;

import static org.junit.Assert.assertFalse;

import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Regression guard: sharing a result wrote to the public Downloads folder (fails under scoped
 * storage) and asked for storage/READ_MEDIA_IMAGES permissions, closing the app when denied.
 */
public class ScopedStorageRegressionTest {
    @Test
    public void shareDoesNotUsePublicStorage() throws IOException {
        String code = new String(Files.readAllBytes(
                Paths.get("src/main/java/com/code/bumbumapps/netspeed/MainActivity.java")));
        assertFalse(code.contains("getExternalStoragePublicDirectory"));
        assertFalse(code.contains("TedPermission"));
    }

    @Test
    public void manifestHasNoStoragePermissions() throws IOException {
        String manifest = new String(Files.readAllBytes(Paths.get("src/main/AndroidManifest.xml")));
        assertFalse(manifest.contains("WRITE_EXTERNAL_STORAGE"));
        assertFalse(manifest.contains("READ_MEDIA_IMAGES"));
    }
}
