package com.fn2101681010.Trackr;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

public class UserDataSource {

    private SQLiteDatabase database;
    private DatabaseHelper dbHelper;

    public UserDataSource(Context context) {
        dbHelper = new DatabaseHelper(context);
    }

    public void open() {
        database = dbHelper.getWritableDatabase();
    }

    public void close() {
        dbHelper.close();
    }

    public long addUser(String username, String email, String password, double longitude, double latitude) {
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_USERNAME, username);
        values.put(DatabaseHelper.COLUMN_EMAIL, email);
        values.put(DatabaseHelper.COLUMN_PASSWORD, password);
        values.put(DatabaseHelper.COLUMN_LONGITUDE, longitude);
        values.put(DatabaseHelper.COLUMN_LATITUDE, latitude);

        return database.insert(DatabaseHelper.TABLE_USERS, null, values);
    }

    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        Cursor cursor = database.query(DatabaseHelper.TABLE_USERS, null, null, null, null, null, null);
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            User user = cursorToUser(cursor);
            users.add(user);
            cursor.moveToNext();
        }
        cursor.close();
        return users;
    }

    private User cursorToUser(Cursor cursor) {
        User user = new User();
        if (cursor != null) {
            int idIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_ID);
            int usernameIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_USERNAME);
            int emailIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_EMAIL);
            int passwordIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_PASSWORD);
            int longitudeIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_LONGITUDE);
            int latitudeIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_LATITUDE);

            if (idIndex >= 0) {
                user.setId(cursor.getLong(idIndex));
            }
            if (usernameIndex >= 0) {
                user.setUsername(cursor.getString(usernameIndex));
            }
            if (emailIndex >= 0) {
                user.setEmail(cursor.getString(emailIndex));
            }
            if (passwordIndex >= 0) {
                user.setPassword(cursor.getString(passwordIndex));
            }
            if (longitudeIndex >= 0) {
                user.setLongitude(cursor.getDouble(longitudeIndex));
            }
            if (latitudeIndex >= 0) {
                user.setLatitude(cursor.getDouble(latitudeIndex));
            }
        }
        return user;
    }
    public User getUserByEmail(String email) {
        SQLiteDatabase database = dbHelper.getReadableDatabase();
        User user = null;

        Cursor cursor = database.query(DatabaseHelper.TABLE_USERS,
                null,
                DatabaseHelper.COLUMN_EMAIL + " = ?",
                new String[]{email},
                null,
                null,
                null);

        if (cursor != null && cursor.moveToFirst()) {
            user = cursorToUser(cursor);
            cursor.close();
        }

        return user;
    }
}

