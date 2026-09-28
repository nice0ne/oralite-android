package com.msam.oralite

import com.msam.oralite.data.engine.SqlHelper
import com.msam.oralite.ui.screen.SqlFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SqlHelperTest {

    @Test
    fun testInjectSafetyLimit_addsRownum() {
        val sql = "SELECT emp_id, emp_name FROM employees"
        val result = SqlHelper.injectSafetyLimit(sql, 100)
        assertTrue(result.contains("ROWNUM <= 100"))
        assertTrue(result.startsWith("SELECT * FROM (SELECT emp_id, emp_name FROM employees)"))
    }

    @Test
    fun testInjectSafetyLimit_preservesExistingRownum() {
        val sql = "SELECT * FROM orders WHERE ROWNUM <= 10"
        val result = SqlHelper.injectSafetyLimit(sql, 100)
        assertEquals(sql, result)
    }

    @Test
    fun testInjectSafetyLimit_preservesFetchFirst() {
        val sql = "SELECT * FROM orders FETCH FIRST 20 ROWS ONLY"
        val result = SqlHelper.injectSafetyLimit(sql, 100)
        assertEquals(sql, result)
    }

    @Test
    fun testInjectSafetyLimit_ignoresUpdate() {
        val sql = "UPDATE employees SET salary = 1000 WHERE emp_id = 1"
        val result = SqlHelper.injectSafetyLimit(sql, 100)
        assertEquals(sql, result)
    }

    @Test
    fun testSqlFormatter_formatsClauses() {
        val raw = "SELECT id, name FROM users WHERE id = 1 ORDER BY name ASC"
        val formatted = SqlFormatter.format(raw)
        assertTrue(formatted.contains("SELECT id, name"))
        assertTrue(formatted.contains("FROM users"))
        assertTrue(formatted.contains("WHERE id = 1"))
        assertTrue(formatted.contains("ORDER BY name ASC"))
    }
}
