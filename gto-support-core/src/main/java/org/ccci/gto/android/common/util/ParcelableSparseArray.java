package org.ccci.gto.android.common.util;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.os.ParcelableCompat;
import android.support.v4.os.ParcelableCompatCreatorCallbacks;
import android.util.SparseArray;

public class ParcelableSparseArray<T extends Parcelable> extends SparseArray<T> implements Parcelable {
    public ParcelableSparseArray() {
        super();
    }

    ParcelableSparseArray(@NonNull final Parcel in, @Nullable ClassLoader loader) {
        if (loader == null) {
            loader = getClass().getClassLoader();
        }

        final int size = in.readInt();
        final int[] keys = new int[size];
        in.readIntArray(keys);
        Parcelable[] values = in.readParcelableArray(loader);
        for (int i = 0; i < size; ++i) {
            //noinspection unchecked
            put(keys[i], (T) values[i]);
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        final int size = size();
        final int[] keys = new int[size];
        Parcelable[] values = new Parcelable[size];
        for (int i = 0; i < size; ++i) {
            keys[i] = keyAt(i);
            values[i] = valueAt(i);
        }
        parcel.writeInt(size);
        parcel.writeIntArray(keys);
        parcel.writeParcelableArray(values, flags);
    }

    public static final Parcelable.Creator<ParcelableSparseArray> CREATOR =
            ParcelableCompat.newCreator(new ParcelableCompatCreatorCallbacks<ParcelableSparseArray>() {
                @Override
                public ParcelableSparseArray createFromParcel(@NonNull final Parcel in,
                                                              @Nullable final ClassLoader loader) {
                    return new ParcelableSparseArray(in, loader);
                }

                @Override
                public ParcelableSparseArray[] newArray(int size) {
                    return new ParcelableSparseArray[size];
                }
            });
}
