package id.ketik.keyboard;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

/**
 * Hands this app's data, as one zip, to the new Ketik app so it can import it in one tap.
 * Only the new app may read it, and only if it is signed with the same key as this app.
 */
public final class ExportProvider extends ContentProvider {

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        if (!"r".equals(mode)) throw new FileNotFoundException("read only");
        checkCaller();
        File f = new File(getContext().getCacheDir(), "export-" + System.nanoTime() + ".zip");
        try (FileOutputStream out = new FileOutputStream(f)) {
            DataTransfer.export(getContext(), out);
        } catch (Exception e) {
            //noinspection ResultOfMethodCallIgnored
            f.delete();
            throw new FileNotFoundException("export failed: " + e.getMessage());
        }
        ParcelFileDescriptor pfd = ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_ONLY);
        //noinspection ResultOfMethodCallIgnored
        f.delete(); // the open descriptor keeps the data readable; nothing is left behind
        return pfd;
    }

    private void checkCaller() {
        String caller = getCallingPackage();
        PackageManager pm = getContext().getPackageManager();
        if (!DataTransfer.NEW_PACKAGE.equals(caller)
                || pm.checkSignatures(getContext().getPackageName(), caller) != PackageManager.SIGNATURE_MATCH) {
            throw new SecurityException("Only the new Ketik app signed with the same key can read this");
        }
    }

    @Override
    public String getType(Uri uri) {
        return "application/zip";
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }
}
