package org.ccci.gto.android.common.api.okhttp3.interceptor;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.ccci.gto.android.common.api.Session;
import org.ccci.gto.android.common.api.okhttp3.InvalidSessionApiException;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public abstract class SessionInterceptor<S extends Session> implements Interceptor {
    protected final Object LOCK_SESSION = new Object();

    @NonNull
    protected final Context mContext;
    @NonNull
    private final String mPrefFile;

    protected SessionInterceptor(@NonNull final Context context) {
        this(context, null);
    }

    protected SessionInterceptor(@NonNull final Context context, @Nullable final String prefFile) {
        mContext = context.getApplicationContext();
        mPrefFile = prefFile != null ? prefFile : getClass().getSimpleName();
    }

    @NonNull
    protected final SharedPreferences getPrefs() {
        return mContext.getSharedPreferences(mPrefFile, Context.MODE_PRIVATE);
    }

    @Override
    public final Response intercept(@NonNull final Chain chain) throws IOException {
        // get the session, establish a session if one doesn't exist or if we have a stale session
        S session;
        synchronized (LOCK_SESSION) {
            session = loadSession();
            if (session == null) {
                session = establishSession();

                // save the newly established session
                if (session != null && session.isValid()) {
                    saveSession(session);
                }
            }
        }

        // throw an exception if we don't have a valid session
        if (session == null) {
            throw new InvalidSessionApiException();
        }

        // process request
        final Response response = chain.proceed(attachSession(chain.request(), session));

        // make sure the response is valid
        if (isSessionInvalid(response)) {
            // reset the session
            synchronized (LOCK_SESSION) {
                // only reset if this is still the same session
                final S active = loadSession();
                if (active != null && active.equals(session)) {
                    deleteSession(session);
                }
            }
        }

        return response;
    }

    @NonNull
    protected abstract Request attachSession(@NonNull Request request, @NonNull S session);

    protected boolean isSessionInvalid(@NonNull final Response response) throws IOException {
        return false;
    }

    @Nullable
    private S loadSession() {
        // load a pre-existing session
        final SharedPreferences prefs = this.getPrefs();
        final S session;
        synchronized (LOCK_SESSION) {
            session = loadSession(prefs);
        }

        // only return valid sessions
        return session != null && session.isValid() ? session : null;
    }

    @Nullable
    protected abstract S loadSession(@NonNull SharedPreferences prefs);

    @Nullable
    protected S establishSession() throws IOException {
        return null;
    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    private void saveSession(@NonNull final S session) {
        final SharedPreferences.Editor prefs = this.getPrefs().edit();
        session.save(prefs);

        synchronized (LOCK_SESSION) {
            prefs.apply();
        }
    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    private void deleteSession(@NonNull final S session) {
        final SharedPreferences.Editor prefs = getPrefs().edit();
        session.delete(prefs);

        synchronized (LOCK_SESSION) {
            prefs.apply();
        }
    }
}
