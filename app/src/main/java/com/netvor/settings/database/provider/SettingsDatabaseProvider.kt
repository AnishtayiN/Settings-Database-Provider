package com.netvor.settings.database.provider

import android.Manifest
import android.content.ContentProvider
import android.content.ContentValues
import android.content.pm.PackageManager
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import androidx.core.content.ContextCompat

// فقط همین authority مجاز است؛ هر URI دیگری که از بیرون بیاید رد می‌شود
private const val SETTINGS_URI_AUTHORITY = "settings"
private const val WRITE_SECURE_PERMISSION_PATH = "/writeSecurePermission"

class SettingsDatabaseProvider : ContentProvider() {

    // این کلیدها از راه پل قابل تغییر نیستند؛ زنجیرهٔ سوءاستفادهٔ شناخته‌شده دارند
    // (CVE-2024-31317: تزریق به پروتکل Zygote از طریق hidden_api_blacklist_exemptions)
    private val DENIED_KEYS = setOf(
        "hidden_api_blacklist_exemptions",
        "adb_enabled", "adb_wifi_enabled",
        "development_settings_enabled", "verifier_verify_adb_installs",
        "package_verifier_enable", "upload_apk_enable",
        "device_provisioned"
    )

    // کلید ممنوع را هم از URI و هم از selection/args می‌گیرد
    private fun forbidden(uri: Uri, selection: String?, args: Array<String>?): Boolean {
        val fromUri = uri.lastPathSegment ?: ""
        val fromSel = selection?.substringAfter("name=")?.trim('\', ", ' ')
            ?: args?.firstOrNull() ?: ""
        return fromUri in DENIED_KEYS || fromSel in DENIED_KEYS
    }

    // URI ورودی را به settings://... بازمی‌سازیم؛ فقط scheme/path معتبر پاس می‌شود
    private fun target(uri: Uri): Uri = uri.buildUpon().authority(SETTINGS_URI_AUTHORITY).build()

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int {
        if (forbidden(uri, selection, selectionArgs)) throw SecurityException("کلید محافظت‌شده")
        return try { context?.contentResolver?.delete(target(uri), selection, selectionArgs) ?: 0 }
        catch (se: SecurityException) { 0 }
    }

    override fun getType(uri: Uri): String? {
        if (uri.path == WRITE_SECURE_PERMISSION_PATH) return "vnd.android.cursor.item/vnd.netvor.writeSecurePermission"
        return try { context?.contentResolver?.getType(target(uri)) } catch (se: SecurityException) { null }
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        if (forbidden(uri, null, null)) throw SecurityException("کلید محافظت‌شده")
        return try { context?.contentResolver?.insert(target(uri), values) }
        catch (se: SecurityException) { null }
    }

    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri, projection: Array<String>?, selection: String?,
        selectionArgs: Array<String>?, sortOrder: String?
    ): Cursor? {
        if (uri.path == WRITE_SECURE_PERMISSION_PATH) {
            // ستون granted فقط از روی وضعیت واقعی پرمیشن پر می‌شود
            val granted = try { checkWriteSecureSettingsPermission() } catch (se: Exception) { false }
            return MatrixCursor(arrayOf("granted")).apply { addRow(arrayOf(if (granted) 1 else 0)) }
        }
        if (forbidden(uri, selection, selectionArgs)) throw SecurityException("کلید محافظت‌شده")
        return try { context?.contentResolver?.query(target(uri), projection, selection, selectionArgs, sortOrder) }
        catch (se: SecurityException) { null }
    }

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<String>?): Int {
        if (forbidden(uri, selection, selectionArgs)) throw SecurityException("کلید محافظت‌شده")
        return try { context?.contentResolver?.update(target(uri), values, selection, selectionArgs) ?: 0 }
        catch (se: SecurityException) { 0 }
    }

    private fun checkWriteSecureSettingsPermission(): Boolean {
        val c = context ?: return false
        return ContextCompat.checkSelfPermission(c, Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED
    }
}
