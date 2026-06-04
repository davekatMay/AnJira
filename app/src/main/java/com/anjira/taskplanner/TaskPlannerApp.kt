package com.anjira.taskplanner

import android.app.Application
import com.anjira.taskplanner.data.local.DataStoreManager
import com.anjira.taskplanner.service.SyncWorker
import com.google.firebase.FirebaseApp

class TaskPlannerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        SyncWorker.enqueue(this)
    }
}
