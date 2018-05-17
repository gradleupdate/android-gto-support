package org.ccci.gto.android.common.util;

import android.annotation.TargetApi;
import android.icu.util.ULocale;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.text.TextUtils;

import org.ccci.gto.android.common.compat.util.LocaleCompat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IllformedLocaleException;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;

public class LocaleUtils {
    // define a few fixed fallbacks
    static final Map<String, String> FALLBACKS = new HashMap<>();
    static {
        FALLBACKS.put("pse", "ms");
    }

    private static final Compat COMPAT;
    static {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            COMPAT = new FroyoCompat();
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            COMPAT = new LollipopCompat();
        } else {
            COMPAT = new NougatCompat();
        }
    }

    @Nullable
    public static Locale getFallback(@NonNull final Locale locale) {
        return COMPAT.getFallback(locale);
    }

    @NonNull
    public static Locale[] getFallbacks(@NonNull final Locale locale) {
        return COMPAT.getFallbacks(locale);
    }

    @NonNull
    public static Locale[] getFallbacks(@NonNull final Locale... locales) {
        final LinkedHashSet<Locale> outputs = new LinkedHashSet<>();

        // generate fallbacks for all provided locales
        for (final Locale locale : locales) {
            Collections.addAll(outputs, getFallbacks(locale));
        }

        return outputs.toArray(new Locale[outputs.size()]);
    }

    @RestrictTo({RestrictTo.Scope.LIBRARY})
    interface Compat {
        @Nullable
        Locale getFallback(@NonNull Locale locale);

        @NonNull
        Locale[] getFallbacks(@NonNull Locale locale);
    }

    @RestrictTo({RestrictTo.Scope.LIBRARY})
    static class FroyoCompat implements Compat {
        @Nullable
        private String getFallback(@NonNull final String locale) {
            // try splitting on "-"
            int c = locale.lastIndexOf('-');
            if (c >= 0) {
                return locale.substring(0, c);
            }

            // try fixed fallbacks
            return FALLBACKS.get(locale);
        }

        @Nullable
        @Override
        public Locale getFallback(@NonNull final Locale locale) {
            final String fallback = getFallback(LocaleCompat.toLanguageTag(locale));
            return fallback != null ? LocaleCompat.forLanguageTag(fallback) : null;
        }

        @NonNull
        @Override
        public Locale[] getFallbacks(@NonNull final Locale locale) {
            // add initial locale
            final LinkedHashSet<Locale> locales = new LinkedHashSet<>();
            locales.add(locale);

            // generate all fallback variants
            for (String raw = getFallback(LocaleCompat.toLanguageTag(locale)); raw != null; raw = getFallback(raw)) {
                locales.add(LocaleCompat.forLanguageTag(raw));
            }

            // return the locales as an array
            return locales.toArray(new Locale[locales.size()]);
        }
    }

    @RestrictTo({RestrictTo.Scope.LIBRARY})
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    static class LollipopCompat extends FroyoCompat {
        @Nullable
        @Override
        public Locale getFallback(@NonNull final Locale locale) {
            return getFallback(locale, toLocaleBuilder(locale));
        }

        @Nullable
        public Locale getFallback(@NonNull final Locale locale, @NonNull final Locale.Builder builder) {
            if (!TextUtils.isEmpty(locale.getVariant())) {
                return builder.setVariant(null).build();
            }
            if (!TextUtils.isEmpty(locale.getCountry())) {
                return builder.setRegion(null).build();
            }
            if (!TextUtils.isEmpty(locale.getScript())) {
                return builder.setScript(null).build();
            }

            return super.getFallback(locale);
        }

        @NonNull
        @Override
        public Locale[] getFallbacks(@NonNull final Locale locale) {
            // add initial locale
            final LinkedHashSet<Locale> locales = new LinkedHashSet<>();
            locales.add(locale);

            // generate all fallback variants
            final Locale.Builder builder = toLocaleBuilder(locale);
            for (Locale fallback = locale; fallback != null; fallback = getFallback(fallback, builder)) {
                locales.add(fallback);
            }

            // return the locales as an array
            return locales.toArray(new Locale[locales.size()]);
        }

        private Locale.Builder toLocaleBuilder(@NonNull final Locale locale) {
            // populate builder from provided locale
            final Locale.Builder builder = new Locale.Builder();
            try {
                builder.setLocale(locale).clearExtensions();
            } catch (final IllformedLocaleException e) {
                /* HACK: There appears to be a bug on Huawei devices running Android 5.0-5.1.1 using Arabic locales.
                         Setting the locale on the Locale Builder throws an IllformedLocaleException for "Invalid
                         variant: LNum". To workaround this bug we manually set the locale components on the builder,
                         skipping any invalid components
                   see: https://gist.github.com/frett/034b8eba09cf815cbcd60f83b3f52eb4
                */
                builder.clear();
                try {
                    builder.setLanguage(locale.getLanguage());
                } catch (final IllformedLocaleException ignored) {
                }
                try {
                    builder.setScript(locale.getScript());
                } catch (final IllformedLocaleException ignored) {
                }
                try {
                    builder.setRegion(locale.getCountry());
                } catch (final IllformedLocaleException ignored) {
                }
                try {
                    builder.setVariant(locale.getVariant());
                } catch (final IllformedLocaleException ignored) {
                }
            }

            return builder;
        }
    }

    @RestrictTo({RestrictTo.Scope.LIBRARY})
    @TargetApi(Build.VERSION_CODES.N)
    static final class NougatCompat extends LollipopCompat {
        @Nullable
        @Override
        public Locale getFallback(@NonNull final Locale locale) {
            final ULocale fallback = getFallback(ULocale.forLocale(locale));
            return fallback != null ? fallback.toLocale() : null;
        }

        @NonNull
        @Override
        public Locale[] getFallbacks(@NonNull final Locale rawLocale) {
            final ArrayList<Locale> locales = new ArrayList<>();

            // handle fallback behavior
            for (ULocale locale = ULocale.forLocale(rawLocale); locale != null && !locale.equals(ULocale.ROOT);
                 locale = getFallback(locale)) {
                locales.add(locale.toLocale());
            }

            return locales.toArray(new Locale[0]);
        }

        @Nullable
        private ULocale getFallback(@NonNull final ULocale locale) {
            ULocale fallback = locale.getFallback();
            if (fallback != null && !ULocale.ROOT.equals(fallback)) {
                return fallback;
            }

            // try a fixed fallback
            final String fixed = FALLBACKS.get(locale.toLanguageTag());
            return fixed != null ? ULocale.forLanguageTag(fixed) : null;
        }
    }
}
