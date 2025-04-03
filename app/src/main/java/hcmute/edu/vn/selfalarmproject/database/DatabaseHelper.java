package hcmute.edu.vn.selfalarmproject.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import hcmute.edu.vn.selfalarmproject.model.CallLogData;
import hcmute.edu.vn.selfalarmproject.model.SMSData;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String TAG = "DatabaseHelper";

    // Database Info
    private static final String DATABASE_NAME = "MessagesCallsDatabase";
    private static final int DATABASE_VERSION = 1;

    // Table Names
    private static final String TABLE_SMS = "sms_messages";
    private static final String TABLE_CALLS = "call_logs";

    // SMS Table Columns
    private static final String KEY_SMS_ID = "id";
    private static final String KEY_SMS_SENDER = "sender";
    private static final String KEY_SMS_RECIPIENT = "recipient";
    private static final String KEY_SMS_MESSAGE = "message";
    private static final String KEY_SMS_DATE = "date";
    private static final String KEY_SMS_TYPE = "type";

    // Call Table Columns
    private static final String KEY_CALL_ID = "id";
    private static final String KEY_CALL_NUMBER = "phone_number";
    private static final String KEY_CALL_DATE = "date";
    private static final String KEY_CALL_TYPE = "type";

    // Singleton instance
    private static DatabaseHelper sInstance;

    // Get singleton instance
    public static synchronized DatabaseHelper getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new DatabaseHelper(context.getApplicationContext());
        }
        return sInstance;
    }

    private DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_SMS_TABLE = "CREATE TABLE " + TABLE_SMS +
                "(" +
                KEY_SMS_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                KEY_SMS_SENDER + " TEXT," +
                KEY_SMS_RECIPIENT + " TEXT," +
                KEY_SMS_MESSAGE + " TEXT," +
                KEY_SMS_DATE + " INTEGER," +
                KEY_SMS_TYPE + " INTEGER" +
                ")";

        String CREATE_CALLS_TABLE = "CREATE TABLE " + TABLE_CALLS +
                "(" +
                KEY_CALL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                KEY_CALL_NUMBER + " TEXT," +
                KEY_CALL_DATE + " INTEGER," +
                KEY_CALL_TYPE + " TEXT" +
                ")";

        db.execSQL(CREATE_SMS_TABLE);
        db.execSQL(CREATE_CALLS_TABLE);

        Log.d(TAG, "Database tables created");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion != newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_SMS);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_CALLS);
            onCreate(db);
        }
    }

    // --- SMS Methods ---

    // Add SMS message to database
    public long addSMS(String sender, String recipient, String message, long date, int type) {
        SQLiteDatabase db = getWritableDatabase();
        long id = -1;

        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put(KEY_SMS_SENDER, sender);
            values.put(KEY_SMS_RECIPIENT, recipient != null ? recipient : "");
            values.put(KEY_SMS_MESSAGE, message);
            values.put(KEY_SMS_DATE, date);
            values.put(KEY_SMS_TYPE, type);

            id = db.insertOrThrow(TABLE_SMS, null, values);
            db.setTransactionSuccessful();
            Log.d(TAG, "SMS saved to database with ID: " + id);
        } catch (Exception e) {
            Log.e(TAG, "Error saving SMS to database: " + e.getMessage());
        } finally {
            db.endTransaction();
        }

        return id;
    }

    // Get all SMS messages from database
    public List<SMSData> getAllSMS() {
        List<SMSData> smsList = new ArrayList<>();

        String QUERY = "SELECT * FROM " + TABLE_SMS + " ORDER BY " + KEY_SMS_DATE + " DESC";

        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(QUERY, null);

        try {
            if (cursor.moveToFirst()) {
                do {
                    SMSData sms = new SMSData();
                    sms.setId(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_SMS_ID)));

                    // Handle sender/address based on type
                    int type = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_SMS_TYPE));
                    if (type == 1) { // Incoming
                        sms.setAddress(cursor.getString(cursor.getColumnIndexOrThrow(KEY_SMS_SENDER)));
                    } else { // Outgoing
                        sms.setAddress(cursor.getString(cursor.getColumnIndexOrThrow(KEY_SMS_RECIPIENT)));
                    }

                    sms.setBody(cursor.getString(cursor.getColumnIndexOrThrow(KEY_SMS_MESSAGE)));
                    sms.setDate(cursor.getLong(cursor.getColumnIndexOrThrow(KEY_SMS_DATE)));
                    sms.setType(type);

                    smsList.add(sms);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting SMS data: " + e.getMessage());
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }

        return smsList;
    }

    // --- Call Methods ---

    // Add call log to database
    public long addCall(String phoneNumber, long date, String type) {
        SQLiteDatabase db = getWritableDatabase();
        long id = -1;

        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put(KEY_CALL_NUMBER, phoneNumber);
            values.put(KEY_CALL_DATE, date);
            values.put(KEY_CALL_TYPE, type);

            id = db.insertOrThrow(TABLE_CALLS, null, values);
            db.setTransactionSuccessful();
            Log.d(TAG, "Call saved to SQLite database with ID: " + id);
        } catch (Exception e) {
            Log.e(TAG, "Error saving call to SQLite: " + e.getMessage());
            e.printStackTrace();
        } finally {
            db.endTransaction();
        }

        return id;
    }

    // Get all call logs from database
    public List<CallLogData> getAllCalls() {
        List<CallLogData> callList = new ArrayList<>();

        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = null;

        try {
            cursor = db.query(TABLE_CALLS, null, null, null, null, null, KEY_CALL_DATE + " DESC");

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    CallLogData call = new CallLogData();

                    int idIndex = cursor.getColumnIndex(KEY_CALL_ID);
                    int numberIndex = cursor.getColumnIndex(KEY_CALL_NUMBER);
                    int dateIndex = cursor.getColumnIndex(KEY_CALL_DATE);
                    int typeIndex = cursor.getColumnIndex(KEY_CALL_TYPE);

                    if (idIndex != -1) call.setId(cursor.getInt(idIndex));
                    if (numberIndex != -1) call.setPhoneNumber(cursor.getString(numberIndex));
                    if (dateIndex != -1) call.setDate(cursor.getLong(dateIndex));
                    if (typeIndex != -1) call.setType(cursor.getString(typeIndex));

                    callList.add(call);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting calls from SQLite: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return callList;
    }
}