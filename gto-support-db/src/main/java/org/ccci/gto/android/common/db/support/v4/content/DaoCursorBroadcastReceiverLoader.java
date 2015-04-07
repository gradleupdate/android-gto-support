package org.ccci.gto.android.common.db.support.v4.content;

import static org.ccci.gto.android.common.db.AbstractDao.ARG_JOINS;
import static org.ccci.gto.android.common.db.AbstractDao.ARG_ORDER_BY;
import static org.ccci.gto.android.common.db.AbstractDao.ARG_PROJECTION;
import static org.ccci.gto.android.common.db.AbstractDao.ARG_WHERE;
import static org.ccci.gto.android.common.db.AbstractDao.ARG_WHERE_ARGS;
import static org.ccci.gto.android.common.db.AbstractDao.bindValues;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Pair;

import org.ccci.gto.android.common.db.AbstractDao;
import org.ccci.gto.android.common.db.Join;
import org.ccci.gto.android.common.support.v4.content.CursorBroadcastReceiverLoader;
import org.ccci.gto.android.common.util.BundleUtils;

public abstract class DaoCursorBroadcastReceiverLoader<T> extends CursorBroadcastReceiverLoader {
    private static final Pair<String, String[]> NO_WHERE = Pair.create(null, new String[0]);

    @NonNull
    private final AbstractDao mDao;

    @NonNull
    private final Class<T> mType;
    @NonNull
    @SuppressWarnings("unchecked")
    private Join<T, ?>[] mJoins = Join.NO_JOINS;
    @NonNull
    private Pair<String, String[]> mWhere = NO_WHERE;

    @SuppressWarnings("unchecked")
    protected DaoCursorBroadcastReceiverLoader(@NonNull final Context context, @NonNull final AbstractDao dao,
                                               @NonNull final Class<T> type, @Nullable final Bundle args) {
        super(context);
        mDao = dao;

        mType = type;
        setJoins(args != null ? BundleUtils.getParcelableArray(args, ARG_JOINS, Join.class) : null);
        setProjection(args != null ? args.getStringArray(ARG_PROJECTION) : null);
        setWhere(args != null ? args.getString(ARG_WHERE) : null,
                 args != null ? args.getStringArray(ARG_WHERE_ARGS) : null);
        setSortOrder(args != null ? args.getString(ARG_ORDER_BY) : null);
    }

    @Nullable
    @Override
    protected final Cursor getCursor() {
        final Pair<String, String[]> where = getWhere();
        return mDao.getCursor(mType, getJoins(), getProjection(), where.first, where.second,
                              getSortOrder());
    }

    @SuppressWarnings("unchecked")
    public void setJoins(@Nullable final Join<T, ?>[] joins) {
        mJoins = joins != null ? joins : Join.NO_JOINS;
    }

    @NonNull
    public Join<T, ?>[] getJoins() {
        return mJoins;
    }

    @Override
    public void setProjection(@Nullable final String[] projection) {
        super.setProjection(projection != null ? projection : mDao.getFullProjection(mType));
    }

    @NonNull
    public String[] getProjection() {
        final String[] projection = super.getProjection();
        return projection != null ? projection : mDao.getFullProjection(mType);
    }

    public void setWhere(@Nullable final String where, @Nullable final Object... args) {
        setWhere(where, args != null ? bindValues(args) : null);
    }

    public void setWhere(@Nullable final String where, @Nullable final String... args) {
        mWhere = Pair.create(where, args);
    }

    @NonNull
    public Pair<String, String[]> getWhere() {
        return mWhere;
    }
}