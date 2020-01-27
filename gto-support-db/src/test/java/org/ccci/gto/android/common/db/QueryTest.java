package org.ccci.gto.android.common.db;

import android.text.TextUtils;
import android.util.Pair;

import org.ccci.gto.android.common.db.Contract.RootTable;
import org.ccci.gto.android.common.db.model.Root;
import org.ccci.gto.android.common.testing.CommonMocks;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AnyOf.anyOf;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Pair.class, TextUtils.class})
public class QueryTest {
    private AbstractDao mDao;

    @Before
    public void setup() throws Exception {
        CommonMocks.mockPair();
        CommonMocks.mockTextUtils();

        mDao = mock(AbstractDao.class);
        when(mDao.getTable(eq(Root.class))).thenReturn(RootTable.TABLE_NAME);
    }

    @Test
    public void testHavingSql() {
        Expression having = RootTable.FIELD_TEST.count().eq(1);
        Query<RootTable> query = Query.select(RootTable.class).groupBy(RootTable.FIELD_TEST).having(having);
        Pair<String, String[]> sqlPair = query.buildSqlHaving(mDao);
        assertThat(sqlPair.first, is("(COUNT (root.test) == 1)"));
    }

    @Test
    public void verifyLimitSql() {
        final Query<RootTable> query = Query.select(RootTable.class);
        assertThat(query.limit(null).buildSqlLimit(), nullValue());
        assertThat(query.limit(null).offset(10).buildSqlLimit(), nullValue());
        assertThat(query.limit(5).offset(null).buildSqlLimit(), is("5"));
        assertThat(query.limit(5).offset(15).buildSqlLimit(), anyOf(is("5 OFFSET 15"), is("15, 5")));
    }
}
