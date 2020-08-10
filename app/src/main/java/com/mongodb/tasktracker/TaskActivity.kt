package com.mongodb.tasktracker

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.mongodb.tasktracker.model.Task
import com.mongodb.tasktracker.model.TaskAdapter
import io.realm.Realm
import io.realm.RealmResults
import io.realm.kotlin.where
import io.realm.mongodb.User
import io.realm.mongodb.sync.SyncConfiguration


class TaskActivity : AppCompatActivity() {
    private lateinit var realm: Realm
    private var user: User? = null
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TaskAdapter
    private lateinit var fab: FloatingActionButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task)

        //Realm.init(baseContext)
        realm = Realm.getDefaultInstance()
        recyclerView = findViewById(R.id.task_list)
        fab = findViewById(R.id.floating_action_button)

        fab.setOnClickListener {
            val input = EditText(this)
            val dialogBuilder = AlertDialog.Builder(this)
            dialogBuilder.setMessage("Enter task name:")
                .setCancelable(true)
                .setPositiveButton("Create") { dialog, _ -> run {
                    dialog.dismiss()
                    try {
                        val task = Task(input.text.toString())
                        realm.executeTransactionAsync { realm ->
                            realm.insert(task)
                        }
                    }
                    catch(exception: Exception){
                        Toast.makeText(baseContext, exception.message, Toast.LENGTH_SHORT).show()
                    }
                    finally{
                        val dogs: RealmResults<Task> = realm.where(Task::class.java).findAll()
                        for(n in 0..dogs.size - 1){
                            Toast.makeText(baseContext, dogs[n]?.name, Toast.LENGTH_LONG).show()
                        }
                    }
                }
                }
                .setNegativeButton("Cancel") { dialog, _ -> dialog.cancel()
                }

            val dialog = dialogBuilder.create()
            dialog.setView(input)
            dialog.setTitle("Create New Task")
            dialog.show()
        }
    }

    override fun onStart() {
        super.onStart()
        try {
            user = taskApp.currentUser()
        } catch (e: IllegalStateException) {
            Log.w(TAG(), e)
        }
        if (user == null) {
            // if no user is currently logged in, start the login activity so the user can authenticate
            startActivity(Intent(this, LoginActivity::class.java))
        }
        else {
            // configure realm to use the current user and the partition corresponding to "My Project"
            val config = SyncConfiguration.Builder(user!!, "My Project")
                .waitForInitialRemoteData()
                .build()

            // save this configuration as the default for this entire app so other activities and threads can open their own realm instances
            Realm.setDefaultConfiguration(config)
            try {
                realm = Realm.getInstance(config)
                val dogs: RealmResults<Task> = realm.where(Task::class.java).findAll()
                for (n in 0..dogs.size - 1) {
                    Toast.makeText(baseContext, dogs[n]?.name, Toast.LENGTH_LONG).show()
                }

                //realm = Realm.getInstance(config)

            // Sync all realm changes via a new instance, and when that instance has been successfully created connect it to an on-screen list (a recycler view)
            Realm.getInstanceAsync(config, object: Realm.Callback() {
                override fun onSuccess(realm: Realm) {
                    // since this realm should live exactly as long as this activity, assign the realm to a member variable
                    this@TaskActivity.realm = realm
                    setUpRecyclerView(realm)
                }
            })
            }
            catch(e: Exception){
                Log.v(TAG(), e.message)
            }
        }
        /*super.onStart()
        try {
            user = taskApp.currentUser()
        } catch (e: IllegalStateException) {
            Log.w(TAG(), e)
        }
        if (user == null) {
            startActivity(Intent(this, LoginActivity::class.java))
        }*/

        /*val dogs: RealmResults<Task> = realm.where(Task::class.java).findAll()
        for(n in 0..dogs.size - 1){
            Toast.makeText(baseContext, dogs[n]?.name, Toast.LENGTH_LONG).show()
        }*/
    }

    private fun setUpRecyclerView(realm: Realm) {
        adapter = TaskAdapter(realm.where<Task>().sort("_id").findAll())
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
        recyclerView.setHasFixedSize(true)
        recyclerView.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
    }

    override fun onDestroy() {
        super.onDestroy()
        recyclerView.adapter = null
        realm.close()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.activity_task_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                user?.logOutAsync {
                    if (it.isSuccess) {
                        realm.close()
                        user = null
                        Log.v(TAG(), "user logged out")
                        startActivity(Intent(this, LoginActivity::class.java))
                    } else {
                        Log.e(TAG(), "log out failed! Error: ${it.error}")
                    }
                }
                true
            }
            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }
}