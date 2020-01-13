package org.ccci.gto.android.common.db;

import android.database.sqlite.SQLiteDatabase;

import java.io.Closeable;

import androidx.annotation.NonNull;
import androidx.core.util.Pools;

/**
 * Not Thread-safe, this object is designed to be used on a single-thread only
 */
public final class Transaction implements Closeable {
    private static final int STATE_INIT = 0;
    private static final int STATE_OPEN = 1;
    private static final int STATE_SUCCESSFUL = 2;
    private static final int STATE_CLOSED = 3;
    private static final int STATE_RECYCLED = 4;

    private SQLiteDatabase mDb;
    private int mState = STATE_INIT;

    private static final Pools.Pool<Transaction> POOL = new Pools.SynchronizedPool<>(10);

    private Transaction(@NonNull final SQLiteDatabase db) {
        mDb = db;
    }

    static Transaction newTransaction(@NonNull final SQLiteDatabase db) {
        // check to see if we have a Transaction object cached in our pool
        final Transaction tx = POOL.acquire();
        if (tx != null) {
            tx.mDb = db;
            tx.mState = STATE_INIT;
            return tx;
        }

        // create a new transaction object
        return new Transaction(db);
    }

    @NonNull
    public Transaction begin() {
        return beginTransaction(true);
    }

    @NonNull
    public Transaction beginTransaction() {
        return beginTransaction(true);
    }

    /**
     * Starts a non-exclusive transaction. Gracefully falls back to an exclusive transaction on pre-Honeycomb
     *
     * @return this Transaction object
     */
    @NonNull
    public Transaction beginTransactionNonExclusive() {
        return beginTransaction(false);
    }

    @NonNull
    public Transaction beginTransaction(final boolean exclusive) {
        if (mState < STATE_OPEN) {
            if (exclusive) {
                mDb.beginTransaction();
            } else {
                mDb.beginTransactionNonExclusive();
            }
            mState = STATE_OPEN;
        }

        return this;
    }

    @NonNull
    public Transaction setSuccessful() {
        return setTransactionSuccessful();
    }

    @NonNull
    public Transaction setTransactionSuccessful() {
        if (mState >= STATE_OPEN && mState < STATE_SUCCESSFUL) {
            mDb.setTransactionSuccessful();
            mState = STATE_SUCCESSFUL;
        }

        return this;
    }

    @NonNull
    public Transaction end() {
        return endTransaction();
    }

    @NonNull
    public Transaction endTransaction() {
        if (mState >= STATE_OPEN && mState < STATE_CLOSED) {
            mDb.endTransaction();
            mState = STATE_CLOSED;
        }

        return this;
    }

    @Override
    public void close() {
        endTransaction();
    }

    public void recycle() {
        mState = STATE_RECYCLED;
        mDb = null;
        POOL.release(this);
    }
}
